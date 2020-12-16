public class Test {
  public static void main(String[] args) {
    @Anchor("M")
    int m = -5;
    @Anchor("H")
    int h = 1;
    @Anchor("L")
    int l = 7;
    int w = 0;
    if (m > 0) {
      w = h;
    } else {
      w = l;
    }
    m = w;
    l = 1;
  }
}