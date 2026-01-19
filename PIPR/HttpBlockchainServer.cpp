#include "HttpBlockchainServer.hpp"

#include <iostream>
#include <sstream>
#include <cstring>
#include <thread>

#if defined(_WIN32)
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "Ws2_32.lib")
using socklen_t = int;
static void closesock(int fd) { closesocket(fd); }
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
static void closesock(int fd) { ::close(fd); }
#endif

// ---- small socket helpers ----
static std::string recv_some(int fd)
{
    char buf[4096];
#if defined(_WIN32)
    int n = ::recv(fd, buf, (int)sizeof(buf), 0);
#else
    ssize_t n = ::recv(fd, buf, sizeof(buf), 0);
#endif
    if (n <= 0)
        return {};
    return std::string(buf, buf + n);
}

static int parse_content_length_case_insensitive(const std::string &headers)
{
    // tolerant: finds "Content-Length:" regardless of case
    // (simple approach: search in lowercased copy)
    std::string h = headers;
    for (char &c : h)
        c = (char)std::tolower((unsigned char)c);

    const std::string key = "content-length:";
    size_t pos = h.find(key);
    if (pos == std::string::npos)
        return 0;

    pos += key.size();
    while (pos < h.size() && (h[pos] == ' ' || h[pos] == '\t'))
        pos++;

    size_t end = pos;
    while (end < h.size() && h[end] >= '0' && h[end] <= '9')
        end++;

    if (end == pos)
        return 0;
    return std::stoi(h.substr(pos, end - pos));
}

static bool read_http_request(int fd, std::string &outHead, std::string &outBody)
{
    std::string buf;
    buf.reserve(8192);

    // 1) Read until end of headers
    while (true)
    {
        auto chunk = recv_some(fd);
        if (chunk.empty())
            return false;
        buf += chunk;

        // safeguard (prevent huge header attacks)
        if (buf.size() > 1024 * 1024)
            return false;

        size_t headerEnd = buf.find("\r\n\r\n");
        if (headerEnd != std::string::npos)
        {
            outHead = buf.substr(0, headerEnd);
            std::string rest = buf.substr(headerEnd + 4);

            int cl = parse_content_length_case_insensitive(outHead);
            if (cl < 0)
                cl = 0;

            outBody = rest;

            // 2) Read remaining bytes for body (exactly Content-Length)
            while ((int)outBody.size() < cl)
            {
                auto more = recv_some(fd);
                if (more.empty())
                    return false;
                outBody += more;

                // safeguard (limit body)
                if (outBody.size() > 5 * 1024 * 1024)
                    return false; // 5MB
            }

            // If we read more than cl (e.g. pipelining), keep only cl bytes
            if ((int)outBody.size() > cl)
                outBody.resize((size_t)cl);
            return true;
        }
    }
}

static std::string trim_ws(const std::string &s)
{
    size_t a = 0, b = s.size();
    while (a < b && (s[a] == ' ' || s[a] == '\r' || s[a] == '\n' || s[a] == '\t'))
        a++;
    while (b > a && (s[b - 1] == ' ' || s[b - 1] == '\r' || s[b - 1] == '\n' || s[b - 1] == '\t'))
        b--;
    return s.substr(a, b - a);
}

static void status_text(int code, std::string &out)
{
    switch (code)
    {
    case 200:
        out = "OK";
        break;
    case 201:
        out = "Created";
        break;
    case 400:
        out = "Bad Request";
        break;
    case 404:
        out = "Not Found";
        break;
    case 405:
        out = "Method Not Allowed";
        break;
    case 413:
        out = "Payload Too Large";
        break;
    default:
        out = "OK";
        break;
    }
}

HttpBlockchainServer::HttpBlockchainServer(Blockchain &bc, uint16_t port,
                                           std::string owner, int miningThreads)
    : bc_(bc), port_(port), owner_(std::move(owner))
{
    if (miningThreads <= 0)
    {
        int hw = (int)std::thread::hardware_concurrency();
        miningThreads_ = (hw > 0) ? hw : 4;
    }
    else
    {
        miningThreads_ = miningThreads;
    }
}

HttpBlockchainServer::~HttpBlockchainServer()
{
    Stop();
}

void HttpBlockchainServer::Stop()
{
    if (!running_.load())
        return;
    stopRequested_.store(true);

    qCv_.notify_all();

    if (listenFd_ >= 0)
    {
        closesock(listenFd_);
        listenFd_ = -1;
    }

    if (worker_.joinable())
        worker_.join();

    running_.store(false);
}

void HttpBlockchainServer::Run()
{
#if defined(_WIN32)
    WSADATA wsa;
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0)
    {
        std::cerr << "WSAStartup failed\n";
        return;
    }
