#include <mpi.h>

#include <atomic>
#include <cstdint>
#include <iostream>
#include <limits>
#include <string>
#include <thread>
#include <vector>
#include <ctime>
#include <iomanip>
#include <sstream>

#include "Blockchain.hpp"
#include "Block.hpp"
#include "nlohmann/json.hpp"

using json = nlohmann::json;

static constexpr int TAG_JOB = 100;
static constexpr int TAG_RESULT = 200;
static constexpr int TAG_STOP = 300;
static constexpr int TAG_STATS = 400;

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

static int optimal_threads()
{
    unsigned int t = std::thread::hardware_concurrency();
    if (t == 0)
        t = 1;
    return (int)t;
}

static void mpi_send_string(int dest, int tag, const std::string &s, MPI_Comm comm)
{
    int len = (int)s.size();
    MPI_Send(&len, 1, MPI_INT, dest, tag, comm);
    if (len > 0)
        MPI_Send(s.data(), len, MPI_CHAR, dest, tag, comm);
}

static std::string mpi_recv_string(int src, int tag, MPI_Comm comm, MPI_Status *st_out = nullptr)
{
    MPI_Status st;
    int len = 0;
    MPI_Recv(&len, 1, MPI_INT, src, tag, comm, &st);
    std::string s;
    s.resize((size_t)len);
    if (len > 0)
        MPI_Recv(s.data(), len, MPI_CHAR, src, tag, comm, &st);
    if (st_out)
        *st_out = st;
    return s;
}

static std::string mpi_recv_string_any(MPI_Comm comm, int &outTag, int &outSrc)
{
    MPI_Status st;
    int len = 0;
    MPI_Recv(&len, 1, MPI_INT, MPI_ANY_SOURCE, MPI_ANY_TAG, comm, &st);

    std::string s;
    s.resize((size_t)len);
    if (len > 0)
        MPI_Recv(s.data(), len, MPI_CHAR, st.MPI_SOURCE, st.MPI_TAG, comm, &st);

    outTag = st.MPI_TAG;
    outSrc = st.MPI_SOURCE;
    return s;
}

struct MiningOutcome
{
    bool found = false;
    Block block;
    uint64_t operations = 0;
};

static MiningOutcome mine_in_range_parallel(
    const Block &previousBlock,
    const std::string &data,
    int difficulty,
    const std::string &owner,
    uint32_t startNonce,
    uint32_t endNonce,
    int numThreads,
    std::atomic<bool> &stopFlag,
    int masterRank,
    const std::string &timestamp)
{
    MiningOutcome out;

    Block base;
    base.Index = previousBlock.Index + 1;
    base.Timestamp = timestamp;
    base.Data = data;
    base.PreviousHash = previousBlock.Hash;
    base.Difficulty = difficulty;
    base.Owner = owner;

    const std::string target((size_t)std::max(0, difficulty), '0');

    if (numThreads <= 0)
        numThreads = 1;

    std::atomic<bool> found(false);
    std::atomic<uint64_t> ops(0);

    auto task = [&](int tid)
    {
        Block local = base;
        constexpr uint32_t PROBE_EVERY = 4096;
        uint32_t i = 0;

        for (uint64_t n = (uint64_t)startNonce + (uint64_t)tid;
             n <= (uint64_t)endNonce && !stopFlag.load(std::memory_order_relaxed) && !found.load(std::memory_order_relaxed);
             n += (uint64_t)numThreads)
        {
            local.Nonce = n;
            local.Hash = local.CalculateHash();
            ops.fetch_add(1, std::memory_order_relaxed);

            if (local.Hash.rfind(target, 0) == 0)
            {
                if (!found.exchange(true, std::memory_order_relaxed))
                {
                    out.found = true;
                    out.block = local;
                    stopFlag.store(true, std::memory_order_relaxed);
                }
                break;
            }

            if ((++i % PROBE_EVERY) == 0)
            {
                int flag = 0;
                MPI_Status st;
                MPI_Iprobe(masterRank, TAG_STOP, MPI_COMM_WORLD, &flag, &st);
                if (flag)
                {
                    int dummy = 0;
                    MPI_Recv(&dummy, 1, MPI_INT, masterRank, TAG_STOP, MPI_COMM_WORLD, &st);
                    stopFlag.store(true, std::memory_order_relaxed);
                    break;
                }
            }
        }
    };

    std::vector<std::thread> threads;
    threads.reserve((size_t)numThreads);
    for (int t = 0; t < numThreads; ++t)
        threads.emplace_back(task, t);
    for (auto &th : threads)
        th.join();

    out.operations = ops.load(std::memory_order_relaxed);
    return out;
}

