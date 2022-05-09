package simplecloud;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ServerThread implements Runnable {

    //private static final List<String> usernames = new ArrayList<>();
    private static final List<String> usernames = Collections.synchronizedList(new ArrayList<String>());

    private final Socket connectionSocket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private boolean authorized;
    private String username;

    private final String pathToStore = "storage\\";
    
    public ServerThread(Socket connectionSocket) throws IOException {
        this.connectionSocket = connectionSocket;
        out = new DataOutputStream(connectionSocket.getOutputStream());
        in = new DataInputStream(connectionSocket.getInputStream());
        authorized = false;
        username = null;
    }
    
    @Override
    public void run() {
        System.out.println("Client connected: <" + connectionSocket.getInetAddress() + ">");
        try {
            while (!authorized) {
                int command = in.readInt();
                if (command == Server.MSG_AUTHORIZE) {
                    String requestedUsername = in.readUTF();
                    if (!usernames.contains(requestedUsername)) {
                        out.writeInt(Server.MSG_VALID);
                        username = requestedUsername;
                        authorized = true;
                    }
                    else {
                        out.writeInt(Server.MSG_INVALID);
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Authorization of client <" + connectionSocket.getInetAddress() + "> failed");
            e.printStackTrace();
            return;
        }
        System.out.println("Authorized <" + connectionSocket.getInetAddress() + "> under username \"" + username + "\"");
        usernames.add(username);
        try {
            while (true) {
                //System.out.println("Listening ...");
                int command = in.readInt();
                
                if (command == Server.MSG_DOWNLOAD) {
                    String fileName = in.readUTF();
                    System.out.println("Download request: " + fileName);
                    File file = new File(pathToStore + "\\" + fileName);
                    if (!file.exists()) {
                        System.out.println("Requested file not found.");
                        out.writeInt(Server.MSG_INVALID);
                        out.flush();
                    }
                    else {
                        out.writeInt(Server.MSG_VALID);
                        out.flush();
                        System.out.println("Sending file ...");
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
                        System.out.println("File sent.");
                        fileReader.close();
                    }
                }
                
                else if (command == Server.MSG_UPLOAD) {
                    String fileName = in.readUTF();
                    System.out.println("Upload request: " + fileName);
                    File file = new File(pathToStore + "\\" + fileName);
                    if (file.exists()) {
                        System.out.println("Upload rejected due to the name conflict.");
                        out.writeInt(Server.MSG_INVALID);
                        out.flush();
                    } else {
                        out.writeInt(Server.MSG_VALID);
                        out.flush();
                        FileOutputStream fileWriter = new FileOutputStream(file);
                        long length = in.readLong();
                        System.out.println("Receiving started ...");
                        int iterNum = (int) (length / Server.BUFFER_LEN);
                        int remaining = (int) (length - iterNum * Server.BUFFER_LEN);
                        int bytesred = -1488;
                        int bytesDownloaded = 0;
                        for (int i = 0; bytesDownloaded < length && i < 9000; i++) {
                            byte[] buffer = new byte[Server.BUFFER_LEN];
                            try {
                                bytesred = in.read(buffer);
                                //System.out.println("Upload iteration " + i + ", " + bytesred + " bytes red");
                            }
                            catch (IOException ioe) {
                                //System.out.println("Upload iteration " + i + ", IOException, " + bytesred + " bytes red");
                                System.out.println("IOException: " + ioe);
                            }
                            fileWriter.write(buffer, 0, bytesred);
                            bytesDownloaded += bytesred;
                        }
                        fileWriter.close();
                        System.out.println("File received.");
                    }
                }

                else if (command == Server.MSG_LIST) {
                    File folder = new File(pathToStore);
                    File[] listOfFiles = folder.listFiles();
                    int filesNumber = -1;
                    if (listOfFiles != null)
                        filesNumber = listOfFiles.length;
                    out.writeInt(filesNumber);

                    for (int i = 0; i < filesNumber; i++) {
                        if (listOfFiles[i].isFile()) {
                            out.writeBytes("file " + listOfFiles[i].getName()+"\n");
                        } else if (listOfFiles[i].isDirectory()) {
                            out.writeBytes("dir  " + listOfFiles[i].getName()+"\n");
                        }
                    }
                }
                
                else if (command == Server.MSG_QUIT) {
                    throw new IOException();
                }
                
                else {
                    System.out.println("Client \"" + username + "\" sent invalid command.");
                }
            }
        } catch (IOException e) {
            usernames.remove(username);
            System.out.println("Client \"" + username + "\" disconnected");
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) { }
        }
    }
    
    public static void startServer(int port) {
        ServerSocket serverSocket;
        try {
            System.out.println("Starting the server on port " + port + " ...");
            serverSocket = new ServerSocket(port);
            System.out.println("Server started. Press Ctrl+C to stop the server.");
            System.out.println("Waiting for connections ...");
        } catch (IOException e) {
            if (e.getMessage().contains("Permission denied")) {
                System.out.println("You should run the program as administrator "
                                       + "(superuser) to start a server.");
            } else {
                System.out.println("A network error occured during the initialization.");
            }
            return;
        }
        
        while (true) {
            try {
                Socket connectionSocket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(connectionSocket);
                Thread thread = new Thread(serverThread);
                thread.start();
            } catch (IOException e) {
                System.out.println("An error occured while a client is connecting.");
            }
        }
    }
    
}
