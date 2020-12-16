package com.rvclass.labelchains.rewriter;

// from java stdlib
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// rewriter utility classes
import com.rvclass.labelchains.rewriter.AssignmentEvaluatorManager;
import com.rvclass.labelchains.rewriter.ConditionEvaluatorManager;
//import com.rvclass.labelchains.rewriter.DeclarationEvaluatorManager;
import com.rvclass.labelchains.rewriter.MethodCallEvaluatorManager;
import com.rvclass.labelchains.rewriter.UntakenBranchEvaluatorManager;
import com.rvclass.labelchains.rewriter.UnsupportedProgramConstructException;
import com.rvclass.labelchains.util.Pair;

// modifier visitor import
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
// compilation unit def
import com.github.javaparser.ast.CompilationUnit;
// auxillary node imports
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
// imports for body
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
// imports for comments
import com.github.javaparser.ast.comments.Comment;
// imports for expressions
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
// imports for statements
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
// imports for types
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

public class SecurityVisitor extends ModifierVisitor<Hashtable<String, String>> {

  AssignmentEvaluatorManager aem = new AssignmentEvaluatorManager();
  ConditionEvaluatorManager cem = new ConditionEvaluatorManager();
  //DeclarationEvaluatorManager dem = new DeclarationEvaluatorManager();
  MethodCallEvaluatorManager mcem = new MethodCallEvaluatorManager();
  UntakenBranchEvaluatorManager ubem = new UntakenBranchEvaluatorManager();

  private BlockStmt flattenBlockStmt(BlockStmt old_block) {
    NodeList<Statement> new_block = new NodeList<>();
    for (Statement child : old_block.getStatements()) {
      if (child instanceof BlockStmt) {
        new_block.addAll(((BlockStmt) child).getStatements());
      } else {
        new_block.add((Statement) child);
      }
    }
    return new BlockStmt(new_block);
  }

  private void updateAssigned(Expression expr, Set<SimpleName> assigned) {
    List<NameExpr> nnames = expr.getChildNodesByType(NameExpr.class);
    for (NameExpr name : nnames) {
      SimpleName s_name = name.getName();
      assigned.add(s_name);
    }
    /*
    List<SimpleName> snames = expr.getChildNodesByType(SimpleName.class);
    for (SimpleName name : snames) {
      assigned.add(name);
    }
    */
  }

  private void handleExpressionVariables(Expression expr, Set<SimpleName> assigned) {
    if (expr instanceof VariableDeclarationExpr) {
      VariableDeclarationExpr vd_expr = (VariableDeclarationExpr) expr;
      for (VariableDeclarator vd : vd_expr.getVariables()) {
        SimpleName s_name = vd.getName();
        assigned.add(s_name);
      }
    } else if (expr instanceof EnclosedExpr) {
      EnclosedExpr e_expr = (EnclosedExpr) expr;
      Expression inner = e_expr.getInner();
      handleExpressionVariables(inner, assigned);
    } /* else if (expr instanceof ArrayAccessExpr) {
      ArrayAccessExpr aa_expr = (ArrayAccessExpr) expr;
      handleExpressionVariables(aa_expr.getIndex(), declared, assigned);
      return;
    } */ else if (expr instanceof AssignExpr) {
      AssignExpr a_expr = (AssignExpr) expr;
      handleExpressionVariables(a_expr.getTarget(), assigned);
      if (!(a_expr.getValue() instanceof NameExpr)) {
        handleExpressionVariables(a_expr.getValue(), assigned);
      }
      /*if (a_expr.getValue() instanceof ArrayAccessExpr ||
          a_expr.getValue() instanceof ArrayCreationExpr ||
          a_expr.getValue() instanceof ArrayInitializerExpr ||
          //a_expr.getValue() instanceof AssignExpr ||
          a_expr.getValue() instanceof MethodCallExpr ||
          a_expr.getValue() instanceof ObjectCreationExpr ||
          a_expr.getValue() instanceof SuperExpr ||
          a_expr.getValue() instanceof ThisExpr ||
          a_expr.getValue() instanceof UnaryExpr) {
        //updateAssigned(a_expr.getValue(), assigned);
      }*/
    } else if (expr instanceof NameExpr) {
      SimpleName s_name = ((NameExpr) expr).getName();
      assigned.add(s_name);
    } else if (expr instanceof SuperExpr) {
      SuperExpr sexpr = (SuperExpr) expr;
      if (sexpr.getClassExpr().isPresent()) {
        handleExpressionVariables(sexpr.getClassExpr().get(), assigned);
      }
    } else if (expr instanceof ThisExpr) {
      ThisExpr sexpr = (ThisExpr) expr;
      if (sexpr.getClassExpr().isPresent()) {
        handleExpressionVariables(sexpr.getClassExpr().get(), assigned);
      }
    } else if (expr instanceof UnaryExpr) {
      UnaryExpr uexpr = (UnaryExpr) expr;
      handleExpressionVariables(uexpr.getExpression(), assigned);
    } else if (expr instanceof MethodCallExpr) {
      MethodCallExpr mcexpr = (MethodCallExpr) expr;
      for (Expression expr2 : mcexpr.getArguments()) {
        handleExpressionVariables(expr2, assigned);
      }
      assigned.add(mcexpr.getName());
    } else if (expr instanceof ArrayAccessExpr ||
          expr instanceof ArrayCreationExpr ||
          expr instanceof ArrayInitializerExpr) {
      throw new UnsupportedProgramConstructException("Arrays are not supported");
    }
    /*else if (expr instanceof ObjectCreationExpr)
      //updateAssigned(expr, assigned);
    }*/
  }

