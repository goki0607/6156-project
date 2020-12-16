public class Monitorable<T> {
  private T data;

  public Monitorable() { }
  public T get() { return data; }
  public void set(T data) { this.data = data; }
}
