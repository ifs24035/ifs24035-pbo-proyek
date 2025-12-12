package org.delcom.app.controllers;

import org.delcom.app.configs.ApiResponse;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.dto.PetForm;
import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
import org.delcom.app.services.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetControllerTests {

    @InjectMocks
    private PetController petController;

    @Mock
    private PetService petService;

    @Mock
    private AuthContext authContext;

    @Mock
    private MultipartFile multipartFile;

    private User mockUser;
    private Pet mockPet;
    private UUID petId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        petId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setName("Test User");

        mockPet = new Pet();
        mockPet.setId(petId);
        mockPet.setUserId(userId);

        ReflectionTestUtils.setField(petController, "authContext", authContext);
    }

    @Test
    void testGetAllPets() {
        // Unauthorized
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.getAllPets().getStatusCode());

        // Success
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getAllPetsByUser(userId)).thenReturn(new ArrayList<>());
        assertEquals(HttpStatus.OK, petController.getAllPets().getStatusCode());
    }

    @Test
    void testGetPetDetail() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.getPetDetail(petId).getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, petController.getPetDetail(petId).getStatusCode());

        when(petService.getPetById(petId)).thenReturn(mockPet);
        assertEquals(HttpStatus.OK, petController.getPetDetail(petId).getStatusCode());
    }

    @Test
    void testCreatePet() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.createPet(new PetForm()).getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.createPet(eq(userId), any(Pet.class))).thenReturn(mockPet);

        assertEquals(HttpStatus.OK, petController.createPet(new PetForm()).getStatusCode());
    }

    @Test
    void testUpdatePet() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.updatePet(petId, new PetForm()).getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        // Not Found
        when(petService.getPetById(petId)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, petController.updatePet(petId, new PetForm()).getStatusCode());

        // Success
        when(petService.getPetById(petId)).thenReturn(mockPet);
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenReturn(mockPet);
        assertEquals(HttpStatus.OK, petController.updatePet(petId, new PetForm()).getStatusCode());
    }

    @Test
    void testUploadImage() throws IOException {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.uploadImage(petId, multipartFile).getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        
        // --- PERBAIKAN: Stubbing isEmpty() DIHAPUS karena tidak dipanggil di Controller ---

        // Success
        when(petService.updatePetImage(petId, multipartFile)).thenReturn(mockPet);
        assertEquals(HttpStatus.OK, petController.uploadImage(petId, multipartFile).getStatusCode());

        // Not Found (Service returns null)
        when(petService.updatePetImage(petId, multipartFile)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, petController.uploadImage(petId, multipartFile).getStatusCode());

        // Exception
        when(petService.updatePetImage(petId, multipartFile)).thenThrow(new IOException());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, petController.uploadImage(petId, multipartFile).getStatusCode());
    }

    @Test
    void testDeletePet() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.deletePet(petId, "081").getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.deletePet(petId, "081")).thenReturn(true);
        assertEquals(HttpStatus.OK, petController.deletePet(petId, "081").getStatusCode());

        when(petService.deletePet(petId, "081")).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, petController.deletePet(petId, "081").getStatusCode());
    }
    
    @Test
    void testUpdateStatus() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, petController.updateStatus(petId, true).getStatusCode());

        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, petController.updateStatus(petId, true).getStatusCode());

        when(petService.getPetById(petId)).thenReturn(mockPet);
        when(petService.createPet(eq(userId), any(Pet.class))).thenReturn(mockPet);
        assertEquals(HttpStatus.OK, petController.updateStatus(petId, true).getStatusCode());
    }
}