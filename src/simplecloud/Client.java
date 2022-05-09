package simplecloud;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private boolean authorized = false;
    private String username;

    public static final String pathToClientProps = "client-properties.txt";

    private Client(String ip, int port, boolean encrypt, boolean decrypt) throws IOException {
        System.out.println("Connecting to " + ip + ":" + port + " ...");
        socket = new Socket(ip, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public static void main (String[] args) {//startClient(String ip, int port, boolean encryption) {
        long port = 0;
        String ip = "";
        try (FileReader reader = new FileReader(pathToClientProps))
        {
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
            client = new Client(ip, (int)port, false, false);
            System.out.println("Connected.");
        } catch (IOException e) {
            System.out.println("Connection failed.");
            return;
        }

        client.takeCommands();
    }

    private void authorize(String requestedUsername) {
        try {
            out.writeInt(Server.MSG_AUTHORIZE);
            out.flush();
            out.writeUTF(requestedUsername);
            out.flush();
            int response = in.readInt();
            if (response == Server.MSG_INVALID) {
                System.out.println("This username is already taken, choose another one:");
            }
            if (response == Server.MSG_VALID) {
                authorized = true;
                username = requestedUsername;
            }
            System.out.println("Successfully authorized.");
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
    }
    
    private void takeCommands() {
        Scanner scan = new Scanner(System.in);
        try {
            System.out.println("Commands: \"DOWNLOAD <filename>\", \"UPLOAD "
                                   + "<filename>\", \"LIST\", \"QUIT\"");
            String command = scan.next();
            while (!command.equalsIgnoreCase("quit")) {
                if (authorized){
                    if (command.equalsIgnoreCase("DOWNLOAD")) {
                        String fileName = scan.next();
                        download(fileName);
                    } else if (command.equalsIgnoreCase("UPLOAD")) {
                        String fileName = scan.next();
                        upload(fileName);
                    } else if (command.equalsIgnoreCase("LIST")) {
                        listFiles();
                    } else {
                        System.out.println("Please enter a valid command.");
                    }
                }
                if (command.equalsIgnoreCase("connect")) {//authorize
                    String requestedUsername = scan.next();
                    authorize(requestedUsername);
                } else if (!authorized) {
                    System.out.println("please authorize to send commands");
                }
                command = scan.next();
            }
            out.writeInt(Server.MSG_QUIT);
            out.flush();
        } catch (IOException e) {
            System.out.println("Disconnected from the server.");
        } finally {
            scan.close();
        }
    }

    private void listFiles() {
        try {
            out.writeInt(Server.MSG_LIST);
        }
        catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        int fileNumber = -1;
        try {
            fileNumber = in.readInt();
        }
        catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        if (fileNumber < 0) {
            System.out.println("Some error occured, \"fileNumber < 0\"");
        }
        else if (fileNumber == 0) {
            System.out.println("Server directory empty, no files stored");
        }
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
        out.writeInt(Server.MSG_DOWNLOAD);
        out.flush();
        out.writeUTF(fileName);
        out.flush();
        int response = in.readInt();
        if (response == Server.MSG_INVALID) {
            System.out.println("This file doesn't exists on the server's working directory.");
        } else if (response == Server.MSG_VALID) {
            File file = new File(fileName);
            FileOutputStream fileWriter = new FileOutputStream(file);
            long length = in.readLong();
            System.out.println("Download started ...");
            int iterNum = (int) (length / Server.BUFFER_LEN);
            int remaining = (int) (length - iterNum * Server.BUFFER_LEN);
            int bytesred = -1488;
            int bytesDownloaded = 0;
            for (int i = 0; bytesDownloaded < length && i < 9000; i++) {
                byte[] buffer = new byte[Server.BUFFER_LEN];
                try {
                    bytesred = in.read(buffer);
                    //System.out.println("Download iteration " + i + ", " + bytesred + " bytes red");
                }
                catch (IOException e) {
                    //System.out.println("Download iteration " + i + ", IOException, " + bytesred + " bytes red");
                }
                fileWriter.write(buffer, 0, bytesred);
                bytesDownloaded += bytesred;
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
            out.writeInt(Server.MSG_UPLOAD);
            out.flush();
            out.writeUTF(fileName);
            out.flush();
            int response = in.readInt();
            if (response == Server.MSG_INVALID) {
                System.out.println("You can't upload this file since there is a "
                                       + "file with the same name on the server.");
            } else if (response == Server.MSG_VALID) {
                System.out.println("Upload started ...");
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
                System.out.println("File uploaded.");
            } else {
                System.out.println("Unexpected response came from the server: " + response);
            }
        }
    }
    
}
