package io.github.yarunkan.sc.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main {
    private final List<Socket> clientSocketList = new ArrayList<>();
    private volatile boolean isClientConnected;

    public static void main(String[] args) {
        new Main().start(args);
    }

    public void start(String[] args) {

        if (!validateArgs(args)) {
            System.exit(0);
        }

        final int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
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
            System.out.println("Usage: java Main <port-number>");
            return false;
        }

        for (int i = 0; i < args[0].length(); i++) {
            final char portChar = args[0].charAt(i);

            if (portChar < '0' || portChar > '9') {
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
                    clientSocketList.add(serverSocket.accept());
                    isClientConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Thread broadcastMessage() {
        return new Thread(() -> {
            if (clientSocketList.isEmpty()) {
                suspendBroadcast();
            }

            int clientSocketSetSize = clientSocketList.size();

            for (Iterator<Socket> clientSocketIterator = clientSocketList.listIterator(); clientSocketIterator.hasNext(); ) {
                final Socket clientSocket = clientSocketIterator.next();

                new Thread(() -> {
                    try (BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        while (true) {
                            final String message = clientReader.readLine();

                            final List<Socket> clientReceiverSet = new ArrayList<>(clientSocketList);
                            clientReceiverSet.remove(clientSocket);

                            for (Socket clientReceiver : clientReceiverSet) {
                                final BufferedWriter clientWriter;
                                clientWriter = new BufferedWriter(new OutputStreamWriter(clientReceiver.getOutputStream()));
                                clientWriter.write(message + "\n");
                                clientWriter.flush();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                if (!clientSocketIterator.hasNext()) {
                    suspendBroadcast();
                    clientSocketIterator = clientSocketList.listIterator(clientSocketSetSize - 1);
                    clientSocketSetSize = clientSocketList.size();
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
}