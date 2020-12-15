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

public class UntakenBranchEvaluatorManager {

  private int ce_counter = 0;
  private final static String BE_NAME = "_ube_";
  private final static String BE_TYP = "UntakenBranchEvaluator";
  private final static String BE_ADD = "add";
  private final static String BE_DONE = "done";
  private final static String BE_DEL = "remove";

  public UntakenBranchEvaluatorManager() { }

  public String getClassName() {
    return BE_TYP;
  }

  public String newName() {
    String name = BE_NAME + ce_counter;
    ce_counter++;
    return name;
  }

  public ExpressionStmt createUBE(String name) {
    ClassOrInterfaceType typ = new ClassOrInterfaceType(null, BE_TYP);
    Expression init = new ObjectCreationExpr(null, typ, new NodeList<Expression>());
    VariableDeclarator decl = new VariableDeclarator(typ, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(decl));
  }

  public ExpressionStmt populateUBE(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      //e_args.add(new NameExpr(args.next()));
      e_args.add(new StringLiteralExpr(args.next().asString()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(BE_ADD);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt depopulateUBE(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      //e_args.add(new NameExpr(args.next()));
      e_args.add(new StringLiteralExpr(args.next().asString()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(BE_DEL);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateBranchDone(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(BE_DONE);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }
}
