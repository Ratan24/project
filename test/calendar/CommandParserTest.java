package calendar;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CommandParserTest {

  @Test
  public void testProcessCommand_InvalidCommand() {
    CalendarManager manager = new CalendarManager();
    try {
      CommandParser.processCommand("nonexistent command", manager);
      fail("Expected Exception for invalid command");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_MissingTo() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Missing "to" keyword in the command.
    CommandParser.processCommand("create event Test from 2025-03-01T10:00", manager);
  }

  @Test(expected = Exception.class)
  public void testProcessEditCommand_MissingWith() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Missing "with" keyword.
    CommandParser.processCommand("edit event description Test from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
  }

  @Test
  public void testProcessShowStatus() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create an event that makes the calendar busy.
    CommandParser.processCommand("create event Test from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should indicate Busy", output.contains("Busy"));
  }

  @Test
  public void testProcessPrintEventsOn() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Test from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("print events on 2025-03-01", manager);
    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should list event Test", output.contains("Test from"));
  }

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

  // --- Create Event Tests ---

  @Test
  public void testProcessCreateEvent_Timed() throws Exception {
    CalendarManager manager = new CalendarManager();
    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
    List<CalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    CalendarEvent ev = events.get(0);
    assertFalse(ev.isAllDay());
    assertEquals("Meeting", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_AllDay() throws Exception {
    CalendarManager manager = new CalendarManager();
    String cmd = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(cmd, manager);
    List<CalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    CalendarEvent ev = events.get(0);
    assertTrue(ev.isAllDay());
    assertEquals("Holiday", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_RepeatingTimed() throws Exception {
    CalendarManager manager = new CalendarManager();
    String cmd = "create event Workshop from 2025-03-02T09:00 to 2025-03-02T10:00 repeats MWF for 3 times";
    CommandParser.processCommand(cmd, manager);
    assertEquals(3, manager.getAllEvents().size());
  }

  @Test
  public void testProcessCreateEvent_RepeatingAllDay() throws Exception {
    CalendarManager manager = new CalendarManager();
    String cmd = "create event Seminar on 2025-03-03 repeats MWF until 2025-03-10";
    CommandParser.processCommand(cmd, manager);
    List<CalendarEvent> events = manager.getAllEvents();
    assertTrue("Should create occurrences", events.size() > 0);
    for (CalendarEvent ev : events) {
      assertTrue(ev.isAllDay());
    }
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_MissingToKeyword() throws Exception {
    CalendarManager manager = new CalendarManager();
    String cmd = "create event Faulty from 2025-03-01T10:00 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
  }

  // --- Edit Command Tests ---

  @Test
  public void testProcessEditCommand_SingularSuccess() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 with UpdatedDesc";
    CommandParser.processCommand(editCmd, manager);
    CalendarEvent ev = manager.getAllEvents().get(0);
    assertEquals("UpdatedDesc", ev.getDescription());
  }

  @Test(expected = Exception.class)
  public void testProcessEditCommand_MissingToForSingular() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    // Missing "to" clause in edit command.
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 with Updated";
    CommandParser.processCommand(editCmd, manager);
  }

  @Test
  public void testProcessEditCommand_Plural() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Conference from 2025-03-01T09:00 to 2025-03-01T10:00", manager);
    CommandParser.processCommand("create event Conference from 2025-03-02T09:00 to 2025-03-02T10:00", manager);
    String editCmd = "edit events location Conference with NewLocation";
    CommandParser.processCommand(editCmd, manager);
    for (CalendarEvent ev : manager.getAllEvents()) {
      assertEquals("NewLocation", ev.getLocation());
    }
  }



  @Test
  public void testProcessPrintEventsOn_Valid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String output = captureOutput(() -> {
      try {
        CommandParser.processCommand("print events on 2025-03-01", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Meeting"));
  }

  @Test(expected = Exception.class)
  public void testProcessPrintEventsOn_InvalidFormat() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Missing "on" keyword.
    CommandParser.processCommand("print events 2025-03-01", manager);
  }

  @Test
  public void testProcessPrintEventsRange_Valid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    CommandParser.processCommand("create event Workshop from 2025-03-01T12:00 to 2025-03-01T13:00", manager);
    String output = captureOutput(() ->
    {
      try {
        CommandParser.processCommand("print events from 2025-03-01T09:00 to 2025-03-01T14:00", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test(expected = Exception.class)
  public void testProcessPrintEventsRange_MissingTo() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("print events from 2025-03-01T09:00 2025-03-01T14:00", manager);
  }

  // --- Export Commands Tests ---

  @Test
  public void testProcessExportCal_Valid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event ExportTest from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String fileName = "test_export_cal.csv";
    CommandParser.processCommand("export cal " + fileName, manager);
    File f = new File(fileName);
    assertTrue(f.exists());
    assertTrue(f.length() > 0);
    f.delete();
  }

  @Test(expected = Exception.class)
  public void testProcessExportCal_Invalid() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Missing file name.
    CommandParser.processCommand("export cal", manager);
  }

  @Test
  public void testProcessExportGoogleCSV_Valid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event ExportGoogle from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String fileName = "test_export_google.csv";
    CommandParser.processCommand("export googlecsv " + fileName, manager);
    File f = new File(fileName);
    assertTrue(f.exists());
    assertTrue(f.length() > 0);
    f.delete();
  }

  @Test(expected = Exception.class)
  public void testProcessExportGoogleCSV_Invalid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("export googlecsv", manager);
  }

  // --- Show Status Test ---

  @Test
  public void testProcessShowStatus_Valid() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event StatusTest from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String output = captureOutput(() ->
    {
      try {
        CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Busy"));
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_AutoDeclineConflict() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create the first event with the autoDecline flag.
    String cmd1 = "create event ConflictTest from 2025-03-01T10:00 to 2025-03-01T11:00 --autodecline";
    CommandParser.processCommand(cmd1, manager);
    // Create a conflicting event with the autoDecline flag.
    String cmd2 = "create event ConflictTest2 from 2025-03-01T10:30 to 2025-03-01T11:30 --autodecline";
    // Because autoDecline is true, a conflict should throw an exception.
    CommandParser.processCommand(cmd2, manager);
  }

  @Test
  public void testHasAutoDecline_Found() {
    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 --autodecline";
    assertTrue("Should detect autoDecline flag", CommandParser.hasAutoDecline(cmd));
  }

  @Test
  public void testHasAutoDecline_NotFound() {
    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    assertFalse("Should not detect autoDecline flag", CommandParser.hasAutoDecline(cmd));
  }


}
