import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UntakenBranchEvaluator {
  
  private Set<Object> vars;

  public UntakenBranchEvaluator() {
    this.vars = new HashSet<Object>();
  }

  public void add(Object ... vars) {
    for (Object var : vars) {
      this.vars.add(var);
    }
  }

  public void remove(Object ... vars) {
    for (Object var : vars) {
      this.vars.remove(var);
    }
  }

  public void done() { 
    Monitor.updateUntakenBranchVars(new ArrayList<Object>(vars)); 
  }
}
