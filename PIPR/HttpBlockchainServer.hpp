#pragma once

#include "Blockchain.hpp"

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <functional>
#include <mutex>
#include <queue>
#include <string>
#include <thread>

class HttpBlockchainServer
{
public:
    using MineFn = std::function<Block(const std::string &payload, const std::string &owner, int threads)>;

    HttpBlockchainServer(Blockchain &bc, uint16_t port,
                         std::string owner = "http-miner",
                         int miningThreads = 0,
                         MineFn mineFn = nullptr);
    ~HttpBlockchainServer();

    void Run();

    void Stop();

private:
    void WorkerLoop();

    void HandleClient(int client_fd);
    void SendResponse(int client_fd, int status_code,
                      const std::string &content_type,
                      const std::string &body);

    static std::string ReadAll(int fd);
    static bool StartsWith(const std::string &s, const std::string &prefix);
    static std::string Trim(const std::string &s);

private:
    Blockchain &bc_;
    uint16_t port_;
    std::string owner_;
    int miningThreads_;
    MineFn mineFn_;

    std::atomic<bool> running_{false};
    std::atomic<bool> stopRequested_{false};

    int listenFd_{-1};

    std::mutex qMtx_;
    std::condition_variable qCv_;
    std::queue<std::string> q_;
    std::thread worker_;

    std::mutex bcMtx_;
};
