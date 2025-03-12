package calendar;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;

public class CalendarAppMainTest {

  // Capture output helper
  private String captureOutput(Runnable runnable) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    try {
      runnable.run();
    } finally {
      System.setOut(originalOut);
    }
    return baos.toString();
  }

  // Test when no arguments are provided.
  @Test
  public void testMainNoArguments() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{}));
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  // Test when only one argument is provided.
  @Test
  public void testMainOneArgument() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode"}));
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  // Test when the first argument is not '--mode'
  @Test
  public void testMainFirstArgNotMode() {
    // If args[0] is not "--mode", nothing happens.
    // Our code simply does nothing (i.e. no output), so we expect empty output.
    String output = captureOutput(() -> CalendarApp.main(new String[]{"wrong", "interactive"}));
    // In our current code, we don't print any message in this case.
    assertEquals("No output expected if first arg is not '--mode'", "", output.trim());
  }

  // Test interactive mode: simulate user input "exit" so interactive mode terminates.
  @Test
  public void testMainInteractiveMode() {
    // Simulate interactive input (a valid command plus "exit").
    String simulatedInput = "create event InteractiveTest on 2025-03-05\nexit\n";
    InputStream originalIn = System.in;
    System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode", "interactive"}));

    System.setIn(originalIn);
    // Expect prompts ("> ") and the message "Exiting."
    assertTrue("Should prompt for input", output.contains(">"));
    assertTrue("Should print 'Exiting.'", output.contains("Exiting."));
  }

  // Test headless mode: valid file with commands (including "exit")
  @Test
  public void testMainHeadlessMode_ValidFile() throws Exception {
    File temp = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("create event HeadlessTest on 2025-03-06");
      writer.println("exit");
    }
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()})
    );
    // Expect the output to include the message that the all-day event was created.
    assertTrue("Headless mode should create an all-day event",
            output.contains("All-day event created:"));
    temp.delete();
  }

  // Test headless mode: missing command file argument.
  @Test
  public void testMainHeadlessMode_MissingFileArg() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode", "headless"}));
    assertTrue("Should print message about missing command file",
            output.contains("Headless mode requires a command file."));
  }

  // Test headless mode: non-existent file.
  @Test
  public void testMainHeadlessMode_NonExistentFile() {
    // Provide a file path that does not exist.
    String fakeFile = "nonexistent_file.txt";
    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode", "headless", fakeFile}));
    assertTrue("Should print error reading file", output.contains("Error reading file:"));
  }

  // Test headless mode: file with an invalid command.
  @Test
  public void testMainHeadlessMode_InvalidCommand() throws Exception {
    File temp = File.createTempFile("invalidCommands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("invalid command");
      writer.println("exit");
    }
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()})
    );
    // Expect the error message from CommandParser.
    assertTrue("Should print command error", output.contains("Command error:"));
    temp.delete();
  }

  // Test mode when a valid mode is provided but not interactive or headless.
  @Test
  public void testMainInvalidMode() {
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "foobar"})
    );
    assertTrue("Should indicate invalid mode",
            output.contains("Invalid mode. Use interactive or headless."));
  }
}
