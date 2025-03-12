package calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RecurringEventGenerator {
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  /**
   * Maps a DayOfWeek to its corresponding character.
   * For example: MONDAY -> 'M', TUESDAY -> 'T', WEDNESDAY -> 'W', THURSDAY -> 'R',
   * FRIDAY -> 'F', SATURDAY -> 'S', SUNDAY -> 'U'.
   */
  public static char dayToChar(DayOfWeek day) {
    switch (day) {
      case MONDAY:    return 'M';
      case TUESDAY:   return 'T';
      case WEDNESDAY: return 'W';
      case THURSDAY:  return 'R';
      case FRIDAY:    return 'F';
      case SATURDAY:  return 'S';
      case SUNDAY:    return 'U';
      default:        throw new IllegalArgumentException("Unknown day: " + day);
    }
  }

  /**
   * Checks whether the given day is included in the allowed weekdays string.
   */
  public static boolean isRecurringDay(DayOfWeek day, String weekdaysStr) {
    char dayChar = dayToChar(day);
    return weekdaysStr.toUpperCase().indexOf(dayChar) >= 0;
  }

  /**
   * Generates recurring events based on the provided repeatPart.
   * The repeatPart can specify either a fixed number of occurrences using "for N times"
   * or an end boundary using "until <dateTime>".
   */
  public static List<CalendarEvent> generateRecurringEvents(String eventName,
                                                            LocalDateTime startDateTime,
                                                            LocalDateTime endDateTime,
                                                            String repeatPart,
                                                            boolean isAllDay)
          throws Exception {

    String trimmed = repeatPart.trim();
    if (trimmed.isEmpty()) {
      throw new Exception("Invalid recurring event format.");
    }

    List<CalendarEvent> occurrences = new ArrayList<>();
    String[] tokens = repeatPart.split(" ");
    String weekdaysStr = tokens[0].trim().toUpperCase();

    if (repeatPart.toLowerCase().contains(" for ")) {
      if (tokens.length < 4 || !tokens[1].equalsIgnoreCase("for") || !tokens[3].equalsIgnoreCase("times")) {
        throw new Exception("Invalid recurring event format (for N times).");
      }
      int occurrencesCount = Integer.parseInt(tokens[2]);
      LocalDateTime current = startDateTime;
      while (occurrences.size() < occurrencesCount) {
        if (isRecurringDay(current.getDayOfWeek(), weekdaysStr)) {
          addOccurrence(occurrences, eventName, current, startDateTime, endDateTime, isAllDay);
        }
        current = current.plusDays(1);
      }
    } else if (repeatPart.toLowerCase().contains(" until ")) {
      int index = repeatPart.toLowerCase().indexOf("until");
      String untilPart = repeatPart.substring(index + "until".length()).trim();
      LocalDateTime untilDateTime;
      if (isAllDay) {
        LocalDate untilDate = LocalDate.parse(untilPart, dateFormatter);
        untilDateTime = untilDate.plusDays(1).atStartOfDay();
      } else {
        untilDateTime = LocalDateTime.parse(untilPart, dateTimeFormatter);
      }
      LocalDateTime current = startDateTime;
      while (!current.isAfter(untilDateTime.minusSeconds(1))) {
        if (isRecurringDay(current.getDayOfWeek(), weekdaysStr)) {
          addOccurrence(occurrences, eventName, current, startDateTime, endDateTime, isAllDay);
        }
        current = current.plusDays(1);
      }
    } else {
      throw new Exception("Invalid recurring event format.");
    }
    return occurrences;
  }

  /**
   * Helper method to add an occurrence to the list.
   */
  private static void addOccurrence(List<CalendarEvent> occurrences, String eventName,
                                    LocalDateTime current, LocalDateTime startDateTime,
                                    LocalDateTime endDateTime, boolean isAllDay) {
    LocalDate currentDate = current.toLocalDate();
    LocalDateTime occStart = LocalDateTime.of(currentDate, startDateTime.toLocalTime());
    LocalDateTime occEnd = LocalDateTime.of(currentDate, endDateTime.toLocalTime());
    occurrences.add(new CalendarEvent(eventName, occStart, occEnd, isAllDay));
  }
}
