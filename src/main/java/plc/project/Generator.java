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
            //convert type names to jvm names (use getjvmname() in environment)
            print(ast.getVariable().getJvmName() ," ",ast.getName()," = ",(ast.getValue().get()),";");
        }
        else{
            print(ast.getVariable().getJvmName() ," ",ast.getName(),";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        //is getparamNames formatted correctly? - used a helper function for formatting in FUNCTION

        String inner = "";
        for(int i = 0; i < ast.getParameters().size(); i++){
            inner = inner + ast.getFunction().getParameterTypes().get(i).getJvmName() +" "+ ast.getParameters().get(i);
            if(i < ast.getParameters().size()-1){
                inner = inner + ", ";
            }
        }
        print(ast.getFunction().getReturnType().getJvmName()," ",ast.getFunction().getJvmName(),"(",inner,") {");
        if(ast.getStatements().size() == 0){
            print("}");
        }
        else{
            indent++;
            for(Ast.Stmt x : ast.getStatements()){
                newline(indent);
                print((x));
            }
            indent--;
            newline(indent);
            print("}");
        }

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
        if(ast.getThenStatements().size() > 0){
            indent++;
            for(Ast.Stmt x : ast.getThenStatements()){
                newline(indent);
                print((x));
            }
            indent--;
            newline(indent);
            print("}");
        }
        else{
            print("}");
        }
        if(ast.getElseStatements().size() > 0){
            print(" else {");
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

    @Override
    public Void visit(Ast.Stmt.For ast) {
        String type = ast.getValue().getType().getJvmName();
        print("for (",type," ",ast.getName()," : ",(ast.getValue()),") {");
        if(ast.getStatements().size() > 0){
            indent++;
            for(Ast.Stmt x : ast.getStatements()){
                newline(indent);
                print((x));
            }
            indent--;
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //special case of statements being empty
        print("while (",(ast.getCondition()),") {");
        if(ast.getStatements().size() > 0){
            indent++;
            for(Ast.Stmt x : ast.getStatements()){
                newline(indent);
                print((x));
            }
            indent--;
            newline(indent);
        }
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

        if(op.equals("AND")){
            print(left, " ", "&&", " ", right);
        }
        else if(op.equals("OR")){
            print(left, " ", "||", " ", right);
        }
        else{
            print(left, " ", op, " ", right);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        String name = ast.getVariable().getJvmName();
        if(ast.getReceiver().isPresent()){
            print((ast.getReceiver().get()),".");
        }
        print(name);
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        String name = ast.getFunction().getJvmName();
        if(ast.getReceiver().isPresent()){
            print((ast.getReceiver().get()),".");
        }
        print(name,"(");
        if(ast.getArguments().size() > 0){
            for(int i = 0; i < ast.getArguments().size(); i++){
                print(ast.getArguments().get(i));
                if(i < ast.getArguments().size()-1){
                    print(", ");
                }
            }
        }
        print(")");
        return null;
    }
}

//ready for final submission

