package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");

        indent++;
        for(Ast.Field x : ast.getFields()){
            newline(indent);
            print(x);
        }
        indent--;
        newline(indent);

        indent++;
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        indent--;
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");
        indent--;
        newline(indent);

        indent++;
        for(Ast.Method x : ast.getMethods()){
            newline(indent);
            print(x);
        }

        indent--;
        newline(indent);

        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        //again do we assume the type name is correctly formatted
        if(ast.getValue().isPresent()){
            print(ast.getTypeName()," ",ast.getName()," = ",visit(ast.getValue().get()),";");
        }
        else{
            print(ast.getTypeName()," ",ast.getName(),";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //USE GetVar.getType.GetJVNname for return typ
        //again the concatenation issue = fix with helper function that visist them in place
        String inner = "";

        for(int i = 0; i < ast.getParameters().size(); i++){
            if(i < ast.getParameters().size()-1){
                inner = inner + ast.getParameterTypeNames().get(i) +" "+ ast.getParameters().get(i) + ", ";
            }
            else{
                inner = inner + ast.getParameterTypeNames().get(i) +" "+ ast.getParameters().get(i);
            }

        }
        print(ast.getFunction().getReturnType().getJvmName()," ",ast.getFunction().getJvmName(),"(",inner,") {");
        indent++;
        for(Ast.Stmt x : ast.getStatements()){
            newline(indent);
            print((x));
        }
        indent--;
        newline(indent);
        print("}");


        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print((ast.getExpression()),";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        String type = ast.getVariable().getType().getJvmName();

        if(ast.getValue().isPresent()){
            print(type," ",ast.getName()," = ",ast.getValue().get(),";");
        }
        else{
            print(type," ",ast.getName(),";");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print((ast.getReceiver())," = ",(ast.getValue()),";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (",(ast.getCondition()),") {");
        indent++;
        for(Ast.Stmt x : ast.getThenStatements()){
            newline(indent);
            print((x));
        }
        indent--;
        newline(indent);
        if(ast.getElseStatements().size() < 1){
            print("}");

        }
        else{
            print("} else {");
            indent++;
            for(Ast.Stmt x : ast.getElseStatements()){
                newline(indent);
                print((x));
            }
            indent--;
            newline(indent);
            print("}");

        }

        return null;
    }

    public String t2s(Environment.Type x){//strint to type
        if(x.equals(Environment.Type.ANY))
            return "Any";
        if(x.equals(Environment.Type.NIL))
            return "Nil";
        if(x.equals(Environment.Type.INTEGER_ITERABLE))
            return "IntegerIterable";
        if(x.equals(Environment.Type.COMPARABLE))
            return "Comparable";
        if(x.equals(Environment.Type.BOOLEAN))
            return "boolean";
        if(x.equals(Environment.Type.INTEGER))
            return "int";
        if(x.equals(Environment.Type.DECIMAL))
            return "double";
        if(x.equals(Environment.Type.CHARACTER))
            return "char";
        if(x.equals(Environment.Type.STRING))
            return "String";
        throw new RuntimeException("Something went wrong with s2t");
    }

    @Override
    public Void visit(Ast.Stmt.For ast) { //estoy perdidisimo
        //would value.getType be LIST or the TYPE OF THE LIST
        Environment.Type vartype = ast.getValue().getType();
        String type = t2s(vartype);
        print("for (",type," ",ast.getName()," : ",visit(ast.getValue()),") {");
        indent++;
        for(Ast.Stmt x : ast.getStatements()){
            newline(indent);
            print((x));
        }
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (",(ast.getCondition()),") {");
        indent++;
        for(Ast.Stmt x : ast.getStatements()){
            newline(indent);
            print((x));
        }
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ",(ast.getValue()),";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object x = ast.getLiteral();
        Environment.Type t = ast.getType();
        if(t.equals(Environment.Type.CHARACTER)){
            print("'",x,"'");
        }
        else if(t.equals(Environment.Type.STRING)){
            print("\"",x,"\"");
        }
        else if(t.equals(Environment.Type.BOOLEAN)){
            if(x.equals(Boolean.TRUE)){
                print("true");
            }
            else{
                print("false");
            }
        }
        else{
            print(x);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(",(ast.getExpression()),")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        Ast.Expr left = ast.getLeft();
        Ast.Expr right = ast.getRight();

        /** NOT VISITING **/ //collapsed under this line

        if(op.equals("AND")){
            print(left," && ",right);
        }
        else if(op.equals("OR")){
            print(left,"||",right);
        }
        else if(op.equals("<")){
            print(left,"<",right);
        }
        else if(op.equals(">")){
            print(left,">",right);
        }
        else if(op.equals("<=")){
            print(left,"<=",right);
        }
        else if(op.equals(">=")){
            print(left,">=",right);
        }
        else if(op.equals("==")){
            print(left,"==",right);
        }
        else if(op.equals("!=")){
            print(left,"!=",right);
        }
        else if(op.equals("+")){
            print(left," + ",right);
        }
        else if(op.equals("-")){
            print((left)," - ",(right));
        }
        else if(op.equals("*")){
            print((left)," * ",(right));
        }
        else if(op.equals("/")){
            print((left)," / ",(right));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        String name = ast.getVariable().getJvmName();
        if(ast.getReceiver().isPresent()){
            print(visit(ast.getReceiver().get()),".",(name));
        }
        else{
            print(name);
        }
        return null;
    }

    public Object functionHelper(List<Ast.Expr> list){
        for(int i = 0; i < list.size(); i++){
            if(i < list.size()-1){
                visit(list.get(i));
                print(", ");
            }
            else{
                visit(list.get(i));
            }
        }
        return new Object();
    }
    @Override
    public Void visit(Ast.Expr.Function ast) {
        String name = ast.getFunction().getJvmName();
        //String args = "";//concatenation of arguuments for printing
        //how to visit them and concatenate them though?

        //issue: when they're visited, they are printed

        //this shouldnt work cause they havent been visited

        if(ast.getReceiver().isPresent()){
            print((ast.getReceiver().get()),".",name,"(");
            functionHelper(ast.getArguments());
            print(")");
        }
        else{
            print(name,"(");
            functionHelper(ast.getArguments());
            print(")");
        }
        return null;
    }

}
/*
- concatenation issue in METHOD and FUNCTION = helpoer fn






DO NOT VISIT INSIDE A CALL TO PRINT

 */
