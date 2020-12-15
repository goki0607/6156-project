public class InformationLeakException extends RuntimeException {
  
  public InformationLeakException() {
    super("Information leak detected, terminating program.");
  }
}
