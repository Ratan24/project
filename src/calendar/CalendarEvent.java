package calendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CalendarEvent {
  private String eventName;
  private LocalDateTime start;
  private LocalDateTime end;
  private boolean isAllDay;
  private String description;
  private String location;
  private boolean isPublic;

  public CalendarEvent(String eventName, LocalDateTime start, LocalDateTime end, boolean isAllDay) {
    this.eventName = eventName;
    this.start = start;
    this.end = end;
    this.isAllDay = isAllDay;
    this.description = "";
    this.location = "";
    this.isPublic = true;
  }

  public boolean conflictsWith(CalendarEvent other) {
    return this.start.isBefore(other.end) && this.end.isAfter(other.start);
  }

  @Override
  public String toString() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String eventDetails;
    if (isAllDay) {
      eventDetails = String.format("%s (All Day on %s)", eventName, start.toLocalDate());
    } else {
      eventDetails = String.format("%s from %s to %s", eventName, start.format(dtf), end.format(dtf));
    }

    String descDetails = (!description.isEmpty()) ? ", Description: " + description : "";
    String locDetails  = (!location.isEmpty()) ? ", Location: " + location : "";
    String privacy     = isPublic ? "Public" : "Private";
    return eventDetails + descDetails + locDetails + ", " + privacy;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public LocalDateTime getStart() {
    return start;
  }

  public void setStart(LocalDateTime start) {
    this.start = start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  public void setEnd(LocalDateTime end) {
    this.end = end;
  }

  public boolean isAllDay() {
    return isAllDay;
  }

  public void setAllDay(boolean isAllDay) {
    this.isAllDay = isAllDay;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

}
