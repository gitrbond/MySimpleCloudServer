package simplecloud;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Client {
    public static final int OP_NOP = 0;
    public static final int OP_CONNECT = 3;
    public static final int OP_SENDTEXT = 2;
    public static final int OP_SENDFILE = 3;
    public static final int OP_GETFILE = 4;
    public static final int OP_SYNC = 5;

    private final Socket socket;
    private static DataInputStream in = null;
    private static DataOutputStream out = null;
    private static String pathToStore = null;

    private static volatile boolean authorized = false;
    private String username;

    private static final List<String> chat = Collections.synchronizedList(new ArrayList<>());
    private static volatile int operation = OP_NOP;
    private static volatile String param = null;
    private static volatile boolean quit = false;

    private static final String pathToClientProps = "client-properties.txt";

    private Client(String ip, int port) throws IOException {
        System.out.println("Connecting to " + ip + ":" + port + " ...");
        socket = new Socket(ip, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public static void main(String[] args) {
        long port = 0;
        String ip = "";
        try (FileReader reader = new FileReader(pathToClientProps)) {
            System.out.println("reading parameters from " + pathToClientProps);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            port = (long) jsonObject.get("port");
            ip = (String) jsonObject.get("server-ip");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        Client client = null;
        try {
            client = new Client(ip, (int) port);
            System.out.println("Connected.");
        } catch (IOException e) {
            System.out.println("Connection failed.");
            return;
        }

        Thread inputThread = new Thread(client::executeCommands);
        inputThread.start();


        System.out.println("Commands: \"connect <username>\", \"send file "
                + "<filename>\", \"send text <text>\", \"quit\"");
        Scanner scan = new Scanner(System.in);
        while (!quit) {
            String command = scan.nextLine();
            if (authorized) {
                if (command.startsWith("send text ")) {
                    while (true) {
                        if (operation == OP_NOP) {
                            operation = OP_SENDTEXT;
                            param = command.substring("send text ".length()).stripLeading();
                            break;
                        }
                    }
                } else if (command.startsWith("send file ")) {
                    while (true) {
                        if (operation == OP_NOP) {
                            operation = OP_SENDFILE;
                            param = command.substring("send file ".length()).stripLeading();
                            break;
                        }
                    }
                } else if (command.startsWith("quit")) {
                    quit = true;
                } else {
                    System.out.println("Please enter a valid command.");
                }
            } else if (command.startsWith("connect ")) {
                while (true) {
                    if (operation == OP_NOP) {
                        operation = OP_CONNECT;
                        String[] splitted = command.split(" ", 0);
                        param = splitted[1];
                        break;
                    }
                }
            } else if (command.startsWith("quit")) {
                quit = true;
            } else {
                System.out.println("please authorize to send commands");
            }
        }
    }

    private void executeCommands() {
        while (!quit) {
            //System.out.println("op = " + operation);
            if (authorized && operation == OP_NOP) {
                operation = OP_SYNC;
                syncChat();
                operation = OP_NOP;
            }
            if (operation == OP_CONNECT) {
                authorize(param);
                operation = OP_NOP;
            } else if (operation == OP_SENDTEXT) {
                say(param);
                operation = OP_NOP;
            } else if (operation == OP_SENDFILE) {
                try {
                    upload(param);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                operation = OP_NOP;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void authorize(String requestedUsername) {
        //System.out.println("authorizing...");
        try {
            out.writeInt(Server.MSG_AUTHORIZE);
            out.flush();
            out.writeUTF(requestedUsername);
            out.flush();
            int response = in.readInt();
            if (response == Server.MSG_INVALID) {
                System.out.println("This username is already taken, choose another one");
            }
            if (response == Server.MSG_VALID) {
                authorized = true;
                username = requestedUsername;
                pathToStore = username + "_files";
            }
            //System.out.println("Successfully authorized.");
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
    }

    private void syncChat() {
        //System.out.println("Syncing...");
        int clientChatPosition = chat.size();
        try {
            out.writeInt(Server.MSG_SYNC);
            out.writeInt(clientChatPosition);
            out.flush();
        } catch (IOException ioe) {
            System.out.println("Unable to send command, IOException: " + ioe);
            return;
        }
        ArrayList<String> filesToLoad = new ArrayList<>();
        try {
            int response = in.readInt();
            if (response == Server.MSG_VALID) {
                int receiveNumber = in.readInt();
                //System.out.println("Receiving " + receiveNumber + " messages");
                for (int i = 0; i < receiveNumber; i++) {
                    int length = in.readInt();
                    byte[] data = new byte[length];
                    in.readFully(data);
                    String chatString = new String(data, StandardCharsets.UTF_8);
                    if (chatString.startsWith("file upload by @")) {
                        String[] splitted = chatString.split(" ", 0);
                        String fileName = splitted[splitted.length - 1];
                        filesToLoad.add(fileName);
                    }
                    System.out.println(chatString);
                    chat.add(chatString);
                }
            } else {
                //System.out.println("Nothing to sync");
            }
        } catch (IOException ioe) {
            System.out.println("Unable to read response from server, IOException: " + ioe);
            return;
        }
        try {
            for (String fileName : filesToLoad) {
                download(fileName);
            }
        } catch (IOException ioe) {
            System.out.println("Unable to load some files from server during SYNC, IOException: " + ioe);
        }
    }

    private void say(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        try {
            out.writeInt(Server.MSG_SAY);
            out.writeInt(data.length);
            out.flush();
        } catch (IOException ioe) {
            System.out.println("Unable to send command, IOException: " + ioe);
            return;
        }
        int response;
        try {
            response = in.readInt();
        } catch (IOException ioe) {
            System.out.println("Unable to read response from server, IOException: " + ioe);
            return;
        }
        try {
            if (response == data.length) {
                out.writeInt(Server.MSG_VALID);
                out.flush();
                out.write(data);
            } else {
                out.writeInt(Server.MSG_INVALID);
            }
            out.flush();
        } catch (IOException ioe) {
            System.out.println("Unable to send command, IOException: " + ioe);
        }
    }

    private void download(String fileName) throws IOException {
        File file = new File(pathToStore + "\\" + fileName);
        if (file.exists()) {
            return;
        }
        //System.out.println("Requesting download for: " + fileName);
        out.writeInt(Server.MSG_DOWNLOAD);
        out.flush();
        out.writeUTF(fileName);
        out.flush();
        int response = in.readInt();
        if (response == Server.MSG_INVALID) {
            System.out.println("This file doesn't exists on the server's working directory.");
        } else if (response == Server.MSG_VALID) {
            FileOutputStream fileWriter = new FileOutputStream(file);
            int length = in.readInt();
            //System.out.println("Download of " + length + " bytes started ...");
            int iterNum = (length / Server.BUFFER_LEN);
            int remaining = (length - iterNum * Server.BUFFER_LEN);
            int bytesred = -1488;
            int bytesDownloaded = 0;
            for (int i = 0; bytesDownloaded < length && i < 9000; i++) {
                byte[] buffer = new byte[Server.BUFFER_LEN];
                try {
                    bytesred = in.read(buffer);
                    //System.out.println("Download iteration " + i + ", " + bytesred + " bytes red");
                } catch (IOException e) {
                    //System.out.println("Download iteration " + i + ", IOException, " + bytesred + " bytes red");
                }
                fileWriter.write(buffer, 0, bytesred);
                bytesDownloaded += bytesred;
            }
            fileWriter.close();
            //System.out.println("File downloaded.");
        } else {
            System.out.println("Unexpected response came from the server: " + response);
        }
    }

    private void upload(String fileName) throws IOException {
        //System.out.println("Uploading the file to the server: " + fileName);
        File file = new File(pathToStore + "\\" + fileName);
        if (!file.exists()) {
            System.out.println("File not found on the working directory.");
        } else {
            out.writeInt(Server.MSG_UPLOAD);
            out.flush();
            out.writeUTF(fileName);
            out.flush();
            int response = in.readInt();
            if (response == Server.MSG_INVALID) {
                System.out.println("You can't upload this file since there is a "
                        + "file with the same name on the server.");
            } else if (response == Server.MSG_VALID) {
                //System.out.println("Upload started ...");
                FileInputStream fileReader = new FileInputStream(file);
                long length = file.length();
                out.writeLong(length);
                out.flush();
                int iterNum = (int) (length / Server.BUFFER_LEN);
                int remaining = (int) (length - iterNum * Server.BUFFER_LEN);
                for (int i = 0; i < iterNum; i++) {
                    byte[] buffer = new byte[Server.BUFFER_LEN];
                    fileReader.read(buffer);
                    out.write(buffer);
                }
                byte[] rembuffer = new byte[remaining];
                fileReader.read(rembuffer);
                out.write(rembuffer);
                out.flush();
                fileReader.close();
                //System.out.println("File uploaded.");
            } else {
                System.out.println("Unexpected response came from the server: " + response);
            }
        }
    }

}
