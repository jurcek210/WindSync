#include "Blockchain.hpp"

#include <iostream>
#include <cmath>
#include <algorithm>
#include <stdexcept>
#include <sstream>
#include <iomanip>
#include <ctime>

#include "nlohmann/json.hpp"

using json = nlohmann::json;

// parse ISO8601 UTC -> time_t (poenostavljeno)
static std::time_t parse_iso8601_utc_to_time_t(const std::string &iso)
{
    std::string core = iso;
    if (core.size() >= 19)
        core = core.substr(0, 19);

    std::tm tm{};
    std::istringstream iss(core);
    iss >> std::get_time(&tm, "%Y-%m-%dT%H:%M:%S");
    if (iss.fail())
        throw std::runtime_error("Neveljaven Timestamp: " + iso);

#if defined(_WIN32)
    return _mkgmtime(&tm);
#else
    return timegm(&tm);
#endif
}

Blockchain::Blockchain()
{
    if (Chain.empty())
    {
        std::cout << "Initializing Genesis Block...\n";
        Chain.push_back(CreateGenesisBlock());
    }
}

const Block &Blockchain::GetLatestBlock() const
{
    if (Chain.empty())
        throw std::runtime_error("Veriga je prazna.");
    return Chain.back();
}

bool Blockchain::AddBlock(const Block &newBlock)
{
    try
    {
        const Block &latest = GetLatestBlock();

        if (!IsBlockValid(newBlock, latest))
        {
            std::cout << "Napaka: Blok ni veljaven in ne bo dodan.\n";
            return false;
        }

        Chain.push_back(newBlock);
        blockCounter++;

        if (blockCounter % 3 == 0)
        {
            if (BlockchainUpdated)
                BlockchainUpdated();
            std::cout << "Dogodek posodobitve blockchaina. Stevec: " << blockCounter << "\n";
        }

        if (static_cast<int>(Chain.size()) % DifficultyAdjustmentInterval == 0)
        {
            Difficulty = CalculateNewDifficulty();
            std::cout << "Nova tezavnost: " << Difficulty << "\n";
            if (DifficultyUpdated)
                DifficultyUpdated();
        }

        return true;
    }
    catch (const std::exception &ex)
    {
        std::cout << "Napaka pri dodajanju bloka: " << ex.what() << "\n";
        return false;
    }
}

Block Blockchain::MineAndAddBlock(const std::string &data, const std::string &owner)
{
    const Block &latest = GetLatestBlock();
    Block newBlock = Block::MineBlock(latest, data, Difficulty, owner);
    if (AddBlock(newBlock))
        return newBlock;
    throw std::runtime_error("Blok ni veljaven in ne more biti dodan.");
}

Block Blockchain::MineAndAddBlockParallel(const std::string &data,
                                          const std::string &owner,
                                          int numThreads)
{
    const Block &prev = GetLatestBlock();

    Block mined = Block::MineBlockParallel(prev, data, Difficulty, owner, numThreads);

    if (!AddBlock(mined))
    {
        throw std::runtime_error("AddBlock failed");
    }

    return mined;
}

bool Blockchain::ValidateAndSyncChain(const std::vector<Block> &receivedChain)
{
    std::cout << "Preverjam prejeto verigo: Dolzina: " << receivedChain.size()
              << ", Trenutna dolzina: " << Chain.size() << "\n";

    if (receivedChain.size() < Chain.size())
    {
        std::cout << "Prejeta veriga je krajsa.\n";
        return false;
    }

    if (receivedChain.size() == Chain.size())
    {
        std::cout << "Enaka dolzina - preverjam tie-break...\n";
    }

    if (!IsValidChain(receivedChain))
    {
        std::cout << "Prejeta veriga ni veljavna.\n";
        return false;
    }

    if (ReplaceChainIfBetter(receivedChain))
    {
        std::cout << "Veriga zamenjana z boljso.\n";
        if (DifficultyUpdated)
            DifficultyUpdated();
        return true;
    }

    std::cout << "Veriga ni bila zamenjana.\n";
    return false;
}

int Blockchain::CalculateCumulativeDifficulty(const std::vector<Block> &chain) const
{
    int cum = 0;
    for (const auto &b : chain)
    {
        cum += static_cast<int>(std::pow(2.0, b.Difficulty));
    }
    return cum;
}

