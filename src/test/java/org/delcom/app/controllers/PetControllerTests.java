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
        mockPet.setPetType("Anjing");
        mockPet.setPetCategory("Besar");
        mockPet.setOwnerPhone("08123456789");

        ReflectionTestUtils.setField(petController, "authContext", authContext);
    }

    // --- 1. Get All Pets ---
    @Test
    void testGetAllPets_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<List<Pet>>> response = petController.getAllPets();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetAllPets_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getAllPetsByUser(userId)).thenReturn(new ArrayList<>());

        ResponseEntity<ApiResponse<List<Pet>>> response = petController.getAllPets();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    // --- 2. Get Detail Pet ---
    @Test
    void testGetPetDetail_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<Pet>> response = petController.getPetDetail(petId);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetPetDetail_NotFound() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(null);

        ResponseEntity<ApiResponse<Pet>> response = petController.getPetDetail(petId);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetPetDetail_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(mockPet);

        ResponseEntity<ApiResponse<Pet>> response = petController.getPetDetail(petId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPet, response.getBody().getData());
    }

    // --- 3. Create Pet ---
    @Test
    void testCreatePet_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<Pet>> response = petController.createPet(new PetForm());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testCreatePet_Success_Standard() {
        PetForm form = new PetForm();
        form.setPetType("Anjing");
        form.setPetCategory("Kecil");
        form.setQuantity(1);
        form.setDescription("Desc");
        form.setOwnerName("Owner");
        form.setOwnerPhone("081");

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.generatePetCode(any(Pet.class), eq(mockUser))).thenReturn("CODE-123");
        when(petService.createPet(eq(userId), any(Pet.class))).thenReturn(mockPet);

        ResponseEntity<ApiResponse<Pet>> response = petController.createPet(form);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petService).createPet(eq(userId), any(Pet.class));
    }

    @Test
    void testCreatePet_Success_OtherType() {
        PetForm form = new PetForm();
        form.setPetType("Lainnya");
        form.setOtherType("Iguana"); // Valid other type
        form.setQuantity(1);

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        // Intercept argument to check logic
        when(petService.createPet(eq(userId), any(Pet.class))).thenAnswer(i -> i.getArguments()[1]);

        ResponseEntity<ApiResponse<Pet>> response = petController.createPet(form);
        
        Pet created = response.getBody().getData();
        assertEquals("Iguana", created.getPetType()); // Logic line 77 hit
        assertNull(created.getPetCategory());
    }

    // --- NEW TEST FOR COVERAGE: Create Pet "Lainnya" but Null/Blank OtherType ---
    @Test
    void testCreatePet_Branch_Lainnya_NullOrBlank() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.createPet(eq(userId), any(Pet.class))).thenAnswer(i -> i.getArguments()[1]);

        // Case 1: Lainnya + Null
        PetForm formNull = new PetForm();
        formNull.setPetType("Lainnya");
        formNull.setOtherType(null); // NULL
        
        ResponseEntity<ApiResponse<Pet>> res1 = petController.createPet(formNull);
        // Expect: Type tetap "Lainnya" karena logic penggantian di-skip, Category jadi null karena bukan Anjing/Kucing
        assertEquals("Lainnya", res1.getBody().getData().getPetType()); 
        assertNull(res1.getBody().getData().getPetCategory());

        // Case 2: Lainnya + Blank
        PetForm formBlank = new PetForm();
        formBlank.setPetType("Lainnya");
        formBlank.setOtherType(""); // BLANK

        ResponseEntity<ApiResponse<Pet>> res2 = petController.createPet(formBlank);
        assertEquals("Lainnya", res2.getBody().getData().getPetType());
    }

    // --- NEW TEST FOR COVERAGE: Create Pet - Anjing/Kucing Keeps Category ---
    @Test
    void testCreatePet_Branch_AnjingOrKucing_KeepsCategory() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.createPet(eq(userId), any(Pet.class))).thenAnswer(i -> i.getArguments()[1]);

        // Case Anjing (Line 82 false)
        PetForm formDog = new PetForm();
        formDog.setPetType("Anjing");
        formDog.setPetCategory("Besar");
        
        ResponseEntity<ApiResponse<Pet>> resDog = petController.createPet(formDog);
        assertEquals("Besar", resDog.getBody().getData().getPetCategory()); // Not reset

        // Case Kucing (Line 82 false part 2)
        PetForm formCat = new PetForm();
        formCat.setPetType("Kucing");
        formCat.setPetCategory("VIP");

        ResponseEntity<ApiResponse<Pet>> resCat = petController.createPet(formCat);
        assertEquals("VIP", resCat.getBody().getData().getPetCategory()); // Not reset
    }

    @Test
    void testCreatePet_Success_ResetCategory() {
        PetForm form = new PetForm();
        form.setPetType("Kelinci"); // Bukan Anjing/Kucing
        form.setPetCategory("HarusNull"); 
        form.setQuantity(1);

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.createPet(eq(userId), any(Pet.class))).thenAnswer(i -> i.getArguments()[1]);

        ResponseEntity<ApiResponse<Pet>> response = petController.createPet(form);

        Pet created = response.getBody().getData();
        assertNull(created.getPetCategory()); // Line 83 hit
    }

    // --- 4. Update Pet ---
    @Test
    void testUpdatePet_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<Pet>> response = petController.updatePet(petId, new PetForm());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdatePet_NotFound() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getPetById(petId)).thenReturn(null);

        ResponseEntity<ApiResponse<Pet>> response = petController.updatePet(petId, new PetForm());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdatePet_Success() {
        PetForm form = new PetForm();
        form.setPetType("Kucing");
        form.setPetCategory("VIP");

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getPetById(petId)).thenReturn(mockPet);
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenReturn(mockPet);

        ResponseEntity<ApiResponse<Pet>> response = petController.updatePet(petId, form);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdatePet_LogicBranches() {
        PetForm form = new PetForm();
        form.setPetType("Lainnya");
        form.setOtherType("Ular");
        form.setPetCategory("Small"); 

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getPetById(petId)).thenReturn(mockPet);
        
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenAnswer(inv -> {
            Pet p = inv.getArgument(1);
            assertEquals("Ular", p.getPetType());
            assertNull(p.getPetCategory());
            return p;
        });

        petController.updatePet(petId, form);
    }

    // --- NEW TEST FOR COVERAGE: Update Pet - Lainnya Null/Blank ---
    @Test
    void testUpdatePet_Branch_Lainnya_NullOrBlank() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        // Setup existing pet
        Pet existing = new Pet();
        existing.setId(petId);
        
        when(petService.getPetById(petId)).thenReturn(existing);
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenAnswer(i -> i.getArguments()[1]);

        // Case Null
        PetForm formNull = new PetForm();
        formNull.setPetType("Lainnya");
        formNull.setOtherType(null);
        
        ResponseEntity<ApiResponse<Pet>> res = petController.updatePet(petId, formNull);
        assertEquals("Lainnya", res.getBody().getData().getPetType());

        // Case Blank
        PetForm formBlank = new PetForm();
        formBlank.setPetType("Lainnya");
        formBlank.setOtherType("");
        
        petController.updatePet(petId, formBlank);
    }

    // --- NEW TEST FOR COVERAGE: Update Pet - Anjing/Kucing Keeps Category ---
    @Test
    void testUpdatePet_Branch_AnjingOrKucing_KeepsCategory() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        Pet existing = new Pet();
        existing.setId(petId);
        when(petService.getPetById(petId)).thenReturn(existing);
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenAnswer(i -> i.getArguments()[1]);

        // Case Anjing
        PetForm formDog = new PetForm();
        formDog.setPetType("Anjing");
        formDog.setPetCategory("Small");
        
        ResponseEntity<ApiResponse<Pet>> res = petController.updatePet(petId, formDog);
        assertEquals("Small", res.getBody().getData().getPetCategory());

        // Case Kucing
        PetForm formCat = new PetForm();
        formCat.setPetType("Kucing");
        formCat.setPetCategory("Big");
        
        ResponseEntity<ApiResponse<Pet>> resCat = petController.updatePet(petId, formCat);
        assertEquals("Big", resCat.getBody().getData().getPetCategory());
    }
    
    @Test
    void testUpdatePet_ResetCategoryOnly() {
        PetForm form = new PetForm();
        form.setPetType("Hamster");
        form.setPetCategory("Small");

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(petService.getPetById(petId)).thenReturn(mockPet);
        
        when(petService.updatePet(eq(petId), any(Pet.class), eq(mockUser))).thenAnswer(inv -> {
            Pet p = inv.getArgument(1);
            assertEquals("Hamster", p.getPetType());
            assertNull(p.getPetCategory());
            return p;
        });

        petController.updatePet(petId, form);
    }

    // --- 5. Upload Image ---
    @Test
    void testUploadImage_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<String>> response = petController.uploadImage(petId, multipartFile);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUploadImage_Success() throws IOException {
        when(authContext.isAuthenticated()).thenReturn(true);
        mockPet.setImagePath("img.jpg");
        when(petService.updatePetImage(petId, multipartFile)).thenReturn(mockPet);

        ResponseEntity<ApiResponse<String>> response = petController.uploadImage(petId, multipartFile);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("img.jpg", response.getBody().getData());
    }

    @Test
    void testUploadImage_NotFound() throws IOException {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.updatePetImage(petId, multipartFile)).thenReturn(null);

        ResponseEntity<ApiResponse<String>> response = petController.uploadImage(petId, multipartFile);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUploadImage_Exception() throws IOException {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.updatePetImage(petId, multipartFile)).thenThrow(new IOException("Fail"));

        ResponseEntity<ApiResponse<String>> response = petController.uploadImage(petId, multipartFile);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // --- 6. Delete Pet ---
    @Test
    void testDeletePet_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<Void>> response = petController.deletePet(petId, "081");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeletePet_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.deletePet(petId, "081")).thenReturn(true);

        ResponseEntity<ApiResponse<Void>> response = petController.deletePet(petId, "081");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeletePet_Fail() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.deletePet(petId, "081")).thenReturn(false);

        ResponseEntity<ApiResponse<Void>> response = petController.deletePet(petId, "081");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- 7. Update Status ---
    @Test
    void testUpdateStatus_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        ResponseEntity<ApiResponse<Pet>> response = petController.updateStatus(petId, true);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateStatus_NotFound() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(null);

        ResponseEntity<ApiResponse<Pet>> response = petController.updateStatus(petId, true);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdateStatus_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(petService.getPetById(petId)).thenReturn(mockPet);
        when(petService.createPet(eq(userId), any(Pet.class))).thenReturn(mockPet);

        // Case Taken = true
        ResponseEntity<ApiResponse<Pet>> response = petController.updateStatus(petId, true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Sudah Diambil"));

        // Case Taken = false
        response = petController.updateStatus(petId, false);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Belum Diambil"));
    }
}