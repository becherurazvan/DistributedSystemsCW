package KeyUtils;

/**
 * Created by becheru on 12/01/2016.
 */
public class ConvertUtils {

    public boolean isInt(String s) {
        try {
            int i = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static Integer getAsInt(String s) {
        Integer i = null;
        try {
           i= Integer.parseInt(s);
        } catch (NumberFormatException nfe) {

        }
        return i;
    }
}