bool Blockchain::IsValidChain(const std::vector<Block> &chain) const
{
    if (chain.empty())
        return false;
    if (!IsGenesisBlockValid(chain.front()))
        return false;

    for (size_t i = 1; i < chain.size(); ++i)
    {
        if (!IsBlockValid(chain[i], chain[i - 1]))
        {
            std::cout << "Napaka pri validaciji bloka " << chain[i].Index << "\n";
            return false;
        }
    }
    return true;
}

bool Blockchain::ReplaceChainIfBetter(const std::vector<Block> &newChain)
{
    if (!IsValidChain(newChain))
    {
        std::cout << "Nova veriga ni veljavna.\n";
        return false;
    }

    int currentDifficulty = CalculateCumulativeDifficulty(Chain);
    int newDifficulty = CalculateCumulativeDifficulty(newChain);

    std::cout << "Trenutna kumulativna tezavnost: " << currentDifficulty << "\n";
    std::cout << "Prejeta kumulativna tezavnost: " << newDifficulty << "\n";

    bool better =
        (newDifficulty > currentDifficulty) ||
        (newDifficulty == currentDifficulty && newChain.back().Hash < Chain.back().Hash);

    if (better)
    {
        Chain = newChain;
        Difficulty = CalculateNewDifficulty();
        if (DifficultyUpdated)
            DifficultyUpdated();
        blockCounter = 0;
        if (BlockchainUpdated)
            BlockchainUpdated();
        std::cout << "Nova veriga sprejeta kot boljsa.\n";
        return true;
    }

    std::cout << "Nova veriga ni boljsa.\n";
    return false;
}

std::string Blockchain::SerializeChain() const
{
    json j = Chain;
    return j.dump(2);
}

std::vector<Block> Blockchain::DeserializeChain(const std::string &serializedChain)
{
    json j = json::parse(serializedChain);
    return j.get<std::vector<Block>>();
}

Block Blockchain::CreateGenesisBlock() const
{
    Block g;
    g.Index = 0;
    g.Timestamp = "1970-01-01T00:00:00Z";
    g.Data = "Genesis Block";
    g.PreviousHash = "0";
    g.Nonce = 0;
    g.Difficulty = 0;
    g.Hash = g.CalculateHash();
    return g;
}

int Blockchain::CalculateNewDifficulty() const
{
    if (static_cast<int>(Chain.size()) <= DifficultyAdjustmentInterval)
        return Difficulty;

    const Block &latest = GetLatestBlock();
    const Block &prevAdjust = Chain[Chain.size() - DifficultyAdjustmentInterval - 1];

    int timeExpected = BlockGenerationInterval * DifficultyAdjustmentInterval;

    double timeTakenSeconds = std::difftime(
        parse_iso8601_utc_to_time_t(latest.Timestamp),
        parse_iso8601_utc_to_time_t(prevAdjust.Timestamp));

    int prevDiff = Difficulty;

    if (timeTakenSeconds < (timeExpected / 2.0))
        return prevDiff + 1;
    if (timeTakenSeconds > (timeExpected * 2.0))
        return std::max(0, prevDiff - 1);
    return prevDiff;
}

bool Blockchain::IsGenesisBlockValid(const Block &g) const
{
    return g.Index == 0 &&
           g.PreviousHash == "0" &&
           g.Hash == g.CalculateHash();
}

bool Blockchain::IsBlockValid(const Block &b, const Block &prev) const
{
    if (b.Index != prev.Index + 1)
        return false;
    if (b.PreviousHash != prev.Hash)
        return false;
    if (b.CalculateHash() != b.Hash)
        return false;

    std::string prefix(static_cast<size_t>(std::max(0, b.Difficulty)), '0');
    if (b.Hash.rfind(prefix, 0) != 0)
        return false;

    std::time_t now = std::time(nullptr);
    std::time_t bt = parse_iso8601_utc_to_time_t(b.Timestamp);
    std::time_t pt = parse_iso8601_utc_to_time_t(prev.Timestamp);

    // ni več kot 1 minuto v prihodnosti
    if (std::difftime(bt, now) > 60.0)
        return false;

    // ni več kot 1 minuto "pred" prejšnjim blokom
    if (std::difftime(pt, bt) > 60.0)
        return false;

    return true;
}
