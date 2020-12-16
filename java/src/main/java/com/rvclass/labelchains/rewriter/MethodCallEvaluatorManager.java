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

public class MethodCallEvaluatorManager {

  private int ce_counter = 0;
  private final static String MCE_NAME = "_mce_";
  private final static String MCE_TYP = "MethodCallEvaluator";
  private final static String MCE_RHS = "rhs";
  private final static String MCE_LHS = "lhs";
  private final static String MCE_ASSN = "call";

  public MethodCallEvaluatorManager() { }

  public String getClassName() {
    return MCE_TYP;
  }

  public String newName() {
    String name = MCE_NAME + ce_counter;
    ce_counter++;
    return name;
  }

  public ExpressionStmt createMCE(String name) {
    ClassOrInterfaceType typ = new ClassOrInterfaceType(null, MCE_TYP);
    Expression init = new ObjectCreationExpr(null, typ, new NodeList<Expression>());
    VariableDeclarator decl = new VariableDeclarator(typ, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(decl));
  }

  public ExpressionStmt populateRHS(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      e_args.add(new NameExpr(args.next()));
      //e_args.add(new StringLiteralExpr(args.next().asString()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(MCE_RHS);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt populateLHS(String name, SimpleName lhs) {
    NodeList<Expression> e_args = new NodeList<>();
    e_args.add(new NameExpr(lhs));
    //e_args.add(new StringLiteralExpr(lhs.asString()));
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(MCE_LHS);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateCallCheck(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(MCE_ASSN);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }

}
