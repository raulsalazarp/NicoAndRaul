package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        //throw new UnsupportedOperationException();
        /*
        private final List<Field> fields;
        private final List<Method> methods;
        * */
        for(Ast.Field f : ast.getFields()){
            visit(f);
        }
        for(Ast.Method m : ast.getMethods()){
            visit(m);
        }
        Environment.Function res = scope.lookupFunction("main",0);
        List<Environment.PlcObject> list = new ArrayList<>();
        return res.invoke(list);

    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        //throw new UnsupportedOperationException();
        /*
        private final String name;
        private final Optional<Expr> value;
        * */
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException();
        /*
        * private final String name;
        private final List<String> parameters;
        private final List<Stmt> statements;*/
        /*
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
        * */
//        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
//
//            return (Environment.PlcObject)ast.getStatements();
//
//        });
//        //the lambda has to return the execution and invoke is what executes the function
//        //takes a list of arguments and returns a single value
//
//        //begin callback function
//        scope = new Scope(scope);
//        for(String s : ast.getParameters()){
//            scope.defineVariable(s, visit(s)); //??define vars for args using param names??
//        }
//
//        //evaluate the methods statements
//        for(Ast.Stmt s : ast.getStatements()){
//            visit(s);
//        }
//
//
//        scope = scope.getParent();
//        //end callback function
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        //TODO
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        //TODO
        if(ast.getReceiver().isPresent()){
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        else{
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        //TODO
        //check if receiver is of type access
        if(ast.getReceiver() instanceof Ast.Expr.Access){
            if(((Ast.Expr.Access) ast.getReceiver()).getReceiver().isPresent()){
                visit(((Ast.Expr.Access) ast.getReceiver()).getReceiver().get()).setField(ast.getReceiver().toString(),visit(ast.getValue()));
            }
            else{
                scope.lookupVariable(((Ast.Expr.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))){
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
        else{
            try{
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getElseStatements()){
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable<Environment.PlcObject> ting = requireType(Iterable.class , visit(ast.getValue()));
        for(Environment.PlcObject x : ting){
            try{
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(),x);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        //(given in lecture)
        while( requireType(Boolean.class, visit(ast.getCondition() ) ) ){
            try{
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        //(given in lecture)
        if( ast.getValue().isPresent() ){
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }else{
            scope.defineVariable(ast.getName(),Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() != null)
            return Environment.create(ast.getLiteral());
        else
            return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    public Object getBinLeft(Ast.Expr left){
        Object leftObj = null;
        if(left instanceof Ast.Expr.Access){
            Environment.PlcObject leftleft = scope.lookupVariable(((Ast.Expr.Access) left).getName()).getValue();
            if((leftleft).getValue() instanceof String){
                String x = ((String)leftleft.getValue());
                leftObj = x;
            }
            else if((leftleft).getValue() instanceof BigInteger){
                BigInteger x = ((BigInteger)leftleft.getValue());
                leftObj = x;
            }
            else if((leftleft).getValue() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)leftleft.getValue());
                leftObj = x;
            }
        }
        else{ //if not access
            if(((Ast.Expr.Literal)left).getLiteral() instanceof String){
                String x = ((String)(((Ast.Expr.Literal)left).getLiteral()));
                leftObj = x;
            }
            else if(((Ast.Expr.Literal)left).getLiteral() instanceof BigInteger){
                BigInteger x = ((BigInteger)(((Ast.Expr.Literal)left).getLiteral()));
                leftObj = x;
            }
            else if(((Ast.Expr.Literal)left).getLiteral() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)(((Ast.Expr.Literal)left).getLiteral()));
                leftObj = x;
            }
        }
        return leftObj;
    }

    public Object getBinRight(Ast.Expr right){
        Object rightObj = null;
        if(right instanceof Ast.Expr.Access){
            Environment.PlcObject rightright = scope.lookupVariable(((Ast.Expr.Access) right).getName()).getValue();
            if((rightright).getValue() instanceof String){
                String x = ((String)rightright.getValue());
                rightObj = x;
            }
            else if((rightright).getValue() instanceof BigInteger){
                BigInteger x = ((BigInteger)rightright.getValue());
                rightObj = x;
            }
            else if((rightright).getValue() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)rightright.getValue());
                rightObj = x;
            }
        }
        else{ //if not access
            if(((Ast.Expr.Literal)right).getLiteral() instanceof String){
                String x = ((String)(((Ast.Expr.Literal)right).getLiteral()));
                rightObj = x;
            }
            else if(((Ast.Expr.Literal)right).getLiteral() instanceof BigInteger){
                BigInteger x = ((BigInteger)(((Ast.Expr.Literal)right).getLiteral()));
                rightObj = x;
            }
            else if(((Ast.Expr.Literal)right).getLiteral() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)(((Ast.Expr.Literal)right).getLiteral()));
                rightObj = x;
            }
        }
        return rightObj;
    }
    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        //AND or OR
        if(ast.getOperator().equals("AND") || ast.getOperator().equals("OR")){
            if(ast.getOperator().equals("OR")) {
                if (requireType(Boolean.class, visit(ast.getLeft())) || requireType(Boolean.class, visit(ast.getRight()))) {
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
            }
            else if(ast.getOperator().equals("AND")) {
                if (requireType(Boolean.class, visit(ast.getLeft())) && requireType(Boolean.class, visit(ast.getRight()))) {
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
            }
            return new Environment.PlcObject(scope, Boolean.FALSE);
        }
        //comparisons
        else if(ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") || ast.getOperator().equals(">=")){
            Object left = getBinLeft(ast.getLeft());
            Object right = getBinRight(ast.getRight());
            if(left instanceof Comparable && right instanceof Comparable){
                System.out.println("JUST CHECKING");
                int compres = (((Comparable<Object>)left).compareTo(right)); //compres = comparison result
                if(ast.getOperator().equals("<")){
                    if(compres == -1){ //right greater than left
                        //if confused about return type look at the interpreter tests
                        return new Environment.PlcObject(scope, Boolean.TRUE);
                    }
                    else {
                        return new Environment.PlcObject(scope, Boolean.FALSE);
                    }
                }
                else if(ast.getOperator().equals("<=")){
                    if(compres == -1 || compres == 0){ //right greater than left
                        return new Environment.PlcObject(scope, Boolean.TRUE);
                    }
                    else{
                        return new Environment.PlcObject(scope, Boolean.FALSE);
                    }
                }
                else if(ast.getOperator().equals(">")){
                    if(compres == 1){
                        return new Environment.PlcObject(scope, Boolean.TRUE);
                    }
                    else{
                        return new Environment.PlcObject(scope, Boolean.FALSE);
                    }
                }
                else if(ast.getOperator().equals(">=")){
                    if(compres == 0 || compres == 1){
                        return new Environment.PlcObject(scope, Boolean.TRUE);
                    }
                    else{
                        return new Environment.PlcObject(scope, Boolean.FALSE);
                    }
                }
            }
            else{
                throw new RuntimeException("Error: HERE IS THE ISSUE - Left and/or Right are not comparable");
            }
        }
        //equalities
        else if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            if(ast.getOperator().equals("==")){
                if(ast.getLeft().equals(ast.getRight())){
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
                else{
                    return new Environment.PlcObject(scope, Boolean.FALSE);
                }
            }
            else if(ast.getOperator().equals("!=")){
                if(ast.getLeft().equals(ast.getRight()) == false){
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
                else{
                    return new Environment.PlcObject(scope, Boolean.FALSE);
                }
            }
        }
        //the plus sign

        else if(ast.getOperator().equals("+")){

            Object left = getBinLeft(ast.getLeft());
            Object right = getBinRight(ast.getRight());
            if(left instanceof String || right instanceof String){
                String x = ((String)left)+((String)right);
                return new Environment.PlcObject(scope, x);
            }
            else if(left instanceof BigInteger && right instanceof BigInteger){
                BigInteger x = ((BigInteger)left).add((BigInteger)right);
                return new Environment.PlcObject(scope,x);
            }
            else if(left instanceof BigDecimal && right instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)left).add((BigDecimal)right);
                return new Environment.PlcObject(scope,x);
            }

//            if(ast.getLeft() instanceof Ast.Expr.Access && ast.getRight() instanceof Ast.Expr.Access){ //is it access
//                Environment.PlcObject left = scope.lookupVariable(((Ast.Expr.Access) ast.getLeft()).getName()).getValue();
//                Environment.PlcObject right = scope.lookupVariable(((Ast.Expr.Access) ast.getRight()).getName()).getValue();
//                if((left).getValue() instanceof String || (right).getValue() instanceof String){
//                    String x = ((String)left)+((String)right);
//                    return new Environment.PlcObject(scope, x);
//                }
//                else if((left).getValue() instanceof BigInteger || (right).getValue() instanceof BigInteger){
//                    BigInteger x = ((BigInteger)left).add((BigInteger)right);
//                    return new Environment.PlcObject(scope,x);
//                }
//                else if((left).getValue() instanceof BigDecimal || (right).getValue() instanceof BigDecimal){
//                    BigDecimal x = ((BigDecimal)left.getValue()).add((BigDecimal)right.getValue());
//                    return new Environment.PlcObject(scope,x);
//                }
//            }
//            else{//if either is a string but not access
//                if(((Ast.Expr.Literal)ast.getLeft()).getLiteral() instanceof String || ((Ast.Expr.Literal)ast.getRight()).getLiteral() instanceof String ){
//                    //concatenate the strings
//                    String x = ((String)(((Ast.Expr.Literal)ast.getLeft()).getLiteral()))+((String)(((Ast.Expr.Literal)ast.getRight()).getLiteral()));
//                    return new Environment.PlcObject(scope, x);
//                }
//                else if(((Ast.Expr.Literal)ast.getLeft()).getLiteral() instanceof BigInteger && ((Ast.Expr.Literal)ast.getRight()).getLiteral() instanceof BigInteger){
//                    BigInteger x = ((BigInteger)(((Ast.Expr.Literal)ast.getLeft()).getLiteral())).add(((BigInteger)(((Ast.Expr.Literal)ast.getRight()).getLiteral())));
//                    return new Environment.PlcObject(scope,x);
//                }
//                else if(((Ast.Expr.Literal)ast.getLeft()).getLiteral() instanceof BigDecimal && ((((Ast.Expr.Literal)ast.getRight()).getLiteral())) instanceof BigDecimal){
//                    BigDecimal x = ((BigDecimal)(((Ast.Expr.Literal)ast.getLeft()).getLiteral())).add(((BigDecimal)(((Ast.Expr.Literal)ast.getRight()).getLiteral())));
//                    return new Environment.PlcObject(scope,x);
//                }
//            }
            throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*")){
            Object left = getBinLeft(ast.getLeft());
            Object right = getBinRight(ast.getRight());
            if(ast.getOperator().equals("-")){
                if(left instanceof BigInteger && right instanceof BigInteger){
                    BigInteger x = ((BigInteger)(left)).subtract((BigInteger)right);
                    return new Environment.PlcObject(scope,x);
                }
                else if(left instanceof BigDecimal && right instanceof BigDecimal){
                    BigDecimal x = ((BigDecimal)(left)).subtract((BigDecimal)right);
                    return new Environment.PlcObject(scope,x);
                }
                else{
                    throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
                }
            }
            else{
                if(left instanceof BigInteger && right instanceof BigInteger){
                    BigInteger x = ((BigInteger)(left)).multiply((BigInteger)right);
                    return new Environment.PlcObject(scope,x);
                }
                else if(left instanceof BigDecimal && right instanceof BigDecimal){
                    BigDecimal x = ((BigDecimal)(left)).multiply((BigDecimal)right);
                    return new Environment.PlcObject(scope,x);
                }
                else{
                    throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
                }
            }
        }
        else if(ast.getOperator().equals("/")){
            Object left = getBinLeft(ast.getLeft());
            Object right = getBinRight(ast.getRight());
            if(left instanceof BigInteger && ((BigInteger)left).compareTo(BigInteger.ZERO) == 0){
                throw new RuntimeException("Error: denominator is zero");
            }
            if(left instanceof BigDecimal && ((BigDecimal)left).compareTo(BigDecimal.ZERO) == 0){
                throw new RuntimeException("Error: denominator is zero");
            }
            if(left instanceof BigInteger && right instanceof BigInteger){
                BigInteger x = ((BigInteger)left).divide((BigInteger)right);
                return new Environment.PlcObject(scope,x);
            }
            else if(left instanceof BigDecimal && right instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)left).divide(((BigDecimal)right),1,BigDecimal.ROUND_HALF_EVEN);
                return new Environment.PlcObject(scope,x);
            }
            else{
                throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
            }
        }
        else{
            throw new RuntimeException("Error: something went wrong in binary method");
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        if(ast.getReceiver().isPresent()) {
            ArrayList<Environment.PlcObject> ting = new ArrayList<>();
            for(int i = 0; i < ast.getArguments().size(); i++){
                ting.add(visit(ast.getArguments().get(i)));
            }
            return visit(ast.getReceiver().get()).callMethod(ast.getName(),ting);
        }
        else{
            ArrayList<Environment.PlcObject> ting = new ArrayList<>();
            for(int i = 0; i < ast.getArguments().size(); i++){
                ting.add(visit(ast.getArguments().get(i)));
            }
            Environment.Function x = scope.lookupFunction(ast.getName(),ast.getArguments().size());
            return x.invoke(ting);
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + "."); //TODO
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
