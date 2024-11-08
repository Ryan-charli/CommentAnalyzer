import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CommentCounter {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java CommentCounter <file_path>");
            return;
        }

        String filePath = args[0];
        countComments(filePath);
    }

    public static void countComments(String filePath) {
        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            int commentLines = 0;
            int commentChars = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("//") || line.trim().startsWith("/*") || line.trim().startsWith("*")) {
                    commentLines++;
                    commentChars += line.length();
                }
            }

            System.out.println("Total comment lines: " + commentLines);
            System.out.println("Total comment characters: " + commentChars);

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}