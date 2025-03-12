package calendar;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RecurringEventGeneratorTest {

  @Test
  public void testIsRecurringDay_True() {
    // "MWF" should return true for Monday, Wednesday, and Friday.
    String weekdays = "MWF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.MONDAY, weekdays));
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.WEDNESDAY, weekdays));
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.FRIDAY, weekdays));
  }

  @Test
  public void testIsRecurringDay_False() {
    // "MWF" should return false for Tuesday, Thursday, Saturday, and Sunday.
    String weekdays = "MWF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.TUESDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.THURSDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SATURDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SUNDAY, weekdays));
  }

  @Test
  public void testGenerateRecurringEvents_ForBranch() throws Exception {
    // Start on Monday, March 3, 2025. Use allowed weekdays "MWF" and request 3 occurrences.
    LocalDateTime start = LocalDate.of(2025, 3, 3).atStartOfDay(); // Monday
    LocalDateTime end = start.plusHours(1); // 1-hour event
    String repeatPart = "MWF for 3 times";
    List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end, repeatPart, false);
    // Expect occurrences on Monday, Wednesday, Friday => exactly 3 occurrences.
    assertEquals(3, occurrences.size());
    for (CalendarEvent ev : occurrences) {
      DayOfWeek dow = ev.getStart().getDayOfWeek();
      assertTrue(dow == DayOfWeek.MONDAY || dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.FRIDAY);
    }
  }

  @Test
  public void testGenerateRecurringEvents_UntilBranch() throws Exception {
    // Start on Monday, March 3, 2025.
    LocalDateTime start = LocalDate.of(2025, 3, 3).atStartOfDay(); // Monday
    LocalDateTime end = start.plusHours(1);
    // Use allowed weekdays "MWF" and set an until boundary so that the period covers 4 occurrences.
    // For instance, if we use "MWF until 2025-03-11T00:00", we should get events on:
    // Monday 3rd, Wednesday 5th, Friday 7th, and Monday 10th.
    String repeatPart = "MWF until 2025-03-11T00:00";
    List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end, repeatPart, false);
    assertEquals(4, occurrences.size());
    // Verify the days of week:
    assertEquals(DayOfWeek.MONDAY, occurrences.get(0).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.WEDNESDAY, occurrences.get(1).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.FRIDAY, occurrences.get(2).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.MONDAY, occurrences.get(3).getStart().getDayOfWeek());
  }

  @Test(expected = Exception.class)
  public void testGenerateRecurringEvents_InvalidFormat() throws Exception {
    LocalDateTime start = LocalDateTime.of(2025, 3, 3, 9, 0);
    LocalDateTime end = start.plusHours(1);
    String repeatPart = "XYZ invalid format";
    RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end, repeatPart, false);
  }
  @Test
  public void testIsRecurringDay_Monday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.MONDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Tuesday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.TUESDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Wednesday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.WEDNESDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Thursday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.THURSDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Friday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.FRIDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Saturday() {
    // Saturday is not allowed in "MTWRF"
    String allowed = "MTWRF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SATURDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Sunday() {
    // Sunday is not allowed in "MTWRF"
    String allowed = "MTWRF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SUNDAY, allowed));
  }

  @Test
  public void testDayToCharForAllDays() {
    assertEquals('M', RecurringEventGenerator.dayToChar(DayOfWeek.MONDAY));
    assertEquals('T', RecurringEventGenerator.dayToChar(DayOfWeek.TUESDAY));
    assertEquals('W', RecurringEventGenerator.dayToChar(DayOfWeek.WEDNESDAY));
    assertEquals('R', RecurringEventGenerator.dayToChar(DayOfWeek.THURSDAY));
    assertEquals('F', RecurringEventGenerator.dayToChar(DayOfWeek.FRIDAY));
    assertEquals('S', RecurringEventGenerator.dayToChar(DayOfWeek.SATURDAY));
    assertEquals('U', RecurringEventGenerator.dayToChar(DayOfWeek.SUNDAY));
  }

  @Test
  public void testGenerateRecurringEvents_InvalidRepeatPart() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 10, 0);
    try {
      // Passing a repeatPart that is just a single space should result in tokens.length==0.
      RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end, " ", false);
      fail("Expected exception for invalid recurring event format.");
    } catch (Exception e) {
      assertEquals("Invalid recurring event format.", e.getMessage());
    }
  }

  @Test(expected = Exception.class)
  public void testGenerateRecurringEvents_EmptyRepeatPart() throws Exception {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 10, 0);
    // This should trigger the defensive check.
    RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end, "   ", false);
  }
}
