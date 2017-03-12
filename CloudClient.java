import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Scanner;

public class CloudClient {
    public static Socket socket = null;
    public static FileInputStream in = null;
    public static PrintWriter out = null;
    
    public static void main(String[] args) throws Exception {
        String IP = "localhost";
        int PORT = 8888;
        Scanner scan = new Scanner(System.in);
        System.out.println("Welcome to the CloudClient!");
        System.out.println("Enter IP and port to connect a server, enter 1 to test on localhost.");
        String input = scan.next().trim();
        if (!input.equals("1")) {
            IP = input;
            PORT = Integer.parseInt(scan.next());
        }
        System.out.println("Connecting to " + IP + ":" + PORT + " ...");
        socket = new Socket(IP, PORT);
        in = (FileInputStream) socket.getInputStream();
        out = new PrintWriter(socket.getOutputStream());
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
        out.println("d");
        out.println(fileName);
        out.flush();
        System.out.println("Looking for the file...");
        
        // Receive and parse the file length.
        String lengthValue = "";
        for (int l = 0; l < 4; l++) {
            String thisByte = Integer.toString(in.read(), 2);
            while (thisByte.length() < 8) {
                thisByte = "0" + thisByte;
            }
            lengthValue += "" + thisByte;
        }
        int length = Integer.parseInt(lengthValue, 2);
        
        // Download the file if it exists.
        if (length == 0) {
            System.out.println("File not found in the cloud directory.");
        }
        else {
            System.out.println("Downloading file...");            
            File file = new File(fileName);
            FileOutputStream outStream = new FileOutputStream(file);
            
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++)
                data[i] = (byte) in.read();
            outStream.write(data);
            outStream.close();
            System.out.println("File downloaded.");
        }
    }
    
    public static void upload(String fileName) throws Exception {
        System.out.println("Upload request: " + fileName);
        File file = new File(fileName);
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
                out.println("u");
                out.println(fileName);
                out.flush();
                FileOutputStream outf = (FileOutputStream) socket.getOutputStream();
                System.out.println("Uploading file ...");
                FileInputStream inputStream = new FileInputStream(file);
                
                // Upload the file length.
                byte[] lengthArray = BigInteger.valueOf(length).toByteArray();
                while (lengthArray.length < 4) {
                    byte[] modifiedLengthArray = new byte[lengthArray.length + 1];
                    for (int i = 0; i < lengthArray.length; i++)
                        modifiedLengthArray[i+1] = lengthArray[i];
                    lengthArray = modifiedLengthArray;
                }
                for (int w = 0; w < 4; w++) {
                    outf.write(lengthArray[w]);
                }
                
                // Upload the file bytes.
                byte[] data = new byte[length];
                inputStream.read(data);
                inputStream.close();
                outf.write(data);
                System.out.println("File uploaded.");
            }
        }
    }
    
    public static void logout() throws Exception {
        out.println("q");
        out.flush();
    }
}