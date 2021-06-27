package org.mariarheon.libusechecker2;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Retriever extends Thread {
    private final SLVerifier verifier;
    private final StringConsumer consumer;
    private ServerSocket serverSocket;
    private Runnable afterFinishFunc;

    public Retriever(SLVerifier verifier, StringConsumer consumer) {
        this.verifier = verifier;
        this.consumer = consumer;
    }

    public void closeSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                /* ignored */
            }
        }
    }

    public void whenFinished(Runnable afterFinishFunc) {
        this.afterFinishFunc = afterFinishFunc;
    }

    public void run() {
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(12213);
            socket = serverSocket.accept();
            consumer.consume("Tested project connected");
            var input = new DataInputStream(socket.getInputStream());
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                String json;
                try {
                    json = input.readUTF();
                } catch (SocketException ex) {
                    System.out.println("SocketException (due to tested project was closed): " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
                var methodInfo = new Gson().fromJson(json, MethodInfo.class);
                if (methodInfo.isDestroyedSignal()) {
                    verifier.killAutomaton(methodInfo.getTargetId());
                } else {
                    verifier.methodIntercepted(methodInfo);
                }
            }
            verifier.checkEndState();
        } catch (Exception ex) {
            ex.printStackTrace();
            consumer.consume("Some error occured: " + ex.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    /* ignored */
                }
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    /* ignored */
                }
            }
            if (afterFinishFunc != null) {
                afterFinishFunc.run();
            }
        }
    }
}
