#include "Blockchain.hpp"
#include "HttpBlockchainServer.hpp"

#include <iostream>
#include <thread>
#include <csignal>

static HttpBlockchainServer *gServer = nullptr;

static void on_signal(int)
{
    if (gServer)
        gServer->Stop();
}

int main(int argc, char **argv)
{
    uint16_t port = 8080;
    if (argc >= 2)
    {
        port = (uint16_t)std::stoi(argv[1]);
    }

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

    HttpBlockchainServer server(bc, port, "http-miner", threads);
    gServer = &server;

    std::signal(SIGINT, on_signal);
#if !defined(_WIN32)
    std::signal(SIGTERM, on_signal);
#endif

    server.Run();
    std::cout << "Server stopped.\n";
    return 0;
}
