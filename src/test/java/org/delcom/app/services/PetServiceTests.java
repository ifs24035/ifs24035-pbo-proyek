package org.delcom.app.services;

import org.delcom.app.entities.Pet;
import org.delcom.app.repositories.PetRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class PetServiceTests {

    @Mock
    private PetRepository petRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PetService petService;

    @Test
    void testGetAllPetsByUser() {
        UUID userId = UUID.randomUUID();
        Mockito.when(petRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(new Pet()));
        
        List<Pet> result = petService.getAllPetsByUser(userId);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    void testGetPetById() {
        UUID id = UUID.randomUUID();
        Pet pet = new Pet();
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.of(pet));
        
        Pet result = petService.getPetById(id);
        Assertions.assertNotNull(result);

        // Not Found
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertNull(petService.getPetById(id));
    }

    @Test
    void testCreatePet() {
        UUID userId = UUID.randomUUID();
        Pet pet = new Pet();
        Mockito.when(petRepository.save(any(Pet.class))).thenReturn(pet);
        
        Pet result = petService.createPet(userId, pet);
        Assertions.assertEquals(userId, result.getUserId());
    }

    @Test
    void testUpdatePet() {
        UUID id = UUID.randomUUID();
        Pet existing = new Pet();
        Pet updateData = new Pet();
        updateData.setPetType("Baru");
        
        // Success
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.of(existing));
        Mockito.when(petRepository.save(any(Pet.class))).thenReturn(existing);
        
        Pet result = petService.updatePet(id, updateData);
        Assertions.assertEquals("Baru", result.getPetType());

        // Not Found
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertNull(petService.updatePet(id, updateData));
    }

    @Test
    void testUpdatePetImage() throws IOException {
        UUID id = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setImagePath("old.jpg");
        MultipartFile file = Mockito.mock(MultipartFile.class);

        // Success
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.of(pet));
        Mockito.when(file.isEmpty()).thenReturn(false);
        Mockito.when(fileStorageService.storeFile(file, id)).thenReturn("new.jpg");
        Mockito.when(petRepository.save(any(Pet.class))).thenReturn(pet);

        Pet result = petService.updatePetImage(id, file);
        Assertions.assertEquals("new.jpg", result.getImagePath());
        Mockito.verify(fileStorageService).deleteFile("old.jpg");

        // Null checks
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertNull(petService.updatePetImage(id, file));
    }

    @Test
    void testDeletePet() {
        UUID id = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setOwnerPhone("08123");
        pet.setImagePath("img.jpg");

        Mockito.when(petRepository.findById(id)).thenReturn(Optional.of(pet));

        // Wrong Phone
        boolean res1 = petService.deletePet(id, "0000");
        Assertions.assertFalse(res1);

        // Correct Phone
        boolean res2 = petService.deletePet(id, "08123");
        Assertions.assertTrue(res2);
        Mockito.verify(fileStorageService).deleteFile("img.jpg");
        Mockito.verify(petRepository).delete(pet);

        // Not Found
        Mockito.when(petRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertFalse(petService.deletePet(id, "08123"));
    }

    @Test
    void testGeneratePetCode() {
        // Case 1: First Code (Repository returns null)
        Mockito.when(petRepository.findLatestCode(anyString())).thenReturn(null);
        Pet p1 = new Pet(); p1.setPetType("Anjing"); p1.setPetCategory("Kecil");
        
        String code1 = petService.generatePetCode(p1);
        Assertions.assertEquals("ANJ-K-001", code1);

        // Case 2: Next Code (Repository returns 005)
        Mockito.when(petRepository.findLatestCode(anyString())).thenReturn("ANJ-K-005");
        String code2 = petService.generatePetCode(p1);
        Assertions.assertEquals("ANJ-K-006", code2);
        
        // Case 3: Invalid Number Parse (Fallback to 001)
        Mockito.when(petRepository.findLatestCode(anyString())).thenReturn("ANJ-K-XYZ");
        String code3 = petService.generatePetCode(p1);
        Assertions.assertEquals("ANJ-K-001", code3);

        // Coverage for GetPrefix logic
        Pet pX = new Pet();
        pX.setPetType("Kucing"); pX.setPetCategory("VIP");
        petService.generatePetCode(pX); // KUC-VP
        
        pX.setPetType("Burung"); 
        petService.generatePetCode(pX); // BRG
        
        pX.setPetType("Lainnya");
        petService.generatePetCode(pX); // RPT
    }
}