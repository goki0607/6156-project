import java.util.ArrayList;

public class LabelLattice {
  public enum Label {
    BOT, L, M, H, TOP
  }

  public static Label toLabel(String l) {
    switch (l) {
      case "_|_": return Label.BOT;
      case "L": return Label.L;
      case "M": return Label.M;
      case "H": return Label.H;
      case "T": return Label.TOP;
      default: throw new IncorrectLabelException(l);
    }
  }

  public static boolean leq(Label l1, Label l2) {
    return l2.ordinal() - l1.ordinal() >= 0;
  }

  public static Label join(Label l1, Label l2) {
    return leq(l1, l2) ? l2 : l1;
  }

  public static Label joins(ArrayList<Label> ls) {
    Label res = Label.BOT;
    for (Label l : ls) {
      res = join(res, l);
    }
    return res;
  }
}
