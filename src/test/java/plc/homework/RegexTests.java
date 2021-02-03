package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(//do 10 examples for this one also
                Arguments.of("Normal Gmail", "peterpotter777@gmail.com", true),
                Arguments.of("UF Domain", "raul.salazar@ufl.edu", true),
                Arguments.of("Magic Is Real", "harrypotter@hogwarts.uk", true),
                Arguments.of("UF Domain", "raul.salazar@ufl.edu", true),
                Arguments.of("Empty Sandwich", "raul.salazar@.edu", true), //nothing between the @ and the period
                Arguments.of("Missing Dot", "wheresthedot@gmailcom", false),
                Arguments.of("Missing At Symbol", "wherestheatgmail.com", false),
                Arguments.of("Wrong Ending", "thefreshprince@of.belair", false),
                Arguments.of("Nothing in beginning", "@wherestherest.com", false),
                Arguments.of("Invalid Symbols", "!#hello$(*&^%$%@yahoo.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "DarthVader", true),
                Arguments.of("12 Characters", "?1234567890?", true),
                Arguments.of("14 Characters", "I-Get-Moneyyyy", true),
                Arguments.of("16 Characters", "-6teencharacter-", true),
                Arguments.of("20 Characters", "batmanarkhamknight99", true),
                Arguments.of("7 Characters", "dis2short", false),
                Arguments.of("11 Characters", "playstation", false),
                Arguments.of("13 Characters", "LionelMessi10", false),
                Arguments.of("11 Characters", "playstation", false),
                Arguments.of("27 Characters", "thisIsTwentySevenCharacters", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[2]", true),
                Arguments.of("Multiple Elements", "[1,2,3,4]", true),
                Arguments.of("Spaced Out", "[1, 2, 3, 4, 5, 6, 7, 8]", true),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Spaced Out Unevenly", "[1, 2,3, 4]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Space Before Closing Bracket", "[1 ]", false),
                Arguments.of("Unnecessary Comma", "[1,]", false),
                Arguments.of("Too Many Spaces", "[1, 2,3,  4]", false),
                Arguments.of("Missing Commas", "[1 2 3]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success); //TODO
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("\"+140\"", "+140", true),
                Arguments.of("\"1.00\"", "1.00", true),
                Arguments.of("\"-040.02\"", "-040.02", true),
                Arguments.of("\"000.000\"", "000.000", true),
                Arguments.of("\"00001\"", "00001", true),
                Arguments.of("\".098\"", ".098", false),
                Arguments.of("\"098.\"", "098.", false),
                Arguments.of("\".098.\"", ".098.", false),
                Arguments.of("\"+.098\"", "+.098", false),
                Arguments.of("\".\"", ".", false)

        ); //TODO
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success); //TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("\"hello\"", "\"hello\"", true),
                Arguments.of("\"\\b test\"", "\"hello \\b marsha\"", true),
                Arguments.of("\"\\n test\"", "\"this that new line \\n ting\"", true),
                Arguments.of("Hello, World", "\"Hello, World!!\"", true),
                Arguments.of("Number", "\"-1999.0730\"", true),
                Arguments.of("No End Quotes", "\"goodbye", false),
                Arguments.of("No Begin Quotes", "Gabagoo\"", false),
                Arguments.of("Stray Slash", "\"Not Supposed to \\ be there\"", false),
                Arguments.of("No Quotes", "nobody said this", false),
                Arguments.of("Three Slashes", "\"Hola \\\\\\ que tal?\"", false)

        ); //TODO
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
