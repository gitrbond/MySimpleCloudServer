package simplecloud;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Server {
    public static final int MSG_UPLOAD = 1;
    public static final int MSG_DOWNLOAD = 2;
    public static final int MSG_VALID = 3;
    public static final int MSG_INVALID = 4;
    public static final int MSG_QUIT = 5;
    public static final int MSG_LIST = 6;
    public static final int MSG_AUTHORIZE = 7;
    public static final int MSG_SYNC = 8;
    public static final int MSG_SAY = 9;

    public static final int BUFFER_LEN = 10000;

    public static final String pathToServerProps = "server-properties.txt";

    public static void main(String[] args) {
        long port;
        try (FileReader reader = new FileReader(pathToServerProps)) {
            System.out.println("reading parameters from " + pathToServerProps);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            port = (long) jsonObject.get("port");
            ServerThread.startServer((int) port);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
