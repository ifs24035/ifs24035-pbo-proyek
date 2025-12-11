package org.delcom.app.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.delcom.app.configs.AuthContext;
import org.delcom.app.dto.PetForm;
import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
import org.delcom.app.services.PetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

public class PetControllerTests {

    @Test
    @DisplayName("Pengujian untuk PetController")
    void testPetController() throws Exception {
        // Setup ID
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();

        // Setup Mock Service
        PetService petService = Mockito.mock(PetService.class);

        // Setup Controller & AuthContext
        PetController petController = new PetController(petService);
        petController.authContext = new AuthContext();

        User authUser = new User("Test User", "test@example.com", "password");
        authUser.setId(userId);

        // ==========================================
        // 1. Test getAllPets
        // ==========================================
        {
            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.getAllPets();
            assert (res.getStatusCode().is4xxClientError());

            // Success
            petController.authContext.setAuthUser(authUser);
            when(petService.getAllPetsByUser(any(UUID.class))).thenReturn(List.of(new Pet()));
            
            res = petController.getAllPets();
            assert (res.getStatusCode().is2xxSuccessful());
            assert (res.getBody().getStatus().equals("success"));
        }

        // ==========================================
        // 2. Test getPetDetail
        // ==========================================
        {
            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.getPetDetail(petId);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Not Found
            when(petService.getPetById(any(UUID.class))).thenReturn(null);
            res = petController.getPetDetail(petId);
            assert (res.getStatusCode().is4xxClientError());

            // Success
            Pet p = new Pet(); p.setId(petId);
            when(petService.getPetById(any(UUID.class))).thenReturn(p);
            res = petController.getPetDetail(petId);
            assert (res.getStatusCode().is2xxSuccessful());
        }

        // ==========================================
        // 3. Test createPet
        // ==========================================
        {
            PetForm form = new PetForm();
            form.setPetType("Anjing");
            form.setQuantity(1);
            
            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.createPet(form);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Success Standard
            when(petService.generatePetCode(any(Pet.class))).thenReturn("ANJ-001");
            when(petService.createPet(any(UUID.class), any(Pet.class))).thenReturn(new Pet());
            
            res = petController.createPet(form);
            assert (res.getStatusCode().is2xxSuccessful());

            // Success "Lainnya" Logic
            PetForm formOther = new PetForm();
            formOther.setPetType("Lainnya");
            formOther.setOtherType("Ular");
            formOther.setQuantity(1);
            
            res = petController.createPet(formOther);
            assert (res.getStatusCode().is2xxSuccessful());
        }

        // ==========================================
        // 4. Test updatePet
        // ==========================================
        {
            PetForm form = new PetForm();
            form.setPetType("Kucing");
            
            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.updatePet(petId, form);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Not Found
            when(petService.getPetById(any(UUID.class))).thenReturn(null);
            res = petController.updatePet(petId, form);
            assert (res.getStatusCode().is4xxClientError());

            // Success
            when(petService.getPetById(any(UUID.class))).thenReturn(new Pet());
            when(petService.updatePet(any(UUID.class), any(Pet.class))).thenReturn(new Pet());
            
            res = petController.updatePet(petId, form);
            assert (res.getStatusCode().is2xxSuccessful());
        }

        // ==========================================
        // 5. Test uploadImage
        // ==========================================
        {
            MultipartFile file = Mockito.mock(MultipartFile.class);

            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.uploadImage(petId, file);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Exception (Internal Server Error)
            when(petService.updatePetImage(any(UUID.class), any())).thenThrow(new IOException());
            res = petController.uploadImage(petId, file);
            assert (res.getStatusCode().is5xxServerError());

            // Not Found (Service returns null)
            when(petService.updatePetImage(any(UUID.class), any())).thenReturn(null);
            res = petController.uploadImage(petId, file);
            assert (res.getStatusCode().is4xxClientError());

            // Success
            Pet p = new Pet(); p.setImagePath("img.jpg");
            when(petService.updatePetImage(any(UUID.class), any())).thenReturn(p);
            res = petController.uploadImage(petId, file);
            assert (res.getStatusCode().is2xxSuccessful());
        }

        // ==========================================
        // 6. Test deletePet
        // ==========================================
        {
            String phone = "08123";

            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.deletePet(petId, phone);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Fail (Wrong phone / Not found)
            when(petService.deletePet(any(UUID.class), any(String.class))).thenReturn(false);
            res = petController.deletePet(petId, phone);
            assert (res.getStatusCode().is4xxClientError());

            // Success
            when(petService.deletePet(any(UUID.class), any(String.class))).thenReturn(true);
            res = petController.deletePet(petId, phone);
            assert (res.getStatusCode().is2xxSuccessful());
        }

        // ==========================================
        // 7. Test updateStatus
        // ==========================================
        {
            // Unauthorized
            petController.authContext.setAuthUser(null);
            var res = petController.updateStatus(petId, true);
            assert (res.getStatusCode().is4xxClientError());

            petController.authContext.setAuthUser(authUser);

            // Not Found
            when(petService.getPetById(any(UUID.class))).thenReturn(null);
            res = petController.updateStatus(petId, true);
            assert (res.getStatusCode().is4xxClientError());

            // Success
            Pet p = new Pet();
            when(petService.getPetById(any(UUID.class))).thenReturn(p);
            when(petService.createPet(any(), any())).thenReturn(p); // Mock save
            
            res = petController.updateStatus(petId, true);
            assert (res.getStatusCode().is2xxSuccessful());
            assert (res.getBody().getMessage().contains("Sudah"));

            res = petController.updateStatus(petId, false);
            assert (res.getBody().getMessage().contains("Belum"));
        }
    }
}