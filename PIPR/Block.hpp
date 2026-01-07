#pragma once

#include <string>
#include <cstdint>
#include "nlohmann/json.hpp"

struct Block
{
    int Index = 0;
    std::string Timestamp;
    std::string Data;
    std::string PreviousHash;
    std::string Hash;
    int Nonce = 0;
    int Difficulty = 0;
    std::string Owner;

    struct ValidationResult
    {
        bool IsValid = false;
        std::string Message;
    };

    std::string CalculateHash() const;

    static Block MineBlock(const Block &previousBlock,
                           const std::string &data,
                           int difficulty,
                           const std::string &owner);

    static Block MineBlockParallel(const Block &previousBlock,
                                   const std::string &data,
                                   int difficulty,
                                   const std::string &owner,
                                   int numThreads);
};

// JSON (de)serializacija
void to_json(nlohmann::json &j, const Block &b);
void from_json(const nlohmann::json &j, Block &b);