  private Set<SimpleName> getGuardedVars(List<ExpressionStmt> exprStmts) {
    Set<SimpleName> assigned = new HashSet<>();
    for (ExpressionStmt exprStmt : exprStmts) {
      handleExpressionVariables(exprStmt.getExpression(), assigned);
    }
    assigned.remove(new SimpleName("System"));
    return assigned;
  }

  private Set<SimpleName> getGuardVars(Expression expr) {
    Set<SimpleName> used = new HashSet<>();
    List<NameExpr> names = expr.getChildNodesByType(NameExpr.class);
    for (NameExpr name : names) {
      SimpleName s_name = name.getName();
      used.add(s_name);
    }
    return used;
  }

  private BlockStmt createOrExtendStmt(Statement stmt_orig, NodeList<Statement> stmt_new, boolean front) {
    if (stmt_orig instanceof BlockStmt) {
      BlockStmt orig_block = ((BlockStmt) stmt_orig);
      NodeList<Statement> new_block = orig_block.getStatements();
      if (front) {
        new_block.addAll(0, stmt_new);
      } else {
        new_block.addAll(stmt_new);
      }
      return flattenBlockStmt(new BlockStmt(new_block)); // orig_block.setStatements(new_block);
    } else {
      NodeList<Statement> stmt_new2 = new NodeList<>();
      for (Statement stmt : stmt_new) {
        if (stmt instanceof BlockStmt) {
          stmt_new2.addAll(((BlockStmt) stmt).getStatements());
        } else {
          stmt_new2.add(stmt);
        }
      }
      NodeList<Statement> new_block = new NodeList<>();
      if (front) {
        new_block.addAll(stmt_new2);
        new_block.add(stmt_orig);
      } else {
        new_block.add(stmt_orig);
        new_block.addAll(stmt_new2);
      }
      return flattenBlockStmt(new BlockStmt(new_block));
    }
  }

  /* add some extra import statements for monitoring purposes */
  @Override
  public CompilationUnit visit(CompilationUnit cu, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    super.visit(cu, _arg);
    return cu/*.addImport(aem.getClassName())
      .addImport(cem.getClassName())
      .addImport(dem.getClassName())
      .addImport(ubem.getClassName())*/;
  }

