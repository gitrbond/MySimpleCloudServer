package simplecloud;

import java.util.Scanner;


public class SimpleCloud {
    
    public static final int MSG_UPLOAD = 1;
    public static final int MSG_DOWNLOAD = 2;
    public static final int MSG_VALID = 3;
    public static final int MSG_INVALID = 4;
    public static final int MSG_QUIT = 5;
    public static final int MSG_LIST = 6;

    public static final int BUFFER_LEN = 200000;
    
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Welcome to the SimpleCloud.");
        System.out.print("Enter H to host a cloud server, C to connect to a "
                             + "cloud server server: ");
        boolean valid = true;
        String input = scan.next();
        do {
            if (input.substring(0, 1).equalsIgnoreCase("H")) {
                System.out.print("Enter a PORT number for your server: ");
                int port = scan.nextInt();
                CloudServer.startServer(port);
            } else if (input.substring(0, 1).equalsIgnoreCase("C")) {
                System.out.print("Enter the IP of the server: ");
                String ip = scan.next();
                System.out.print("Enter the PORT of the server: ");
                int port = scan.nextInt();
                CloudClient.startClient(ip, port);
            } else {
                valid = false;
                System.out.println("Please enter a valid input.");
                input = scan.next();
            }
        } while (!valid);
    }
    
}
