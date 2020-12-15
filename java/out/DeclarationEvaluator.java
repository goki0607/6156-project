public class DeclarationEvaluator {
  
  private Object var;
  private String lvl;

  public DeclarationEvaluator() { }

  public void var(Object var) {
    this.var = var;
  }

  public void lvl(String lvl) {
    this.lvl = lvl;
  }

  public void declare() { }
}
