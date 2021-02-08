package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<Token>();
        while(chars.has(0)) {
            //if(chars.input.charAt(chars.index) == ' '){
            if(peek("[' '\b\n\t\r]")){ //TODO: fix issue
                chars.advance();
                chars.skip();
            }
            else{
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */

    public Token lexToken() {
         //TODO
        if(peek("[A-Za-z_]")){
            return lexIdentifier();
        }
        else if(peek("[\\+|\\-]") || peek("[0-9]")){
            if(peek("[\\+|\\-]")){
                match("[\\+|\\-]");
                if(!peek("[0-9]")){
                    return lexOperator();
                }
            }
            return lexNumber();
        }
        else if(peek("\'")){
            return lexCharacter();
        }
        else if(peek("\"")){
            return lexString();
        }
        else if(peek("\\\\")){
            lexEscape();
        }
        else if(peek("[<>!=]") || peek("[^\b\n\r\t' ']")){
            return lexOperator();
        }
        throw new ParseException("Error: Unidentified token",chars.index); //parse exception
    }

    public Token lexIdentifier() {//String tok = ""+chars.input.charAt(chars.index);
        match("[A-Za-z_]");
        while (match("[A-Za-z0-9_-]")) {}
        //while(peek("[A-Za-z0-9_-]*")){ //tok+=chars.input.charAt(chars.index);
        //    match("[A-Za-z0-9_-]*");
        //}
        return chars.emit(Token.Type.IDENTIFIER);

    }

    public Token lexNumber() {

        //if we get here, next characters MUST start a number

        match("[+\\-]");
        while (match("[0-9]")) {}
        if (match("\\.", "[0-9]")) {
            while (match("[0-9]")) {}
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);

        // [\+|\-] -> + or | or - |5

//        if(peek("[\\+|\\-]")){
//            match("[\\+|\\-]");
//            if(!peek("[0-9]+")){
//                throw new ParseException("Error: Sign must be followed by an integer",chars.index);
//            }
//        }
//        if(!peek("[0-9]+")){
//            throw new ParseException("Error: Invalid Number Token",chars.index);
//        }
//        match("[0-9]+");
//        while(peek("[0-9]+")) {
//            match("[0-9]+");
//        }
//
//        if(peek("\\.")){
//            match("\\.");
//            if(peek("[0-9]+")) {
//                while(peek("[0-9]+")){
//                    match("[0-9]+");
//                }
//                return chars.emit(Token.Type.DECIMAL);
//            }
//            else{
//                throw new ParseException("Error: Trailing decimal",chars.index);
//            }
//        }
//        return chars.emit(Token.Type.INTEGER);

    }

    public Token lexCharacter() {
        match("\'");
        if(peek("\'"))
            throw new ParseException("Error: Character Token Invalid",chars.index);

        if(peek("\\\\")){
            lexEscape();
            if(peek("\'")){
                match("\'");
            }
            else{
                throw new ParseException("Error: Invalid character token", chars.index);
            }
            return chars.emit(Token.Type.CHARACTER);
        }
        else{
            if(peek("[^\n\r]")){ //fix this
                match("[^\n\r]");
                if(peek("\'")){
                    match("\'");
                    return chars.emit(Token.Type.CHARACTER);
                }
            }

        }

        throw new ParseException("Error: Character Token Invalid",chars.index);
    }

    public Token lexString() {
        match("\"");
        while(peek("[^\"\n\r]")){
            if(peek("\\\\")){
                lexEscape();
            }
            else{
                match("[^\"\n\r]");
            }
        }
        //if we left the while loop because we have an ending quote we will match with it
        //if there is something after this ending quote this is an invalid string
        if(peek("\"")){
            match("\"");
            return chars.emit(Token.Type.STRING);
            /*if(peek("(.)")){
                throw new ParseException("Error: String Token Invalid",chars.index);
            }
            else{
                //nothing after ending quote so we are chillen
                return chars.emit(Token.Type.STRING);
            }*/
        }
        //left the while loop but no end quote
        throw new ParseException("Error: String Token Invalid",chars.index);
    }

    public void lexEscape() {
        match("\\\\");
        if (!match("[bnrt\'\"\\\\]")) { //TODO fix regex
            throw new ParseException("Error: Invalid escape character", chars.index);
        }
        //nothing

//        boolean allgood = false;
//        match("\\\\");
//        if(peek("b")){
//            match("b");
//            allgood = true;
//        }
//        else if(peek("n")){
//            match("n");
//            allgood = true;
//        }
//        else if(peek("r")){
//            match("r");
//            allgood = true;
//        }
//        else if(peek("t")){
//            match("t");
//            allgood = true;
//        }
//        else if(peek("\'")){
//            match("\'");
//            allgood = true;
//        }
//        else if(peek("\"")){
//            match("\"");
//            allgood = true;
//        }
//        else if(peek("\\\\")){
//            match("\\\\");
//            allgood = true;
//        }
//        if(!allgood){
//            throw new ParseException("Error: Invalid escape character", chars.index);
//        }
//        //make sure indeed escape else exception
//        //call this in lex string and lex char to make sure escape is valid
    }

    public Token lexOperator() {

        if(peek("[<>!=]")){
            match("[<>!=]");
            if(peek("=")){
                match("=");
                return chars.emit(Token.Type.OPERATOR);
            }
            return chars.emit(Token.Type.OPERATOR);
        }
        else{
            match(".");
            return chars.emit(Token.Type.OPERATOR);
        }

    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    /* [A-Za-z_] [A-Za-z0-9_-]* for identifier
     *
     * peek("[A-Za-z_]") and if true then call the lexIdetnfiier and loop
     *
     *
     * */
    public boolean peek(String... patterns) { //patterns is meant to be a regex that cooressponds to one character
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) { //first check if we're past the index
                return false; //testing version control 1
            }
        }
        return true;//testing version control 2
    } //while we match on regulkar expression with asterisk
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) { //string... is variable arghuments -> arbitrary amount of strings "match(string1,string2)"
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        //length is length of current token
        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
//passes example lexer tests