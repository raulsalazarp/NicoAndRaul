package plc.project;

import java.io.PrintWriter;

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
        //assume return type name is formatted correctly or do as we did in declaration
        //again the concatenation issue
        String inner = "";
        for(int i = 0; i < ast.getParameters().size(); i++){
            if(i < ast.getParameters().size()-1){
                inner = inner + ast.getParameterTypeNames().get(i) +" "+ ast.getParameters().get(i) + ", ";
            }
            else{
                inner = inner + ast.getParameterTypeNames().get(i) +" "+ ast.getParameters().get(i);
            }

        }
        print(ast.getReturnTypeName()," ",ast.getName(),"(",inner,") {");
        for(Ast.Stmt x : ast.getStatements()){
            newline(1);
            print(visit(x),";");
        }
        newline(0);
        print("}");
        newline(0); //new line after closing bracket

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(visit(ast.getExpression()),";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if(ast.getTypeName().isPresent()){  //what does type name look like
            String type = (ast.getTypeName().get());
            if(type.equals("Integer"))
                type = "int";
            else if(type.equals("Decimal"))
                type = "double";
            else if(type.equals("Character"))
                type = "char";
            else if(type.equals("Boolean"))
                type = "boolean";

            if(ast.getValue().isPresent()){
                print(type," ",ast.getName()," = ",ast.getValue().get(),";");
            }
            else{
                print(type," ",ast.getName(),";");
            }
        }
        //

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(visit(ast.getReceiver())," = ",visit(ast.getValue()),";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (",visit(ast.getCondition()),") {");
        for(Ast.Stmt x : ast.getThenStatements()){
            newline(1);
            print(visit(x),";");
        }
        newline(0);
        if(ast.getElseStatements().size() < 1){
            print("}");
            newline(0);
        }
        else{
            print("} else {");
            for(Ast.Stmt x : ast.getElseStatements()){
                newline(1);
                print(visit(x),";");
            }
            newline(0);
            print("}");
            newline(0);
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
        for(Ast.Stmt x : ast.getStatements()){
            newline(1);
            print(visit(x),";");
        }
        newline(0);
        print("}");
        newline(0); //new line after closing bracket
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (",visit(ast.getCondition()),") {");
        for(Ast.Stmt x : ast.getStatements()){
            newline(1);
            print(visit(x),";");
        }
        newline(0);
        print("}");
        newline(0); //new line after closing bracket
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ",visit(ast.getValue()),";");
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
        print("(",visit(ast.getExpression()),")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        Ast.Expr left = ast.getLeft();
        Ast.Expr right = ast.getRight();

        /** VISITING **/ //so that literals are formatted
        //print(visit(left),"&&",visit(right));
        if(op.equals("AND")){
            print(visit(left)," && ",visit(right));
        }
        else if(op.equals("OR")){
            print(visit(left)," || ",visit(right));
        }
        else if(op.equals("<")){
            print(visit(left)," < ",visit(right));
        }
        else if(op.equals(">")){
            print(visit(left)," > ",visit(right));
        }
        else if(op.equals("<=")){
            print(visit(left)," <= ",visit(right));
        }
        else if(op.equals(">=")){
            print(visit(left)," >= ",visit(right));
        }
        else if(op.equals("==")){
            print(visit(left)," == ",visit(right));
        }
        else if(op.equals("!=")){
            print(visit(left)," != ",visit(right));
        }
        else if(op.equals("+")){
            print(visit(left)," + ",visit(right));
        }
        else if(op.equals("-")){
            print(visit(left)," - ",visit(right));
        }
        else if(op.equals("*")){
            print(visit(left)," * ",visit(right));
        }
        else if(op.equals("/")){
            print(visit(left)," / ",visit(right));
        }

        /** NOT VISITING **/ //collapsed under this line
        /*
//        if(op.equals("AND")){
//            print(left,"&&",right);
//        }
//        else if(op.equals("OR")){
//            print(left,"||",right);
//        }
//        else if(op.equals("<")){
//            print(left,"<",right);
//        }
//        else if(op.equals(">")){
//            print(left,">",right);
//        }
//        else if(op.equals("<=")){
//            print(left,"<=",right);
//        }
//        else if(op.equals(">=")){
//            print(left,">=",right);
//        }
//        else if(op.equals("==")){
//            print(left,"==",right);
//        }
//        else if(op.equals("!=")){
//            print(left,"!=",right);
//        }
*/
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

    @Override
    public Void visit(Ast.Expr.Function ast) {
        String name = ast.getFunction().getJvmName();
        String args = "";//concatenation of arguuments for printing
        //how to visit them and concatenate them though?

        //issue: when they're visited, they are printed

        //this shouldnt work cause they havent been visited
        for(int i = 0; i < ast.getArguments().size(); i++){
            if(i < ast.getArguments().size()-1){
                args = args + ast.getArguments().get(i) + ", ";
            }
            else{
               args = args + ast.getArguments().get(i);
            }
        }
        if(ast.getReceiver().isPresent()){
            print(visit(ast.getReceiver().get()),".",name,"(",args,");");
        }
        else{
            print(name,"(",args,");");
        }
        return null;
    }

}
/*
- concatenation issue in METHOD and FUNCTION

- ask about visiting in binary

- for FOR and WHILE which one is which between value and name

- does the word 'generated' in the instructions mean 'visited'

- DECLARATION - if there is no typename present then how to set the type

- look at note atop METHOD , first line



 */
