#include <mpi.h>
#include <omp.h>

#include "Blockchain.hpp"
#include "Block.hpp"
#include "HttpBlockchainServer.hpp"

#include <atomic>
#include <csignal>
#include <cstdint>
#include <ctime>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <thread>
#include <algorithm>

#include "nlohmann/json.hpp"

using json = nlohmann::json;

// -------------------- MPI tags / commands --------------------
static constexpr int TAG_CMD = 50;
static constexpr int TAG_JOB = 100;
static constexpr int TAG_RESULT = 200;
static constexpr int TAG_STOP = 999;

static constexpr int CMD_JOB = 0;
static constexpr int CMD_SHUTDOWN = 1;

static HttpBlockchainServer *gServer = nullptr;

static void on_signal(int)
{
    if (gServer)
        gServer->Stop();
}

// UTC ISO8601 npr: 2026-01-04T12:34:56Z
static std::string utc_now_iso8601()
{
    std::time_t t = std::time(nullptr);
    std::tm tm{};
#if defined(_WIN32)
    gmtime_s(&tm, &t);
#else
    gmtime_r(&t, &tm);
#endif
    std::ostringstream oss;
    oss << std::put_time(&tm, "%Y-%m-%dT%H:%M:%SZ");
    return oss.str();
}

static void mpi_send_string(int dstRank, int tag, const std::string &s)
{
    int len = static_cast<int>(s.size());
    MPI_Send(&len, 1, MPI_INT, dstRank, tag, MPI_COMM_WORLD);
    if (len > 0)
        MPI_Send(s.data(), len, MPI_CHAR, dstRank, tag + 1, MPI_COMM_WORLD);
}

