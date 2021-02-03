package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._\\-]+@[A-Za-z0-9-]*\\.[a-z]{2,3}"),
            EVEN_STRINGS = Pattern.compile("((..){5,10})"), //TODO
            //EVEN_STRINGS = Pattern.compile("^.{20}$|^.{18}$|^.{16}$|^.{14}$|^.{12}$|^.{10}$"), //TODO// TOO COMPLEX
            INTEGER_LIST = Pattern.compile("((^\\[(((((([1-9])*),\\s?))*))([1-9]+)\\])$)|(\\[\\])"), //TODO
            NUMBER = Pattern.compile("(((\\-|\\+)?(((([1-9][0-9]+|[0-9]+)+)(\\.+)([0-9]+))|((?<!\\.)(([0-9])*)))))"), //TODO
            STRING = Pattern.compile("(^\\\"(\\\\b|\\\\n|\\\\r|\\\\t|\\\\'|\\\\\"|\\\\\\\\|[^\\\\])*\\\"$)"); //TODO

}
