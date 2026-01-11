#include "Block.hpp"

#include <sstream>
#include <iomanip>
#include <algorithm>
#include <thread>
#include <atomic>
#include <vector>
#include <mutex>
#include <cstdlib>

#include <openssl/sha.h>

static std::string sha256_hex(const std::string &input)
{
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256(reinterpret_cast<const unsigned char *>(input.data()), input.size(), hash);

    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (unsigned char c : hash)
        oss << std::setw(2) << (int)c;
    return oss.str();
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

std::string Block::CalculateHash() const
{
    std::ostringstream input;
    input << Index << Timestamp << Data << PreviousHash << Difficulty << Nonce;
    return sha256_hex(input.str());
}

Block Block::MineBlock(const Block &previousBlock,
                       const std::string &data,
                       int difficulty,
                       const std::string &owner)
{
    Block b;
    b.Index = previousBlock.Index + 1;
    b.Timestamp = utc_now_iso8601();
    b.Data = data;
    b.PreviousHash = previousBlock.Hash;
    b.Difficulty = difficulty;
    b.Owner = owner;

    const std::string prefix((size_t)std::max(0, difficulty), '0');

    b.Nonce = 0;
    do
    {
        b.Hash = b.CalculateHash();
        if (b.Hash.rfind(prefix, 0) == 0)
            break;
        b.Nonce++;
    } while (true);

    return b;
}

Block Block::MineBlockParallel(const Block &previousBlock,
                               const std::string &data,
                               int difficulty,
                               const std::string &owner,
                               int numThreads)
{
    Block base;
    base.Index = previousBlock.Index + 1;
    base.Timestamp = utc_now_iso8601();
    base.Data = data;
    base.PreviousHash = previousBlock.Hash;
    base.Difficulty = difficulty;
    base.Owner = owner;

    const std::string target(difficulty, '0');

    if (numThreads <= 0)
        numThreads = 1;

    std::atomic<bool> found(false);
    std::atomic<uint64_t> nonce(0);

    Block winner;
    std::mutex m;

    auto miningTask = [&]()
    {
        Block local = base;

        while (!found.load(std::memory_order_relaxed))
        {
            uint64_t n = nonce.fetch_add(1, std::memory_order_relaxed);
            local.Nonce = n;

            local.Hash = local.CalculateHash();

            if (local.Hash.rfind(target, 0) == 0)
            {
                std::lock_guard<std::mutex> lock(m);
                if (!found.load(std::memory_order_relaxed))
                {
                    found.store(true, std::memory_order_relaxed);
                    winner = local;
                }
                break;
            }
        }
    };

    std::vector<std::thread> threads;
    threads.reserve((size_t)numThreads);
    for (int i = 0; i < numThreads; ++i)
        threads.emplace_back(miningTask);
    for (auto &t : threads)
        t.join();

    return winner;
}

void to_json(nlohmann::json &j, const Block &b)
{
    j = nlohmann::json{
        {"Index", b.Index},
        {"Timestamp", b.Timestamp},
        {"Data", b.Data},
        {"PreviousHash", b.PreviousHash},
        {"Hash", b.Hash},
        {"Nonce", b.Nonce},
        {"Difficulty", b.Difficulty},
        {"Owner", b.Owner}};
}

void from_json(const nlohmann::json &j, Block &b)
{
    j.at("Index").get_to(b.Index);
    j.at("Timestamp").get_to(b.Timestamp);
    j.at("Data").get_to(b.Data);
    j.at("PreviousHash").get_to(b.PreviousHash);
    j.at("Hash").get_to(b.Hash);
    j.at("Nonce").get_to(b.Nonce);
    j.at("Difficulty").get_to(b.Difficulty);
    if (j.contains("Owner"))
        j.at("Owner").get_to(b.Owner);
}
