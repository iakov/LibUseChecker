library JavaNet;

types {
    ServerSocket(java.net.ServerSocket);
    SocketAddress(java.net.SocketAddress);
    InetSocketAddress(java.net.InetSocketAddress);
    Socket(java.net.Socket);
    PORT(int);
}

automaton AServerSocket {
    state Created;
    state Binded;
    state Accepted;
    finishstate Closed;

    shift Created->Binded (bind);
    shift Binded->Accepted (accept);
    shift Accepted->self (accept);
    shift any->Closed (close);
}

automaton ASocket {
    state Created;
    finishstate Closed;

    shift Created->Closed (close);
}

fun ServerSocket.ServerSocket(): ServerSocket {
    result = new AServerSocket(Created);
}

fun ServerSocket.ServerSocket(port: PORT): ServerSocket {
    requires port >= 0 && port <= 65535;
    result = new AServerSocket(Binded);
}

fun InetSocketAddress.InetSocketAddress(port: PORT) {
    requires port >= 0 && port <= 65535;
}

fun ServerSocket.bind(sockAddr: SocketAddress);

fun ServerSocket.accept(): Socket {
    result = new ASocket(Created);
}

fun Socket.close();

fun ServerSocket.close();
