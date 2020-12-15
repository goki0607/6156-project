package com.rvclass.labelchains.rewriter;

// java util imports
import java.util.Iterator;

// javaparser imports
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class DeclarationEvaluatorManager {

  private int ce_counter = 0;
  private final static String DE_NAME = "_de_";
  private final static String DE_TYP = "DeclarationEvaluator";
  private final static String DE_VAR = "var";
  private final static String DE_LVL = "lvl";
  private final static String DE_DECL = "declare";

  public DeclarationEvaluatorManager() { }

  public String getClassName() {
    return DE_TYP;
  }

  public String newName() {
    String name = DE_NAME + ce_counter;
    ce_counter++;
    return name;
  }

  public ExpressionStmt createDE(String name) {
    ClassOrInterfaceType typ = new ClassOrInterfaceType(null, DE_TYP);
    Expression init = new ObjectCreationExpr(null, typ, new NodeList<Expression>());
    VariableDeclarator decl = new VariableDeclarator(typ, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(decl));
  }

  public ExpressionStmt populateVar(String name, SimpleName arg) {
    NodeList<Expression> e_args = new NodeList<>();
    /*while (args.hasNext()) {
      e_args.add(new NameExpr(args.next()));
    }*/
    //e_args.add(new NameExpr(arg));
    e_args.add(new StringLiteralExpr(arg.asString()));
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(DE_VAR);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt populateLvl(String name, String lvl) {
    StringLiteralExpr lvl_e = new StringLiteralExpr(lvl);
    NodeList<Expression> e_args = new NodeList<>();
    e_args.add(lvl_e);
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(DE_LVL);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateDeclareCheck(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(DE_DECL);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }
}
