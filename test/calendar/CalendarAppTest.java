package calendar;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CalendarAppTest {

  @Test
  public void testCreateTimedEvent() throws Exception {
    CalendarManager manager = new CalendarManager();
    String command = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(command, manager);
    // Assuming CalendarManager now exposes events via getAllEvents()
    assertEquals("One timed event should be created", 1, manager.getAllEvents().size());
  }

  @Test
  public void testCreateAllDayEvent() throws Exception {
    CalendarManager manager = new CalendarManager();
    String command = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(command, manager);
    assertEquals("One all-day event should be created", 1, manager.getAllEvents().size());
    assertTrue("Event should be all-day", manager.getAllEvents().get(0).isAllDay());
  }

  @Test
  public void testRecurringEventFixedTimes() throws Exception {
    CalendarManager manager = new CalendarManager();
    String command = "create event Workshop on 2025-03-02 repeats MTWRF for 3 times";
    CommandParser.processCommand(command, manager);
    assertEquals("Three occurrences should be created", 3, manager.getAllEvents().size());
  }

  @Test
  public void testRecurringEventUntil() throws Exception {
    CalendarManager manager = new CalendarManager();
    String command = "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30 repeats WF until 2025-03-10T00:00";
    CommandParser.processCommand(command, manager);
    assertTrue("At least one occurrence should be created", manager.getAllEvents().size() > 0);
  }

  @Test
  public void testEditSingleEvent() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create event
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    // Edit event description
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 with Quarterly results";
    CommandParser.processCommand(editCmd, manager);
    assertEquals("Quarterly results", manager.getAllEvents().get(0).getDescription());
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create two events with the same name at different times
    CommandParser.processCommand("create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30", manager);
    CommandParser.processCommand("create event Seminar from 2025-03-04T09:00 to 2025-03-04T10:30", manager);
    // Bulk edit: update events starting from 2025-03-04T00:00
    String editCmd = "edit events description Seminar from 2025-03-04T00:00 with UpdatedSeminar";
    CommandParser.processCommand(editCmd, manager);
    for (CalendarEvent event : manager.getAllEvents()) {
      if (event.getStart().equals(LocalDateTime.parse("2025-03-04T09:00"))) {
        assertEquals("UpdatedSeminar", event.getDescription());
      } else {
        assertEquals("", event.getDescription());
      }
    }
  }

  @Test
  public void testEditEventsByName() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create two all-day events with the same name
    CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    // Bulk edit by event name without 'from' clause
    String editCmd = "edit events location Holiday with Beach";
    CommandParser.processCommand(editCmd, manager);
    for (CalendarEvent event : manager.getAllEvents()) {
      if (event.getEventName().equals("Holiday")) {
        assertEquals("Beach", event.getLocation());
      }
    }
  }

  @Test(expected = Exception.class)
  public void testMissingFromKeyword() throws Exception {
    CalendarManager manager = new CalendarManager();
    String command = "create event Meeting 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(command, manager);
  }

  @Test(expected = Exception.class)
  public void testMissingToKeywordInEdit() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 with NoToClause";
    CommandParser.processCommand(editCmd, manager);
  }

  @Test
  public void testPrintEventsOn() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String printCmd = "print events on 2025-03-01";
    // Execute print command (we assume no exception is thrown)
    CommandParser.processCommand(printCmd, manager);
    List<CalendarEvent> events = manager.getEventsOn(LocalDate.parse("2025-03-01"));
    assertFalse("There should be events on 2025-03-01", events.isEmpty());
  }

  @Test
  public void testShowStatus() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String statusCmd = "show status on 2025-03-01T10:30";
    // Execute status command; we assume it prints "Busy" (output assertion can be done separately)
    CommandParser.processCommand(statusCmd, manager);
  }

  @Test
  public void testExportGoogleCSV() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export googlecsv test_google.csv";
    CommandParser.processCommand(exportCmd, manager);
    File file = new File("test_google.csv");
    assertTrue("The exported Google CSV file should exist", file.exists());
    assertTrue("The exported file should not be empty", file.length() > 0);
    file.delete(); // Cleanup after test
  }

  @Test
  public void testInvalidCommand() {
    CalendarManager manager = new CalendarManager();
    try {
      CommandParser.processCommand("invalid command", manager);
      fail("Expected exception for invalid command");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test
  public void testMainHeadlessMode() throws Exception {
    // Create a temporary command file with a couple of commands and "exit"
    File temp = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("create event Test on 2025-03-05");
      writer.println("exit");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    // Call main with headless mode arguments.
    CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue(output.contains("All-day event created:"));
    temp.delete();
  }

  @Test
  public void testExportCalCommand() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export cal test_export.csv";
    CommandParser.processCommand(exportCmd, manager);
    File f = new File("test_export.csv");
    assertTrue("Exported CSV file should exist", f.exists());
    assertTrue("Exported file should not be empty", f.length() > 0);
    f.delete(); // Cleanup
  }

  @Test
  public void testShowStatusOutput() throws Exception {
    CalendarManager manager = new CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Expected status output to contain 'Busy'", output.contains("Busy"));
  }

  @Test
  public void testEditEventsWithoutFromClause() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    CommandParser.processCommand("edit events location Holiday with Beach", manager);
    for (CalendarEvent event : manager.getAllEvents()) {
      if (event.getEventName().equals("Holiday")) {
        assertEquals("Beach", event.getLocation());
      }
    }
  }

  @Test
  public void testPrintEventsRange() throws Exception {
    CalendarManager manager = new CalendarManager();
    CommandParser.processCommand("create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    CommandParser.processCommand("create event Workshop on 2025-03-02", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("print events from 2025-03-01T00:00 to 2025-03-03T00:00", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test
  public void testMainInsufficientArgs() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    // Call main with no arguments.
    CalendarApp.main(new String[]{});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  @Test
  public void testMainInvalidMode() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.main(new String[]{"--mode", "foobar"});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should indicate invalid mode",
            output.contains("Invalid mode. Use interactive or headless."));
  }

  @Test
  public void testMainInteractiveMode() throws Exception {
    // Simulate input "exit" so that interactive mode terminates.
    String simulatedInput = "exit\n";
    ByteArrayInputStream bais = new ByteArrayInputStream(simulatedInput.getBytes());
    InputStream oldIn = System.in;
    System.setIn(bais);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));

    // Call the interactive mode method directly.
    CalendarApp.runInteractiveMode(new CalendarManager());

    System.setOut(oldOut);
    System.setIn(oldIn);
    String output = baos.toString();
    assertTrue("Interactive mode should print exiting message",
            output.contains("Exiting."));
  }

  @Test
  public void testMainHeadlessModeUsingBaos() throws Exception {
    File temp = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("create event Test on 2025-03-05");
      writer.println("exit");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));

    CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()});

    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Headless mode should create an all-day event",
            output.contains("All-day event created:"));
    temp.delete();
  }

  @Test
  public void testConflictsWithOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 30);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 30);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertTrue("Events that overlap should conflict", event1.conflictsWith(event2));
    assertTrue("Events that overlap should conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithNonOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 12, 0);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that do not overlap should not conflict", event1.conflictsWith(event2));
    assertFalse("Events that do not overlap should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithBoundaryTouching() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 0);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that touch boundaries should not conflict", event1.conflictsWith(event2));
    assertFalse("Events that touch boundaries should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testToString_AllDayWithDescriptionAndLocation() {
    LocalDateTime start = LocalDate.of(2025, 3, 1).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    CalendarEvent event = new CalendarEvent("Holiday", start, end, true);
    event.setDescription("Vacation");
    event.setLocation("Beach");
    event.setPublic(false);
    String expected = "Holiday (All Day on 2025-03-01), Description: Vacation, Location: Beach, Private";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_TimedEventWithoutExtras() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 11, 0);
    CalendarEvent event = new CalendarEvent("Meeting", start, end, false);
    String expected = "Meeting from 2025-03-01 10:00 to 2025-03-01 11:00, Public";
    assertEquals(expected, event.toString());
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictAutoDecline() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent event1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);
    CalendarEvent event2 = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, true);
  }

  @Test
  public void testAddEventWithConflictWarning() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent event1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    CalendarEvent event2 = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, false);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should print warning about conflict", output.contains("Warning: Event conflicts with Meeting"));
  }

  @Test
  public void testEventsAreSortedAfterAdd() throws Exception {
    CalendarManager manager = new CalendarManager();

    CalendarEvent event1 = new CalendarEvent(
            "Event1",
            LocalDateTime.of(2025, 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0),
            false);
    CalendarEvent event2 = new CalendarEvent(
            "Event2",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0),
            false);
    CalendarEvent event3 = new CalendarEvent(
            "Event3",
            LocalDateTime.of(2025, 3, 1, 14, 0),
            LocalDateTime.of(2025, 3, 1, 15, 0),
            false);

    manager.addEvent(event1, false);
    manager.addEvent(event2, false);
    manager.addEvent(event3, false);

    List<CalendarEvent> sorted = manager.getAllEvents();
    assertEquals("First event should be Event2", "Event2", sorted.get(0).getEventName());
    assertEquals("Second event should be Event1", "Event1", sorted.get(1).getEventName());
    assertEquals("Third event should be Event3", "Event3", sorted.get(2).getEventName());
  }

  @Test(expected = Exception.class)
  public void testConflictAutoDecline() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    CalendarEvent e2 = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(e2, true);
  }
}
