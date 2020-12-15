import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AssignmentEvaluator {
  
  private Set<Object> rhs;
  private Object lhs;
  private String lvl;

  public AssignmentEvaluator() {
    this.rhs = new HashSet<Object>();
  }

  public void rhs(Object ... vars) {
    for (Object var : vars) {
      this.rhs.add(var);
    }
  }

  public void lhs(Object lhs) {
    this.lhs = lhs;
  }

  public void assign() {
    Monitor.assign(lhs, new ArrayList<Object>(rhs));
  }

  public void simulate(String lvl) { 
    this.lvl = lvl;
    if (!lvl.equals("flex")) {
      Monitor.checkBlockCondition(lvl, new ArrayList<Object>(rhs));
    }
  }

  public void decl(Object v) {
    Monitor.decl(v, new ArrayList<Object>(rhs), lvl);
  }
}
