package simplecloud;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


public class CloudClient {
    
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    
    public static void startClient(String ip, int port) {
        CloudClient client = null;
        try {
            client = new CloudClient(ip, port);
        } catch (IOException e) {
            System.out.println("Connection failed.");
            return;
        }
        client.takeCommands();
    }
    
    private CloudClient(String ip, int port) throws IOException {
        System.out.println("Connecting to " + ip + ":" + port + " ...");
        socket = new Socket(ip, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }
    
    private void takeCommands() {
        Scanner scan = new Scanner(System.in);
        try {
            System.out.println("Connected.");
            System.out.println("Commands: \"DOWNLOAD <filename>\", \"UPLOAD "
                                   + "<filename>\", \"LIST\", \"QUIT\"");
            String command = scan.next();
            while (!command.equalsIgnoreCase("QUIT")) {
                if (command.equalsIgnoreCase("DOWNLOAD")) {
                    String fileName = scan.next();
                    download(fileName);
                }
                else if (command.equalsIgnoreCase("UPLOAD")) {
                    String fileName = scan.next();
                    upload(fileName);
                }
                else if (command.equalsIgnoreCase("LIST")) {
                    listFiles();
                }
                else {
                    System.out.println("Please enter a valid command.");
                }
                command = scan.next();
            }
            out.writeInt(SimpleCloud.MSG_QUIT);
            out.flush();
        } catch (IOException e) {
            System.out.println("Disconnected from the server.");
        }
    }

    private void listFiles() {
        int fileNumber = -1;
        try {
            fileNumber = in.readInt();
        }
        catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        if (fileNumber < 0)
            System.out.println("Some error occured, \"fileNumber < 0\"");
        else if (fileNumber == 0)
            System.out.println("Server directory empty, no files stored");
        else {
            System.out.println("List of files on the server:");
            String tmp;
            try {
                for (int i = 0; i < fileNumber && (tmp = in.readLine()) != null; i++) {
                    //inputLine.append(tmp);
                    System.out.println(tmp);
                }
            } catch (IOException ioe) {
                System.out.println("IOException: " + ioe);
            }
        }
    }

    private void download(String fileName) throws IOException {
        System.out.println("Requesting download for: " + fileName);
        out.writeInt(SimpleCloud.MSG_DOWNLOAD);
        out.flush();
        out.writeUTF(fileName);
        out.flush();
        int response = in.readInt();
        if (response == SimpleCloud.MSG_INVALID) {
            System.out.println("This file doesn't exists on the server's working directory.");
        } else if (response == SimpleCloud.MSG_VALID) {
            File file = new File(fileName);
            FileOutputStream fileWriter = new FileOutputStream(file);
            long length = in.readLong();
            System.out.println("Download started ...");
            int iterNum = (int) (length / SimpleCloud.BUFFER_LEN);
            int remaining = (int) (length - iterNum * SimpleCloud.BUFFER_LEN);
            long lastUpdate = System.currentTimeMillis();
            for (int i = 0; i < iterNum; i++) {
                byte[] buffer = new byte[SimpleCloud.BUFFER_LEN];
                in.read(buffer);
                fileWriter.write(buffer);
                if (i % 1024 == 0 && (System.currentTimeMillis()
                        - lastUpdate> 3000 || i == 0)) {
                    lastUpdate = System.currentTimeMillis();
                    System.out.printf("%.3f MB / %.3f MB%n",
                        ((i * SimpleCloud.BUFFER_LEN) / (1024.0 * 1024.0)),
                        (length / (1024.0 * 1024.0)));
                }
            }
            for (int i = 0; i < remaining; i++) {
                fileWriter.write(in.read());
            }
            fileWriter.close();
            System.out.println("File downloaded.");
        } else {
            System.out.println("Unexpected response came from the server: " + response);
        }
    }
    
    private void upload(String fileName) throws IOException {
        System.out.println("Uploading the file to the server: " + fileName);
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found on the working directory.");
        }
        else {
            out.writeInt(SimpleCloud.MSG_UPLOAD);
            out.flush();
            out.writeUTF(fileName);
            out.flush();
            int response = in.readInt();
            if (response == SimpleCloud.MSG_INVALID) {
                System.out.println("You can't upload this file since there is a "
                                       + "file with the same name on the server.");
            } else if (response == SimpleCloud.MSG_VALID) {
                System.out.println("Upload started ...");
                FileInputStream fileReader = new FileInputStream(file);
                long length = file.length();
                out.writeLong(length);
                out.flush();
                int iterNum = (int) (length / SimpleCloud.BUFFER_LEN);
                int remaining = (int) (length - iterNum * SimpleCloud.BUFFER_LEN);
                long lastUpdate = System.currentTimeMillis();
                for (int i = 0; i < iterNum; i++) {
                    byte[] buffer = new byte[SimpleCloud.BUFFER_LEN];
                    fileReader.read(buffer);
                    out.write(buffer);
                    if (i % 1024 == 0 && (System.currentTimeMillis()
                            - lastUpdate > 3000 || i == 0)) {
                        lastUpdate = System.currentTimeMillis();
                        System.out.printf("%.3f MB / %.3f MB%n",
                            ((i * SimpleCloud.BUFFER_LEN) / (1024.0 * 1024.0)),
                            (length / (1024.0 * 1024.0)));
                    }
                }
                for (int i = 0; i < remaining; i++) {
                    out.write(fileReader.read());
                }
                out.flush();
                fileReader.close();
                System.out.println("File uploaded.");
            } else {
                System.out.println("Unexpected response came from the server: " + response);
            }
        }
    }
    
}
