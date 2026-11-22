package com.java.system;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;


public class Main {
    private static final Scanner scanner = new Scanner(System.in);   // one Scanner for the whole program
    private static LabService service;
   

    public static void main(String[] args) {
        String dataFolder = (args.length > 0) ? args[0] : "data";
        service = new LabService(new FileManager(dataFolder));
        try {
            service.load();
        } catch (IOException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Fix the file in the '" + dataFolder + "' folder and run the program again.");
            System.exit(1);
        }

        try {
            runMainMenu();
        } catch (java.util.NoSuchElementException e) {
            System.out.println();
            System.out.println("Input ended. Goodbye.");
        }
    }

    

    private static void runMainMenu() {
        while (true) {
            System.out.println();
            System.out.println("MAIN MENU");
            System.out.println("1. Lab assistant menu");
            System.out.println("2. Student menu");
            System.out.println("0. Exit");
            int choice = readChoice("Choose: ", 0, 2);
            if (choice == 1) {
                showLabAssistantMenu();
            } else if (choice == 2) {
                showStudentMenu();
            } else {
                System.out.println("Goodbye.");
                return;
            }
        }
    }

    private static void showLabAssistantMenu() {
        while (true) {
            System.out.println();
            System.out.println("LAB ASSISTANT MENU");
            System.out.println("1. Register student");
            System.out.println("2. Add equipment item");
            System.out.println("3. View equipment and availability");
            System.out.println("4. Issue equipment");
            System.out.println("5. Record return");
            System.out.println("6. View reports");
            System.out.println("0. Back");
            int choice = readChoice("Choose: ", 0, 6);
            switch (choice) {
                case 1 -> registerStudent();
                case 2 -> addEquipment();
                case 3 -> viewEquipment();
                case 4 -> issueEquipment();
                case 5 -> recordReturn();
                case 6 -> viewReports();
                default -> { return; }
            }
        }
    }

    private static void showStudentMenu() {
        System.out.println();
        System.out.println("STUDENT MENU");
        Student student = readExistingStudent();
        if (student == null) {
            return;
        }
        while (true) {
            System.out.println();
            System.out.println("STUDENT MENU (" + student.getName() + ")");
            System.out.println("1. View my profile");
            System.out.println("2. View equipment");
            System.out.println("3. View my borrowing history");
            System.out.println("0. Back");
            int choice = readChoice("Choose: ", 0, 3);
            switch (choice) {
                case 1 -> student.showProfile();
                case 2 -> viewEquipment();
                case 3 -> viewHistory(student);
                default -> { return; }
            }
        }
    }

   

    private static void registerStudent() {
        System.out.println();
        System.out.println("REGISTER STUDENT");
        String name = readText("Name: ", "Name");
        String department = readText("Department: ", "Department");
        try {
            Student s = service.registerStudent(name, department);
            System.out.println("Student " + s.getId() + " saved. Name: " + s.getName());
        } catch (IOException e) {
            System.out.println("Not saved. " + e.getMessage());
        }
    }

    private static void addEquipment() {
        System.out.println();
        System.out.println("ADD EQUIPMENT");
        String name = readText("Equipment name: ", "Equipment name");
        String tag = readUniqueTag();
        String[] categories = Equipment.getCategories();
        for (int i = 0; i < categories.length; i++) {
            System.out.println((i + 1) + ". " + categories[i]);
        }
        int choice = readChoice("Category: ", 1, categories.length);   // validated before indexing
        String category = categories[choice - 1];
        try {
            Equipment e = service.addEquipment(name, tag, category);
            System.out.println("Equipment " + e.getId() + " saved. Tag: " + e.getAssetTag());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Not saved. " + e.getMessage());
        }
    }

    private static void viewEquipment() {
        System.out.println();
        System.out.println("EQUIPMENT");
        List<Equipment> items = service.getEquipment();
        if (items.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Equipment e : items) {
            System.out.println(service.equipmentLine(e));
        }
    }

    private static void issueEquipment() {
        System.out.println();
        System.out.println("ISSUE EQUIPMENT");
        if (service.getStudents().isEmpty() || service.getEquipment().isEmpty()) {
            System.out.println("Register a student and add equipment first.");
            return;
        }
        Student student = readExistingStudent();
        if (student == null) return;
        Equipment item = readExistingEquipment();
        if (item == null) return;
        try {
            EquipmentLoan loan = service.issue(student.getId(), item.getId());
            System.out.println("Loan " + loan.getId() + " saved. Status: " + loan.getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Not saved. " + e.getMessage());
        }
    }

    private static void recordReturn() {
        System.out.println();
        System.out.println("RECORD RETURN");
        if (service.getLoans().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        while (true) {
            int id = readInt("Return loan ID (0 to cancel): ");
            if (id == 0) return;
            EquipmentLoan loan = service.findLoan(id);
            if (loan == null) {
                System.out.println("No loan with ID " + id + ".");
                continue;
            }
            try {
                service.returnItem(id);
                Equipment item = service.findEquipment(loan.getEquipmentId());
                System.out.println("Loan saved. Status: " + loan.getStatus());
                System.out.println(item.getName() + " is now available.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Not saved. " + e.getMessage());
            }
            return;
        }
    }

    private static void viewHistory(Student student) {
        System.out.println();
        System.out.println("MY BORROWING HISTORY");
        boolean any = false;
        for (EquipmentLoan loan : service.getLoans()) {
            if (loan.getStudentId() != student.getId()) continue;   // filter by student ID
            Equipment item = service.findEquipment(loan.getEquipmentId());
            System.out.println("Loan " + loan.getId() + " | " + item.getName()
                    + " (" + item.getAssetTag() + ") | " + loan.getStatus());
            any = true;
        }
        if (!any) {
            System.out.println("No records found.");
        }
    }

    private static void viewReports() {
        System.out.println();
        service.showReport();
        System.out.println();
        System.out.println("1. Save report to report.txt");
        System.out.println("0. Back");
        if (readChoice("Choose: ", 0, 1) == 1) {
            try {
                service.saveReport();
                System.out.println("Report saved to report.txt.");
            } catch (IOException e) {
                System.out.println("Not saved. " + e.getMessage());
            }
        }
    }

   

    
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int readInt(String prompt) {
        while (true) {
            String text = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= 0) return value;
            } catch (NumberFormatException e) {
                // fall through to message
            }
            System.out.println("Enter a whole number, 0 or higher.");
        }
    }

    private static int readChoice(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) return value;
            System.out.println("Choose a number from " + min + " to " + max + ".");
        }
    }

    private static String readText(String prompt, String label) {
        while (true) {
            try {
                return LabService.cleanText(readLine(prompt), label);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

   
    private static String readUniqueTag() {
        while (true) {
            String tag = Equipment.normalizeTag(readText("Asset tag: ", "Asset tag"));
            boolean used = false;
            for (Equipment e : service.getEquipment()) {
                if (e.getAssetTag().equals(tag)) used = true;
            }
            if (!used) return tag;
            System.out.println("Asset tag " + tag + " already exists.");
        }
    }

    private static Student readExistingStudent() {
        while (true) {
            int id = readInt("Student ID (0 to cancel): ");
            if (id == 0) return null;
            Student s = service.findStudent(id);
            if (s != null) return s;
            System.out.println("No student with ID " + id + ".");
        }
    }

    private static Equipment readExistingEquipment() {
        while (true) {
            int id = readInt("Equipment ID (0 to cancel): ");
            if (id == 0) return null;
            Equipment e = service.findEquipment(id);
            if (e != null) return e;
            System.out.println("No equipment with ID " + id + ".");
        }
    }
}
