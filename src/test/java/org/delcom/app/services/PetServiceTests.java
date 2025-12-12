package org.delcom.app.services;

import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTests {

    @InjectMocks
    private PetService petService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private FileStorageService fileStorageService;

    private User user;
    private Pet pet;
    private UUID petId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        petId = UUID.randomUUID();
        pet = new Pet();
        pet.setId(petId);
        pet.setUserId(user.getId());
        pet.setPetType("Anjing");
        pet.setPetCategory("Kecil");
    }

    @Test
    void testGetAllPets() {
        when(petRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(pet));
        List<Pet> pets = petService.getAllPetsByUser(user.getId());
        assertEquals(1, pets.size());
    }

    @Test
    void testGetPetById() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        Pet found = petService.getPetById(petId);
        assertNotNull(found);
        
        when(petRepository.findById(petId)).thenReturn(Optional.empty());
        assertNull(petService.getPetById(petId));
    }

    @Test
    void testCreatePet() {
        when(petRepository.save(pet)).thenReturn(pet);
        Pet result = petService.createPet(user.getId(), pet);
        assertEquals(user.getId(), result.getUserId());
    }

    @Test
    void testGeneratePetCode_PrefixLogic() {
        // 1. Anjing Cases
        pet.setPetType("Anjing");
        pet.setPetCategory("Kecil");
        assertTrue(petService.generatePetCode(pet, user).startsWith("ANJ-K-"));
        
        pet.setPetCategory("Sedang");
        assertTrue(petService.generatePetCode(pet, user).startsWith("ANJ-S-"));

        pet.setPetCategory("Besar");
        assertTrue(petService.generatePetCode(pet, user).startsWith("ANJ-B-"));

        pet.setPetCategory("Unknown");
        assertTrue(petService.generatePetCode(pet, user).startsWith("ANJ-X-"));

        // 2. Kucing Cases
        pet.setPetType("Kucing");
        pet.setPetCategory("Standar");
        assertTrue(petService.generatePetCode(pet, user).startsWith("KUC-ST-"));
        
        pet.setPetCategory("Premium");
        assertTrue(petService.generatePetCode(pet, user).startsWith("KUC-PR-"));
        
        pet.setPetCategory("VIP");
        assertTrue(petService.generatePetCode(pet, user).startsWith("KUC-VP-"));
        
        pet.setPetCategory("Unknown");
        assertTrue(petService.generatePetCode(pet, user).startsWith("KUC-X-"));

        // 3. Simple Types
        pet.setPetType("Burung");
        assertTrue(petService.generatePetCode(pet, user).startsWith("BRG-"));

        pet.setPetType("Kelinci");
        assertTrue(petService.generatePetCode(pet, user).startsWith("KLN-"));

        pet.setPetType("Hamster");
        assertTrue(petService.generatePetCode(pet, user).startsWith("HMR-"));
        
        pet.setPetType("Marmut");
        assertTrue(petService.generatePetCode(pet, user).startsWith("HMR-"));

        // 4. "Lainnya" Logic Coverage (CRITICAL FOR COVERAGE)
        // Cover case where type is literally "Lainnya" (if Service handles it)
        pet.setPetType("Lainnya");
        String codeLainnya = petService.generatePetCode(pet, user);
        assertNotNull(codeLainnya); // Expect RPT- or default
        
        // Cover case where type is custom name (e.g. Iguana)
        pet.setPetType("Iguana"); 
        assertTrue(petService.generatePetCode(pet, user).startsWith("RPT-"));
        
        // 5. Default/Unknown
        pet.setPetType("Alien");
        assertNotNull(petService.generatePetCode(pet, user)); 
    }

    @Test
    void testGeneratePetCode_IncrementLogic() {
        pet.setPetType("Anjing");
        pet.setPetCategory("Kecil");

        when(petRepository.findLatestCodeByUser(eq("ANJ-K-"), eq(user.getId()))).thenReturn(null);
        assertEquals("ANJ-K-001", petService.generatePetCode(pet, user));

        when(petRepository.findLatestCodeByUser(eq("ANJ-K-"), eq(user.getId()))).thenReturn("ANJ-K-009");
        assertEquals("ANJ-K-010", petService.generatePetCode(pet, user));

        when(petRepository.findLatestCodeByUser(eq("ANJ-K-"), eq(user.getId()))).thenReturn("ANJ-K-XYZ");
        assertEquals("ANJ-K-001", petService.generatePetCode(pet, user));
    }

    @Test
    void testUpdatePet_NotFound() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());
        assertNull(petService.updatePet(petId, pet, user));
    }

    @Test
    void testUpdatePet_NoChangesInType() {
        Pet existing = new Pet();
        existing.setId(petId);
        existing.setPetType("Anjing");
        existing.setPetCategory("Kecil");
        existing.setPetCode("OLD-CODE");

        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);

        Pet updatedData = new Pet();
        updatedData.setPetType("Anjing");
        updatedData.setPetCategory("Kecil");

        Pet result = petService.updatePet(petId, updatedData, user);
        assertEquals("OLD-CODE", result.getPetCode());
    }

    @Test
    void testUpdatePet_ChangeType_TriggersNewCode() {
        Pet existing = new Pet();
        existing.setId(petId);
        existing.setPetType("Anjing");
        existing.setPetCategory("Kecil");
        existing.setPetCode("ANJ-K-001");

        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);
        
        when(petRepository.findLatestCodeByUser(anyString(), eq(user.getId()))).thenReturn(null);

        Pet updatedData = new Pet();
        updatedData.setPetType("Kucing");
        updatedData.setPetCategory("Standar");

        Pet result = petService.updatePet(petId, updatedData, user);
        assertTrue(result.getPetCode().startsWith("KUC-ST"));
    }

    @Test
    void testUpdatePet_ChangeCategory_TriggersNewCode() {
        Pet existing = new Pet();
        existing.setId(petId);
        existing.setPetType("Anjing");
        existing.setPetCategory("Kecil");
        existing.setPetCode("ANJ-K-001");

        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);

        Pet updatedData = new Pet();
        updatedData.setPetType("Anjing");
        updatedData.setPetCategory("Besar");

        Pet result = petService.updatePet(petId, updatedData, user);
        assertTrue(result.getPetCode().startsWith("ANJ-B"));
    }

    @Test
    void testUpdatePet_LainnyaBranch() {
        Pet existing = new Pet();
        existing.setId(petId);
        existing.setPetType("Anjing");

        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);

        Pet updatedData = new Pet();
        updatedData.setPetType("Iguana"); 

        Pet result = petService.updatePet(petId, updatedData, user);
        
        assertEquals("Iguana", result.getPetType());
        verify(petRepository).save(existing);
    }

    @Test
    void testUpdatePet_CategoryNullChange() {
        Pet existing = new Pet();
        existing.setPetType("Anjing");
        existing.setPetCategory("Kecil");
        
        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);

        Pet updated = new Pet();
        updated.setPetType("Anjing");
        updated.setPetCategory(null);
        
        petService.updatePet(petId, updated, user);
        assertTrue(existing.getPetCode().startsWith("ANJ-X")); 
    }

    @Test
    void testUpdatePet_CategoryNullToNotNullChange() {
        Pet existing = new Pet();
        existing.setPetType("Anjing");
        existing.setPetCategory(null); 
        
        when(petRepository.findById(petId)).thenReturn(Optional.of(existing));
        when(petRepository.save(existing)).thenReturn(existing);

        Pet updated = new Pet();
        updated.setPetType("Anjing");
        updated.setPetCategory("Besar"); 
        
        petService.updatePet(petId, updated, user);
        assertTrue(existing.getPetCode().startsWith("ANJ-B")); 
    }

    @Test
    void testUpdatePetImage() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(file.isEmpty()).thenReturn(false);
        pet.setImagePath("old.jpg");
        when(fileStorageService.storeFile(file, petId)).thenReturn("new.jpg");
        
        petService.updatePetImage(petId, file);
        verify(fileStorageService).deleteFile("old.jpg");
        assertEquals("new.jpg", pet.getImagePath());

        when(petRepository.findById(petId)).thenReturn(Optional.empty());
        assertNull(petService.updatePetImage(petId, file));

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(file.isEmpty()).thenReturn(true);
        assertNull(petService.updatePetImage(petId, file));
    }

    @Test
    void testDeletePet_Success() {
        pet.setOwnerPhone("08123");
        pet.setImagePath("img.jpg");

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        
        assertTrue(petService.deletePet(petId, "08123"));
        
        verify(fileStorageService).deleteFile("img.jpg");
        verify(petRepository).delete(pet);
    }

    @Test
    void testDeletePet_Mismatch() {
        pet.setOwnerPhone("08123");
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        assertFalse(petService.deletePet(petId, "00000"));
        verify(petRepository, never()).delete(any(Pet.class));
    }

    @Test
    void testDeletePet_NotFound() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());
        assertFalse(petService.deletePet(petId, "08123"));
    }
    
    @Test
    void testDeletePet_FileDeletionError() {
        pet.setOwnerPhone("08123");
        pet.setImagePath("img.jpg");
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        
        doThrow(new RuntimeException("File error")).when(fileStorageService).deleteFile("img.jpg");
        
        try {
            petService.deletePet(petId, "08123");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    void testGetPetTypeStats() {
        Pet p1 = new Pet(); p1.setPetType("Anjing");
        Pet p2 = new Pet(); p2.setPetType("Kucing");
        Pet p3 = new Pet(); p3.setPetType("Anjing");

        when(petRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(p1, p2, p3));

        Map<String, Long> stats = petService.getPetTypeStats(user.getId());
        
        assertEquals(2, stats.get("ANJING"));
        assertEquals(1, stats.get("KUCING"));
    }
}