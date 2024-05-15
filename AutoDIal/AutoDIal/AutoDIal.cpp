#include <winsock2.h>
#include <iostream>
#include "TcpServer.h"
#pragma comment(lib, "Ws2_32.lib")
#define BUFFER_SIZE 255;

std::string WideStringToString(const std::wstring& wstr)
{
    if (wstr.empty()) return std::string();
    int sizeNeeded = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
    std::string strTo(sizeNeeded, 0);
    WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], sizeNeeded, NULL, NULL);
    return strTo;
}

void findServer()
{
    WSADATA wsaData;
    SOCKET udpSocket;
    struct sockaddr_in serverAddr, clientAddr;

    int result= WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (result != 0) {
        std::cerr << "WSAStartup failed: " << result << std::endl;
        // 在这里处理错误，例如退出程序
        return; // 或者其他适当的错误处理代码
    }
    udpSocket = socket(AF_INET, SOCK_DGRAM, 0);

    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(11000);
    serverAddr.sin_addr.s_addr = INADDR_ANY;

    bind(udpSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr));

    char buffer[1024];
    int clientAddrSize = sizeof(clientAddr);

    while (true)
    {
        int received = recvfrom(udpSocket, buffer, 1024, 0, (struct sockaddr*)&clientAddr, &clientAddrSize);
        std::string receivedData(buffer, received);
        if (receivedData == "Asqnw_AutoDial_FindServer")
        {
            std::string response;
            WCHAR UserName[255];
            DWORD size = sizeof(UserName) / sizeof(UserName[0]);
            if (GetUserNameW(UserName, &size))
            {
                std::wstring wUserName(UserName);
                response = "Server System User Name:" + WideStringToString(wUserName);
            }
            else
            {
                response = "Server System User Name:Unknown";
            }
            sendto(udpSocket, response.c_str(), static_cast<int>(response.length()), 0, (struct sockaddr*)&clientAddr, clientAddrSize);
            break;
        }
    }

    closesocket(udpSocket);
    WSACleanup();
}

int main()
{
    printf("欢迎使用影幽网络工作室产品——AutoDial自动拨号\n现在准备发送UDP广播，请打开客户端配对设备\n");
    findServer();
    printf("已被设备搜索，广播停止\n");
    TcpServer tcpServer;
    tcpServer.startServer();
    return 0;
}