static void workerProcess(int rank, int threads)
{
    if (threads <= 0)
        threads = optimal_threads();

    while (true)
    {
        MPI_Status st;
        std::string jobStr = mpi_recv_string(0, TAG_JOB, MPI_COMM_WORLD, &st);
        if (jobStr == "EXIT")
            break;

        int difficulty = 0;
        uint32_t startNonce = 0;
        uint32_t endNonce = 0;
        std::string data;
        std::string owner;
        Block prev;
        std::string timestamp;

        try
        {
            json job = json::parse(jobStr);

            difficulty = job["difficulty"].get<int>();
            startNonce = job["startNonce"].get<uint32_t>();
            endNonce = job["endNonce"].get<uint32_t>();
            data = job["data"].get<std::string>();
            owner = job["owner"].get<std::string>();
            prev = job["previousBlock"].get<Block>();
            timestamp = job["timestamp"].get<std::string>();
        }
        catch (const std::exception &e)
        {
            std::cerr << "[rank " << rank << "] ERROR parsing job: " << e.what() << "\n";
            continue;
        }

        std::atomic<bool> stopFlag(false);

        std::cout << "[rank " << rank << "] mining range "
                  << startNonce << "-" << endNonce
                  << " diff=" << difficulty
                  << " ts=" << timestamp
                  << " threads=" << threads << "\n";

        MiningOutcome res = mine_in_range_parallel(
            prev, data, difficulty, owner,
            startNonce, endNonce,
            threads,
            stopFlag,
            0,
            timestamp);

        if (res.found)
        {
            json payload;
            payload["found"] = true;
            payload["block"] = res.block;
            payload["operations"] = res.operations;
            payload["rank"] = rank;
            mpi_send_string(0, TAG_RESULT, payload.dump(), MPI_COMM_WORLD);
        }
        else
        {
            json payload;
            payload["found"] = false;
            payload["operations"] = res.operations;
            payload["rank"] = rank;
            mpi_send_string(0, TAG_STATS, payload.dump(), MPI_COMM_WORLD);
        }
    }
}

static void send_stop_to_all_workers(int worldSize, int winnerRank)
{
    int dummy = 1;
    for (int r = 1; r < worldSize; ++r)
    {
        if (r == winnerRank)
            continue;
        MPI_Send(&dummy, 1, MPI_INT, r, TAG_STOP, MPI_COMM_WORLD);
    }
}

