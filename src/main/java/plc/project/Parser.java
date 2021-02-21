package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        int steps = 0;
        //i think this whole thing is wrong
//        while(tokens.has(1) && peek(".") == false){ //while theres a ting ahead of me
//            tokens.advance();
//            steps++;
//        }
//        //we know how many tokens between the beginning and the period
//        if(peek(".")){ // the 2ndary expresssion has the parenthesis part
//            for(int i = 0; i < steps; i++){
//                tokens.goback();
//            }
//        }
//        else if(peek(".") == false){
//            for(int i = 0; i < steps; i++){
//                tokens.goback();
//            }
//        }
//        //how we're back wherer we started
        return null;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {

        //TODO: gotta handle the 'NIL' ting - thats the only thing missing

        //TODO: i think we gotta do the [ '(' expression ')' ] and the last one too from the grammar

        if(peek("TRUE")){
            //match to advance
            match("TRUE");
            Boolean b = new Boolean(true);
            //Ast.Expr.Literal x = new Ast.Expr.Literal(b);
            return new Ast.Expr.Literal(b);
        }
        else if(peek("FALSE")){
            match("FALSE");
            Boolean b = new Boolean(false);
            return new Ast.Expr.Literal(b);
        }
        else if(peek(Token.Type.INTEGER)){
            int x = Integer.parseInt(tokens.get(0).getLiteral());
            BigInteger i = BigInteger.valueOf(x);
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(i);
        }
        else if(peek(Token.Type.DECIMAL)){
            double x = Double.parseDouble(tokens.get(0).getLiteral());
            BigDecimal i = BigDecimal.valueOf(x);
            match(Token.Type.DECIMAL);
            return new Ast.Expr.Literal(i);
        }
        else if(peek(Token.Type.CHARACTER)){
            String c = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            //lets get rid of the (') tings at each end
            c = c.substring(1);
            c = c.substring(0,(c.length()-1));
            //now lets handle converting the escapes
            if(c.equals("\\\\b")){
                c.replace("\\\\b","\b");
            }
            else if(c.equals("\\\\n")){
                c.replace("\\\\n","\n");
            }
            else if(c.equals("\\\\r")){
                c.replace("\\\\r","\r");
            }
            else if(c.equals("\\\\t")){
                c.replace("\\\\t","\t");
            }
            else if(c.equals("\\\\\'")){
                c.replace("\\\\\'","\'");
            }
            else if(c.equals("\\\\\"")){
                c.replace("\\\\\"","\"");
            }
            else if(c.equals("\\\\\\\\")){
                c.replace("\\\\\\\\","\\");
            }

            Character cc = new Character(c.charAt(0));
            return new Ast.Expr.Literal(cc);
        }
        else if(peek(Token.Type.STRING)){
            String c = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            c = c.substring(1);
            c = c.substring(0,(c.length()-1));
            // " at begining and end have been removed
            c.replace("\\\\b","\b");
            c.replace("\\\\n","\n");
            c.replace("\\\\r","\r");
            c.replace("\\\\t","\t");
            c.replace("\\\\\'","\'");
            c.replace("\\\\\"","\"");
            c.replace("\\\\\\\\","\\");
            //now we have taken care of the escaped tings
            return new Ast.Expr.Literal(c);
        }
        //now the recursive tings
        else if(peek("(")){

        }
        else if(peek(Token.Type.IDENTIFIER)){

        }
        return null;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        for(int i = 0; i < patterns.length; i++){
            if(!tokens.has(i)){
                return false;
            }
            else if(patterns[i] instanceof Token.Type){
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }
            else if(patterns[i] instanceof String){
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }
            else{
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for(int i = 0 ; i < patterns.length;i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }
        public void goback() { index--; }

    }

}
