import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class CloudClient {
    static Socket socket = null;
    static DataInputStream in = null;
    static DataOutputStream out = null;
    
    static FileInputStream fileReader;
    static FileOutputStream fileWriter;
    
    static File file;
    
    public static void main(String[] args) throws Exception {
        String IP = "localhost";
        int PORT = 8888;
        Scanner scan = new Scanner(System.in);
        System.out.println("Welcome to the CloudClient!");
        System.out.println("Enter IP and port to connect a server, enter 1 to test on localhost.");
        String input = scan.next().trim();
        if (!input.equals("1")) {
            IP = input;
            PORT = scan.nextInt();
        }
        System.out.println("Connecting to " + IP + ":" + PORT + " ...");
        socket = new Socket(IP, PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        System.out.println("Connected.");
        System.out.println("Commands: \"DOWNLOAD filename\", \"UPLOAD filename\", \"QUIT\"");
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
            else {
                System.out.println("Invalid command(s) given");
            }
            command = scan.next();
        }
        logout();
    }
    
    public static void download(String fileName) throws Exception {
        System.out.println("Download request: " + fileName);
        out.writeChar('d');
        out.writeInt(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            out.writeChar(fileName.charAt(i));
        }
        out.flush();
        System.out.println("Looking for the file...");
        
        // Receive and parse the file length.
        int length = in.readInt();
        
        // Download the file if it exists.
        if (length == 0) {
            System.out.println("File not found in the cloud directory.");
        }
        else {
            System.out.println("Downloading file...");            
            file = new File(fileName);
            fileWriter = new FileOutputStream(file);
            
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++)
                data[i] = (byte) in.read();
            fileWriter.write(data);
            fileWriter.close();
            System.out.println("File downloaded.");
        }
    }
    
    public static void upload(String fileName) throws Exception {
        System.out.println("Upload request: " + fileName);
        file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found on the working directory.");
        }
        else {
            
            final int MAX_SIZE = 5242880; // 5 MB
            int length = (int) file.length();
            if (length > MAX_SIZE) {
                System.out.println("Upload failed. The max allowed size is 5 MB");
            }
            else {
                out.writeChar('u');
                out.writeInt(fileName.length());
                for (int i = 0; i < fileName.length(); i++) {
                    out.writeChar(fileName.charAt(i));
                }
                out.flush();
                
                System.out.println("Uploading file ...");
                fileReader = new FileInputStream(file);
                
                // Upload the file length.
                out.writeInt(length);
                
                // Upload the file bytes.
                byte[] data = new byte[length];
                fileReader.read(data);
                fileReader.close();
                out.write(data);
                System.out.println("File uploaded.");
            }
        }
    }
    
    public static void logout() throws Exception {
        out.writeChar('q');
        out.flush();
    }
}
