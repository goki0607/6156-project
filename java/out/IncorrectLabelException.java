public class IncorrectLabelException extends RuntimeException {
  
  public IncorrectLabelException(String lbl) {
    super("Unsupported label " + lbl + " used in program text.");
  }
}
