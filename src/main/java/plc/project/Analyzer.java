package plc.project;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) { //finished we think
        if(ast.getCondition().getType() != Environment.Type.BOOLEAN)
            throw new RuntimeException("The value is not of type boolean");
        else{
            //while( requireType(Boolean.class, visit(ast.getCondition() ) ) ){
            try{ //reused from Interpreter
                scope = new Scope(scope);
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
    public Void visit(Ast.Stmt.Return ast) {

        try{
            requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
        } catch(RuntimeException x){
            //if catches a runtime exception
            throw new RuntimeException("The value is not assignable to the return type of the method it's in");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {

        //throw new UnsupportedOperationException();  // TODO
        if(ast.getLiteral() == Environment.NIL || ast.getLiteral() instanceof Boolean || ast.getLiteral() instanceof Character || ast.getLiteral() instanceof String){
            return null;
        }
        else if(ast.getType() == Environment.Type.INTEGER){
            if(((Integer) ast.getLiteral()).intValue() > Integer.MAX_VALUE){
                throw new RuntimeException("Integer is bigger than the 32-bit signed integer max value");
            }
        }
        else if(ast.getType() == Environment.Type.DECIMAL){
            double x = ((BigDecimal)ast.getLiteral()).doubleValue();
            if(x > Double.MAX_VALUE)
                throw new RuntimeException("Decimal is bigger than the 64-bit signed float max value");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {

        //throw new UnsupportedOperationException();  // TODO
        //validates group expression
        if(ast.getExpression() instanceof Ast.Expr.Binary == false){
            throw new RuntimeException("contained expression in group is not a binary expr");
        }
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) { //done?

        //validates? binary expression - setting its type to appr. result type
        if(ast.getOperator().equals("AND") || ast.getOperator().equals("OR")){
            if(ast.getLeft().getType() != Environment.Type.BOOLEAN || ast.getRight().getType() != Environment.Type.BOOLEAN){
                throw new RuntimeException("Error: both operators must be booleans");
            }
            else{
                ast.setType(Environment.Type.BOOLEAN);
            }
        }
        else if(ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") || ast.getOperator().equals(">=") || ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            compAndSame(ast.getLeft().getType(), ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getOperator().equals("+")){
            if(ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING){
                ast.setType(Environment.Type.STRING);
            }
            else if(ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL){
                if(ast.getLeft().getType() == ast.getRight().getType()){
                    ast.setType(ast.getLeft().getType());
                }
            }
            else{
                throw new RuntimeException("The '+' section is going wrong");
            }
        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")){
            if(ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL){
                if(ast.getLeft().getType() == ast.getRight().getType()){
                    ast.setType(ast.getLeft().getType());
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

        throw new UnsupportedOperationException();  // TODO
        //same shit as FUNCTION below
    }

    @Override
    public Void visit(Ast.Expr.Function ast) { //missing the validate step
        //validate function expression


        //then sets function of expr which internally sets the type
        // of the expression to be the return type of the function

        if((ast.getReceiver().isPresent())){ //"the variable is a method of the receiver is present"
            method.setFunction(ast.getFunction()); //?
        }
        else if(!(ast.getReceiver().isPresent())){ //otherwise it is a function in the current scope.
//            scope.defineFunction(ast.getName(), ast.getArguments().size()-1, args -> {
//                ast.getFunction().invoke();
//            });
            //method.setFunction(ast.getFunction()); //?
        }

        //also checks that arguments are assignable to the param types of the function (method field)
        for(int i = 1; i < method.getParameterTypeNames().size(); i++){
            requireAssignable(s2t(method.getParameterTypeNames().get(i)), ast.getArguments().get(i).getType());
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
        if((target == Environment.Type.COMPARABLE && type == Environment.Type.COMPARABLE) && (target == type)){
            return;
        }
        else{
            throw new RuntimeException("No comply with COMP AND SAME");
        }
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //assume correct
        if(!(target == Environment.Type.COMPARABLE && type == Environment.Type.COMPARABLE) && !(target == Environment.Type.ANY) && !(target == type)){
            throw new RuntimeException("Error: Not Assignable");
        }
        /*
        if(target == Environment.Type.COMPARABLE && type == Environment.Type.COMPARABLE){
            return;
        }
        else if(target == Environment.Type.ANY){
            return;
        }
        else if(target == type){
            return;
        }
        else{
            //..throwe the tinfg
            throw new RuntimeException("Error: Not Assignable");
        }*/
    }

    //OH NOTES
    /*
    ask about function ting - what they be meaning by 'validate'



     */

}
