package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommandParser {
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static void processCommand(String command, CalendarManager calendar) throws Exception {
    String lowerCmd = command.toLowerCase();
    if (lowerCmd.startsWith("create event")) {
      processCreateEvent(command, calendar);
    } else if (lowerCmd.startsWith("edit events")) {
      processEditCommand(command, calendar, true);
    } else if (lowerCmd.startsWith("edit event")) {
      processEditCommand(command, calendar, false);
    } else if (lowerCmd.startsWith("print events on")) {
      processPrintEventsOn(command, calendar);
    } else if (lowerCmd.startsWith("print events from")) {
      processPrintEventsRange(command, calendar);
    } else if (lowerCmd.startsWith("export cal")) {
      processExportCal(command, calendar);
    } else if (lowerCmd.startsWith("show status on")) {
      processShowStatus(command, calendar);
    } else if (lowerCmd.startsWith("export googlecsv")) {
      processExportGoogleCSV(command, calendar);
    } else {
      throw new Exception("Invalid command: " + command);
    }
  }

  static boolean hasAutoDecline(String command) {
    return command.toLowerCase().contains("--autodecline");
  }


  private static void processCreateEvent(String command, CalendarManager calendar) throws Exception {
    boolean autoDecline = false;
    if (command.toLowerCase().contains("--autodecline")) {
      autoDecline = true;
      command = command.replace("--autodecline", "").trim();
    }
    if (command.contains(" from ")) {
      String[] parts = command.split(" from ", 2);
      String eventName = parts[0].replace("create event", "").trim();
      String remainder = parts[1];
      if (!remainder.contains(" to ")) {
        throw new Exception("Invalid format: missing 'to' keyword.");
      }
      String[] timeParts = remainder.split(" to ", 2);
      String startStr = timeParts[0].trim();
      String afterTo = timeParts[1].trim();
      if (afterTo.toLowerCase().contains(" repeats ")) {
        String[] toParts = afterTo.split(" repeats ", 2);
        String endStr = toParts[0].trim();
        String repeatPart = toParts[1].trim();
        LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                eventName, startDateTime, endDateTime, repeatPart, false);
        for (CalendarEvent occurrence : occurrences) {
          calendar.addEvent(occurrence, autoDecline);
        }
        OutputHandler.getInstance().println("Recurring event created with " + occurrences.size() + " occurrences.");
      } else {
        String endStr = afterTo.trim();
        LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
        CalendarEvent event = new CalendarEvent(eventName, startDateTime, endDateTime, false);
        calendar.addEvent(event, autoDecline);
        OutputHandler.getInstance().println("Event created: " + event);
      }
    } else if (command.contains(" on ")) {
      String[] parts = command.split(" on ", 2);
      String eventName = parts[0].replace("create event", "").trim();
      String remainder = parts[1].trim();
      if (remainder.toLowerCase().contains(" repeats ")) {
        String[] dateParts = remainder.split(" repeats ", 2);
        String dateStr = dateParts[0].trim();
        String repeatPart = dateParts[1].trim();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                eventName, startDateTime, endDateTime, repeatPart, true);
        for (CalendarEvent occurrence : occurrences) {
          calendar.addEvent(occurrence, autoDecline);
        }
        OutputHandler.getInstance().println("Recurring all-day event created with " + occurrences.size() + " occurrences.");
      } else {
        String dateStr = remainder.trim();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        CalendarEvent event = new CalendarEvent(eventName, startDateTime, endDateTime, true);
        calendar.addEvent(event, autoDecline);
        OutputHandler.getInstance().println("All-day event created: " + event);
      }
    } else {
      throw new Exception("Invalid create event command format.");
    }
  }

  private static void processEditCommand(String command, CalendarManager calendar, boolean plural) throws Exception {
    String prefix = plural ? "edit events" : "edit event";
    String remainder = command.substring(prefix.length()).trim();
    if (remainder.contains(" with ")) {
      String[] parts = remainder.split(" with ", 2);
      String beforeWith = parts[0].trim();
      String newValue = parts[1].trim();
      if (beforeWith.contains(" from ")) {
        String[] splitFrom = beforeWith.split(" from ", 2);
        String firstPart = splitFrom[0].trim();
        String afterFrom = splitFrom[1].trim();
        if (!plural && !afterFrom.contains(" to ")) {
          throw new Exception("Missing 'to' clause for singular edit command.");
        }
        String property;
        String eventName;
        String[] tokens = firstPart.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        property = tokens[0].trim();
        eventName = tokens[1].trim();
        if (!plural) {
          String[] splitTo = afterFrom.split(" to ", 2);
          if (splitTo.length < 2) {
            throw new Exception("Missing 'to' clause for singular edit command.");
          }
          String startStr = splitTo[0].trim();
          String endStr = splitTo[1].trim();
          LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
          LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
          boolean updated = calendar.editSingleEvent(property, eventName, startDateTime, endDateTime, newValue);
          if (updated) {
            OutputHandler.getInstance().println("Event updated successfully.");
          } else {
            OutputHandler.getInstance().println("Event not found or update failed.");
          }
        } else {
          LocalDateTime startDateTime = LocalDateTime.parse(afterFrom, dateTimeFormatter);
          int count = calendar.editEventsByStart(property, eventName, startDateTime, newValue);
          OutputHandler.getInstance().println(count + " event(s) updated starting from " + startDateTime);
        }
      } else {
        String[] tokens = beforeWith.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String property = tokens[0].trim();
        String eventName = tokens[1].trim();
        int count = calendar.editEventsByName(property, eventName, newValue);
        OutputHandler.getInstance().println(count + " event(s) updated with new " + property);
      }
    } else {
      throw new Exception("Edit command must contain 'with' clause.");
    }
  }

  private static void processPrintEventsOn(String command, CalendarManager calendar) throws Exception {
    String[] parts = command.split(" on ", 2);
    if (parts.length < 2) {
      throw new Exception("Invalid command format for printing events.");
    }
    String dateStr = parts[1].trim();
    LocalDate date = LocalDate.parse(dateStr, dateFormatter);
    List<CalendarEvent> events = calendar.getEventsOn(date);
    if (events.isEmpty()) {
      OutputHandler.getInstance().println("No events found on " + date);
    } else {
      OutputHandler.getInstance().println("Events on " + date + ":");
      for (CalendarEvent event : events) {
        OutputHandler.getInstance().println(" - " + event);
      }
    }
  }

  private static void processPrintEventsRange(String command, CalendarManager calendar) throws Exception {
    String[] parts = command.split(" from ", 2);
    if (parts.length < 2) {
      throw new Exception("Invalid command format for printing events in range.");
    }
    String remainder = parts[1].trim();
    if (!remainder.contains(" to ")) {
      throw new Exception("Missing 'to' clause in range query.");
    }
    String[] timeParts = remainder.split(" to ", 2);
    String startStr = timeParts[0].trim();
    String endStr = timeParts[1].trim();
    LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
    LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
    List<CalendarEvent> events = calendar.getEventsInRange(startDateTime, endDateTime);
    if (events.isEmpty()) {
      OutputHandler.getInstance().println("No events found between " + startDateTime + " and " + endDateTime);
    } else {
      OutputHandler.getInstance().println("Events between " + startDateTime + " and " + endDateTime + ":");
      for (CalendarEvent event : events) {
        OutputHandler.getInstance().println(" - " + event);
      }
    }
  }

  private static void processExportCal(String command, CalendarManager calendar) throws Exception {
    String[] tokens = command.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToCSV(fileName);
  }

  private static void processExportGoogleCSV(String command, CalendarManager calendar) throws Exception {
    String[] tokens = command.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export googlecsv command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToGoogleCSV(fileName);
  }

  private static void processShowStatus(String command, CalendarManager calendar) throws Exception {
    String[] parts = command.split(" on ", 2);
    if (parts.length < 2) {
      throw new Exception("Invalid command format for show status.");
    }
    String dateTimeStr = parts[1].trim();
    LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter);
    boolean busy = calendar.isBusyAt(dateTime);
    OutputHandler.getInstance().println("Status at " + dateTime + ": " + (busy ? "Busy" : "Available"));
  }
}
