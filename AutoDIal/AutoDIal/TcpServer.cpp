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
    // ���� Winsock
    iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (iResult != 0)
    {
        printf("WSAStartup failed: %d\n", iResult);
        return 1;
    }
    m_printf("����Winsock�ɹ�");

    ZeroMemory(&hints, sizeof(hints));
    hints.ai_family = AF_INET;          // IPv4 address family
    hints.ai_socktype = SOCK_STREAM;    // Stream socket
    hints.ai_protocol = IPPROTO_TCP;    // TCP
    hints.ai_flags = AI_PASSIVE;        // For wildcard IP address

    // ����������Ҫʹ�õı��ص�ַ�Ͷ˿�
    iResult = getaddrinfo(NULL, DEFAULT_PORT, &hints, &result);
    if (iResult != 0)
    {
        printf("getaddrinfo failed: %d\n", iResult);
        WSACleanup();
        return 1;
    }
    m_printf("����������ʹ�õ������ַ��˿ڳɹ�");

    // ���� SOCKET �������ͻ�������
    ListenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
    if (ListenSocket == INVALID_SOCKET)
    {
        printf("socket failed: %ld\n", WSAGetLastError());
        freeaddrinfo(result);
        WSACleanup();
        return 1;
    }
    m_printf("�����ͻ������ӵ��׽��ִ����ɹ�");

    // ���׽���
    iResult = bind(ListenSocket, result->ai_addr, (int)result->ai_addrlen);
    if (iResult == SOCKET_ERROR)
    {
        printf("bind failed: %d\n", WSAGetLastError());
        freeaddrinfo(result);
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }
    m_printf("�󶨼����ͻ������ӵ��׽��ֳɹ�");

    freeaddrinfo(result);

    // �����׽����ϵĿͻ�������
    iResult = listen(ListenSocket, SOMAXCONN);
    if (iResult == SOCKET_ERROR)
    {
        printf("listen failed: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    while (true) // ��ѭ����������ܶ���ͻ���
    {
        // ���ܿͻ����׽���
        m_printf("���ڵȴ��û�����...");
        ClientSocket = accept(ListenSocket, NULL, NULL);
        if (ClientSocket == INVALID_SOCKET)
        {
            printf("accept failed: %d\n", WSAGetLastError());
            closesocket(ListenSocket);
            WSACleanup();
            return 1;
        }
        m_printf("�ͻ������ӳɹ�");

        // ���ܺͷ�����Ϣ
        while (true)
        {
            iResult = recv(ClientSocket, recvbuf, recvbuflen - 1, 0);
            if (iResult > 0)
            {
                recvbuf[iResult] = '\0'; // �Խ��յ������ݽ��� Null ��ֹ

                m_printf("Enter message to send: ");

                cin.getline(sendbuf, DEFAULT_BUFLEN);
                strncat_s(sendbuf, DEFAULT_BUFLEN, "\n", _TRUNCATE);
                m_printf("3���Ӻ�ʼ���ţ�������׼��");
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
                m_printf("�ͻ��˶Ͽ�����...");
                break; // ��������/����ѭ�����ȴ��µ�����
            }
            else
            {
                printf("%s ����Ϣ����: %d", getTime(), WSAGetLastError());
                break;
            }
        }

        // �رյ�ǰ�ͻ��˵����ӣ�׼��������һ������
        closesocket(ClientSocket);
    }

    // ������Ҫ�������׽���
    closesocket(ListenSocket);
    WSACleanup();

    return 0;
}

const char* TcpServer::getTime()
{
    static char buf[80]; // ��̬�������ڴ洢����ʱ���ַ���
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
