package io.github.yarunkan.sc.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Main {
    private final static String CLIENT_NAME_SEPARATOR = ": ";
    private String currentClientName;

    public static void main(String[] args) {
        new Main().start(args);
    }

    public void start(String[] args) {
        System.out.println("Usage: java SocketChat <host-name> <port-number> <user-name>\n");

        if (!validateArgs(args)) {
            System.exit(0);
        }

        final int port = Integer.parseInt(args[1]);

        if (port < 0 || port > 65535) {
            System.out.println("A port number should not be less than 0 or more than 65535");
            System.exit(0);
        }

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

    private Thread writeMessage(BufferedWriter currentClientWriter, BufferedReader currentClientReader) {
        return new Thread(() -> {
            try {
                while (true) {
                    final String currentClientIdentifier = currentClientName + CLIENT_NAME_SEPARATOR;

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
                    final String currentClientIdentifier = currentClientName + CLIENT_NAME_SEPARATOR;

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