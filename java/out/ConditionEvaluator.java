import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ConditionEvaluator {
  
  private Set<Object> vars;

  public ConditionEvaluator() {
    this.vars = new HashSet<Object>();
  }

  public void add(Object ... vars) {
    for (Object var : vars) {
      this.vars.add(var);
    }
  }

  public void enter() { 
    Monitor.enterCtx(new ArrayList<Object>(vars));
  }

  public void done() { 
    Monitor.leaveCtx();
  }
}
