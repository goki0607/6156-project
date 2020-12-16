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

public class ConditionEvaluatorManager {

  private int be_counter = 0;
  private final static String CE_NAME = "_ce_";
  private final static String CE_TYP = "ConditionEvaluator";
  private final static String CE_ADD = "add";
  /* private final static String CE_DEL = "remove"; */
  private final static String CE_ENTER = "enter";
  private final static String CE_DONE = "done";

  public ConditionEvaluatorManager() { }

  public String getClassName() {
    return CE_TYP;
  }

  public String newName() {
    String name = CE_NAME + be_counter;
    be_counter++;
    return name;
  }

  public ExpressionStmt createCE(String name) {
    ClassOrInterfaceType typ = new ClassOrInterfaceType(null, CE_TYP);
    Expression init = new ObjectCreationExpr(null, typ, new NodeList<Expression>());
    VariableDeclarator decl = new VariableDeclarator(typ, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(decl));
  }

  public ExpressionStmt populateCE(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      e_args.add(new NameExpr(args.next()));
      //e_args.add(new StringLiteralExpr(args.next().asString()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(CE_ADD);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  /*
  public ExpressionStmt depopulateCE(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      e_args.add(new NameExpr(args.next()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(CE_DEL);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }
  */

  public ExpressionStmt generateBranchEnter(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(CE_ENTER);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateBranchDone(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(CE_DONE);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }
}
