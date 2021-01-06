package io.github.yarunkan.sc.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Main {
    private final String clientNameSeparator = ": ";
    private String currentClientName;

    public static void main(String[] args) {
        new Main().start(args);
    }

    private boolean validateArgs(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java SocketChat <host-name> <port-number> <user-name>");
            return false;
        }

        for (int i = 0; i < args[1].length(); i++) {
            final char portChar = args[1].charAt(i);

            if (portChar < '0' || portChar > '9') {
                System.out.println("<port-number> must contain only digits");
                return false;
            }
        }

        return true;
    }

    public void start(String[] args) {
        System.out.println("Usage: java SocketChat <host-name> <port-number> <user-name>\n");

        if (!validateArgs(args)) {
            System.exit(0);
        }

        final int port = Integer.parseInt(args[1]);
        currentClientName = args[2];

        try (Socket clientSocket = new Socket(args[0], port);
             BufferedWriter currentClientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
             BufferedReader otherClientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedReader currentClientReader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("WELCOME TO THE SOCKET CHAT\n");

            final Thread currentClientThread = writeMessage(currentClientWriter, currentClientReader);
            final Thread otherClientThread = readMessage(otherClientReader);

            currentClientThread.start();
            otherClientThread.start();

            currentClientThread.join();
            otherClientThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Thread writeMessage(BufferedWriter currentClientWriter, BufferedReader currentClientReader) {
        return new Thread(() -> {
            try {
                while (true) {
                    final String currentClientIdentifier = currentClientName + clientNameSeparator;

                    System.out.print(currentClientIdentifier);

                    final String message = currentClientReader.readLine();

                    if (message.equals("")) {
                        continue;
                    }

                    currentClientWriter.write(currentClientIdentifier + message + "\n");
                    currentClientWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Thread readMessage(BufferedReader otherClientReader) {
        return new Thread(() -> {
            try {
                while (true) {
                    final String message = otherClientReader.readLine();
                    final String currentClientIdentifier = currentClientName + clientNameSeparator;

                    for (int i = 0; i < currentClientIdentifier.length(); i++) {
                        System.out.print("\b");
                    }

                    System.out.println(message);
                    System.out.print(currentClientIdentifier);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}