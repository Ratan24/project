package calendar;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CalendarManagerTest {

  @Test
  public void testNoConflictAdd() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    CalendarEvent e2 = new CalendarEvent("Lunch", LocalDateTime.of(2025, 3, 1, 11, 30),
            LocalDateTime.of(2025, 3, 1, 12, 30), false);
    manager.addEvent(e2, true);
    assertEquals(2, manager.getAllEvents().size());
  }

  @Test(expected = Exception.class)
  public void testConflictAutoDecline() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    // Conflicting event should cause exception
    CalendarEvent e2 = new CalendarEvent("Meeting2", LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(e2, true);
  }

  @Test
  public void testConflictWarning() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    // Capture output for warning
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarEvent e2 = new CalendarEvent("Meeting2", LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(e2, false);
    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Warning: Event conflicts with Meeting"));
  }

  @Test
  public void testGetEventsOn_Boundary() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create an all-day event
    CalendarEvent e1 = new CalendarEvent("Holiday", LocalDateTime.of(2025, 3, 5, 0, 0),
            LocalDateTime.of(2025, 3, 6, 0, 0), true);
    manager.addEvent(e1, false);
    // Create a timed event that spans midnight
    CalendarEvent e2 = new CalendarEvent("LateMeeting", LocalDateTime.of(2025, 3, 5, 23, 0),
            LocalDateTime.of(2025, 3, 6, 1, 0), false);
    manager.addEvent(e2, false);
    List<CalendarEvent> eventsOn5 = manager.getEventsOn(LocalDate.of(2025, 3, 5));
    assertEquals(2, eventsOn5.size());
  }

  @Test
  public void testGetEventsInRange() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    CalendarEvent e2 = new CalendarEvent("Lunch", LocalDateTime.of(2025, 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    List<CalendarEvent> range = manager.getEventsInRange(LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 12, 30));
    assertEquals(2, range.size());
  }

  @Test
  public void testIsBusyAt() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    assertTrue(manager.isBusyAt(LocalDateTime.of(2025, 3, 1, 10, 30)));
    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3, 1, 11, 30)));
  }

  @Test
  public void testEditSingleEvent_Success() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    boolean updated = manager.editSingleEvent("description", "Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0), LocalDateTime.of(2025, 3, 1, 11, 0), "UpdatedDesc");
    assertTrue(updated);
    assertEquals("UpdatedDesc", e1.getDescription());
  }

  @Test
  public void testEditSingleEvent_Failure() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Try editing an event that doesn't exist.
    boolean updated = manager.editSingleEvent("description", "NonExistent", LocalDateTime.now(),
            LocalDateTime.now().plusHours(1), "Test");
    assertFalse(updated);
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Seminar", LocalDateTime.of(2025, 3, 3, 9, 0),
            LocalDateTime.of(2025, 3, 3, 10, 30), false);
    CalendarEvent e2 = new CalendarEvent("Seminar", LocalDateTime.of(2025, 3, 4, 9, 0),
            LocalDateTime.of(2025, 3, 4, 10, 30), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByStart("description", "Seminar",
            LocalDateTime.of(2025, 3, 4, 0, 0), "Updated");
    assertEquals(1, count);
    assertEquals("Updated", e2.getDescription());
    assertEquals("", e1.getDescription());
  }

  @Test
  public void testEditEventsByName() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Holiday", LocalDateTime.of(2025, 3, 5, 0, 0),
            LocalDateTime.of(2025, 3, 6, 0, 0), true);
    CalendarEvent e2 = new CalendarEvent("Holiday", LocalDateTime.of(2025, 3, 6, 0, 0),
            LocalDateTime.of(2025, 3, 7, 0, 0), true);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByName("location", "Holiday", "Beach");
    assertEquals(2, count);
    assertEquals("Beach", e1.getLocation());
    assertEquals("Beach", e2.getLocation());
  }

  @Test
  public void testExportToCSV() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    String fileName = "test_export.csv";
    manager.exportToCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    assertTrue(file.length() > 0);
    file.delete();
  }

  @Test
  public void testExportToGoogleCSV() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    String fileName = "test_google.csv";
    manager.exportToGoogleCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    assertTrue(file.length() > 0);
    file.delete();
  }

  @Test
  public void testSortingAfterAdd() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Event1",
            LocalDateTime.of(2025, 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0), false);
    CalendarEvent e2 = new CalendarEvent("Event2",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    CalendarEvent e3 = new CalendarEvent("Event3",
            LocalDateTime.of(2025, 3, 1, 14, 0),
            LocalDateTime.of(2025, 3, 1, 15, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    manager.addEvent(e3, false);
    List<CalendarEvent> events = manager.getAllEvents();
    // Should be sorted by start time: Event2, Event1, Event3.
    assertEquals("Event2", events.get(0).getEventName());
    assertEquals("Event1", events.get(1).getEventName());
    assertEquals("Event3", events.get(2).getEventName());
  }

  @Test
  public void testGetEventsOn_MultiDayBoundary() throws Exception {
    CalendarManager manager = new CalendarManager();
    // Create a timed event that spans two days.
    CalendarEvent e1 = new CalendarEvent("Overnight",
            LocalDateTime.of(2025, 3, 1, 23, 0),
            LocalDateTime.of(2025, 3, 2, 1, 0), false);
    manager.addEvent(e1, false);
    List<CalendarEvent> eventsDay1 = manager.getEventsOn(LocalDate.of(2025, 3, 1));
    List<CalendarEvent> eventsDay2 = manager.getEventsOn(LocalDate.of(2025, 3, 2));
    assertTrue("Event should be found on start day", eventsDay1.contains(e1));
    assertTrue("Event should be found on end day", eventsDay2.contains(e1));
  }

  @Test
  public void testGetEventsInRange_Boundaries() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    // Range exactly from event start to event end.
    List<CalendarEvent> range = manager.getEventsInRange(
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0));
    // Based on the condition (start < endRange && end > startRange) the event should be included.
    assertTrue("Event should be in range", range.contains(e1));
  }

  @Test
  public void testIsBusyAt_Boundary() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("BusyTest",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(e1, false);
    // At exactly 9:00, should be busy.
    assertTrue(manager.isBusyAt(LocalDateTime.of(2025, 3, 1, 9, 0)));
    // At exactly 10:00, not busy because event.end equals 10:00 (and condition is event.end > dateTime).
    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3, 1, 10, 0)));
  }

  @Test
  public void testUpdateProperty_ValidAndInvalid() {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("TestEvent",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    // Add the event to the manager so that it can be found for editing.
    try {
      manager.addEvent(e1, false);
    } catch (Exception ex) {
      fail("Unexpected exception while adding event: " + ex.getMessage());
    }

    // Test updating valid property via editSingleEvent.
    boolean updated = manager.editSingleEvent("description", "TestEvent", e1.getStart(), e1.getEnd(), "NewDesc");
    assertTrue(updated);
    assertEquals("NewDesc", e1.getDescription());

    // Test updating an invalid property returns false.
    boolean result = manager.editSingleEvent("unknown", "TestEvent", e1.getStart(), e1.getEnd(), "X");
    assertFalse(result);
  }

  @Test
  public void testEditEventsByStart_Multiple() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("GroupEdit",
            LocalDateTime.of(2025, 3, 1, 8, 0),
            LocalDateTime.of(2025, 3, 1, 9, 0), false);
    CalendarEvent e2 = new CalendarEvent("GroupEdit",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByStart("description", "GroupEdit",
            LocalDateTime.of(2025, 3, 1, 9, 0), "Updated");
    // Only e2 should be updated because its start is after 9:00.
    assertEquals(1, count);
    assertEquals("", e1.getDescription());
    assertEquals("Updated", e2.getDescription());
  }

  @Test
  public void testEditEventsByName_Multiple() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("SameName",
            LocalDateTime.of(2025, 3, 1, 8, 0),
            LocalDateTime.of(2025, 3, 1, 9, 0), false);
    CalendarEvent e2 = new CalendarEvent("SameName",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByName("location", "SameName", "Office");
    assertEquals(2, count);
    assertEquals("Office", e1.getLocation());
    assertEquals("Office", e2.getLocation());
  }

  @Test
  public void testGetAllEvents_ReturnsCopy() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("CopyTest",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    manager.addEvent(e1, false);
    List<CalendarEvent> copy = manager.getAllEvents();
    copy.clear();
    // Manager's list should still contain one event.
    assertEquals(1, manager.getAllEvents().size());
  }

  // Test export methods by writing to temporary files and verifying content.
  @Test
  public void testExportToCSV_Content() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("CSVTest",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    e1.setDescription("Desc");
    e1.setLocation("Loc");
    e1.setPublic(false);
    manager.addEvent(e1, false);
    String fileName = "temp_export.csv";
    manager.exportToCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    String content = new String(Files.readAllBytes(file.toPath()));
    assertTrue(content.contains("EventName,Start,End,AllDay,Description,Location,Public"));
    assertTrue(content.contains("CSVTest"));
    file.delete();
  }

  @Test
  public void testExportToGoogleCSV_Content() throws Exception {
    CalendarManager manager = new CalendarManager();
    CalendarEvent e1 = new CalendarEvent("GoogleTest",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    e1.setDescription("GDesc");
    e1.setLocation("GLoc");
    e1.setPublic(true);
    manager.addEvent(e1, false);
    String fileName = "temp_export_google.csv";
    manager.exportToGoogleCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    String content = new String(Files.readAllBytes(file.toPath()));
    assertTrue(content.contains("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private"));
    assertTrue(content.contains("GoogleTest"));
    file.delete();
  }
}
