package calendar;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.io.*;

public class CalendarManager {
  private List<CalendarEvent> events;

  public CalendarManager() {
    events = new ArrayList<>();
  }

  /**
   * Adds an event to the calendar.
   * If autoDecline is true and a conflict is found, an Exception is thrown.
   * Otherwise, a warning is printed.
   */

  public void addEvent(CalendarEvent newEvent, boolean autoDecline) throws Exception {
    // Check for conflicts
    checkAndHandleConflict(newEvent, autoDecline);
    events.add(newEvent);
    // Sort events by start time
    events.sort(Comparator.comparing(e -> e.getStart()));
  }

  /**
   * Iterates over the existing events and handles any conflicts with newEvent.
   * Returns true if any conflict is found.
   */
  private boolean checkAndHandleConflict(CalendarEvent newEvent, boolean autoDecline) throws Exception {
    boolean conflictFound = false;
    for (CalendarEvent event : events) {
      if (newEvent.conflictsWith(event)) {
        conflictFound = true;
        if (autoDecline == true) {
          throw new Exception("Conflict detected with event: " + event.getEventName());
        } else {
          OutputHandler.getInstance().println("Warning: Event conflicts with " + event.getEventName());
        }
      }
    }
    return conflictFound;
  }

  /**
   * Returns a list of events that occur on the given date.
   */
  public List<CalendarEvent> getEventsOn(LocalDate date) {
    List<CalendarEvent> result = new ArrayList<>();
    for (CalendarEvent event : events) {
      if (event.isAllDay()) {
        if (event.getStart().toLocalDate().equals(date)) {
          result.add(event);
        }
      } else {
        if (!event.getStart().toLocalDate().isAfter(date) &&
                !event.getEnd().toLocalDate().isBefore(date)) {
          result.add(event);
        }
      }
    }
    return result;
  }

  /**
   * Returns a list of events that occur within the given time range.
   */
  public List<CalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange) {
    List<CalendarEvent> result = new ArrayList<>();
    for (CalendarEvent event : events) {
      if (event.getStart().isBefore(endRange) && event.getEnd().isAfter(startRange)) {
        result.add(event);
      }
    }
    return result;
  }

  /**
   * Exports the calendar events to a CSV file.
   */
  public void exportToCSV(String fileName) {
    try (PrintWriter writer = new PrintWriter(new File(fileName))) {
      StringBuilder sb = new StringBuilder();
      sb.append("EventName,Start,End,AllDay,Description,Location,Public\n");
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      for (CalendarEvent event : events) {
        sb.append("\"" + event.getEventName() + "\",");
        sb.append(event.getStart().format(dtf) + ",");
        sb.append(event.getEnd().format(dtf) + ",");
        sb.append(event.isAllDay() + ",");
        sb.append("\"" + event.getDescription() + "\",");
        sb.append("\"" + event.getLocation() + "\",");
        sb.append(event.isPublic() + "\n");
      }
      writer.write(sb.toString());
      OutputHandler.getInstance().println("Exported to CSV: " + new File(fileName).getAbsolutePath());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting CSV: " + e.getMessage());
    }
  }

  /**
   * Exports the calendar events to a Google CSV file.
   */
  public void exportToGoogleCSV(String fileName) {
    try (PrintWriter writer = new PrintWriter(new File(fileName))) {
      StringBuilder sb = new StringBuilder();
      sb.append("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

      for (CalendarEvent event : events) {
        sb.append("\"" + event.getEventName() + "\",");
        if (event.isAllDay()) {
          sb.append(event.getStart().format(dateFormatter) + ",,");
          sb.append(event.getStart().format(dateFormatter) + ",,");
          sb.append("True,");
        } else {
          sb.append(event.getStart().format(dateFormatter) + ",");
          sb.append(event.getStart().format(timeFormatter) + ",");
          sb.append(event.getEnd().format(dateFormatter) + ",");
          sb.append(event.getEnd().format(timeFormatter) + ",");
          sb.append("False,");
        }
        sb.append("\"" + event.getDescription() + "\",");
        sb.append("\"" + event.getLocation() + "\",");
        sb.append(event.isPublic() ? "False" : "True");
        sb.append("\n");
      }
      writer.write(sb.toString());
      OutputHandler.getInstance().println("Exported to Google CSV: " + new File(fileName).getAbsolutePath());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting Google CSV: " + e.getMessage());
    }
  }

  /**
   * Checks if the calendar is busy at the specified dateTime.
   */
  public boolean isBusyAt(LocalDateTime dateTime) {
    for (CalendarEvent event : events) {
      if (!event.getStart().isAfter(dateTime) && event.getEnd().isAfter(dateTime)) {
        return true;
      }
    }
    return false;
  }

  public boolean editSingleEvent(String property, String eventName, LocalDateTime start,
                                 LocalDateTime end, String newValue) {
    for (CalendarEvent event : events) {
      if (event.getEventName().equals(eventName) &&
              event.getStart().equals(start) &&
              event.getEnd().equals(end)) {
        if (updateProperty(event, property, newValue)) {
          return true;
        }
      }
    }
    return false;
  }

  public int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue) {
    int count = 0;
    for (CalendarEvent event : events) {
      if (event.getEventName().equals(eventName) &&
              (event.getStart().equals(start) || event.getStart().isAfter(start))) {
        if (updateProperty(event, property, newValue)) {
          count++;
        }
      }
    }
    return count;
  }

  public int editEventsByName(String property, String eventName, String newValue) {
    int count = 0;
    for (CalendarEvent event : events) {
      if (event.getEventName().equals(eventName)) {
        if (updateProperty(event, property, newValue)) {
          count++;
        }
      }
    }
    return count;
  }

  private boolean updateProperty(CalendarEvent event, String property, String newValue) {
    switch (property.toLowerCase()) {
      case "name":
        event.setEventName(newValue);
        break;
      case "description":
        event.setDescription(newValue);
        break;
      case "location":
        event.setLocation(newValue);
        break;
      case "public":
        event.setPublic(Boolean.parseBoolean(newValue));
        break;
      default:
        return false;
    }
    return true;
  }

  // Expose a copy of the events list for testing purposes.
  public List<CalendarEvent> getAllEvents() {
    return new ArrayList<>(events);
  }
}
