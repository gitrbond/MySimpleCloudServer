import java.io.*;
import java.util.Scanner;

public class CopyFiles {
    public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    public static void main(String[] args) {
        System.out.println("Enter fullpath of file: ");
        Scanner in = new Scanner(System.in);
        String pathnameOfSource = in.nextLine(); //"/Users/admin/Desktop/";
        System.out.println("Enter fullpath of destination: ");
        String pathnameOfDest = in.nextLine();
        System.out.println("Enter name of file with format (example text.txt) : ");
        String format = in.nextLine();
        in.close();
        File source =  new File(pathnameOfSource);
        for (int i = 0; i <= 1; i++) {
            File dest =  new File(pathnameOfDest + i + "c" + format);
            try {
                copyFileUsingStream(source, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}


