package com.java.system;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class LabService {
    private final FileManager files;
    private ArrayList<Student> students = new ArrayList<>();
    private ArrayList<Equipment> equipment = new ArrayList<>();
    private ArrayList<EquipmentLoan> loans = new ArrayList<>();
    private HashSet<String> assetTags = new HashSet<>();   // normalized tags, rebuilt at startup
    private int nextStudentId = 1;
    private int nextEquipmentId = 1;
    private int nextLoanId = 1;

    public LabService(FileManager files) {
        this.files = files;
    }

   
    public void load() throws IOException {
        files.prepareFolder();
        List<Student> loadedStudents = files.loadStudents();
        List<Equipment> loadedEquipment = files.loadEquipment();
        List<EquipmentLoan> loadedLoans = files.loadLoans(loadedStudents, loadedEquipment);

        students = new ArrayList<>(loadedStudents);
        equipment = new ArrayList<>(loadedEquipment);
        loans = new ArrayList<>(loadedLoans);

        assetTags = new HashSet<>();
        for (Equipment e : equipment) assetTags.add(e.getAssetTag());

        nextStudentId = 1;
        for (Student s : students) nextStudentId = Math.max(nextStudentId, s.getId() + 1);
        nextEquipmentId = 1;
        for (Equipment e : equipment) nextEquipmentId = Math.max(nextEquipmentId, e.getId() + 1);
        nextLoanId = 1;
        for (EquipmentLoan l : loans) nextLoanId = Math.max(nextLoanId, l.getId() + 1);
    }

    
    public List<Student> getStudents() { return Collections.unmodifiableList(students); }
    public List<Equipment> getEquipment() { return Collections.unmodifiableList(equipment); }
    public List<EquipmentLoan> getLoans() { return Collections.unmodifiableList(loans); }

    public Student findStudent(int id) {
        for (Student s : students) if (s.getId() == id) return s;
        return null;
    }

    public Equipment findEquipment(int id) {
        for (Equipment e : equipment) if (e.getId() == id) return e;
        return null;
    }

    public EquipmentLoan findLoan(int id) {
        for (EquipmentLoan l : loans) if (l.getId() == id) return l;
        return null;
    }

    /** The BORROWED loan for this equipment item, or null. */
    public EquipmentLoan activeLoanForEquipment(int equipmentId) {
        for (EquipmentLoan l : loans) {
            if (l.getEquipmentId() == equipmentId && l.isActive()) return l;
        }
        return null;
    }

    /** Counts this student's BORROWED loans (rule: at most one). */
    public int countActiveLoans(int studentId) {
        int count = 0;
        for (EquipmentLoan l : loans) {
            if (l.getStudentId() == studentId && l.isActive()) count++;
        }
        return count;
    }

    /** Available means no BORROWED loan references this equipment ID. */
    public boolean isAvailable(int equipmentId) {
        return activeLoanForEquipment(equipmentId) == null;
    }

    // ------------------------------------------------------------ validation

    /** Trims text and rejects blank values, '|' and line breaks. */
    public static String cleanText(String value, String label) {
        String text = (value == null) ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
        if (text.indexOf('|') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " cannot contain | or line breaks.");
        }
        return text;
    }

    // --------------------------------------------------------------- actions

    public Student registerStudent(String name, String department) throws IOException {
        String cleanName = cleanText(name, "Name");
        String cleanDepartment = cleanText(department, "Department");
        Student student = new Student(nextStudentId, cleanName, cleanDepartment);
        students.add(student);
        try {
            files.saveStudents(students);
        } catch (IOException e) {
            students.remove(student);   // cancel the change
            throw e;
        }
        nextStudentId++;
        return student;
    }

    public Equipment addEquipment(String name, String assetTag, String category) throws IOException {
        String cleanName = cleanText(name, "Equipment name");
        String tag = Equipment.normalizeTag(cleanText(assetTag, "Asset tag"));
        if (!Equipment.isValidCategory(category)) {
            throw new IllegalArgumentException("Choose a valid category.");
        }
        if (assetTags.contains(tag)) {
            throw new IllegalArgumentException("Asset tag " + tag + " already exists.");
        }
        Equipment item = new Equipment(nextEquipmentId, cleanName, tag, category);
        equipment.add(item);
        try {
            files.saveEquipment(equipment);
        } catch (IOException e) {
            equipment.remove(item);     // cancel the change
            throw e;
        }
        assetTags.add(tag);             // add the key only after saving
        nextEquipmentId++;
        return item;
    }

    public EquipmentLoan issue(int studentId, int equipmentId) throws IOException {
        if (findStudent(studentId) == null) {
            throw new IllegalArgumentException("No student with ID " + studentId + ".");
        }
        if (findEquipment(equipmentId) == null) {
            throw new IllegalArgumentException("No equipment with ID " + equipmentId + ".");
        }
        if (!isAvailable(equipmentId)) {
            throw new IllegalArgumentException("This equipment is already borrowed.");
        }
        if (countActiveLoans(studentId) >= 1) {
            throw new IllegalArgumentException("This student already has one borrowed item.");
        }
        EquipmentLoan loan = new EquipmentLoan(nextLoanId, studentId, equipmentId, LoanStatus.BORROWED);
        loans.add(loan);
        try {
            files.saveLoans(loans);
        } catch (IOException e) {
            loans.remove(loan);         // cancel the change
            throw e;
        }
        nextLoanId++;
        return loan;
    }

    public EquipmentLoan returnItem(int loanId) throws IOException {
        EquipmentLoan loan = findLoan(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("No loan with ID " + loanId + ".");
        }
        if (!loan.isActive()) {
            throw new IllegalArgumentException("Loan " + loanId + " was already returned.");
        }
        loan.markReturned();
        try {
            files.saveLoans(loans);
        } catch (IOException e) {
            loan.undoReturn();          // cancel the change
            throw e;
        }
        return loan;
    }

    // --------------------------------------------------------------- reports

    private int countLoans(LoanStatus status) {
        int count = 0;
        for (EquipmentLoan l : loans) if (l.getStatus() == status) count++;
        return count;
    }

    public List<String> reportLines() {
        int borrowed = 0;
        for (Equipment e : equipment) if (!isAvailable(e.getId())) borrowed++;

        List<String> lines = new ArrayList<>();
        lines.add("LAB EQUIPMENT REPORT");
        lines.add("Students: " + students.size());
        lines.add("Total equipment items: " + equipment.size());
        lines.add("Borrowed items: " + borrowed);
        lines.add("Available items: " + (equipment.size() - borrowed));
        lines.add("Total loans: " + loans.size());
        lines.add("Returned loans: " + countLoans(LoanStatus.RETURNED));
        lines.add("");
        lines.add("EQUIPMENT SUMMARY");
        if (equipment.isEmpty()) {
            lines.add("No records found.");
        }
        for (Equipment e : equipment) {
            lines.add(equipmentLine(e));
        }
        return lines;
    }

    /** One readable line per item, including who has it if it is borrowed. */
    public String equipmentLine(Equipment e) {
        String line = "ID " + e.getId() + " | " + e.getName() + " | Tag: " + e.getAssetTag()
                + " | " + e.getCategory() + " | ";
        EquipmentLoan loan = activeLoanForEquipment(e.getId());
        if (loan == null) {
            return line + "Available";
        }
        Student s = findStudent(loan.getStudentId());
        String who = (s == null) ? "student " + loan.getStudentId() : s.getName() + " (ID " + s.getId() + ")";
        return line + "Borrowed by " + who + ", loan " + loan.getId();
    }

    public void showReport() {
        for (String line : reportLines()) System.out.println(line);
    }

    public void saveReport() throws IOException {
        files.saveReport(reportLines());
    }
}
