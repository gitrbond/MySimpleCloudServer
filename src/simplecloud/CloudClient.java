package simplecloud;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;


public class CloudClient {
    
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private static byte[] randombytes;
    private boolean decrypt = true;
    private boolean encrypt = true;

    public static void startClient(String ip, int port) {
        CloudClient client = null;
        try {
            client = new CloudClient(ip, port);
        } catch (IOException e) {
            System.out.println("Connection failed.");
            return;
        }

        randombytes = new byte [SimpleCloud.BUFFER_LEN];
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the encrypting password:");
        String password = scan.next();
        SecureRandom random = new SecureRandom();
        random.setSeed(password.getBytes(StandardCharsets.UTF_8));
        random.nextBytes(randombytes);

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
        try {out.writeInt(SimpleCloud.MSG_LIST);}
        catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        int fileNumber = -1;
        try {fileNumber = in.readInt();}
        catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        if (fileNumber < 0)
            System.out.println("Some error occured, \"fileNumber < 0\"");
        else if (fileNumber == 0)
            System.out.println("Server directory empty, no files stored");
        else {
            System.out.println("List of files on the server:");
            byte[] tmp = new byte[300];
            try {
                for (int i = 0; i < 1; i++) {
                    //in.read(tmp);
                    int bytesred = in.read(tmp);
                    if (bytesred > 0)
                        System.out.print(new String(tmp, 0, bytesred));
                }
            } catch (IOException ioe) {
                System.out.println("IOException: " + ioe);
            }
        }
    }

    private void download(String fileName) throws IOException {
        System.out.println("Requesting download for: " + fileName);
        ByteArrayInputStream randommask = new ByteArrayInputStream(randombytes);
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
            int bytesred = -1488;
            int bytesDownloaded = 0;
            for (int i = 0; bytesDownloaded < length && i < 9000; i++) {
                byte[] buffer = new byte[SimpleCloud.BUFFER_LEN];
                byte[] rbuffer = new byte[SimpleCloud.BUFFER_LEN];
                try {
                    bytesred = in.read(buffer);
                    //System.out.println("Download iteration " + i + ", " + bytesred + " bytes red");
                }
                catch (IOException e) {
                    //System.out.println("Download iteration " + i + ", IOException, " + bytesred + " bytes red");
                }
                randommask.read(rbuffer, 0, bytesred);
                if (decrypt)
                    for (int j = 0; j < bytesred; j++)
                        buffer[j] = (byte) (buffer[j] ^ rbuffer[j]);
                fileWriter.write(buffer, 0, bytesred);
                bytesDownloaded += bytesred;
                if (i % 1024 == 0 && (System.currentTimeMillis()
                        - lastUpdate> 1000 || i == 0)) {
                    lastUpdate = System.currentTimeMillis();
                    System.out.printf("%.3f MB / %.3f MB%n",
                        (bytesDownloaded / (1024.0 * 1024.0)),
                        (length / (1024.0 * 1024.0)));
                }
            }
            fileWriter.close();
            randommask.close();
            System.out.println("File downloaded.");
        } else {
            System.out.println("Unexpected response came from the server: " + response);
        }
    }
    
    private void upload(String fileName) throws IOException {
        System.out.println("Uploading the file to the server: " + fileName);
        ByteArrayInputStream randommask = new ByteArrayInputStream(randombytes);
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
                    byte[] rbuffer = new byte[SimpleCloud.BUFFER_LEN];
                    fileReader.read(buffer);
                    randommask.read(rbuffer);
                    if (encrypt)
                        for (int j = 0; j < buffer.length; j++)
                            buffer[j] = (byte) (buffer[j] ^ rbuffer[j]);
                    out.write(buffer);
                    if (i % 1024 == 0 && (System.currentTimeMillis()
                            - lastUpdate > 1000 || i == 0)) {
                        lastUpdate = System.currentTimeMillis();
                        System.out.printf("%.3f MB / %.3f MB%n",
                            ((i * SimpleCloud.BUFFER_LEN) / (1024.0 * 1024.0)),
                            (length / (1024.0 * 1024.0)));
                    }
                }
                byte[] rembuffer = new byte[remaining];
                byte[] rrembuffer = new byte[remaining];
                fileReader.read(rembuffer);
                randommask.read(rrembuffer);
                if (encrypt)
                    for (int i = 0; i < rembuffer.length; i++)
                        rembuffer[i] = (byte) ((int)rembuffer[i] ^ (int)rrembuffer[i]);
                out.write(rembuffer);
                out.flush();
                fileReader.close();
                randommask.close();
                System.out.println("File uploaded.");
            } else {
                System.out.println("Unexpected response came from the server: " + response);
            }
        }
    }
    
}
