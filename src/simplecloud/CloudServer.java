package simplecloud;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class CloudServer implements Runnable {
    
    private final Socket connectionSocket;
    private final DataOutputStream out;
    private final DataInputStream in;

    private final String pathToStore = "C:\\Users\\user\\MySimpleCloudServer\\storage";
    
    public CloudServer(Socket connectionSocket) throws IOException {
        this.connectionSocket = connectionSocket;
        this.out = new DataOutputStream(connectionSocket.getOutputStream());
        this.in = new DataInputStream(connectionSocket.getInputStream());
    }
    
    @Override
    public void run() {
        System.out.println("Client connected: <" + connectionSocket.getInetAddress() + ">");
        try {
            while (true) {
                System.out.println("Listening ...");
                int command = in.readInt();
                
                if (command == SimpleCloud.MSG_DOWNLOAD) {
                    String fileName = in.readUTF();
                    System.out.println("Download request: " + fileName);
                    File file = new File(pathToStore + "\\" + fileName);
                    if (!file.exists()) {
                        System.out.println("Requested file not found.");
                        out.writeInt(SimpleCloud.MSG_INVALID);
                        out.flush();
                    }
                    else {
                        out.writeInt(SimpleCloud.MSG_VALID);
                        out.flush();
                        System.out.println("Sending file ...");
                        FileInputStream fileReader = new FileInputStream(file);
                        long length = file.length();
                        out.writeLong(length);
                        out.flush();
                        int iterNum = (int) (length / SimpleCloud.BUFFER_LEN);
                        int remaining = (int) (length - iterNum * SimpleCloud.BUFFER_LEN);
                        for (int i = 0; i < iterNum; i++) {
                            byte[] buffer = new byte[SimpleCloud.BUFFER_LEN];
                            fileReader.read(buffer);
                            out.write(buffer);
                        }
                        for (int i = 0; i < remaining; i++) {
                            out.write(fileReader.read());
                        }
                        out.flush();
                        fileReader.close();
                        System.out.println("File sent.");
                    }
                }
                
                else if (command == SimpleCloud.MSG_UPLOAD) {
                    String fileName = in.readUTF();
                    System.out.println("Upload request: " + fileName);
                    File file = new File(pathToStore + "\\" + fileName);
                    if (file.exists()) {
                        System.out.println("Upload rejected due to the name conflict.");
                        out.writeInt(SimpleCloud.MSG_INVALID);
                        out.flush();
                    } else {
                        out.writeInt(SimpleCloud.MSG_VALID);
                        out.flush();
                        FileOutputStream fileWriter = new FileOutputStream(file);
                        long length = in.readLong();
                        System.out.println("Receiving started ...");
                        int iterNum = (int) (length / SimpleCloud.BUFFER_LEN);
                        int remaining = (int) (length - iterNum * SimpleCloud.BUFFER_LEN);
                        for (int i = 0; i < iterNum; i++) {
                            byte[] buffer = new byte[SimpleCloud.BUFFER_LEN];
                            in.read(buffer);
                            fileWriter.write(buffer);
                        }
                        for (int i = 0; i < remaining; i++) {
                            fileWriter.write(in.read());
                        }
                        fileWriter.close();
                        System.out.println("File received.");
                    }
                }

                else if (command == SimpleCloud.MSG_LIST) {
                    File folder = new File(pathToStore);
                    File[] listOfFiles = folder.listFiles();

                    out.writeInt(listOfFiles.length);

                    for (int i = 0; i < listOfFiles.length; i++) {
                        //System.out.println(".");
                        if (listOfFiles[i].isFile()) {
                            out.writeBytes("file " + listOfFiles[i].getName());
                        } else if (listOfFiles[i].isDirectory()) {
                            out.writeBytes("dir  " + listOfFiles[i].getName());
                        }
                    }
                }
                
                else if (command == SimpleCloud.MSG_QUIT) {
                    throw new IOException();
                }
                
                else {
                    System.out.println("Client <" + connectionSocket.getInetAddress() 
                                           + "> sent invalid command.");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: <"
                + connectionSocket.getInetAddress() + ">");
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
                CloudServer cloudServer = new CloudServer(connectionSocket);
                Thread thread = new Thread(cloudServer);
                thread.start();
            } catch (IOException e) {
                System.out.println("An error occured while a client is connecting.");
            }
        }
    }
    
}
