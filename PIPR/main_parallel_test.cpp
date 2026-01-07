#include "Blockchain.hpp"
#include <iostream>
#include <thread>
#include <string>

int main()
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

    std::cout << "=== STRESS TEST: PARALLEL MINING ===\n";
    std::cout << "Threads: " << threads << "\n";
    std::cout << "Initial Difficulty: " << bc.Difficulty << "\n\n";

    for (int i = 1; i <= 40; ++i)
    {
        std::string data = "Stress block " + std::to_string(i);

        Block mined = bc.MineAndAddBlockParallel(data, "stress-miner", threads);

        std::cout << "Block " << mined.Index
                  << " | diff=" << mined.Difficulty
                  << " | nonce=" << mined.Nonce
                  << " | hash=" << mined.Hash.substr(0, 16) << "...\n";

        if (!bc.IsValidChain(bc.Chain))
        {
            std::cout << "Chain invalid!\n";
            return 1;
        }

        std::cout << "    Current chain size = " << bc.Chain.size()
                  << " | Current difficulty = " << bc.Difficulty << "\n\n";
    }

    std::cout << "DONE.\n";
    return 0;
}
