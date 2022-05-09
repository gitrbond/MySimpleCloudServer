package simplecloud;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SimpleCloud {
    public static final int MSG_UPLOAD = 1;
    public static final int MSG_DOWNLOAD = 2;
    public static final int MSG_VALID = 3;
    public static final int MSG_INVALID = 4;
    public static final int MSG_QUIT = 5;
    public static final int MSG_LIST = 6;
    public static final int MSG_AUTHORIZE = 7;

    public static final int BUFFER_LEN = 10000;

    public static final String pathToServerProps = "server-properties.json";
    public static final String pathToClientProps = "client-properties.json";

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Welcome to the SimpleCloud.");
        System.out.print("Enter H to host a cloud server, C to connect to a "
                             + "cloud server server: ");
        boolean valid = true;
        String input = scan.next();
        do {
            if (input.substring(0, 1).equalsIgnoreCase("H")) {
                long port;
                try (FileReader reader = new FileReader(pathToServerProps))
                {
                    System.out.println("reading parameters from " + pathToServerProps);
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
                    port = (long) jsonObject.get("port");
                    System.out.print("Starting Server on port " + port);
                    ServerThread.startServer((int)port);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return;
            } else if (input.substring(0, 1).equalsIgnoreCase("C")) {
                System.out.print("Enter the IP of the server: ");
                String ip = scan.next();
                System.out.print("Enter the PORT of the server: ");
                int port = scan.nextInt();
                //CloudClient.startClient(ip, port, false);
                return;
            } else {
                valid = false;
                System.out.println("Please enter a valid input.");
                input = scan.next();
            }
        } while (!valid);
    }
    
}
