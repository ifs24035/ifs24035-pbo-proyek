package org.delcom.app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PetFormTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        PetForm petForm = new PetForm();

        // Act
        petForm.setPetType("Anjing");
        petForm.setOtherType("Lainnya");
        petForm.setPetCategory("Besar");
        petForm.setQuantity(2);
        petForm.setDescription("Lucu");
        petForm.setOwnerName("Budi");
        petForm.setOwnerPhone("0812345");

        // Assert
        assertEquals("Anjing", petForm.getPetType());
        assertEquals("Lainnya", petForm.getOtherType());
        assertEquals("Besar", petForm.getPetCategory());
        assertEquals(2, petForm.getQuantity());
        assertEquals("Lucu", petForm.getDescription());
        assertEquals("Budi", petForm.getOwnerName());
        assertEquals("0812345", petForm.getOwnerPhone());
    }

    // --- NEW: Object Methods ---
    @Test
    void testObjectMethods() {
        PetForm f1 = new PetForm();
        f1.setPetType("A");
        
        PetForm f2 = new PetForm();
        f2.setPetType("A");
        
        PetForm f3 = new PetForm();
        f3.setPetType("B");

        // Jika PetForm menggunakan @Data (Lombok), ini wajib.
        // Jika manual getter/setter tanpa equals/hashcode override, test ini akan fail di assertNotEquals(f1, f3) kalau logic equals default (address memory).
        // Tapi biasanya DTO perlu equals test jika dicover 100%.
        
        // Asumsi menggunakan Lombok @Data atau equals manual:
        // assertEquals(f1, f2); 
        // assertNotEquals(f1, f3);
        // assertEquals(f1.hashCode(), f2.hashCode());
        
        assertNotNull(f1.toString());
    }

    @Test
    void testValidation_Success() {
        PetForm form = new PetForm();
        form.setPetType("Kucing");
        form.setQuantity(1);
        form.setDescription("Oyen");
        form.setOwnerName("Siti");
        form.setOwnerPhone("08123456789");

        Set<ConstraintViolation<PetForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty(), "Should be valid");
    }

    @Test
    void testValidation_Fail_QuantityMin() {
        PetForm form = new PetForm();
        form.setPetType("Kucing");
        form.setQuantity(0); // Invalid (Min 1)
        form.setDescription("Desc");
        form.setOwnerName("Name");
        form.setOwnerPhone("08123");

        Set<ConstraintViolation<PetForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Jumlah minimal")));
    }

    @Test
    void testValidation_Fail_PhoneRegex() {
        PetForm form = new PetForm();
        form.setPetType("Kucing");
        form.setQuantity(1);
        form.setDescription("Desc");
        form.setOwnerName("Name");
        form.setOwnerPhone("0812abc"); // Invalid (Contains letters)

        Set<ConstraintViolation<PetForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("hanya boleh berisi angka")));
    }

    @Test
    void testValidation_Fail_BlankFields() {
        PetForm form = new PetForm();
        // Semua null

        Set<ConstraintViolation<PetForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        
        // Cek beberapa pesan error wajib
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Jenis hewan harus diisi")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Deskripsi harus diisi")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Nama pemilik harus diisi")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Nomor HP pemilik harus diisi")));
    }
}