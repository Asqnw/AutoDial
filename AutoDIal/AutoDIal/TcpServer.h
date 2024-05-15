#pragma once
#undef UNICODE

#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <thread>

using namespace std;

#pragma comment (lib, "Ws2_32.lib")

#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "12453"
class TcpServer
{
	std::atomic<bool> isConnected;
public:
	int __cdecl startServer();
private:
	const char* getTime();
	void m_printf(const char* msg);
	void handleUserInput(SOCKET ClientSocket);
};

