package plc.project;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//ready for final submission


/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        //reused from interpreter
        try{
            Environment.Function f = scope.lookupFunction("main",0);
            requireAssignable(Environment.Type.INTEGER, f.getReturnType());
        } catch(RuntimeException x){
            throw new RuntimeException("in SOURCE fn: something went wrong with main function lookup");
        }
        for(Ast.Field f : ast.getFields()){
            visit(f);
        }
        for(Ast.Method m : ast.getMethods()){
            visit(m);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {

        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            try{
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
            } catch(RuntimeException x){
                throw new RuntimeException("Field function breaks in 'requireAssignable' line");
            }
        }

        Environment.Variable x = scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()),Environment.NIL);
        ast.setVariable(x);

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        List<Environment.Type> ptypes = new ArrayList<Environment.Type>();
        for(int i = 0; i < ast.getParameterTypeNames().size();i++){
            Environment.Type x = Environment.getType(ast.getParameterTypeNames().get(i));
            ptypes.add(x);
        }
        Environment.Function x = scope.defineFunction(ast.getName(), ast.getName(),ptypes,ast.getReturnTypeName().map(Environment::getType).orElse(Environment.Type.NIL), args -> {
            return Environment.NIL;
        });

        ast.setFunction(x);

        try{
            scope = new Scope(scope);
            method = ast;
            for(Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
            method = null;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if(!(ast.getExpression() instanceof Ast.Expr.Function)){
            throw new RuntimeException("Expression function's ast Expression is not an Ast.Expr.Function");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if(ast.getTypeName().isPresent()){
            if(ast.getValue().isPresent()){
                visit(ast.getValue().get());
                try{
                    requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
                } catch(RuntimeException x){
                    throw new RuntimeException("Declaration function breaks in 'requireAssignable' line");
                }
            }
            ast.setVariable(scope.defineVariable(ast.getName(),ast.getName(),Environment.getType(ast.getTypeName().get()),Environment.NIL));
        }
        else if (ast.getValue().isPresent()){
            visit(ast.getValue().get());
            ast.setVariable(scope.defineVariable(ast.getName(),ast.getName(),ast.getValue().get().getType(), Environment.NIL));
        }
        else{
            throw new RuntimeException("Neither the variable's typeName or the Value are present.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        if(!(ast.getReceiver() instanceof Ast.Expr.Access)){
            throw new RuntimeException("Receiver is not an access expression");
        }
        visit(ast.getReceiver());
        visit(ast.getValue()); //added after second feedback cause of third error
        try{
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        } catch(RuntimeException x){
            throw new RuntimeException("Assignment function breaks in 'requireAssignable' line");
        }
        //now we've checked both conditions for throwing exceptions
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN,ast.getCondition().getType());
        if(ast.getThenStatements().size() == 0){
            throw new RuntimeException("The then statement is DNE");
        }
        else{
            try{
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getThenStatements()){
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        try{
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getElseStatements()){
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE,ast.getValue().getType());
        if(ast.getStatements().size() == 0){
            throw new RuntimeException("The for statement is invalid");
        }
        else{
            try{ //reused from Interpreter
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) { //finished we think
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN,ast.getCondition().getType());

        try{ //reused from Interpreter
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {

        visit(ast.getValue()); //because we are getting the type later
        try{
            requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
            //requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
        } catch(RuntimeException x){
            //if catches a runtime exception
            throw new RuntimeException("The value is not assignable to the return type of the method it's in");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) { //GOTTA FIX THIS
        //explicitly set TYPE of each literal to get rid of the initial NULL value
        if(ast.getLiteral().equals(Environment.NIL)){
            ast.setType(Environment.Type.NIL);
        }
        else if(ast.getLiteral() instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getLiteral() instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
        }
        else if(ast.getLiteral() instanceof String){
            ast.setType(Environment.Type.STRING);
        }
        else if(ast.getLiteral() instanceof BigInteger){
            //reference: https://stackoverflow.com/questions/50588328/how-to-check-a-number-is-out-of-the-range-of-integer-type
            BigInteger val = new BigInteger(""+ast.getLiteral());
            BigInteger max = new BigInteger(String.valueOf(Integer.MAX_VALUE));
            BigInteger min = new BigInteger(String.valueOf(Integer.MIN_VALUE));
            if(val.compareTo(max) > 0 || val.compareTo(min) < 0){ //research Java BigInteger class to check it against values
                throw new RuntimeException("Integer is bigger/smaller than the 32-bit signed integer max/min value");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if(ast.getLiteral() instanceof BigDecimal){
            BigDecimal x = new BigDecimal(""+ast.getLiteral());
            if((x.doubleValue()) == (Double.POSITIVE_INFINITY) || (x.doubleValue()) == (Double.NEGATIVE_INFINITY)){
                throw new RuntimeException("Decimal is bigger/smaller than the 32-bit signed decimal max/min value");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //throw new UnsupportedOperationException();  // TODO
        //validates group expression
        if(!(ast.getExpression() instanceof Ast.Expr.Binary)){
            throw new RuntimeException("contained expression in group is not a binary expr");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) { //done?
        visit(ast.getLeft());
        visit(ast.getRight());
        //validates? binary expression - setting its type to appr. result type
        if(ast.getOperator().equals("AND") || ast.getOperator().equals("OR")){
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") || ast.getOperator().equals(">=") || ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            compAndSame(ast.getLeft().getType(), ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getOperator().equals("+")){
            if(ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)){
                ast.setType(Environment.Type.STRING);
            }
            else if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                if(ast.getLeft().getType().equals(ast.getRight().getType())){
                    ast.setType(ast.getLeft().getType());
                }
                else{
                    throw new RuntimeException("The '+' section is going wrong - inside the INTEGER/DECIMAL sub-section");
                }
            }
            else{
                throw new RuntimeException("The '+' section is going wrong");
            }
        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")){
            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                if(ast.getLeft().getType().equals(ast.getRight().getType())){
                    ast.setType(ast.getLeft().getType());
                }
                else{
                    throw new RuntimeException("The '-' & '*' & '/' section is going wrong - in the subsection");
                }
            }
            else{
                throw new RuntimeException("The '-' & '*' & '/' section is going wrong");
            }
        }
        return null;

    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if((ast.getReceiver().isPresent())){
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        }
        else{ //otherwise it is a function in the current scope.
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if((ast.getReceiver().isPresent())){ //
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(),ast.getArguments().size()));
            for(int i = 0; i < ast.getArguments().size(); i++){
                visit(ast.getArguments().get(i)); //to set the type to something other than null
                requireAssignable((ast.getFunction().getParameterTypes().get(i+1)), ast.getArguments().get(i).getType());
            }
        }
        else{ //otherwise it is a function in the current scope.
            ast.setFunction(scope.lookupFunction(ast.getName(),ast.getArguments().size()));
            for(int i = 0; i < ast.getArguments().size(); i++){
                visit(ast.getArguments().get(i)); //to set the type to something other than null
                requireAssignable((ast.getFunction().getParameterTypes().get(i)), ast.getArguments().get(i).getType());
            }
        }
        return null;
    }

    //helper function
    public Environment.Type s2t(String x){//strint to type
        if(x.equals("Any"))
            return Environment.Type.ANY;
        if(x.equals("Nil"))
            return Environment.Type.NIL;
        if(x.equals("IntegerIterable"))
            return Environment.Type.INTEGER_ITERABLE;
        if(x.equals("Comparable"))
            return Environment.Type.COMPARABLE;
        if(x.equals("Boolean"))
            return Environment.Type.BOOLEAN;
        if(x.equals("Integer"))
            return Environment.Type.INTEGER;
        if(x.equals("Decimal"))
            return Environment.Type.DECIMAL;
        if(x.equals("Character"))
            return Environment.Type.CHARACTER;
        if(x.equals("String"))
            return Environment.Type.STRING;
        throw new RuntimeException("Something went wrong with s2t");
    }

    public static void compAndSame(Environment.Type target, Environment.Type type) {
        if((target.equals(Environment.Type.COMPARABLE) && type.equals(Environment.Type.COMPARABLE)) && (target.equals(type))){
            return;
        }
        else{
            throw new RuntimeException("No comply with COMP AND SAME");
        }
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //assume correct
        if(target.equals(type)){
            return;
        }
        else if(target.equals(Environment.Type.COMPARABLE)){
            if(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)){
                return;
            }
        }
        else if(target.equals(Environment.Type.ANY)){
            return;
        }
        else{
            throw new RuntimeException("Error: Not Assignable");
        }
    }
    //ready for final submission

}
