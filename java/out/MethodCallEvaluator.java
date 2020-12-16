import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MethodCallEvaluator {
  
  private Set<Object> rhs;
  private Object lhs;

  public MethodCallEvaluator() {
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

  public void call() {
    ArrayList<Object> rhs_ls = new ArrayList<>(rhs);
    Monitor.assign(lhs, rhs_ls);
    for (Object rhs_var : rhs_ls) {
      ArrayList<Object> tmp = new ArrayList<>();
      tmp.add(lhs);
      Monitor.assign(rhs_var, tmp);
    }
  }

}
