package org.delcom.app.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pet_code", unique = true)
    private String petCode; // Contoh: ANJ-K-001

    @Column(name = "pet_type", nullable = false)
    private String petType;

    @Column(name = "pet_category")
    private String petCategory; // Kecil, Besar, VIP, dll

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owner_phone", nullable = false)
    private String ownerPhone;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "is_taken", nullable = false)
    private boolean isTaken = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Pet() {}

    // ==========================================
    // LOGIKA PERHITUNGAN BIAYA & WAKTU (UPDATE)
    // ==========================================

    // 1. Menentukan Harga Dasar per Ekor/Jam berdasarkan Jenis & Kategori
    public long getBaseHourlyRate() {
        String type = this.petType != null ? this.petType.trim() : "";
        String cat = this.petCategory != null ? this.petCategory.trim() : "";

        if (type.equalsIgnoreCase("Anjing")) {
            if ("Kecil".equalsIgnoreCase(cat)) return 2700;
            if ("Sedang".equalsIgnoreCase(cat)) return 4200;
            if ("Besar".equalsIgnoreCase(cat)) return 7700;
            return 4200; 
        } else if (type.equalsIgnoreCase("Kucing")) {
            if ("Standar".equalsIgnoreCase(cat)) return 1700;
            if ("Premium".equalsIgnoreCase(cat)) return 3800;
            if ("VIP".equalsIgnoreCase(cat)) return 9400;
            return 1700; 
        } else if (type.equalsIgnoreCase("Burung")) {
            return 700;
        } else if (type.equalsIgnoreCase("Kelinci")) {
            return 1000;
        } else if (type.equalsIgnoreCase("Hamster") || type.equalsIgnoreCase("Marmut")) {
            return 400;
        } else {
            return 1100; // Lainnya (Reptil dll)
        }
    }

    // 2. Menghitung Total Harga per Jam untuk Semua Ekor (BasePrice * Quantity)
    public long getTotalHourlyRate() {
        return getBaseHourlyRate() * (this.quantity != null ? this.quantity : 0);
    }

    // 3. Menghitung Durasi Tagihan (Billable Hours) - PERBAIKAN LOGIKA
    // Aturan: 
    // - Jika sisa menit < 30, dibulatkan ke bawah (ikut jam sebelumnya).
    // - Jika sisa menit >= 30, dibulatkan ke atas (tambah 1 jam).
    // - Minimal tagihan tetap 1 jam.
    public long getBillableHours() {
        if (this.createdAt == null) return 0;

        long totalMinutes = ChronoUnit.MINUTES.between(this.createdAt, LocalDateTime.now());

        // Ambil jam murni (integer division)
        // Contoh: 929 menit / 60 = 15 jam
        long hours = totalMinutes / 60;
        
        // Ambil sisa menit (modulus)
        // Contoh: 929 menit % 60 = 29 menit
        long minutesPart = totalMinutes % 60;

        // Logika Threshold 30 Menit
        if (minutesPart >= 30) {
            hours += 1; // Jika menit >= 30, tambah 1 jam
        }

        // Minimal bayar 1 jam (walaupun baru 5 menit)
        if (hours < 1) {
            hours = 1;
        }

        return hours;
    }

    // 4. Helper Teks Durasi Asli (Contoh: "1 Jam 45 Menit")
    public String getDurationText() {
        if (this.createdAt == null) return "-";
        long totalMinutes = ChronoUnit.MINUTES.between(this.createdAt, LocalDateTime.now());
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + " Jam " + minutes + " Menit";
    }

    // 5. TOTAL BIAYA AKHIR (TotalRate * BillableHours)
    public long getEstimatedCost() {
        return getTotalHourlyRate() * getBillableHours();
    }

    // ==========================================
    // LOGIKA STATUS (UPDATE MERAH/HIJAU)
    // ==========================================
    
    public String getStatusLabel() {
        if (isTaken) {
            return "Sudah Diambil";
        } else {
            return "Belum Diambil"; // Merah
        }
    }

    public String getStatusColor() {
        if (isTaken) {
            return "success"; // Hijau
        } else {
            return "danger";  // Merah
        }
    }

    public boolean isOverdue() {
        if (this.createdAt == null) return false;
        return ChronoUnit.DAYS.between(this.createdAt, LocalDateTime.now()) > 7;
    }

    // ==========================================
    // GETTERS & SETTERS STANDARD
    // ==========================================
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPetCode() { return petCode; }
    public void setPetCode(String petCode) { this.petCode = petCode; }
    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }
    public String getPetCategory() { return petCategory; }
    public void setPetCategory(String petCategory) { this.petCategory = petCategory; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isTaken() { return isTaken; }
    public void setTaken(boolean taken) { isTaken = taken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}