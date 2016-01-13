
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    protected String id;

    public Client() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your id?");
        id = scanner.nextLine();


        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }




        System.out.println("Enter your commands");
        while (scanner.hasNextLine()) {
            List<String> splittedWord = new ArrayList<String>();
            Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
            Matcher regexMatcher = regex.matcher(scanner.nextLine());

            while (regexMatcher.find()) {
                splittedWord.add(regexMatcher.group().replace("\"", ""));
            }

            if (splittedWord.size() < 1) {
                System.out.println("enter something...");
                continue;
            }
            inputDetected(splittedWord);
        }
    }

    protected void inputDetected(List<String> input) {
    }

    protected void init() throws Exception{

    }




    public boolean isDouble(String s) {
        try {
            double d = Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public boolean isInt(String s) {
        try {
            int i = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }




}
