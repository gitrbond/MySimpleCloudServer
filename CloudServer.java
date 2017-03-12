import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class CloudServer implements Runnable {
    public Socket connectionSocket;
    public Scanner in;
    public FileOutputStream out;
    
    public CloudServer(Socket connectionSocket) throws Exception {
        this.connectionSocket = connectionSocket;
        this.in = new Scanner(connectionSocket.getInputStream());
        this.out = (FileOutputStream) connectionSocket.getOutputStream();
    }
    
    public void run() {
        System.out.println("Client connected: " + connectionSocket);
        try {
            while (true) {
                System.out.println("Listening ...");
                String command = in.next();
                if (command.equals("d")) {
                    String fileName = in.next();
                    System.out.println("Download request: " + fileName);
                    File file = new File(fileName);
                    if (!file.exists()) {
                        System.out.println("File not found.");
                        out.write(new byte[] {0, 0, 0, 0});
                    }
                    else {
                        System.out.println("Sending file ...");
                        long start = System.currentTimeMillis();
                        
                        // Send the file length.
                        FileInputStream inputStream = new FileInputStream(file);
                        int length = (int) file.length();
                        byte[] lengthArray = BigInteger.valueOf(length).toByteArray();
                        while (lengthArray.length < 4) {
                            byte[] modifiedLengthArray = new byte[lengthArray.length + 1];
                            for (int i = 0; i < lengthArray.length; i++)
                                modifiedLengthArray[i+1] = lengthArray[i];
                            lengthArray = modifiedLengthArray;
                        }
                        for (int w = 0; w < 4; w++) {
                            out.write(lengthArray[w]);
                        }
                        
                        // Send the file bytes.
                        byte[] data = new byte[length];
                        inputStream.read(data);
                        inputStream.close();
                        out.write(data);
                        
                        // Measure time.
                        long end = System.currentTimeMillis();
                        double time = (int) (end - start) / 1000.0;
                        double speed = ((double) length / (1024* 1024)) / time;
                        System.out.println("File sent.");
                        System.out.printf("Transfer time: %.2f seconds%nTransfer speed: %.2f MB/s%n", time, speed);
                    }
                }
                else if (command.equals("u")) {
                    String fileName = in.next();
                    System.out.println("Upload request: " + fileName);
                    File file = new File(fileName);
                    FileOutputStream outStream = new FileOutputStream(file);
                    FileInputStream inf = (FileInputStream) connectionSocket.getInputStream();
                    System.out.println("Receiving file ...");
                    long start = System.currentTimeMillis();
                    
                    // Receive and parse the file length.
                    String lengthValue = "";
                    for (int l = 0; l < 4; l++) {
                        String thisByte = Integer.toString(inf.read(), 2);
                        while (thisByte.length() < 8){
                            thisByte = "0" + thisByte;
                        }
                        lengthValue += "" + thisByte;
                    }
                    int length = Integer.parseInt(lengthValue, 2);
                    
                    // Receive the file bytes.
                    byte[] data = new byte[length];
                    for (int i = 0; i < length; i++)
                        data[i] = (byte) inf.read();
                    outStream.write(data);
                    outStream.close();
                    
                    // Measure time.
                    long end = System.currentTimeMillis();
                    double time = (int) (end - start) / 1000.0;
                    double speed = ((double) length / (1024 * 1024)) / time;
                    System.out.println("File received.");
                    System.out.printf("Transfer time: %.2f seconds%nTransfer speed: %.2f MB/s%n", time, speed);
                }
                else if (command.equals("q")) {
                    break;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public static void main(String[] args) throws Exception {
        final int PORT = 8888;
        System.out.println("Welcome to the CloudServer!");
        System.out.println("Starting server on port " + PORT + " ...");
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started.");
        System.out.println("Waiting for connections ...");
        while (true)
        {
            Socket connectionSocket = serverSocket.accept();
            CloudServer cloudServer = new CloudServer(connectionSocket);
            Thread thread = new Thread(cloudServer);
            thread.start();
        }
    }
}