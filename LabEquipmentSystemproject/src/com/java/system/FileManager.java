package com.java.system;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class FileManager {
    public static final String STUDENTS_FILE = "students.txt";
    public static final String EQUIPMENT_FILE = "equipment.txt";
    public static final String LOANS_FILE = "loans.txt";
    public static final String REPORT_FILE = "report.txt";

    private final Path folder;

    public FileManager(String folderName) {
        this.folder = Path.of(folderName);
    }

   
    public void prepareFolder() throws IOException {
        Files.createDirectories(folder);
    }

    public Path getFolder() { return folder; }

    

    public List<Student> loadStudents() throws IOException {
        List<Student> result = new ArrayList<>();
        HashSet<Integer> ids = new HashSet<>();
        List<String> lines = readLines(STUDENTS_FILE);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = split(line, 3, STUDENTS_FILE, n);
            int id = positiveId(f[0], STUDENTS_FILE, n);
            if (!ids.add(id)) throw bad(STUDENTS_FILE, n, "duplicate ID " + id);
            result.add(new Student(id, requiredText(f[1], "name", STUDENTS_FILE, n),
                    requiredText(f[2], "department", STUDENTS_FILE, n)));
        }
        return result;
    }

    public List<Equipment> loadEquipment() throws IOException {
        List<Equipment> result = new ArrayList<>();
        HashSet<Integer> ids = new HashSet<>();
        HashSet<String> tags = new HashSet<>();
        List<String> lines = readLines(EQUIPMENT_FILE);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = split(line, 4, EQUIPMENT_FILE, n);
            int id = positiveId(f[0], EQUIPMENT_FILE, n);
            if (!ids.add(id)) throw bad(EQUIPMENT_FILE, n, "duplicate ID " + id);
            String name = requiredText(f[1], "name", EQUIPMENT_FILE, n);
            String tag = Equipment.normalizeTag(requiredText(f[2], "asset tag", EQUIPMENT_FILE, n));
            if (!tags.add(tag)) throw bad(EQUIPMENT_FILE, n, "duplicate asset tag " + tag);
            String category = f[3].trim();
            if (!Equipment.isValidCategory(category)) {
                throw bad(EQUIPMENT_FILE, n, "unknown category '" + category + "'");
            }
            result.add(new Equipment(id, name, tag, category));
        }
        return result;
    }

  
    public List<EquipmentLoan> loadLoans(List<Student> students, List<Equipment> equipment) throws IOException {
        HashSet<Integer> studentIds = new HashSet<>();
        for (Student s : students) studentIds.add(s.getId());
        HashSet<Integer> equipmentIds = new HashSet<>();
        for (Equipment e : equipment) equipmentIds.add(e.getId());

        List<EquipmentLoan> result = new ArrayList<>();
        HashSet<Integer> ids = new HashSet<>();
        HashSet<Integer> studentsWithLoan = new HashSet<>();
        HashSet<Integer> equipmentOnLoan = new HashSet<>();
        List<String> lines = readLines(LOANS_FILE);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = split(line, 4, LOANS_FILE, n);
            int id = positiveId(f[0], LOANS_FILE, n);
            if (!ids.add(id)) throw bad(LOANS_FILE, n, "duplicate ID " + id);
            int studentId = positiveId(f[1], LOANS_FILE, n);
            int equipmentId = positiveId(f[2], LOANS_FILE, n);
            if (!studentIds.contains(studentId)) throw bad(LOANS_FILE, n, "unknown student ID " + studentId);
            if (!equipmentIds.contains(equipmentId)) throw bad(LOANS_FILE, n, "unknown equipment ID " + equipmentId);
            LoanStatus status;
            try {
                status = LoanStatus.valueOf(f[3].trim());
            } catch (IllegalArgumentException e) {
                throw bad(LOANS_FILE, n, "unknown status '" + f[3].trim() + "'");
            }
            if (status == LoanStatus.BORROWED) {
                if (!studentsWithLoan.add(studentId)) {
                    throw bad(LOANS_FILE, n, "student " + studentId + " has more than one BORROWED loan");
                }
                if (!equipmentOnLoan.add(equipmentId)) {
                    throw bad(LOANS_FILE, n, "equipment " + equipmentId + " has more than one BORROWED loan");
                }
            }
            result.add(new EquipmentLoan(id, studentId, equipmentId, status));
        }
        return result;
    }

    // ----------------------------------------------------------------- saving

    public void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Student s : students) {
            lines.add(s.getId() + "|" + s.getName() + "|" + s.getDepartment());
        }
        writeSafely(STUDENTS_FILE, lines);
    }

    public void saveEquipment(List<Equipment> equipment) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Equipment e : equipment) {
            lines.add(e.getId() + "|" + e.getName() + "|" + e.getAssetTag() + "|" + e.getCategory());
        }
        writeSafely(EQUIPMENT_FILE, lines);
    }

    public void saveLoans(List<EquipmentLoan> loans) throws IOException {
        List<String> lines = new ArrayList<>();
        for (EquipmentLoan l : loans) {
            lines.add(l.getId() + "|" + l.getStudentId() + "|" + l.getEquipmentId() + "|" + l.getStatus());
        }
        writeSafely(LOANS_FILE, lines);
    }

    /** report.txt is only for people to read; it is never loaded as project data. */
    public void saveReport(List<String> lines) throws IOException {
        writeSafely(REPORT_FILE, lines);
    }

    // ---------------------------------------------------------------- helpers

    /** A missing file means an empty list (first run). */
    private List<String> readLines(String fileName) throws IOException {
        Path file = folder.resolve(fileName);
        List<String> lines = new ArrayList<>();
        if (!Files.exists(file)) {
            return lines;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new IOException(fileName + ": file is not valid UTF-8 text.");
        } catch (IOException e) {
            throw new IOException(fileName + ": cannot read file (" + e.getMessage() + ")");
        }
        return lines;
    }

    /** Write to a temp file, close it, then replace the original. */
    private void writeSafely(String fileName, List<String> lines) throws IOException {
        Path target = folder.resolve(fileName);
        Path temp = Files.createTempFile(folder, fileName, ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw new IOException("Could not save " + fileName + ": " + e.getMessage());
        }
    }

    private static String[] split(String line, int expected, String file, int lineNumber) throws IOException {
        String[] fields = line.split("\\|", -1);   // -1 keeps an empty last field
        if (fields.length != expected) {
            throw bad(file, lineNumber, "expected " + expected + " fields but found " + fields.length);
        }
        return fields;
    }

    private static int positiveId(String text, String file, int lineNumber) throws IOException {
        try {
            int value = Integer.parseInt(text.trim());
            if (value > 0) return value;
        } catch (NumberFormatException e) {
            // falls through to the error below
        }
        throw bad(file, lineNumber, "ID '" + text.trim() + "' must be a positive whole number");
    }

    private static String requiredText(String text, String label, String file, int lineNumber) throws IOException {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) throw bad(file, lineNumber, label + " is blank");
        return trimmed;
    }

    private static IOException bad(String file, int lineNumber, String problem) {
        return new IOException(file + " line " + lineNumber + ": " + problem);
    }
}
