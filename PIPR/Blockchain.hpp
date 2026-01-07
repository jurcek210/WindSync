#pragma once

#include <vector>
#include <functional>
#include <string>
#include <cstdint>

#include "Block.hpp"

class Blockchain
{
public:
    std::vector<Block> Chain;
    int Difficulty = 2;

    std::function<void()> DifficultyUpdated;
    std::function<void()> BlockchainUpdated;

    Blockchain();

    const Block &GetLatestBlock() const;

    bool AddBlock(const Block &newBlock);
    Block MineAndAddBlock(const std::string &data, const std::string &owner);

    bool ValidateAndSyncChain(const std::vector<Block> &receivedChain);
    bool IsValidChain(const std::vector<Block> &chain) const;

    int CalculateCumulativeDifficulty(const std::vector<Block> &chain) const;

    bool ReplaceChainIfBetter(const std::vector<Block> &newChain);

    std::string SerializeChain() const;
    static std::vector<Block> DeserializeChain(const std::string &serializedChain);

    Block MineAndAddBlockParallel(const std::string &data,
                                  const std::string &owner,
                                  int numThreads);

private:
    static constexpr int BlockGenerationInterval = 10;
    static constexpr int DifficultyAdjustmentInterval = 6;

    int blockCounter = 0;

    Block CreateGenesisBlock() const;

    int CalculateNewDifficulty() const;

    bool IsGenesisBlockValid(const Block &g) const;
    bool IsBlockValid(const Block &b, const Block &prev) const;
};
