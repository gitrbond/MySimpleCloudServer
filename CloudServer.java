import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CloudServer implements Runnable {
    Socket connectionSocket;
    DataInputStream in;
    DataOutputStream out;
    
    File file;
    FileInputStream fileReader;
    FileOutputStream fileWriter;
    
    public CloudServer(Socket connectionSocket) throws Exception {
        this.connectionSocket = connectionSocket;
        this.in = new DataInputStream(connectionSocket.getInputStream());
        this.out = new DataOutputStream(connectionSocket.getOutputStream());
    }
    
    public void run() {
        System.out.println("Client connected: " + connectionSocket);
        try {
            while (true) {
                System.out.println("Listening ...");
                char command = in.readChar();
                if (command == 'd') {
                    int nameLength = in.readInt();
                    String fileName = "";
                    for (int i = 0; i < nameLength; i++) {
                        fileName = fileName + in.readChar();
                    }
                    System.out.println("Download request: " + fileName);
                    file = new File(fileName);
                    if (!file.exists()) {
                        System.out.println("File not found.");
                        out.write(new byte[] {0, 0, 0, 0});
                    }
                    else {
                        System.out.println("Sending file ...");
                        long start = System.currentTimeMillis();
                        
                        // Send the file length.
                        fileReader = new FileInputStream(file);
                        int length = (int) file.length();
                        out.writeInt(length);
                        
                        // Send the file bytes.
                        byte[] data = new byte[length];
                        fileReader.read(data);
                        fileReader.close();
                        out.write(data);
                        
                        // Measure time.
                        long end = System.currentTimeMillis();
                        double time = (int) (end - start) / 1000.0;
                        double speed = ((double) length / (1024* 1024)) / time;
                        System.out.println("File sent.");
                        System.out.printf("Transfer time: %.2f seconds%nTransfer speed: %.2f MB/s%n", time, speed);
                    }
                }
                else if (command == 'u') {
                    int nameLength = in.readInt();
                    String fileName = "";
                    for (int i = 0; i < nameLength; i++) {
                        fileName = fileName + in.readChar();
                    }
                    System.out.println("Upload request: " + fileName);
                    file = new File(fileName);
                    fileWriter = new FileOutputStream(file);

                    System.out.println("Receiving file ...");
                    long start = System.currentTimeMillis();
                    
                    // Receive the file length
                    int length = in.readInt();
                    
                    // Receive the file bytes.
                    byte[] data = new byte[length];
                    for (int i = 0; i < length; i++)
                        data[i] = (byte) in.read();
                    fileWriter.write(data);
                    fileWriter.close();
                    
                    // Measure time.
                    long end = System.currentTimeMillis();
                    double time = (int) (end - start) / 1000.0;
                    double speed = ((double) length / (1024 * 1024)) / time;
                    System.out.println("File received.");
                    System.out.printf("Transfer time: %.2f seconds%nTransfer speed: %.2f MB/s%n", time, speed);
                }
                else if (command == 'q') {
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
