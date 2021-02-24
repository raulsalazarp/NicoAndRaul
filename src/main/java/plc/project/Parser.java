package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;

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

    //index helper function 
    public int indexFinder() {
        if (tokens.has(0)) {
            return tokens.get(0).getIndex();
        } else {
            return ((tokens.get(-1).getIndex()) + (tokens.get(-1).getLiteral().length()));
        }
    }

    public Ast.Source parseSource() throws ParseException {
        //we need list of fields and list of methods
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (peek("LET")) {
            fields.add(parseField());
        }
        while (peek("DEF")) {
            methods.add(parseMethod());
        }
        if (peek("LET"))
            throw new ParseException("Error: @ index " + indexFinder() + " there is a field after a method", indexFinder());
        return new Ast.Source(fields, methods);

    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        //we need string name and Optional<Expr> value;
        if (match("LET")) {
            if (peek(Token.Type.IDENTIFIER)) {
                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                if (match("=")) {
                    Ast.Expr value = parseExpression();
                    if (match(";")) {
                        return new Ast.Field(name, Optional.of(value));
                    }
                } else if (match(";")) {
                    return new Ast.Field(name, empty());
                }
            }
        }
        throw new ParseException("Error: @ index " + indexFinder() + " something went wrong in field", indexFinder());
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        //we need string name list of strings parameters and list of statements
        if (peek("DEF")) {
            match("DEF");
            List<String> params = new ArrayList<>();
            List<Ast.Stmt> stats = new ArrayList<>();
            if (peek(Token.Type.IDENTIFIER)) {
                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                if (match("(")) {
                    if (peek(Token.Type.IDENTIFIER)) {
                        String temp = tokens.get(0).getLiteral();
                        params.add(temp);
                        match(Token.Type.IDENTIFIER);
                        while (match(",")) {
                            if (peek(Token.Type.IDENTIFIER)) {
                                temp = tokens.get(0).getLiteral();
                                params.add(temp);
                                match(Token.Type.IDENTIFIER);
                            } else {
                                throw new ParseException("Error: @ index " + indexFinder() + " not an identifier", indexFinder());
                            }
                        }
                    }
                    if (match(")")) {
                        if (match("DO")) {
                            while (match("END") == false) { //check if theres tokens left
                                Ast.Stmt s = parseStatement();
                                stats.add(s);
                            }
                            return new Ast.Method(name, params, stats);
                        }
                    }

                }
            }
        }
        throw new ParseException("Error: @ index " + indexFinder() + " the formatting for method is wrong", indexFinder());
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        //delegate the work and then deal with the assignment and expression tings
        if (peek("LET"))
            return parseDeclarationStatement();
        else if (peek("IF"))
            return parseIfStatement();
        else if (peek("FOR"))
            return parseForStatement();
        else if (peek("WHILE"))
            return parseWhileStatement();
        else if (peek("RETURN"))
            return parseReturnStatement();
        else {
            Ast.Expr x = parseExpression();
            if (match(";")) {
                return new Ast.Stmt.Expression(x);
            } else if (match("=")) {
                Ast.Expr xx = parseExpression();
                if (match(";"))
                    return new Ast.Stmt.Assignment(x, xx);
            }
        }
        throw new ParseException("Error: @ index " + indexFinder() + " parseStatement couldnt delegate the work correctly", indexFinder());
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        //we need string name and optional expr value
        if (match("LET")) {
            if (peek(Token.Type.IDENTIFIER)) {
                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                if (match("=")) {
                    Ast.Expr value = parseExpression();
                    if (match(";"))
                        return new Ast.Stmt.Declaration(name, Optional.of(value));
                } else if (match(";")) {
                    return new Ast.Stmt.Declaration(name, empty());
                }
            }
        }
        throw new ParseException("Error: @ index " + indexFinder() + " something went wrong", indexFinder());
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        //we need expr condition and list of thenstmts and list of elsestmts
        if (match("IF")) {
            Ast.Expr condition = parseExpression();
            List<Ast.Stmt> thenstats = new ArrayList<>();
            List<Ast.Stmt> elsestats = new ArrayList<>();
            if (match("DO")) {
                while (peek("ELSE") == false && peek("END") == false) {
                    Ast.Stmt s = parseStatement();
                    thenstats.add(s);
                }
                if (peek("ELSE")) {
                    match("ELSE");
                    while (peek("END") == false) {
                        Ast.Stmt ss = parseStatement();
                        elsestats.add(ss);
                    }
                    if (match("END")) {
                        return new Ast.Stmt.If(condition, thenstats, elsestats);
                    }
                } else if (match("END")) {
                    return new Ast.Stmt.If(condition, thenstats, elsestats);
                }
            } else {
                throw new ParseException("Error: @ index " + indexFinder() + " - no 'DO' present", indexFinder());
            }
        }
        return null;
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        //we need String name, Expr value and list of statements
        if (match("FOR")) {
            if (peek(Token.Type.IDENTIFIER)) {
                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                if (match("IN")) {
                    Ast.Expr value = parseExpression();
                    List<Ast.Stmt> list = new ArrayList<>();
                    if (match("DO")) {
                        while (match("END") == false) { //this could be an infinite loop is END never there ??
                            //but if there is no end we believe it will break when
                            // we try to parseStmt something that aint a stmt
                            Ast.Stmt s = parseStatement();
                            list.add(s);
                            //tokens.advance();
                        }
                        return new Ast.Stmt.For(name, value, list);
                    } else {
                        throw new ParseException("Error: @ index " + indexFinder() + " no 'DO' present", indexFinder());
                    }
                } else {
                    throw new ParseException("Error: @ index " + indexFinder() + " no 'IN' present", indexFinder());
                }
            } else {
                throw new ParseException("Error: @ index " + indexFinder() + " no identifier present", indexFinder());
            }
        }
        throw new ParseException("Error: something has gone terribly wrong", indexFinder());
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //we need Expr condition and List<stmt> list
        if (match("WHILE")) {
            Ast.Expr condition = parseExpression();
            List<Ast.Stmt> list = new ArrayList<>();
            if (match("DO")) {
                while (peek("END") == false) {
                    Ast.Stmt s = parseStatement();
                    list.add(s);
                }
                if (peek("END")) {
                    match("END");
                    return new Ast.Stmt.While(condition, list);
                }
            }
            System.out.println(indexFinder());
            throw new ParseException("Error: at index" + indexFinder() + " - missing the DO or END in the while", indexFinder());
        }
        throw new ParseException("Error: something has gone terribly wrong", indexFinder());
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (match("RETURN")) {
            Ast.Expr x = parseExpression();
            return new Ast.Stmt.Return(x);
        }
        throw new ParseException("Error: @ index " + indexFinder() + " something in return statement went wrong", indexFinder());
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr left = parseEqualityExpression();
        while (match("AND") || match("OR")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseEqualityExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr left = parseAdditiveExpression();
        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr left = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr left = parseSecondaryExpression();
        while (match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr result = parsePrimaryExpression();
        String name;
        List<Ast.Expr> list;
        while (match(".")) {
            Ast.Expr receiver = result;
            if (peek(Token.Type.IDENTIFIER)) {
                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                list = new ArrayList<Ast.Expr>();
                if (peek("(") == false) {
                    result = new Ast.Expr.Access(Optional.of(receiver), name);
                } else if (peek("(", ")")) {
                    match("(", ")");
                    result = new Ast.Expr.Function(Optional.of(receiver), name, list);
                } else if (peek("(")) {//theres stuff inside the parameters
                    match("(");
                    Ast.Expr x = parseExpression();
                    list.add(x);
                    while (peek(",")) {
                        match(",");
                        x = parseExpression();
                        list.add(x);
                    }
                    if (peek(")")) {
                        match(")");
                        result = new Ast.Expr.Function(Optional.of(receiver), name, list);
                    } else {
                        throw new ParseException("Error(169): No closing bracket in nested comma ting or subsequent expressions", indexFinder());
                    }
                }
            } else {
                throw new ParseException("Error: @ index " + indexFinder() + " there is no identifier after the period", indexFinder());
            }
        }
        return result;
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

        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expr.Literal(null);
        } else if (peek("TRUE")) {
            //match to advance
            match("TRUE");
            Boolean b = new Boolean(true);
            //Ast.Expr.Literal x = new Ast.Expr.Literal(b);
            return new Ast.Expr.Literal(b);
        } else if (peek("FALSE")) {
            match("FALSE");
            Boolean b = new Boolean(false);
            return new Ast.Expr.Literal(b);
        } else if (peek(Token.Type.INTEGER)) {
            int x = Integer.parseInt(tokens.get(0).getLiteral());
            BigInteger i = BigInteger.valueOf(x);
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(i);
        } else if (peek(Token.Type.DECIMAL)) {
            double x = Double.parseDouble(tokens.get(0).getLiteral());
            BigDecimal i = BigDecimal.valueOf(x);
            match(Token.Type.DECIMAL);
            return new Ast.Expr.Literal(i);
        } else if (peek(Token.Type.CHARACTER)) {
            String c = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            //lets get rid of the (') tings at each end
            c = c.substring(1);
            c = c.substring(0, (c.length() - 1));
            //now lets handle converting the escapes
            c = c.replace("\\b", "\b");
            c = c.replace("\\n", "\n");
            c = c.replace("\\r", "\r");
            c = c.replace("\\t", "\t");
            c = c.replace("\\\'", "\'");
            c = c.replace("\\\"", "\"");
            c = c.replace("\\\\", "\\");

            Character cc = new Character(c.charAt(0));
            return new Ast.Expr.Literal(cc);
        } else if (peek(Token.Type.STRING)) {
            String c = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            System.out.println(c);
            c = c.substring(1);
            c = c.substring(0, (c.length() - 1));
            // " at begining and end have been removed

            c = c.replace("\\b", "\b");
            c = c.replace("\\n", "\n");
            c = c.replace("\\r", "\r");
            c = c.replace("\\t", "\t");
            c = c.replace("\\\'", "\'");
            c = c.replace("\\\"", "\"");
            c = c.replace("\\\\", "\\");
            //now we have taken care of the escaped tings
            System.out.println(c);
            return new Ast.Expr.Literal(c);
        }
        //now the recursive tings
        else if (peek("(")) {
            match("(");
            Ast.Expr x = parseExpression();
            if (match(")")) {
                return new Ast.Expr.Group(x);
            } else {
                throw new ParseException("Error: @ index " + indexFinder() + " missing the closing ting", indexFinder());
            }
        } else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            List<Ast.Expr> list = new ArrayList<Ast.Expr>();
            if (peek("(") == false) {
                return new Ast.Expr.Access(empty(), name);
            } else if (peek("(", ")")) {
                match("(", ")");
            } else if (peek("(")) {//theres stuff inside the parameters
                match("(");
                Ast.Expr x = parseExpression();
                list.add(x);
                while (peek(",")) {
                    match(",");
                    x = parseExpression();
                    list.add(x);
                }
                if (peek(")")) {
                    match(")");
                } else {
                    throw new ParseException("Error(169): No closing bracket in nested comma ting or subsequent expressions", indexFinder());
                }
            }
            return new Ast.Expr.Function(empty(), name, list);
        }

        throw new ParseException("Error: @ index " + indexFinder() + " no handle-able ting found", indexFinder());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
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
            for (int i = 0; i < patterns.length; i++) {
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

        public void goback() {
            index--;
        }

    }

}
//ready to submit final ting