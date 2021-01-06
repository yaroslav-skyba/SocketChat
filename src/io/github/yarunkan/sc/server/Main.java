package io.github.yarunkan.sc.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private final List<Socket> clientSocketList = new ArrayList<>();
    private volatile boolean isClientConnected;

    public static void main(String[] args) {
        new Main().start(args);
    }

    public void start(String[] args) {
        System.out.println("Usage: java SocketChat <port-number>\n");

        if (!validateArgs(args)) {
            System.exit(0);
        }

        final int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("The server is listening on the port: " + port + "\n");

            final Thread connectClientsThread = connectClients(serverSocket);
            connectClientsThread.start();

            final Thread broadcastMessageThread = broadcastMessage();
            broadcastMessageThread.start();

            connectClientsThread.join();
            broadcastMessageThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean validateArgs(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SocketChat <port-number>");
            return false;
        }

        for (int i = 0; i < args[0].length(); i++) {
            final char portChar = args[0].charAt(i);

            if (portChar < '0') {
                System.out.println("<port-number> must contain only digits");
                return false;
            } else if (portChar > '9') {
                System.out.println("<port-number> must contain only digits");
                return false;
            }
        }

        return true;
    }

    private Thread connectClients(ServerSocket serverSocket) {
        return new Thread(() -> {
            while (true) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    clientSocketList.add(clientSocket);

                    resumeBroadcast();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void resumeBroadcast() {
        isClientConnected = true;
    }

    private Thread broadcastMessage() {
        return new Thread(() -> {
            if (clientSocketList.isEmpty()) {
                suspendBroadcast();
            }

            final AtomicInteger clientSocketListSize = new AtomicInteger(clientSocketList.size());
            final Collection<String> messageCollection = new ArrayList<>();

            Iterator<Socket> clientSocketIterator = clientSocketList.listIterator();

            while (true) {
                final Socket clientSocket = clientSocketIterator.next();

                final Thread clientThread = new Thread(() -> {
                    try (BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        for (String message : messageCollection) {
                            writeClient(clientSocket, message);
                        }

                        while (true) {
                            final String message = clientReader.readLine();
                            messageCollection.add(message);

                            System.out.println(message);

                            final Collection<Socket> clientReceiverList = new ArrayList<>(clientSocketList);
                            clientReceiverList.remove(clientSocket);

                            for (Socket clientReceiver : clientReceiverList) {
                                writeClient(clientReceiver, message);
                            }
                        }
                    } catch (IOException e) {
                        clientSocketList.removeIf(Socket::isClosed);
                        clientSocketListSize.getAndDecrement();
                    }
                });

                clientThread.start();

                if (!clientSocketIterator.hasNext()) {
                    suspendBroadcast();
                    clientSocketIterator = clientSocketList.listIterator(clientSocketListSize.get());
                    clientSocketListSize.set(clientSocketList.size());
                }
            }
        });
    }

    private void suspendBroadcast() {
        isClientConnected = false;

        while (!isClientConnected) {
            Thread.onSpinWait();
        }
    }

    private void writeClient(Socket clientSocket, String message) throws IOException {
        final BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        clientWriter.write(message + "\n");
        clientWriter.flush();
    }
}