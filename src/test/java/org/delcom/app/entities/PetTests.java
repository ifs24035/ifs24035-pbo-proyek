package org.delcom.app.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PetTests {

    private Pet pet;

    @BeforeEach
    void setUp() {
        pet = new Pet();
    }

    @Test
    void testGettersAndSetters() {
        UUID id = UUID.randomUUID();
        pet.setId(id);
        pet.setPetType("Anjing");
        // ... (coverage standard)
        assertEquals(id, pet.getId());
    }

    @Test
    void testObjectMethods() {
        // Ensure toString/hashCode/equals are executed
        assertNotNull(pet.toString());
        int h = pet.hashCode();
        assertTrue(pet.equals(pet));
        assertFalse(pet.equals(null));
        assertFalse(pet.equals(new Object()));
        Pet p2 = new Pet(); p2.setId(UUID.randomUUID());
        assertFalse(pet.equals(p2));
    }

    @Test
    void testGetBaseHourlyRate() {
        // 1. Anjing & Branches
        pet.setPetType("Anjing");
        pet.setPetCategory("Kecil"); assertEquals(2700, pet.getBaseHourlyRate());
        pet.setPetCategory("Besar"); assertEquals(7700, pet.getBaseHourlyRate());
        pet.setPetCategory("Sedang"); assertEquals(4200, pet.getBaseHourlyRate());
        pet.setPetCategory(null); assertEquals(4200, pet.getBaseHourlyRate()); // Cover NULL category

        // 2. Kucing & Branches
        pet.setPetType("Kucing");
        pet.setPetCategory("Standar"); assertEquals(1700, pet.getBaseHourlyRate());
        pet.setPetCategory("Premium"); assertEquals(3800, pet.getBaseHourlyRate());
        pet.setPetCategory("VIP"); assertEquals(9400, pet.getBaseHourlyRate());
        pet.setPetCategory("Unknown"); assertEquals(1700, pet.getBaseHourlyRate());

        // 3. Others
        pet.setPetType("Burung"); assertEquals(700, pet.getBaseHourlyRate());
        pet.setPetType(null); assertEquals(1100, pet.getBaseHourlyRate()); // Cover NULL type
    }

    @Test
    void testGetTotalHourlyRate() {
        pet.setPetType("Burung");
        pet.setQuantity(5);
        assertEquals(3500, pet.getTotalHourlyRate()); 
        
        pet.setQuantity(null);
        assertEquals(0, pet.getTotalHourlyRate());
    }

    @Test
    void testTimeCalculations() {
        // 1. Init
        assertEquals(0, pet.getBillableHours());
        assertEquals("-", pet.getDurationText());

        LocalDateTime now = LocalDateTime.now();
        
        // 2. < 30 mins -> round down (but min 1 hour usually? No, rule is round nearest or min 1)
        // Logic check: if < 1 hour, is it 1?
        ReflectionTestUtils.setField(pet, "createdAt", now.minusMinutes(10));
        assertEquals(1, pet.getBillableHours());
        assertTrue(pet.getDurationText().contains("0 Jam 10 Menit"));

        // 3. > 1 hour, < 30 mins remainder
        ReflectionTestUtils.setField(pet, "createdAt", now.minusMinutes(75)); 
        assertEquals(1, pet.getBillableHours());

        // 4. > 1 hour, > 30 mins remainder
        ReflectionTestUtils.setField(pet, "createdAt", now.minusMinutes(100)); 
        assertEquals(2, pet.getBillableHours());
        
        // 5. Exact Hour
        ReflectionTestUtils.setField(pet, "createdAt", now.minusMinutes(60));
        assertTrue(pet.getDurationText().contains("1 Jam 0 Menit"));
    }

    @Test
    void testGetEstimatedCost() {
        pet.setPetType("Hamster");
        pet.setQuantity(2);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(pet, "createdAt", now.minusMinutes(100)); 
        assertEquals(1600, pet.getEstimatedCost());
        
        // Zero Cost
        pet.setQuantity(null);
        assertEquals(0, pet.getEstimatedCost());
    }
    
    @Test
    void testLifecycle() {
        pet.onCreate();
        pet.onUpdate();
        assertNotNull(pet.getCreatedAt());
    }
    
    @Test
    void testOverdue() {
        assertFalse(pet.isOverdue());
        ReflectionTestUtils.setField(pet, "createdAt", LocalDateTime.now().minusDays(8));
        assertTrue(pet.isOverdue());
    }
}