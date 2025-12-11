package org.delcom.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PetForm {

    @NotBlank(message = "Jenis hewan harus diisi")
    private String petType;

    // Field opsional untuk menangkap input manual jika jenisnya "Lainnya"
    private String otherType;

    private String petCategory; // Bisa null jika bukan Anjing/Kucing

    @NotNull(message = "Jumlah hewan harus diisi")
    @Min(value = 1, message = "Jumlah minimal 1 ekor")
    private Integer quantity;

    @NotBlank(message = "Deskripsi harus diisi")
    private String description;

    @NotBlank(message = "Nama pemilik harus diisi")
    private String ownerName;

    @NotBlank(message = "Nomor HP pemilik harus diisi")
    @Pattern(regexp = "[0-9]+", message = "Nomor HP hanya boleh berisi angka")
    private String ownerPhone;

    // Constructor
    public PetForm() {
    }

    // Getters and Setters
    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }

    public String getOtherType() { return otherType; }
    public void setOtherType(String otherType) { this.otherType = otherType; }

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
}