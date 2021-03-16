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
        Scope temp = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            //this is the callback function
            //begin callback function
            Scope b4 = scope;
            scope = new Scope(temp);
            //Scope child = scope; //perhaps change all instances of scope inside lambda to child
            int i = 0;
            for(String s : ast.getParameters()){
                scope.defineVariable(s, args.get(i));
                i++;
            }
            //evaluate the methods statements
            try{
                ast.getStatements().forEach(this::visit);
                return Environment.NIL;
            } catch(Return ret){
                return ret.value;
            }
            finally {
                scope = b4;//scope.getParent(); //done defining variables?
            }
            //end callback function
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()){
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        else{
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        //check if receiver is of type access
        if(ast.getReceiver() instanceof Ast.Expr.Access){
            if(((Ast.Expr.Access) ast.getReceiver()).getReceiver().isPresent()){
                visit(((Ast.Expr.Access)ast.getReceiver()).getReceiver().get()).setField(((Ast.Expr.Access) ast.getReceiver()).getName(),visit(ast.getValue()));
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
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());
            //if(requireType(Comparable.class, left) && requireType(Comparable.class, right)){
                int compres = (requireType(Comparable.class, left)).compareTo(requireType(Comparable.class, right)); //compres = comparison result
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
            //}
            //else{
            //    throw new RuntimeException("Error: HERE IS THE ISSUE - Left and/or Right are not comparable");
            //}
        }
        //equalities
        else if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            if(ast.getOperator().equals("==")){
                if(visit(ast.getLeft()).equals(visit(ast.getRight()))){
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
                else{
                    return new Environment.PlcObject(scope, Boolean.FALSE);
                }
            }
            else if(ast.getOperator().equals("!=")){
                if(visit(ast.getLeft()).equals(visit(ast.getRight())) == false){
                    return new Environment.PlcObject(scope, Boolean.TRUE);
                }
                else{
                    return new Environment.PlcObject(scope, Boolean.FALSE);
                }
            }
        }
        //the plus sign
        else if(ast.getOperator().equals("+")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());
            if(left.getValue() instanceof String || right.getValue() instanceof String){
                String x = ((String)left.getValue())+((String)right.getValue());
                return new Environment.PlcObject(scope, x);
            }
            else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger){
                BigInteger x = ((BigInteger)left.getValue()).add((BigInteger)right.getValue());
                return new Environment.PlcObject(scope,x);
            }
            else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)left.getValue()).add((BigDecimal)right.getValue());
                return new Environment.PlcObject(scope,x);
            }
            throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());
            if(ast.getOperator().equals("-")){
                if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger){
                    BigInteger x = ((BigInteger)(left).getValue()).subtract((BigInteger)right.getValue());
                    return new Environment.PlcObject(scope,x);
                }
                else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal){
                    BigDecimal x = ((BigDecimal)(left).getValue()).subtract((BigDecimal)right.getValue());
                    return new Environment.PlcObject(scope,x);
                }
                else{
                    throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
                }
            }
            else{
                if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger){
                    BigInteger x = ((BigInteger)(left).getValue()).multiply((BigInteger)right.getValue());
                    return new Environment.PlcObject(scope,x);
                }
                else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal){
                    BigDecimal x = ((BigDecimal)(left).getValue()).multiply((BigDecimal)right.getValue());
                    return new Environment.PlcObject(scope,x);
                }
                else{
                    throw new RuntimeException("Error: the BigInteger/BigDecimal types are mismatched for ast.left and ast.right");
                }
            }
        }
        else if(ast.getOperator().equals("/")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());
            if(left.getValue() instanceof BigInteger && ((BigInteger)left.getValue()).compareTo(BigInteger.ZERO) == 0){
                throw new RuntimeException("Error: denominator is zero");
            }
            if(left.getValue() instanceof BigDecimal && ((BigDecimal)left.getValue()).compareTo(BigDecimal.ZERO) == 0){
                throw new RuntimeException("Error: denominator is zero");
            }
            if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger){
                BigInteger x = ((BigInteger)left.getValue()).divide((BigInteger)right.getValue());
                return new Environment.PlcObject(scope,x);
            }
            else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal){
                BigDecimal x = ((BigDecimal)left.getValue()).divide(((BigDecimal)right.getValue()),1,BigDecimal.ROUND_HALF_EVEN);
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
            System.out.println("theres a receiver");
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
//ready for test submission