#endif

    stopRequested_.store(false);
    running_.store(true);

    worker_ = std::thread(&HttpBlockchainServer::WorkerLoop, this);

    listenFd_ = ::socket(AF_INET, SOCK_STREAM, 0);
    if (listenFd_ < 0)
    {
        std::cerr << "socket() failed\n";
        Stop();
        return;
    }

    int opt = 1;
#if !defined(_WIN32)
    setsockopt(listenFd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#else
    setsockopt(listenFd_, SOL_SOCKET, SO_REUSEADDR, (const char *)&opt, sizeof(opt));
#endif

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port = htons(port_);

    if (::bind(listenFd_, (sockaddr *)&addr, sizeof(addr)) < 0)
    {
        std::cerr << "bind() failed (port " << port_ << ")\n";
        Stop();
        return;
    }

    if (::listen(listenFd_, 16) < 0)
    {
        std::cerr << "listen() failed\n";
        Stop();
        return;
    }

    std::cout << "HTTP server listening on port " << port_ << "\n";
    std::cout << "Endpoints:\n"
              << "  POST /data   (raw body = string)\n"
              << "  GET  /chain  (returns bc.SerializeChain())\n";

    while (!stopRequested_.load())
    {
        sockaddr_in client{};
        socklen_t len = sizeof(client);
        int clientFd = ::accept(listenFd_, (sockaddr *)&client, &len);
        if (clientFd < 0)
        {
            if (stopRequested_.load())
                break;
            continue;
        }

        HandleClient(clientFd);
        closesock(clientFd);
    }

    Stop();

#if defined(_WIN32)
    WSACleanup();
#endif
}

void HttpBlockchainServer::WorkerLoop()
{
    while (!stopRequested_.load())
    {
        std::string item;

        {
            std::unique_lock<std::mutex> lock(qMtx_);
            qCv_.wait(lock, [&]
                      { return stopRequested_.load() || !q_.empty(); });
            if (stopRequested_.load())
                break;

            item = std::move(q_.front());
            q_.pop();
        }

        try
        {
            std::lock_guard<std::mutex> bcLock(bcMtx_);
            Block mined = bc_.MineAndAddBlockParallel(item, owner_, miningThreads_);
            std::cout << "Mined block " << mined.Index
                      << " | diff=" << mined.Difficulty
                      << " | nonce=" << mined.Nonce
                      << " | hash=" << mined.Hash.substr(0, 16) << "...\n";
        }
        catch (const std::exception &e)
        {
            std::cerr << "Mining/Add failed: " << e.what() << "\n";
        }
    }
}

void HttpBlockchainServer::SendResponse(int client_fd, int status_code,
                                        const std::string &content_type,
                                        const std::string &body)
{
    std::string st;
    status_text(status_code, st);

    std::ostringstream oss;
    oss << "HTTP/1.1 " << status_code << " " << st << "\r\n";
    oss << "Content-Type: " << content_type << "\r\n";
    oss << "Content-Length: " << body.size() << "\r\n";
    oss << "Connection: close\r\n";
    oss << "\r\n";
    oss << body;

    const std::string resp = oss.str();
#if defined(_WIN32)
    ::send(client_fd, resp.c_str(), (int)resp.size(), 0);
#else
    ::send(client_fd, resp.c_str(), resp.size(), 0);
#endif
}

void HttpBlockchainServer::HandleClient(int client_fd)
{
    std::string head, body;
    if (!read_http_request(client_fd, head, body))
    {
        SendResponse(client_fd, 400, "text/plain", "Bad Request\n");
        return;
    }

    std::istringstream hs(head);
    std::string method, path, ver;
    hs >> method >> path >> ver;

    if (method.empty() || path.empty())
    {
        SendResponse(client_fd, 400, "text/plain", "Bad Request\n");
        return;
    }

    // Body limit
    if (body.size() > 1024 * 1024)
    {
        SendResponse(client_fd, 413, "text/plain", "Payload Too Large\n");
        return;
    }

    if (method == "POST" && path == "/data")
    {
        std::string payload = trim_ws(body);
        if (payload.empty())
        {
            SendResponse(client_fd, 400, "text/plain", "Empty body\n");
            return;
        }

        {
            std::lock_guard<std::mutex> lock(qMtx_);
            q_.push(payload);
        }
        qCv_.notify_one();

        SendResponse(client_fd, 200, "text/plain", "ENQUEUED\n");
        return;
    }

    if (method == "GET" && path == "/chain")
    {
        std::string j;
        {
            std::lock_guard<std::mutex> bcLock(bcMtx_);
            j = bc_.SerializeChain();
        }
        SendResponse(client_fd, 200, "application/json", j);
        return;
    }

    if (method == "GET" && path == "/")
    {
        const char *help =
            "OK\n"
            "POST /data   (raw body = string)\n"
            "GET  /chain  (json)\n";
        SendResponse(client_fd, 200, "text/plain", help);
        return;
    }

    SendResponse(client_fd, 404, "text/plain", "Not Found\n");
}