  /* change class name */
  @Override
  public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration ciod, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    super.visit(ciod, _arg);
    for (MethodDeclaration md : ciod.getChildNodesByType(MethodDeclaration.class)) {
      if (md.getName().asString().equals("main")) {
        return ciod.setName(new SimpleName(ciod.getName().asString() + "Monitorable"));
      }
    }
    return ciod;
  }

  @Override
  public MethodDeclaration visit(MethodDeclaration md, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    super.visit(md, _arg);
    Optional<BlockStmt> old_body = md.getBody();
    if (old_body.isPresent()) {
      return md.setBody(flattenBlockStmt(old_body.get()));
    } else {
      return md;
    }
  }

  /* box all primitive Java types */
  @Override
  public ClassOrInterfaceType visit(PrimitiveType pt, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    //super.visit(pt, _arg);
    //return pt.toBoxedType();
    Type typ = pt.toBoxedType();
    NodeList<Type> args = new NodeList<>();
    args.add(typ);
    ClassOrInterfaceType n_Typ = new ClassOrInterfaceType(null, new SimpleName("Monitorable"), args);
    return n_Typ;
  }
  
  /* throw exceptions on array types */
  public ArrayType visit(ArrayType at, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    ArrayType at_p = at.clone();
    //super.visit(at, _arg);
    Optional<MethodDeclaration> md_opt = at.findAncestor(MethodDeclaration.class);
    if (md_opt.isPresent()) {
      MethodDeclaration md = md_opt.get();
      if (md.getName().asString().equals("main")) {
        return at_p;
      }
    }
    throw new UnsupportedProgramConstructException("Arrays are not supported");
  }

  /* wrap all class or interface types up */
  public ClassOrInterfaceType visit(ClassOrInterfaceType typ, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    //super.visit(typ, _arg);
    NodeList<Type> args = new NodeList<>();
    args.add(typ);
    ClassOrInterfaceType n_Typ = new ClassOrInterfaceType(null, new SimpleName("Monitorable"), args);
    return n_Typ;
  }

  /* we need to visit all branches of an if-statement by side-stepping the visitor pattern */
  private IfStmt visitAllIfBranches(IfStmt is, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    Expression condition = (Expression) is.getCondition().accept(this, _arg);
    Optional<Statement> elseStmtOpt = is.getElseStmt();
    Statement elseStmt;
    Hashtable<String, String> fresh_arg = new Hashtable<String, String>();
    fresh_arg.putAll(_arg);
    if (!elseStmtOpt.isPresent()) {
      elseStmt = null;
    } else if (is.hasCascadingIfStmt()) {
      elseStmt = visitAllIfBranches((IfStmt) elseStmtOpt.get(), fresh_arg);
    } else /*if (is.hasElseBranch())*/ {
      elseStmt = (Statement) elseStmtOpt.get().accept(this, fresh_arg);
    }
    fresh_arg.clear();
    fresh_arg.putAll(_arg);
    Statement thenStmt = (Statement) is.getThenStmt().accept(this, fresh_arg);
    Optional<Comment> commentOpt = is.getComment();
    Comment comment;
    if (!commentOpt.isPresent()) {
      comment = null;
    } else {
      comment = (Comment) commentOpt.get().accept(this, _arg);
    }
    is.setCondition(condition);
    is.setElseStmt(elseStmt);
    is.setThenStmt(thenStmt);
    is.setComment(comment);
    return is;
  }

  private NodeList<Statement> processAssign(AssignExpr ae, Hashtable<String, String> inits) {
    if (ae.getValue() instanceof ConditionalExpr) {
      ConditionalExpr ce = (ConditionalExpr) ae.getValue();
      Expression conde = ce.getCondition();
      Set<SimpleName> condUsed = getGuardVars(conde);
      String new_ce_name = cem.newName();
      AssignExpr thene = new AssignExpr(ae.getTarget().clone(), ce.getThenExpr(), ae.getOperator());
      NodeList<Statement> thens = processAssign(thene, inits);
      thens.add(0, cem.generateBranchEnter(new_ce_name));
      AssignExpr elsee = new AssignExpr(ae.getTarget().clone(), ce.getElseExpr(), ae.getOperator());
      NodeList<Statement> elses = processAssign(elsee, inits);
      elses.add(0, cem.generateBranchEnter(new_ce_name));
      IfStmt ifElse = new IfStmt(conde, flattenBlockStmt(new BlockStmt(thens)), flattenBlockStmt(new BlockStmt(elses)));
      NodeList<Statement> new_block = new NodeList<>();
      new_block.add(cem.createCE(new_ce_name));
      new_block.add(cem.populateCE(new_ce_name, condUsed.iterator()));
      new_block.add(ifElse);
      new_block.add(cem.generateBranchDone(new_ce_name));
      return new_block;
    } else {
      String new_ae_name = aem.newName();
      ExpressionStmt init = aem.createAE(new_ae_name);
      List<SimpleName> rhs_s_names = new ArrayList<>();
      if (ae.getValue() instanceof NameExpr) {
        rhs_s_names.add(((NameExpr) ae.getValue()).getName());
      } else {
        List<NameExpr> rhs_names = ae.getValue().getChildNodesByType(NameExpr.class);
        for (NameExpr name : rhs_names) {
          rhs_s_names.add(name.getName());
        }
      }
      //rhs_s_names.addAll(ae.getValue().getChildNodesByType(SimpleName.class));
      ExpressionStmt rhs = aem.populateRHS(new_ae_name, rhs_s_names.iterator());
      List<SimpleName> lhs_names = ae.getTarget().getChildNodesByType(SimpleName.class);
      SimpleName lhs_s_name = lhs_names.get(0);
      ExpressionStmt lhs = aem.populateLHS(new_ae_name, lhs_s_name);
      NodeList<Statement> new_block = new NodeList<>();
      new_block.add(init);
      boolean flag = false;
      ExpressionStmt check;
      if (inits.containsKey(lhs_s_name.asString())) {
        check = aem.generateSimCheck(new_ae_name, inits.get(lhs_s_name.asString()));
        inits.remove(lhs_s_name.asString());
        flag = true;
      } else {
        check = aem.generateAssignCheck(new_ae_name);
        new_block.add(lhs);
      }
      new_block.add(rhs);
      new_block.add(check);
      //new_block.add(new ExpressionStmt(ae));
      NodeList<Expression> args = new NodeList<>();
      Expression rhs_e = ae.getValue();
      if (rhs_e instanceof NameExpr) {
        rhs_e = new MethodCallExpr((NameExpr) rhs_e, new SimpleName("get"));
      } else {
        List<NameExpr> names_e = rhs_e.getChildNodesByType(NameExpr.class);
        for (NameExpr name_e : names_e) {
          name_e.replace(new MethodCallExpr(name_e, new SimpleName("get")));
        }
      }
      args.add(rhs_e);
      MethodCallExpr mce = new MethodCallExpr(new NameExpr(lhs_s_name), new SimpleName("set"), args);
      new_block.add(new ExpressionStmt(mce));
      if (flag) {
        new_block.add(aem.generateDeclCheck(new_ae_name, lhs_s_name));
      }
      return new_block;
    }
  }

  private Pair<NodeList<Statement>, Hashtable<String, String>> processDeclaration(VariableDeclarationExpr vde) {
    NodeList<AnnotationExpr> annotations = vde.getAnnotations();
    SingleMemberAnnotationExpr anchor_annotation = null;
    for (AnnotationExpr annotation : annotations) {
      if (annotation instanceof SingleMemberAnnotationExpr) {
        if (((SingleMemberAnnotationExpr) annotation).getName().asString().equals("Anchor")) {
          anchor_annotation = (SingleMemberAnnotationExpr) annotation;
          break;
        }
      }
    }
    String lvl = null;
    if (anchor_annotation != null) {
      lvl = ((StringLiteralExpr) anchor_annotation.getMemberValue()).asString();
    } else {
      lvl = "flex";
    }
    NodeList<Statement> new_block = new NodeList<>();
    Hashtable<String, String> tbd = new Hashtable<>();
    for (VariableDeclarator vd : vde.getVariables()) {
      Type typ = vd.getType();
      if (typ instanceof PrimitiveType) {
        typ = ((PrimitiveType) typ).toBoxedType();
      } else if (typ instanceof ArrayType) {
        throw new UnsupportedProgramConstructException("Arrays are not supported");
      }
      NodeList<Type> args = new NodeList<>();
      args.add(typ);
      ClassOrInterfaceType typ_2 = new ClassOrInterfaceType(null, new SimpleName("Monitorable"), args);
      ObjectCreationExpr initer = new ObjectCreationExpr(null, typ_2, new NodeList<Expression>());
      VariableDeclarator vd1 = new VariableDeclarator(typ_2, vd.getName(), initer);
      //VariableDeclarationExpr decl = new VariableDeclarationExpr(typ, vd.getName().asString());
      VariableDeclarationExpr decl = new VariableDeclarationExpr(vd1);
      new_block.add(new ExpressionStmt(decl));
      /*
      String new_de_name = dem.newName();
      ExpressionStmt init = dem.createDE(new_de_name);
      new_block.add(init);
      ExpressionStmt lhs = dem.populateVar(new_de_name, vd.getName());
      new_block.add(lhs);
      if (lvl != null) {
        ExpressionStmt lvl_s = dem.populateLvl(new_de_name, lvl);
        new_block.add(lvl_s);
      }
      ExpressionStmt check = dem.generateDeclareCheck(new_de_name);
      new_block.add(check);
      */
      Optional<Expression> init_e = vd.getInitializer();
      if (init_e.isPresent()) {
        AssignExpr ae = new AssignExpr(new NameExpr(vd.getName()), init_e.get(), AssignExpr.Operator.ASSIGN);
        Hashtable<String, String> tmp = new Hashtable<>();
        tmp.put(vd.getName().asString(), lvl);
        new_block.addAll(processAssign(ae, tmp));
      } else {
        tbd.put(vd.getName().asString(), lvl);
      }
    }
    return new Pair(new_block, tbd);
  }

  /* process names */
  @Override
  public Expression visit(NameExpr ne, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    super.visit(ne, _arg);
    MethodCallExpr mce = new MethodCallExpr(ne, new SimpleName("get"));
    return mce;
  }

  /* process method calls */
  private NodeList<Statement> processMethodCalls(MethodCallExpr mce) {
    NodeList<Statement> res = new NodeList<>();
    List<SimpleName> rhs_names = new ArrayList<>();
    for (Expression arg : mce.getArguments()) {
      if (arg instanceof MethodCallExpr) {
        res.addAll(processMethodCalls((MethodCallExpr) arg));
      } else if (arg instanceof NameExpr) {
        rhs_names.add(((NameExpr) arg).getName());
      }
      for (MethodCallExpr arg_mce : arg.getChildNodesByType(MethodCallExpr.class)) {
        res.addAll(processMethodCalls(arg_mce));
      }
      for (NameExpr name : arg.getChildNodesByType(NameExpr.class)) {
        rhs_names.add(name.getName());
      }
    }
    Optional<Expression> scope_opt = mce.getScope();
    if (scope_opt.isPresent()) {
      Expression scope = scope_opt.get();
      SimpleName name;
      if (scope instanceof NameExpr) {
        name = ((NameExpr) scope).getName();
      } else {
        name = scope.getChildNodesByType(NameExpr.class).get(0).getName();
      }
      String new_mcem_name = mcem.newName();
      Statement new_mcem = mcem.createMCE(new_mcem_name);
      Statement rhs = mcem.populateRHS(new_mcem_name, rhs_names.iterator());
      Statement lhs = mcem.populateLHS(new_mcem_name, name);
      Statement check = mcem.generateCallCheck(new_mcem_name);
      res.add(new_mcem);
      res.add(rhs);
      res.add(lhs);
      res.add(check);
    }
    return res;

  }

  /* process assignment expression and variable declaration expressions */
  @Override
  public Statement visit(ExpressionStmt st, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    if (st.getExpression() instanceof ArrayAccessExpr ||
        st.getExpression() instanceof ArrayCreationExpr ||
        st.getExpression() instanceof ArrayInitializerExpr ||
        !st.getExpression().getChildNodesByType(ArrayAccessExpr.class).isEmpty() ||
        !st.getExpression().getChildNodesByType(ArrayCreationExpr.class).isEmpty() ||
        !st.getExpression().getChildNodesByType(ArrayInitializerExpr.class).isEmpty()) {
      throw new UnsupportedProgramConstructException("Arrays are not supported");
    } else if (st.getExpression() instanceof AssignExpr) {
      NodeList<Statement> new_block = new NodeList<>();
      AssignExpr ae = (AssignExpr) st.clone().getExpression();
      if (ae.getValue() instanceof MethodCallExpr) {
        MethodCallExpr mce = (MethodCallExpr) ae.getValue();
        new_block.addAll(processMethodCalls(mce));
      } else {
        for (MethodCallExpr mce : ae.getValue().getChildNodesByType(MethodCallExpr.class)) {
          new_block.addAll(processMethodCalls(mce));
        }
      }
      new_block.addAll(processAssign(ae, _arg));
      super.visit(st, _arg);
      return flattenBlockStmt(new BlockStmt(new_block));
    } else if (st.getExpression() instanceof VariableDeclarationExpr) {
      NodeList<Statement> new_block = new NodeList<>();
      VariableDeclarationExpr vde = (VariableDeclarationExpr) st.clone().getExpression();
      for (VariableDeclarator vd : vde.getVariables()) {
        Optional<Expression> vd_init_opt = vd.getInitializer();
        if (vd_init_opt.isPresent()) {
          Expression vd_init = vd_init_opt.get();
          if (vd_init instanceof MethodCallExpr) {
            MethodCallExpr init_mce = (MethodCallExpr) vd_init;
            new_block.addAll(processMethodCalls(init_mce));
          } else {
            for (MethodCallExpr mce : vd_init.getChildNodesByType(MethodCallExpr.class)) {
              new_block.addAll(processMethodCalls(mce));
            }
          }
        }
      }
      Pair<NodeList<Statement>, Hashtable<String, String>> res = processDeclaration(vde);
      new_block.addAll(res.getFst());
      _arg.putAll(res.getSnd());
      super.visit(st, _arg);
      return flattenBlockStmt(new BlockStmt(new_block));
    } else if (st.getExpression() instanceof MethodCallExpr) {
      NodeList<Statement> new_block = new NodeList<>();
      MethodCallExpr mce = (MethodCallExpr) st.clone().getExpression();
      new_block.addAll(processMethodCalls(mce));
      super.visit(st, _arg);
      new_block.add(st);
      return flattenBlockStmt(new BlockStmt(new_block));
    } else {
      super.visit(st, _arg);
      return st;
    }
  }

  /* process an If statement */
  @Override
  public BlockStmt visit(IfStmt isp, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    //IfStmt is_new = isp.clone();
    //super.visit(isp, _arg);
    //IfStmt is_new = is;
    IfStmt is = isp.clone();
    IfStmt is_new = visitAllIfBranches(isp, _arg);
    Expression cond = is_new.getCondition();
    Set<SimpleName> condUsed = getGuardVars(cond);
    IfStmt is_iter1 = is;
    IfStmt is_iter2 = is_new;
    String new_ube_name = ubem.newName();
    String new_ce_name = cem.newName();
    Set<SimpleName> assigned = new HashSet<>();
    while (is_iter1.isIfStmt() && is_iter1.hasCascadingIfStmt()) {
      cond = is_iter1.getCondition();
      condUsed.addAll(getGuardVars(cond));
      List<ExpressionStmt> exprStmts = is_iter1.getThenStmt().getChildNodesByType(ExpressionStmt.class);
      Set<SimpleName> assigned_one = getGuardedVars(exprStmts);
      NodeList<Statement> gend_body = new NodeList<>();
      gend_body.add(ubem.depopulateUBE(new_ube_name, assigned_one.iterator()));
      gend_body.add(cem.generateBranchEnter(new_ce_name));
      is_iter2.setThenStmt(createOrExtendStmt(is_iter2.getThenStmt(), gend_body, true));
      is_iter1 = (IfStmt) is_iter1.getElseStmt().get();
      is_iter2 = (IfStmt) is_iter2.getElseStmt().get();
      assigned.addAll(assigned_one);
    }
    boolean flag = is_iter1.hasElseBranch();
    cond = is_iter1.getCondition();
    condUsed.addAll(getGuardVars(cond));
    List<ExpressionStmt> exprStmts = is_iter1.getThenStmt().getChildNodesByType(ExpressionStmt.class);
    Set<SimpleName> assigned_one = getGuardedVars(exprStmts);
    assigned.addAll(assigned_one);
    NodeList<Statement> gend_body = new NodeList<>();
    gend_body.add(ubem.depopulateUBE(new_ube_name, assigned_one.iterator()));
    gend_body.add(cem.generateBranchEnter(new_ce_name));
    is_iter2.setThenStmt(createOrExtendStmt(is_iter2.getThenStmt(), gend_body, true));
    if (flag) {
      exprStmts = is_iter1.getElseStmt().get().getChildNodesByType(ExpressionStmt.class);
      assigned_one = getGuardedVars(exprStmts);
      gend_body = new NodeList<>();
      gend_body.add(ubem.depopulateUBE(new_ube_name, assigned_one.iterator()));
      gend_body.add(cem.generateBranchEnter(new_ce_name));
      is_iter2.setElseStmt(createOrExtendStmt(is_iter2.getElseStmt().get(), gend_body, true));
      assigned.addAll(assigned_one);
    }
    NodeList<Statement> new_block = new NodeList<>();
    new_block.add(ubem.createUBE(new_ube_name));
    new_block.add(ubem.populateUBE(new_ube_name, assigned.iterator()));
    new_block.add(cem.createCE(new_ce_name));
    new_block.add(cem.populateCE(new_ce_name, condUsed.iterator()));
    NodeList<Statement> tail = new NodeList<>();
    tail.add(is_new);
    tail.add(ubem.generateBranchDone(new_ube_name));
    tail.add(cem.generateBranchDone(new_ce_name));
    return flattenBlockStmt(createOrExtendStmt(flattenBlockStmt(new BlockStmt(new_block)), tail, false));
  }

  /* process a Do-While statement */
  @Override
  public BlockStmt visit(DoStmt dsp, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    DoStmt ds = dsp.clone();
    super.visit(dsp, _arg);
    Statement new_block = ds.getBody().clone();
    Statement body = ds.getBody().clone();
    List<ExpressionStmt> exprStmts = body.getChildNodesByType(ExpressionStmt.class);
    Set<SimpleName> assigned = getGuardedVars(exprStmts);
    Expression cond = ds.getCondition();
    Set<SimpleName> condUsed = getGuardVars(cond);
    NodeList<Statement> rest_of_block = new NodeList<Statement>();
    String new_ube_name = ubem.newName();
    rest_of_block.add(ubem.createUBE(new_ube_name));
    rest_of_block.add(ubem.populateUBE(new_ube_name, assigned.iterator()));
    String new_ce_name = cem.newName();
    rest_of_block.add(cem.createCE(new_ce_name));
    rest_of_block.add(cem.populateCE(new_ce_name, condUsed.iterator()));
    NodeList<Statement> gend_body = new NodeList<>();
    gend_body.add(cem.generateBranchEnter(new_ce_name));
    rest_of_block.add(new WhileStmt(cond, createOrExtendStmt(body, gend_body, true)));
    rest_of_block.add(ubem.generateBranchDone(new_ube_name));
    rest_of_block.add(cem.generateBranchDone(new_ce_name));
    return flattenBlockStmt(createOrExtendStmt(new_block, rest_of_block, false));
  }

  /* process a Switch statement */
  @Override
  public BlockStmt visit(SwitchStmt ssp, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    SwitchStmt ss = ssp.clone();
    super.visit(ssp, _arg);
    Expression selector = ss.getSelector();
    NodeList<SwitchEntry> cases = ss.getEntries();
    Set<SimpleName> condUsed = getGuardVars(selector);
    NodeList<Statement> new_block = new NodeList<>();
    String new_ube_name = ubem.newName();
    String new_ce_name = cem.newName();
    NodeList<SwitchEntry> new_cases = new NodeList<>();
    Set<SimpleName> assigned = new HashSet<>();
    for (SwitchEntry a_case : cases) {
      List<ExpressionStmt> exprStmts = a_case.getChildNodesByType(ExpressionStmt.class);
      Set<SimpleName> assigned_one = getGuardedVars(exprStmts);
      NodeList<Statement> curr = a_case.getStatements();
      //curr.add(0, ubem.depopulateUBE(new_ube_name, assigned_one.iterator()));
      curr.add(0, cem.generateBranchEnter(new_ce_name));
      new_cases.add(a_case.setStatements(curr));
      assigned.addAll(assigned_one);
    }
    new_block.add(ubem.createUBE(new_ube_name));
    new_block.add(ubem.populateUBE(new_ube_name, assigned.iterator()));
    new_block.add(cem.createCE(new_ce_name));
    new_block.add(cem.populateCE(new_ce_name, condUsed.iterator()));
    new_block.add(ss.setEntries(new_cases));
    new_block.add(ubem.generateBranchDone(new_ube_name));
    new_block.add(cem.generateBranchDone(new_ce_name));
    return flattenBlockStmt(new BlockStmt(new_block));
  }

  /* process a While statement */
  @Override
  public BlockStmt visit(WhileStmt wsp, Hashtable<String, String> _arg) {
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    WhileStmt ws = wsp.clone();
    super.visit(wsp, _arg);
    Statement body = ws.getBody();
    List<ExpressionStmt> exprStmts = body.getChildNodesByType(ExpressionStmt.class);
    Set<SimpleName> assigned = getGuardedVars(exprStmts);
    Expression cond = ws.getCondition();
    Set<SimpleName> condUsed = getGuardVars(cond);
    NodeList<Statement> new_block = new NodeList<>();
    String new_ube_name = ubem.newName();
    new_block.add(ubem.createUBE(new_ube_name));
    new_block.add(ubem.populateUBE(new_ube_name, assigned.iterator()));
    String new_ce_name = cem.newName();
    new_block.add(cem.createCE(new_ce_name));
    new_block.add(cem.populateCE(new_ce_name, condUsed.iterator()));
    //NodeList<Statement> new_body_block = new NodeList<>();
    //new_body_block.add(cem.generateBranchDone(new_ce_name));
    //new_body_block.add(body);
    List<SimpleName> dummy = new ArrayList<>();
    NodeList<Statement> gend_body = new NodeList<>();
    //gend_body.add(ubem.depopulateUBE(new_ube_name, dummy.iterator()));
    gend_body.add(cem.generateBranchEnter(new_ce_name));
    new_block.add(ws.setBody(createOrExtendStmt(ws.getBody(), gend_body, true)));
    new_block.add(ubem.generateBranchDone(new_ube_name));
    new_block.add(cem.generateBranchDone(new_ce_name));
    return flattenBlockStmt(new BlockStmt(new_block));
  }

  /* process a For statement */
  @Override
  public BlockStmt visit(ForStmt fs, Hashtable<String, String> _arg) {
    //super.visit(fs, _arg);
    if (_arg == null) {
      _arg = new Hashtable<String, String>();
    }
    NodeList<Expression> init = fs.getInitialization();
    NodeList<Statement> init_s = new NodeList<>();
    for (Expression init_e : init) {
      if (init_e instanceof AssignExpr) {
        init_s.addAll(processAssign(((AssignExpr) init_e), _arg));
      } else if (init_e instanceof VariableDeclarationExpr) {
        Pair<NodeList<Statement>, Hashtable<String, String>> res = processDeclaration(((VariableDeclarationExpr) init_e));
        _arg.putAll(res.getSnd());
        init_s.addAll(res.getFst());
      } else {
        init_s.add(new ExpressionStmt(init_e));
      }
    }
    Optional<Expression> compare = fs.getCompare();
    Expression new_cond = null;
    if (compare.isPresent()) {
      new_cond = compare.get();
    } else {
      new_cond = new BooleanLiteralExpr(true);
    }
    Statement body = fs.getBody();
    NodeList<Statement> new_s = new NodeList<Statement>();
    if (body instanceof BlockStmt) {
      new_s.addAll(((BlockStmt) body).getStatements());
    } else {
      new_s.add(body); 
    }
    NodeList<Expression> updates = fs.getUpdate();
    for (Expression update : updates) {
     new_s.add(new ExpressionStmt(update));
    }
    BlockStmt new_body = flattenBlockStmt(new BlockStmt(new_s));
    WhileStmt ws = new WhileStmt(new_cond, new_body);
    BlockStmt new_block = flattenBlockStmt(this.visit(ws, _arg));
    NodeList<Statement> new_block_s = new_block.getStatements();
    new_block_s.addAll(0, init_s);
    return flattenBlockStmt(new BlockStmt(new_block_s));
  }

  /* process a ForEach statement by throwing an exception */
  @Override
  public Statement visit(ForEachStmt fe, Hashtable<String, String> _arg) {
    super.visit(fe, _arg);
    throw new UnsupportedProgramConstructException("ForEach loops are not supported.");
    //return fe;
  }

}
