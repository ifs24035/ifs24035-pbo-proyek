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

    // --- PROCESS ADD (UPDATED: Data Preserved on Error) ---
    @PostMapping("/add")
    public String processAdd(@ModelAttribute Pet pet, 
                             @AuthenticationPrincipal User user,
                             @RequestParam(value = "otherType", required = false) String otherType,
                             Model model, // Gunakan Model untuk kirim data kembali jika error
                             RedirectAttributes redirectAttributes) {
        
        // 1. VALIDASI BACKEND: Nomor HP
        String phone = pet.getOwnerPhone();
        if (phone == null || phone.length() < 9 || !phone.matches("\\d+")) {
            // Jika Gagal:
            // a. Kirim pesan error via Model (bukan RedirectAttributes)
            model.addAttribute("error", "Gagal! Nomor HP harus berupa angka dan minimal 9 digit.");
            
            // b. Reset HANYA Nomor HP (Data lain tetap ada di objek 'pet')
            pet.setOwnerPhone(""); 
            
            // c. JANGAN REDIRECT. Return langsung ke template agar data tidak hilang
            return ConstUtil.TEMPLATE_PAGES_PET_ADD; 
        }

        // 2. Logika Jenis Hewan "Lainnya"
        if ("Lainnya".equals(pet.getPetType()) && otherType != null && !otherType.isBlank()) {
            pet.setPetType(otherType);
            pet.setPetCategory(null);
        }

        // 3. Reset Kategori jika bukan Anjing/Kucing
        if (!"Anjing".equals(pet.getPetType()) && !"Kucing".equals(pet.getPetType())) {
            pet.setPetCategory(null);
        }

        // 4. Generate Kode & Simpan
        String uniqueCode = petService.generatePetCode(pet, user);
        pet.setPetCode(uniqueCode);

        petService.createPet(user.getId(), pet);
        
        redirectAttributes.addFlashAttribute("success", "Hewan berhasil ditambahkan dengan Kode: " + uniqueCode);
        return "redirect:/";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable UUID id, Model model) {
        Pet pet = petService.getPetById(id);
        if (pet == null) return "redirect:/";
        model.addAttribute("pet", pet);
        return ConstUtil.TEMPLATE_PAGES_PET_DETAIL;
    }

    // --- PROCESS UPDATE (UPDATED: Re-Generate Code & Dynamic Message) ---
    @PostMapping("/{id}/update")
    public String processUpdate(@PathVariable UUID id, 
                                @ModelAttribute Pet pet,
                                @AuthenticationPrincipal User user, // Tambahkan User
                                @RequestParam(value = "otherType", required = false) String otherType,
                                RedirectAttributes redirectAttributes) {
        
        // 1. Validasi HP
        String phone = pet.getOwnerPhone();
        if (phone == null || phone.length() < 9 || !phone.matches("\\d+")) {
            redirectAttributes.addFlashAttribute("error", "Gagal Update! Nomor HP harus berupa angka dan minimal 9 digit.");
            return "redirect:/pets/" + id;
        }

        // 2. Logic "Lainnya" (Input Manual)
        if ("Lainnya".equals(pet.getPetType()) && otherType != null && !otherType.isBlank()) {
            pet.setPetType(otherType);
            pet.setPetCategory(null);
        }
        
        // 3. Reset Kategori jika bukan Anjing/Kucing
        if (!"Anjing".equals(pet.getPetType()) && !"Kucing".equals(pet.getPetType())) {
             pet.setPetCategory(null);
        }

        // 4. Update Data (Pass User ke Service untuk Generate Code Baru jika perlu)
        Pet updatedPet = petService.updatePet(id, pet, user);
        
        if (updatedPet != null) {
            String msg = String.format("Berhasil edit hewan menjadi %s dengan kode baru %s", 
                                       updatedPet.getPetType(), updatedPet.getPetCode());
            redirectAttributes.addFlashAttribute("success", msg);
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal mengupdate data hewan.");
        }

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
        if (phone == null || phone.length() < 9) {
             redirectAttributes.addFlashAttribute("error", "Gagal hapus! Masukkan Nomor HP yang valid (Min 9 digit).");
             return "redirect:/pets/" + id;
        }

        boolean isDeleted = petService.deletePet(id, phone);
        if (isDeleted) {
            redirectAttributes.addFlashAttribute("success", "Data berhasil dihapus!");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal hapus! Nomor HP Pemilik tidak cocok.");
            return "redirect:/pets/" + id;
        }
    }

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

    @PostMapping("/{id}/untake")
    public String markAsNotTaken(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Pet pet = petService.getPetById(id);
        if (pet != null) {
            pet.setTaken(false);
            petService.createPet(pet.getUserId(), pet);
            redirectAttributes.addFlashAttribute("success", "Status dikembalikan ke: Belum Diambil.");
        }
        return "redirect:/pets/" + id;
    }
}