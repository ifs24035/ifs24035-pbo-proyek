package org.delcom.app.controllers;

import org.delcom.app.configs.ApiResponse;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.dto.PetForm;
import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
import org.delcom.app.services.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    @Autowired
    protected AuthContext authContext;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    // 1. Get All Pets (By User)
    @GetMapping
    public ResponseEntity<ApiResponse<List<Pet>>> getAllPets() {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }
        User user = authContext.getAuthUser();
        
        List<Pet> pets = petService.getAllPetsByUser(user.getId());
        return ResponseEntity.ok(new ApiResponse<>("success", "Berhasil mengambil data hewan", pets));
    }

    // 2. Get Detail Pet
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Pet>> getPetDetail(@PathVariable UUID id) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }
        
        Pet pet = petService.getPetById(id);
        if (pet == null) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Hewan tidak ditemukan", null));
        }
        
        return ResponseEntity.ok(new ApiResponse<>("success", "Berhasil mengambil detail hewan", pet));
    }

    // 3. Create Pet
    @PostMapping
    public ResponseEntity<ApiResponse<Pet>> createPet(@RequestBody PetForm form) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }
        User user = authContext.getAuthUser();

        // Mapping DTO ke Entity
        Pet pet = new Pet();
        pet.setPetType(form.getPetType());
        pet.setQuantity(form.getQuantity());
        pet.setDescription(form.getDescription());
        pet.setOwnerName(form.getOwnerName());
        pet.setOwnerPhone(form.getOwnerPhone());
        pet.setPetCategory(form.getPetCategory());

        // Logic "Lainnya" (Jika input manual)
        if ("Lainnya".equals(form.getPetType()) && form.getOtherType() != null && !form.getOtherType().isBlank()) {
            pet.setPetType(form.getOtherType());
            pet.setPetCategory(null);
        }

        // Logic Reset Kategori untuk hewan non-anjing/kucing
        if (!"Anjing".equals(pet.getPetType()) && !"Kucing".equals(pet.getPetType())) {
            pet.setPetCategory(null);
        }

        // Generate Kode Unik (Pass user)
        String uniqueCode = petService.generatePetCode(pet, user);
        pet.setPetCode(uniqueCode);

        Pet createdPet = petService.createPet(user.getId(), pet);
        return ResponseEntity.ok(new ApiResponse<>("success", "Hewan berhasil ditambahkan", createdPet));
    }

    // 4. Update Pet (PERBAIKAN UTAMA DISINI)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Pet>> updatePet(@PathVariable UUID id, @RequestBody PetForm form) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }
        // AMBIL USER
        User user = authContext.getAuthUser();

        Pet existingPet = petService.getPetById(id);
        if (existingPet == null) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Hewan tidak ditemukan", null));
        }

        // Update Fields
        existingPet.setPetType(form.getPetType());
        existingPet.setQuantity(form.getQuantity());
        existingPet.setDescription(form.getDescription());
        existingPet.setOwnerName(form.getOwnerName());
        existingPet.setOwnerPhone(form.getOwnerPhone());
        existingPet.setPetCategory(form.getPetCategory());

        // Logic "Lainnya"
        if ("Lainnya".equals(form.getPetType()) && form.getOtherType() != null && !form.getOtherType().isBlank()) {
            existingPet.setPetType(form.getOtherType());
            existingPet.setPetCategory(null);
        }
        
        // Logic Reset Kategori
        if (!"Anjing".equals(existingPet.getPetType()) && !"Kucing".equals(existingPet.getPetType())) {
            existingPet.setPetCategory(null);
        }

        // PERBAIKAN: Masukkan 'user' sebagai parameter ke-3
        Pet updatedPet = petService.updatePet(id, existingPet, user);
        
        return ResponseEntity.ok(new ApiResponse<>("success", "Data hewan berhasil diperbarui", updatedPet));
    }

    // 5. Upload Image
    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<String>> uploadImage(@PathVariable UUID id, @RequestParam("image") MultipartFile file) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }

        try {
            Pet updatedPet = petService.updatePetImage(id, file);
            if (updatedPet == null) {
                return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Hewan tidak ditemukan", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("success", "Gambar berhasil diupload", updatedPet.getImagePath()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ApiResponse<>("error", "Gagal upload gambar", null));
        }
    }

    // 6. Delete Pet (Dengan Verifikasi No HP di Query Param)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePet(@PathVariable UUID id, @RequestParam("verificationPhone") String phone) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }

        boolean isDeleted = petService.deletePet(id, phone);
        if (isDeleted) {
            return ResponseEntity.ok(new ApiResponse<>("success", "Hewan berhasil dihapus", null));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Gagal hapus! Nomor HP Pemilik tidak cocok", null));
        }
    }

    // 7. Update Status (Take/Untake)
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Pet>> updateStatus(@PathVariable UUID id, @RequestParam("isTaken") boolean isTaken) {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("fail", "Unauthorized", null));
        }

        Pet pet = petService.getPetById(id);
        if (pet == null) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Hewan tidak ditemukan", null));
        }

        pet.setTaken(isTaken);
        Pet updatedPet = petService.createPet(pet.getUserId(), pet); // Save

        String msg = isTaken ? "Status: Sudah Diambil" : "Status: Belum Diambil";
        return ResponseEntity.ok(new ApiResponse<>("success", msg, updatedPet));
    }
}