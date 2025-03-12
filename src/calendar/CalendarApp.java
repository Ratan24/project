package calendar;

import java.util.*;
import java.io.*;

public class CalendarApp {

  public static void main(String[] args) {
    CalendarManager calendar = new CalendarManager();
    if (args.length < 2) {
      OutputHandler.getInstance().println("Usage: --mode interactive OR --mode headless <commandFile.txt>");
      return;
    }
    if (args[0].equalsIgnoreCase("--mode")) {
      if (args[1].equalsIgnoreCase("interactive")) {
        runInteractiveMode(calendar);
      } else if (args[1].equalsIgnoreCase("headless")) {
        if (args.length < 3) {
          OutputHandler.getInstance().println("Headless mode requires a command file.");
          return;
        }
        runHeadlessMode(calendar, args[2]);
      } else {
        OutputHandler.getInstance().println("Invalid mode. Use interactive or headless.");
      }
    }
  }

  static void runInteractiveMode(CalendarManager calendar) {
    Scanner scanner = new Scanner(System.in);
    OutputHandler.getInstance().println("Calendar App Interactive Mode. Type 'exit' to quit.");
    while (true) {
      OutputHandler.getInstance().println("> ");
      String command = scanner.nextLine();
      if (command.equalsIgnoreCase("exit")) {
        OutputHandler.getInstance().println("Exiting.");
        break;
      }
      try {
        CommandParser.processCommand(command, calendar);
      } catch (Exception e) {
        OutputHandler.getInstance().println("Error: " + e.getMessage());
      }
    }
    scanner.close();
  }

  private static void runHeadlessMode(CalendarManager calendar, String fileName) {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String command;
      while ((command = br.readLine()) != null) {
        OutputHandler.getInstance().println("> " + command);
        if (command.equalsIgnoreCase("exit")) {
          OutputHandler.getInstance().println("Exiting.");
          break;
        }
        CommandParser.processCommand(command, calendar);
      }
    } catch (IOException e) {
      OutputHandler.getInstance().println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Command error: " + e.getMessage());
    }
  }
}
