package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        //TODO
        visit(ast.getExpression());
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
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
                scope.lookupVariable(ast.getReceiver().toString()).setValue(visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {

        //throw new UnsupportedOperationException(); //TODO
        /*
        private final Expr condition;
            private final List<Stmt> thenStatements;
            private final List<Stmt> elseStatements;*/
        if(requireType(Boolean.class, visit(ast.getCondition() ) ) ){
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

        /*private final String name;
            private final Expr value;
            private final List<Stmt> statements;*/
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
        /*private final Expr condition;
            private final List<Stmt> statements;*/
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

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException(); //TODO
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
