#include "TcpServer.h"
#undef UNICODE

#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <stdio.h>
#include <iostream>

using namespace std;

#pragma comment (lib, "Ws2_32.lib")

#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "12453"

int __cdecl TcpServer::startServer()
{
    WSADATA wsaData;
    int iResult;

    SOCKET ListenSocket = INVALID_SOCKET;
    SOCKET ClientSocket = INVALID_SOCKET;

    struct addrinfo* result = NULL, hints;

    int recvbuflen = DEFAULT_BUFLEN;
    char recvbuf[DEFAULT_BUFLEN];
    char sendbuf[DEFAULT_BUFLEN];
    // 加载 Winsock
    iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (iResult != 0)
    {
        printf("WSAStartup failed: %d\n", iResult);
        return 1;
    }
    m_printf("加载Winsock成功");

    ZeroMemory(&hints, sizeof(hints));
    hints.ai_family = AF_INET;          // IPv4 address family
    hints.ai_socktype = SOCK_STREAM;    // Stream socket
    hints.ai_protocol = IPPROTO_TCP;    // TCP
    hints.ai_flags = AI_PASSIVE;        // For wildcard IP address

    // 解析服务器要使用的本地地址和端口
    iResult = getaddrinfo(NULL, DEFAULT_PORT, &hints, &result);
    if (iResult != 0)
    {
        printf("getaddrinfo failed: %d\n", iResult);
        WSACleanup();
        return 1;
    }
    m_printf("解析服务器使用的网络地址与端口成功");

    // 创建 SOCKET 以侦听客户端连接
    ListenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
    if (ListenSocket == INVALID_SOCKET)
    {
        printf("socket failed: %ld\n", WSAGetLastError());
        freeaddrinfo(result);
        WSACleanup();
        return 1;
    }
    m_printf("监听客户端连接的套接字创建成功");

    // 绑定套接字
    iResult = bind(ListenSocket, result->ai_addr, (int)result->ai_addrlen);
    if (iResult == SOCKET_ERROR)
    {
        printf("bind failed: %d\n", WSAGetLastError());
        freeaddrinfo(result);
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }
    m_printf("绑定监听客户端连接的套接字成功");

    freeaddrinfo(result);

    // 侦听套接字上的客户端连接
    iResult = listen(ListenSocket, SOMAXCONN);
    if (iResult == SOCKET_ERROR)
    {
        printf("listen failed: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    while (true) // 主循环以允许接受多个客户端
    {
        // 接受客户端套接字
        m_printf("正在等待用户连接...");
        ClientSocket = accept(ListenSocket, NULL, NULL);
        if (ClientSocket == INVALID_SOCKET)
        {
            printf("accept failed: %d\n", WSAGetLastError());
            closesocket(ListenSocket);
            WSACleanup();
            return 1;
        }
        m_printf("客户端连接成功");

        // 接受和发送消息
        while (true)
        {
            iResult = recv(ClientSocket, recvbuf, recvbuflen - 1, 0);
            if (iResult > 0)
            {
                recvbuf[iResult] = '\0'; // 对接收到的数据进行 Null 终止

                m_printf("Enter message to send: ");

                cin.getline(sendbuf, DEFAULT_BUFLEN);
                strncat_s(sendbuf, DEFAULT_BUFLEN, "\n", _TRUNCATE);
                m_printf("3秒钟后开始拨号，请做好准备");
                Sleep(3000);
                iResult = send(ClientSocket, sendbuf, (int)strlen(sendbuf), 0);

                if (iResult == SOCKET_ERROR)
                {
                    printf("send failed: %d\n", WSAGetLastError());
                    break;
                }
            }
            else if (iResult == 0)
            {
                m_printf("客户端断开连接...");
                break; // 跳出接收/发送循环，等待新的连接
            }
            else
            {
                printf("%s 收消息错误: %d", getTime(), WSAGetLastError());
                break;
            }
        }

        // 关闭当前客户端的连接，准备接受下一个连接
        closesocket(ClientSocket);
    }

    // 不再需要服务器套接字
    closesocket(ListenSocket);
    WSACleanup();

    return 0;
}

const char* TcpServer::getTime()
{
    static char buf[80]; // 静态数组用于存储日期时间字符串
    time_t now = time(0);
    struct tm tstruct;

    localtime_s(&tstruct, &now);

    strftime(buf, sizeof(buf), "[%Y-%m-%d %X]", &tstruct);

    return buf;
}

void TcpServer::m_printf(const char* msg)
{
    printf("%s %s\n", getTime(), msg);
}
