library JavaNet;

types {
    ServerSocket(java.net.ServerSocket);
    SocketAddress(java.net.SocketAddress);
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
    shift any->Closed (close);
}

automaton ASocket {
    state Created;
    finishstate Closed;

    shift Created->Closed (close);
}

fun ServerSocket.ServerSocket(): ServerSocket {
    result = new AServerSocket(Created);
    post("ONE", "After creating ServerSocket via ServerSocket()-noargs-constructor, it should not be bound",
        !result.isBound());
}

fun ServerSocket.ServerSocket(port: PORT): ServerSocket {
    requires port > 0 && port <= 65535;
    // pre("TWO", ("Port value cannot be negative. You used %s", port), port >= 0);
    post("THREE", "After creating ServerSocket via ServerSocket(port)-constructor, it should become bound",
        result.isBound());
    result = new AServerSocket(Binded);
}

fun ServerSocket.bind(sockAddr: SocketAddress) {
    pre("FOUR", "Port value cannot be negative", ((java.net.InetSocketAddress)sockAddr).getPort() >= 0);
}

fun ServerSocket.accept(): Socket {
    result = new ASocket(Created);
}

fun Socket.close();

fun ServerSocket.close();