static void mainProcess(int worldSize, int blocksToMine)
{
    Blockchain bc;

    const int workers = worldSize - 1;
    if (workers <= 0)
    {
        std::cerr << "Potrebujes vsaj 2 MPI procesa (1 master + 1 worker).\n";
        return;
    }

    const uint32_t CHUNK_SIZE = 5'000'000u;
    const uint64_t MAXN = (uint64_t)std::numeric_limits<uint32_t>::max();

    for (int b = 0; b < blocksToMine; ++b)
    {
        std::string blockTs = utc_now_iso8601();
        const Block &latest = bc.GetLatestBlock();
        int difficulty = bc.Difficulty;

        std::cout << "[master] mining index=" << (latest.Index + 1)
                  << " diff=" << difficulty
                  << " ts=" << blockTs << "\n";

        std::string data = std::string("{\"block\":") + std::to_string(latest.Index + 1) + "}";
        std::string owner = "MPI";

        uint64_t nextNonce = 0;

        bool found = false;
        Block mined;
        int winnerRank = -1;
        uint64_t totalOps = 0;

        for (int r = 1; r < worldSize; ++r)
        {
            uint64_t start = nextNonce;
            uint64_t end = std::min<uint64_t>(start + (uint64_t)CHUNK_SIZE - 1, MAXN);
            nextNonce = (end == MAXN) ? 0 : (end + 1);

            json job;
            job["difficulty"] = difficulty;
            job["data"] = data;
            job["owner"] = owner;
            job["startNonce"] = (uint32_t)start;
            job["endNonce"] = (uint32_t)end;
            job["previousBlock"] = latest;
            job["timestamp"] = blockTs;

            mpi_send_string(r, TAG_JOB, job.dump(), MPI_COMM_WORLD);
        }

        while (!found)
        {
            int tag = 0, src = -1;
            std::string msg = mpi_recv_string_any(MPI_COMM_WORLD, tag, src);

            if (tag == TAG_RESULT)
            {
                json res = json::parse(msg);
                if (res.contains("found") && res["found"].get<bool>())
                {
                    found = true;
                    winnerRank = res["rank"].get<int>();
                    mined = res["block"].get<Block>();
                    totalOps += res["operations"].get<uint64_t>();
                    send_stop_to_all_workers(worldSize, winnerRank);
                    break;
                }
            }
            else if (tag == TAG_STATS)
            {
                json st = json::parse(msg);
                totalOps += st["operations"].get<uint64_t>();

                if (found)
                    continue;

                uint64_t start = nextNonce;
                uint64_t end = std::min<uint64_t>(start + (uint64_t)CHUNK_SIZE - 1, MAXN);
                nextNonce = (end == MAXN) ? 0 : (end + 1);

                json job;
                job["difficulty"] = difficulty;
                job["data"] = data;
                job["owner"] = owner;
                job["startNonce"] = (uint32_t)start;
                job["endNonce"] = (uint32_t)end;
                job["previousBlock"] = latest;
                job["timestamp"] = blockTs;

                std::cout << "[master] reassigned chunk to rank " << src
                          << " start=" << (uint32_t)start << " end=" << (uint32_t)end << "\n";

                mpi_send_string(src, TAG_JOB, job.dump(), MPI_COMM_WORLD);
            }
        }

        int needStats = workers - 1;
        while (needStats > 0)
        {
            int tag = 0, src = -1;
            std::string msg = mpi_recv_string_any(MPI_COMM_WORLD, tag, src);
            if (tag == TAG_STATS)
            {
                json st = json::parse(msg);
                totalOps += st["operations"].get<uint64_t>();
                needStats--;
            }
        }

        bool ok = bc.AddBlock(mined);
        if (!ok)
        {
            std::cerr << "Master: prejeti blok NI veljaven (AddBlock failed).\n";
            break;
        }

        std::cout << "Blok #" << mined.Index
                  << " izrudaril rank " << winnerRank
                  << " (diff=" << difficulty
                  << ", totalOps=" << totalOps << ")\n";
    }

    for (int r = 1; r < worldSize; ++r)
        mpi_send_string(r, TAG_JOB, "EXIT", MPI_COMM_WORLD);
}

int main(int argc, char **argv)
{
    MPI_Init(&argc, &argv);

    std::ios::sync_with_stdio(false);
    std::cout.setf(std::ios::unitbuf);
    std::cerr.setf(std::ios::unitbuf);

    int rank = 0, size = 0;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    int blocksToMine = 3;
    int threadsPerWorker = 0;

    for (int i = 1; i + 1 < argc; ++i)
    {
        std::string arg = argv[i];
        if (arg == "-n")
            blocksToMine = std::stoi(argv[i + 1]);
        else if (arg == "-t")
            threadsPerWorker = std::stoi(argv[i + 1]);
    }

    if (rank == 0)
        mainProcess(size, blocksToMine);
    else
        workerProcess(rank, threadsPerWorker);

    MPI_Finalize();
    return 0;
}
