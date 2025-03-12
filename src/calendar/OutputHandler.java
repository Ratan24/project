package calendar;

import javax.annotation.processing.Generated;

public class OutputHandler {
  private static OutputHandler instance = new OutputHandler();

  private OutputHandler() { }

  public static OutputHandler getInstance() {
    return instance;
  }

  @Generated("Excluded from mutation testing")
  public void println(String s) {
    System.out.println(s);
  }
}
