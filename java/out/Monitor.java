import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

public class Monitor {
  private static int k;
  private static Stack<LabelLattice.Label> cc = new Stack<>();
  private static LabelLattice.Label bc = LabelLattice.Label.BOT;
  private static Map<Object, LabelLattice.Label> anchors = new IdentityHashMap<>();
  private static Map<Object, ArrayList<LabelLattice.Label>> flexibles = new IdentityHashMap<>();

  static {
    cc.push(LabelLattice.Label.BOT);
    k = 5;
  }

  private static void checkBlockCondition(LabelLattice.Label lbl, ArrayList<Object> rhs_vars) {
    ArrayList<LabelLattice.Label> rhs_lbls = new ArrayList<>();
    for (Object var : rhs_vars) {
      if (anchors.containsKey(var)) {
        rhs_lbls.add(anchors.get(var));
      } else {
        rhs_lbls.add(flexibles.get(var).get(0));
      }
    }
    rhs_lbls.add(cc.peek());
    rhs_lbls.add(bc);
    LabelLattice.Label join = LabelLattice.joins(rhs_lbls);
    boolean cond = LabelLattice.leq(join, lbl);
    if (!cond) {
      throw new InformationLeakException();
    }
  }

  public static void checkBlockCondition(String lvl, ArrayList<Object> rhs_vars) {
    LabelLattice.Label lvl_lbl = LabelLattice.toLabel(lvl);
    checkBlockCondition(lvl_lbl, rhs_vars);
  }

  private static void anchorAssign(Object lhs, ArrayList<Object> rhs) {
    LabelLattice.Label lhs_lbl = anchors.get(lhs);
    checkBlockCondition(lhs_lbl, rhs);
    ArrayList<LabelLattice.Label> rhs_lbls = new ArrayList<>();
    for (Object var : rhs) {
      if (flexibles.containsKey(var)) {
        rhs_lbls.add(flexibles.get(var).get(1));
      }
    }
    rhs_lbls.add(cc.peek());
    rhs_lbls.add(bc);
    LabelLattice.Label bc_p = LabelLattice.joins(rhs_lbls);
    bc = bc_p;
  }

  private static void flexAssign(Object lhs, ArrayList<Object> rhs) {
    ArrayList<LabelLattice.Label> old_lbls = flexibles.get(lhs);
    ArrayList<LabelLattice.Label> new_lbls = new ArrayList<>();
    ArrayList<ArrayList<LabelLattice.Label>> rhs_lbls = new ArrayList<>();
    for (Object var : rhs) {
      if (flexibles.containsKey(var)) {
        rhs_lbls.add(flexibles.get(var));
      } else {
        ArrayList<LabelLattice.Label> tmp = new ArrayList<>();
        for (int i = 0; i < k; i++) {
          tmp.add(LabelLattice.Label.BOT);
        }
        tmp.add(0,  anchors.get(var));
        rhs_lbls.add(tmp);
      }
    }
    for (int i = 0; i < k; i++) {
      ArrayList<LabelLattice.Label> lvl_lbls = new ArrayList<>();
      for (ArrayList<LabelLattice.Label> rhs_lbl : rhs_lbls) {
        lvl_lbls.add(rhs_lbl.get(i));
      }
      lvl_lbls.add(cc.peek());
      lvl_lbls.add(bc);
      lvl_lbls.add(old_lbls.get(i));
      new_lbls.add(LabelLattice.joins(lvl_lbls));
    }
    flexibles.replace(lhs, new_lbls);
  }

  public static void assign(Object lhs, ArrayList<Object> rhs) {
    if (anchors.containsKey(lhs)) {
      anchorAssign(lhs, rhs);
    } else {
      flexAssign(lhs, rhs);
    }
  }

  public static void decl(Object lhs, ArrayList<Object> rhs, String lvl) {
    if (lvl.equals("flex")) {
      ArrayList<LabelLattice.Label> init = new ArrayList<>();
      for (int i = 0; i < k; i++) {
        init.add(LabelLattice.Label.BOT);
      }
      flexibles.put(lhs, init);
      assign(lhs, rhs);
    } else {
      LabelLattice.Label init = LabelLattice.toLabel(lvl);
      anchors.put(lhs, init);
      assign(lhs, rhs);
    }
  }

  public static void updateUntakenBranchVars(ArrayList<Object> vars) {
    boolean untaken_anchors_flag = false;
    ArrayList<Object> untaken_flexibles = new ArrayList<>();
    for (Object var : vars) {
      if (anchors.containsKey(var)) {
        untaken_anchors_flag = true;
      } else {
        untaken_flexibles.add(var);
      }
    }
    LabelLattice.Label bc_p = LabelLattice.join(bc, cc.peek());
    if (untaken_anchors_flag) {
      bc = bc_p;
    }
    for (Object flex_var : untaken_flexibles) {
      ArrayList<LabelLattice.Label> old_flex_var = flexibles.get(flex_var);
      ArrayList<LabelLattice.Label> new_flex_var = new ArrayList<>();
      for (LabelLattice.Label l : old_flex_var) {
        new_flex_var.add(LabelLattice.join(l, bc_p));
      }
      flexibles.replace(flex_var, new_flex_var);
    }
  }

  public static void enterCtx(ArrayList<Object> vars) {
    ArrayList<LabelLattice.Label> new_labels = new ArrayList<>();
    new_labels.add(cc.peek());
    for (Object var : vars) {
      if (anchors.containsKey(var)) {
        new_labels.add(anchors.get(var));
      } else if (flexibles.containsKey(var)) {
        new_labels.add(flexibles.get(var).get(0));
      }
    }
    LabelLattice.Label new_ctx = LabelLattice.joins(new_labels);
    cc.push(new_ctx);
  }

  public static void leaveCtx() {
    cc.pop();
  }
}
