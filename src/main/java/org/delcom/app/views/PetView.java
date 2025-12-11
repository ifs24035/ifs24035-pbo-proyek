package org.delcom.app.views;

import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
import org.delcom.app.services.PetService;
import org.delcom.app.utils.ConstUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/pets")
public class PetView {

    private final PetService petService;

    public PetView(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("pet", new Pet());
        return ConstUtil.TEMPLATE_PAGES_PET_ADD;
    }

    // --- PROSES TAMBAH (UPDATED) ---
    @PostMapping("/add")
    public String processAdd(@ModelAttribute Pet pet, 
                             @AuthenticationPrincipal User user,
                             @RequestParam(value = "otherType", required = false) String otherType,
                             RedirectAttributes redirectAttributes) {
        
        // 1. Logika Input Manual (Lainnya)
        if ("Lainnya".equals(pet.getPetType()) && otherType != null && !otherType.isBlank()) {
            pet.setPetType(otherType);
            pet.setPetCategory(null);
        }

        // 2. Reset Kategori untuk hewan non-Anjing/Kucing
        if (!"Anjing".equals(pet.getPetType()) && !"Kucing".equals(pet.getPetType())) {
            pet.setPetCategory(null);
        }

        // 3. GENERATE KODE UNIK (NEW)
        String uniqueCode = petService.generatePetCode(pet);
        pet.setPetCode(uniqueCode);

        // 4. Simpan
        petService.createPet(user.getId(), pet);
        
        redirectAttributes.addFlashAttribute("success", "Hewan berhasil ditambahkan dengan Kode Kandang: " + uniqueCode);
        return "redirect:/";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable UUID id, Model model) {
        Pet pet = petService.getPetById(id);
        if (pet == null) return "redirect:/";
        model.addAttribute("pet", pet);
        return ConstUtil.TEMPLATE_PAGES_PET_DETAIL;
    }

    // --- PROSES UPDATE DATA (UPDATED) ---
    @PostMapping("/{id}/update")
    public String processUpdate(@PathVariable UUID id, 
                                @ModelAttribute Pet pet,
                                @RequestParam(value = "otherType", required = false) String otherType,
                                RedirectAttributes redirectAttributes) {
        
        // Logika yang sama dengan Tambah
        if ("Lainnya".equals(pet.getPetType()) && otherType != null && !otherType.isBlank()) {
            pet.setPetType(otherType);
            pet.setPetCategory(null);
        }
        
        // Pastikan Kategori null jika bukan Anjing/Kucing
        if (!"Anjing".equals(pet.getPetType()) && !"Kucing".equals(pet.getPetType())) {
             pet.setPetCategory(null);
        }

        petService.updatePet(id, pet);
        redirectAttributes.addFlashAttribute("success", "Data hewan berhasil diperbarui!");
        return "redirect:/pets/" + id;
    }

    @PostMapping("/{id}/image")
    public String uploadImage(@PathVariable UUID id, 
                              @RequestParam("image") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            petService.updatePetImage(id, file);
            redirectAttributes.addFlashAttribute("success", "Foto hewan berhasil diubah!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengupload gambar.");
        }
        return "redirect:/pets/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePet(@PathVariable UUID id, 
                            @RequestParam("verificationPhone") String phone,
                            RedirectAttributes redirectAttributes) {
        boolean isDeleted = petService.deletePet(id, phone);
        if (isDeleted) {
            redirectAttributes.addFlashAttribute("success", "Data berhasil dihapus!");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal hapus! Nomor HP Pemilik tidak cocok.");
            return "redirect:/pets/" + id;
        }
    }

    // --- FITUR TANDAI SUDAH DIAMBIL ---
    @PostMapping("/{id}/take")
    public String markAsTaken(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Pet pet = petService.getPetById(id);
        if (pet != null) {
            pet.setTaken(true);
            petService.createPet(pet.getUserId(), pet);
            redirectAttributes.addFlashAttribute("success", "Status: Sudah Diambil.");
        }
        return "redirect:/pets/" + id;
    }

    // --- FITUR BARU: TANDAI BELUM DIAMBIL (REVERT STATUS) ---
    @PostMapping("/{id}/untake")
    public String markAsNotTaken(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Pet pet = petService.getPetById(id);
        if (pet != null) {
            pet.setTaken(false); // Kembalikan ke false
            petService.createPet(pet.getUserId(), pet);
            // Flash message agar user tahu
            redirectAttributes.addFlashAttribute("success", "Status dikembalikan ke: Belum Diambil.");
        }
        return "redirect:/pets/" + id;
    }
}