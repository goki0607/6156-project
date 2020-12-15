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

public class AssignmentEvaluatorManager {

  private int ce_counter = 0;
  private final static String AE_NAME = "_ae_";
  private final static String AE_TYP = "AssignmentEvaluator";
  private final static String AE_RHS = "rhs";
  private final static String AE_LHS = "lhs";
  private final static String AE_ASSN = "assign";
  private final static String AE_SIM = "simulate";
  private final static String AE_DECL = "decl";

  public AssignmentEvaluatorManager() { }

  public String getClassName() {
    return AE_TYP;
  }

  public String newName() {
    String name = AE_NAME + ce_counter;
    ce_counter++;
    return name;
  }

  public ExpressionStmt createAE(String name) {
    ClassOrInterfaceType typ = new ClassOrInterfaceType(null, AE_TYP);
    Expression init = new ObjectCreationExpr(null, typ, new NodeList<Expression>());
    VariableDeclarator decl = new VariableDeclarator(typ, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(decl));
  }

  public ExpressionStmt populateRHS(String name, Iterator<SimpleName> args) {
    NodeList<Expression> e_args = new NodeList<>();
    while (args.hasNext()) {
      //e_args.add(new NameExpr(args.next()));
      e_args.add(new StringLiteralExpr(args.next().asString()));
    }
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(AE_RHS);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt populateLHS(String name, SimpleName lhs) {
    NodeList<Expression> e_args = new NodeList<>();
    //e_args.add(new NameExpr(lhs));
    e_args.add(new StringLiteralExpr(lhs.asString()));
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(AE_LHS);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateAssignCheck(String name) {
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(AE_ASSN);
    MethodCallExpr call = new MethodCallExpr(receiver, method);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateSimCheck(String name, String lvl) {
    StringLiteralExpr lvl_e = new StringLiteralExpr(lvl);
    NodeList<Expression> e_args = new NodeList<>();
    //e_args.add(lvl_e);
    e_args.add(new StringLiteralExpr(lvl));
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(AE_SIM);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }

  public ExpressionStmt generateDeclCheck(String name, SimpleName var) {
    NodeList<Expression> e_args = new NodeList<>();
    //e_args.add(new NameExpr(var));
    e_args.add(new StringLiteralExpr(var.asString()));
    NameExpr receiver = new NameExpr(name);
    SimpleName method = new SimpleName(AE_DECL);
    MethodCallExpr call = new MethodCallExpr(receiver, method, e_args);
    return new ExpressionStmt(call);
  }
}