static std::string mpi_recv_string(int srcRank, int tag)
{
    int len = 0;
    MPI_Recv(&len, 1, MPI_INT, srcRank, tag, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    std::string s;
    s.resize(std::max(0, len));
    if (len > 0)
        MPI_Recv(s.data(), len, MPI_CHAR, srcRank, tag + 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    return s;
}

static bool try_consume_stop(int coordinatorRank = 0)
{
    int flag = 0;
    MPI_Status st;
    MPI_Iprobe(coordinatorRank, TAG_STOP, MPI_COMM_WORLD, &flag, &st);
    if (!flag)
        return false;

    int stop = 0;
    MPI_Recv(&stop, 1, MPI_INT, coordinatorRank, TAG_STOP, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    return stop != 0;
}

static bool hash_meets_difficulty(const std::string &hash, int difficulty)
{
    if (difficulty <= 0)
        return true;
    if ((int)hash.size() < difficulty)
        return false;
    for (int i = 0; i < difficulty; ++i)
        if (hash[i] != '0')
            return false;
    return true;
}

// -------------------- Worker mining (MPI + OpenMP) --------------------
static void worker_service_loop(int rank, int worldSize)
{
    const int coordinator = 0;
    const int numWorkers = worldSize - 1;
    const int workerIndex = rank - 1;

    const uint64_t chunkSize = 1000;
    const uint64_t stride = static_cast<uint64_t>(numWorkers) * chunkSize;

    while (true)
    {
        int cmd = CMD_JOB;
        MPI_Recv(&cmd, 1, MPI_INT, coordinator, TAG_CMD, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
        if (cmd == CMD_SHUTDOWN)
            return;

        std::string jobStr = mpi_recv_string(coordinator, TAG_JOB);
        json job = json::parse(jobStr);

        Block prev = job.at("prev").get<Block>();
        std::string data = job.at("data").get<std::string>();
        std::string owner = job.at("owner").get<std::string>();
        int difficulty = job.at("difficulty").get<int>();
        int threads = job.value("threads", 0);

        if (threads > 0)
            omp_set_num_threads(threads);

        Block base;
        base.Index = prev.Index + 1;
        base.PreviousHash = prev.Hash;
        base.Data = data;
        base.Difficulty = difficulty;
        base.Owner = owner;

        uint64_t batch = 0;
        bool shouldQuitJob = false;

        while (!shouldQuitJob)
        {
            if (try_consume_stop(coordinator))
                break;

            uint64_t start = static_cast<uint64_t>(workerIndex) * chunkSize + 1 + batch * stride;
            uint64_t end = start + chunkSize - 1;

            base.Timestamp = utc_now_iso8601();

            std::atomic<bool> found(false);
            uint64_t foundNonce = 0;
            std::string foundHash;

#pragma omp parallel
            {
                Block local = base;
                std::string localHash;

#pragma omp for schedule(static)
                for (long long i = 0; i < (long long)chunkSize; ++i)
                {
                    if (found.load(std::memory_order_relaxed))
                        continue;

                    if ((i % 200) == 0)
                    {
                        if (try_consume_stop(coordinator))
                        {
                            found.store(true, std::memory_order_relaxed);
                            continue;
                        }
                    }

                    uint64_t n = start + (uint64_t)i;
                    local.Nonce = n;
                    localHash = local.CalculateHash();

                    if (hash_meets_difficulty(localHash, difficulty))
                    {
                        bool expected = false;
                        if (found.compare_exchange_strong(expected, true, std::memory_order_relaxed))
                        {
                            foundNonce = n;
                            foundHash = localHash;
                        }
                    }
                }
            }

            if (found.load(std::memory_order_relaxed) && !foundHash.empty())
            {
                Block candidate = base;
                candidate.Nonce = foundNonce;
                candidate.Hash = foundHash;

                json msg;
                msg["block"] = candidate;
                msg["worker_rank"] = rank;
                msg["range_start"] = start;
                msg["range_end"] = end;

                mpi_send_string(coordinator, TAG_RESULT, msg.dump());

                int stop = 0;
                MPI_Recv(&stop, 1, MPI_INT, coordinator, TAG_STOP, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                shouldQuitJob = true;
                break;
            }

            batch++;
        }
    }
}

// -------------------- Coordinator: mine one block via MPI workers --------------------
static void mpi_send_shutdown_to_workers(int worldSize)
{
    for (int r = 1; r < worldSize; ++r)
    {
        int cmd = CMD_SHUTDOWN;
        MPI_Send(&cmd, 1, MPI_INT, r, TAG_CMD, MPI_COMM_WORLD);
    }
}

static Block mpi_mine_one_block(Blockchain &bc, int worldSize,
                                const std::string &data,
                                const std::string &owner,
                                int threads)
{
    const int numWorkers = worldSize - 1;
    if (numWorkers <= 0)
        throw std::runtime_error("MPI needs at least 2 processes (1 coordinator + >=1 worker)");

    const Block &prev = bc.GetLatestBlock();

    json job;
    job["prev"] = prev;
    job["data"] = data;
    job["owner"] = owner;
    job["difficulty"] = bc.Difficulty;
    job["threads"] = threads;

    std::string jobStr = job.dump();

    for (int r = 1; r < worldSize; ++r)
    {
        int cmd = CMD_JOB;
        MPI_Send(&cmd, 1, MPI_INT, r, TAG_CMD, MPI_COMM_WORLD);
        mpi_send_string(r, TAG_JOB, jobStr);
    }

    Block winner;
    bool accepted = false;

    while (!accepted)
    {
        MPI_Status st;
        int flag = 0;
        MPI_Iprobe(MPI_ANY_SOURCE, TAG_RESULT, MPI_COMM_WORLD, &flag, &st);
        if (!flag)
        {
            std::this_thread::yield();
            continue;
        }

        std::string msgStr = mpi_recv_string(st.MPI_SOURCE, TAG_RESULT);
        json msg = json::parse(msgStr);
        Block candidate = msg.at("block").get<Block>();

        const Block &latest = bc.GetLatestBlock();
        bool linkOK = (candidate.Index == latest.Index + 1) && (candidate.PreviousHash == latest.Hash);
        bool hashOK = (candidate.CalculateHash() == candidate.Hash);
        bool diffOK = hash_meets_difficulty(candidate.Hash, bc.Difficulty);

        if (linkOK && hashOK && diffOK)
        {
            if (bc.AddBlock(candidate))
            {
                accepted = true;
                winner = candidate;
            }
        }
    }

    int stop = 1;
    for (int r = 1; r < worldSize; ++r)
        MPI_Send(&stop, 1, MPI_INT, r, TAG_STOP, MPI_COMM_WORLD);

    for (;;)
    {
        MPI_Status st;
        int flag = 0;
        MPI_Iprobe(MPI_ANY_SOURCE, TAG_RESULT, MPI_COMM_WORLD, &flag, &st);
        if (!flag)
            break;
        (void)mpi_recv_string(st.MPI_SOURCE, TAG_RESULT);
    }

    return winner;
}

// -------------------- Rank0 HTTP server --------------------
static void coordinator_http_server(int worldSize, uint16_t port)
{
    int threads = (int)std::thread::hardware_concurrency();
    if (threads <= 0)
        threads = 4;

    Blockchain bc;

    bc.BlockchainUpdated = [&]()
    {
        std::cout << "Blockchain updated, size = " << bc.Chain.size() << "\n";
    };

    bc.DifficultyUpdated = [&]()
    {
        std::cout << "DIFFICULTY CHANGED -> " << bc.Difficulty << "\n\n";
    };

    HttpBlockchainServer::MineFn mineFn = [&](const std::string &payload, const std::string &reqOwner, int t) -> Block
    {
        return mpi_mine_one_block(bc, worldSize, payload, reqOwner, t);
    };

    HttpBlockchainServer server(bc, port, "mpi-coordinator", threads, mineFn);
    gServer = &server;

    std::signal(SIGINT, on_signal);
#if !defined(_WIN32)
    std::signal(SIGTERM, on_signal);
#endif

    server.Run();

    mpi_send_shutdown_to_workers(worldSize);
    std::cout << "Server stopped. Workers shutdown sent.\n";
}

int main(int argc, char **argv)
{
    MPI_Init(&argc, &argv);

    int rank = 0;
    int worldSize = 0;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &worldSize);

    uint16_t port = 8080;
    if (argc >= 2)
        port = (uint16_t)std::stoi(argv[1]);

    if (rank == 0)
    {
        coordinator_http_server(worldSize, port);
    }
    else
    {
        worker_service_loop(rank, worldSize);
    }

    MPI_Finalize();
    return 0;
}
