package com.java.system;

public class EquipmentLoan {
    private final int id;
    private final int studentId;
    private final int equipmentId;
    private LoanStatus status;

    public EquipmentLoan(int id, int studentId, int equipmentId, LoanStatus status) {
        this.id = id;
        this.studentId = studentId;
        this.equipmentId = equipmentId;
        this.status = status;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getEquipmentId() { return equipmentId; }
    public LoanStatus getStatus() { return status; }

    public boolean isActive() {
        return status == LoanStatus.BORROWED;
    }

    
    public void markReturned() {
        if (status != LoanStatus.BORROWED) {
            throw new IllegalStateException("Loan " + id + " was already returned.");
        }
        status = LoanStatus.RETURNED;
    }

    
    void undoReturn() {
        status = LoanStatus.BORROWED;
    }
}
