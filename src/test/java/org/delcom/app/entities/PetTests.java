package org.delcom.app.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class PetTests {

    @Test
    void testGettersSettersAndConstructors() {
        Pet pet = new Pet();
        pet.setPetType("Anjing");
        pet.setQuantity(2);
        pet.setDescription("Lucu");
        pet.setOwnerName("Budi");
        pet.setOwnerPhone("081234");
        
        // Trigger PrePersist logic manually or via simulation if needed, 
        // but for simple getter/setter coverage:
        Assertions.assertEquals("Anjing", pet.getPetType());
        Assertions.assertEquals(2, pet.getQuantity());
        Assertions.assertEquals("Lucu", pet.getDescription());
        Assertions.assertEquals("Budi", pet.getOwnerName());
        Assertions.assertEquals("081234", pet.getOwnerPhone());
        
        // Test boolean isTaken setter/getter
        pet.setTaken(true);
        Assertions.assertTrue(pet.isTaken());
        
        // Test Code & Image
        pet.setPetCode("ANJ-001");
        pet.setImagePath("img.jpg");
        Assertions.assertEquals("ANJ-001", pet.getPetCode());
        Assertions.assertEquals("img.jpg", pet.getImagePath());
        
        // Test Category
        pet.setPetCategory("Kecil");
        Assertions.assertEquals("Kecil", pet.getPetCategory());
    }

    @Test
    void testLifecycleMethods() {
        Pet pet = new Pet();
        pet.onCreate(); // @PrePersist
        Assertions.assertNotNull(pet.getCreatedAt());
        Assertions.assertNotNull(pet.getUpdatedAt());

        pet.onUpdate(); // @PreUpdate
        Assertions.assertNotNull(pet.getUpdatedAt());
    }

    @Test
    void testBillableHours() {
        Pet pet = new Pet();
        pet.onCreate();

        // 1. Case < 90 mins (misal 30 menit) -> Harusnya 1 jam
        // Kita manipulasi createdAt seolah-olah dibuat 30 menit lalu
        // Reflection atau set via field tidak bisa karena private/protected, 
        // tapi kita bisa test logic internalnya atau override createdAt via subclass/mock if necessary.
        // TAPI, karena kita test unit Entity pojo, kita bisa akali dengan membuat class turunan untuk test 
        // atau kita asumsikan logic ChronoUnit benar.
        // Cara paling bersih di unit test sederhana:
        
        // Simulate: Created 10 mins ago
        TestablePet pet1 = new TestablePet(LocalDateTime.now().minusMinutes(10));
        Assertions.assertEquals(1, pet1.getBillableHours());

        // Simulate: Created 100 mins ago (1 jam 40 menit) -> 40 >= 30 -> 2 Jam
        TestablePet pet2 = new TestablePet(LocalDateTime.now().minusMinutes(100));
        Assertions.assertEquals(2, pet2.getBillableHours());
        
        // Simulate: Created 65 mins ago (1 jam 5 menit) -> 5 < 30 -> 1 Jam
        TestablePet pet3 = new TestablePet(LocalDateTime.now().minusMinutes(65));
        Assertions.assertEquals(1, pet3.getBillableHours());
        
        // Null check
        Pet nullPet = new Pet();
        Assertions.assertEquals(0, nullPet.getBillableHours());
        Assertions.assertEquals("-", nullPet.getDurationText());
    }

    @Test
    void testPricingLogic() {
        // Test Anjing Kecil
        Pet p1 = new Pet(); p1.setPetType("Anjing"); p1.setPetCategory("Kecil"); p1.setQuantity(1);
        Assertions.assertEquals(2700, p1.getBaseHourlyRate());
        
        // Test Anjing Sedang
        Pet p2 = new Pet(); p2.setPetType("Anjing"); p2.setPetCategory("Sedang");
        Assertions.assertEquals(4200, p2.getBaseHourlyRate());

        // Test Anjing Besar
        Pet p3 = new Pet(); p3.setPetType("Anjing"); p3.setPetCategory("Besar");
        Assertions.assertEquals(7700, p3.getBaseHourlyRate());
        
        // Test Anjing Default
        Pet p4 = new Pet(); p4.setPetType("Anjing"); p4.setPetCategory("X");
        Assertions.assertEquals(4200, p4.getBaseHourlyRate());

        // Test Kucing VIP
        Pet p5 = new Pet(); p5.setPetType("Kucing"); p5.setPetCategory("VIP");
        Assertions.assertEquals(9400, p5.getBaseHourlyRate());
        
        // Test Kucing Premium
        Pet p6 = new Pet(); p6.setPetType("Kucing"); p6.setPetCategory("Premium");
        Assertions.assertEquals(3800, p6.getBaseHourlyRate());

        // Test Kucing Standar/Default
        Pet p7 = new Pet(); p7.setPetType("Kucing"); p7.setPetCategory("Standar");
        Assertions.assertEquals(1700, p7.getBaseHourlyRate());
        
        // Test Hewan Lain
        Pet p8 = new Pet(); p8.setPetType("Burung"); Assertions.assertEquals(700, p8.getBaseHourlyRate());
        Pet p9 = new Pet(); p9.setPetType("Kelinci"); Assertions.assertEquals(1000, p9.getBaseHourlyRate());
        Pet p10 = new Pet(); p10.setPetType("Hamster"); Assertions.assertEquals(400, p10.getBaseHourlyRate());
        Pet p11 = new Pet(); p11.setPetType("Iguana"); Assertions.assertEquals(1100, p11.getBaseHourlyRate());
    }

    @Test
    void testTotalCost() {
        // Mocking behavior with subclass to control time
        TestablePet pet = new TestablePet(LocalDateTime.now().minusMinutes(120)); // 2 Jam
        pet.setPetType("Burung"); // 700
        pet.setQuantity(2); // x2
        
        // Rate = 700 * 2 = 1400
        Assertions.assertEquals(1400, pet.getTotalHourlyRate());
        
        // Cost = 1400 * 2 jam = 2800
        Assertions.assertEquals(2800, pet.getEstimatedCost());
    }

    @Test
    void testStatusAndOverdue() {
        Pet pet = new Pet();
        pet.onCreate();
        
        // 1. Not Taken, Not Overdue
        Assertions.assertFalse(pet.isTaken());
        Assertions.assertEquals("Belum Diambil", pet.getStatusLabel());
        Assertions.assertEquals("danger", pet.getStatusColor());
        
        // 2. Taken
        pet.setTaken(true);
        Assertions.assertEquals("Sudah Diambil", pet.getStatusLabel());
        Assertions.assertEquals("success", pet.getStatusColor());

        // 3. Overdue Check
        TestablePet overduePet = new TestablePet(LocalDateTime.now().minusDays(8));
        Assertions.assertTrue(overduePet.isOverdue());
        
        Pet newPet = new Pet();
        Assertions.assertFalse(newPet.isOverdue()); // CreatedAt null check
    }

    // Helper class to inject createdAt
    static class TestablePet extends Pet {
        public TestablePet(LocalDateTime created) {
            try {
                java.lang.reflect.Field f = Pet.class.getDeclaredField("createdAt");
                f.setAccessible(true);
                f.set(this, created);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}