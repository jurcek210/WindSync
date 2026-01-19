#include "Blockchain.hpp"
#include <iostream>
#include <chrono>
#include <vector>
#include <string>
#include <iomanip>

using SteadyClock = std::chrono::steady_clock;

static double elapsed_seconds(const SteadyClock::time_point &t0)
{
    return std::chrono::duration<double>(SteadyClock::now() - t0).count();
}

int main()
{
    const int fixedDifficulty = 6;
    const int blocksPerRun = 10;
    const std::vector<int> threadCounts = {1, 2, 4, 8, 16, 32};

    std::cout << "=== BENCHMARK: PARALLEL MINING ===\n";
    std::cout << "Blocks per run : " << blocksPerRun << "\n";
    std::cout << "Fixed difficulty: " << fixedDifficulty << "\n\n";

    for (int threads : threadCounts)
    {
        Blockchain bc;

        bc.BlockchainUpdated = []() {};
        bc.DifficultyUpdated = []() {};

        auto t0 = SteadyClock::now();

        for (int i = 1; i <= blocksPerRun; ++i)
        {
            bc.Difficulty = fixedDifficulty;

            std::string data = "bench block " + std::to_string(i);
            bc.MineAndAddBlockParallel(data, "bench", threads);
        }

        double totalTime = elapsed_seconds(t0);
        double avgTime = totalTime / blocksPerRun;

        std::cout << "threads=" << std::setw(2) << threads
                  << " | total=" << std::fixed << std::setprecision(3) << totalTime << " s"
                  << " | avg=" << avgTime << " s/block\n";

        // // blockchain izpis šele na koncu (ne vpliva na meritve)
        // std::cout << "\n--- BLOCKCHAIN (threads=" << threads << ") ---\n";
        // std::cout << bc.SerializeChain() << "\n";
        // std::cout << "--------------------------------------------\n\n";
    }

    return 0;
}
