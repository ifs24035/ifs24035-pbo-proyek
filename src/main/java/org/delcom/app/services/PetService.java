package org.delcom.app.services;

import org.delcom.app.entities.Pet;
import org.delcom.app.repositories.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final FileStorageService fileStorageService;

    // Method Utama untuk Generate Kode Unik
    public String generatePetCode(Pet pet) {
        String prefix = getPrefix(pet);
        
        // Cari kode terakhir di database untuk prefix ini
        String lastCode = petRepository.findLatestCode(prefix + "-");
        
        int nextNumber = 1;
        if (lastCode != null) {
            // Contoh lastCode: ANJ-K-005
            // Ambil bagian angkanya saja (substring setelah strip terakhir)
            String numberPart = lastCode.substring(lastCode.lastIndexOf("-") + 1);
            try {
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                nextNumber = 1; // Fallback jika error parsing
            }
        }

        // Format hasil: PREFIX + "-" + 3 Digit Angka (001, 002, dst)
        return prefix + "-" + String.format("%03d", nextNumber);
    }

    // Helper: Menentukan Prefix berdasarkan Jenis & Kategori
    private String getPrefix(Pet pet) {
        String type = pet.getPetType() != null ? pet.getPetType() : "";
        String cat = pet.getPetCategory() != null ? pet.getPetCategory() : "";

        if (type.equals("Anjing")) {
            if ("Kecil".equals(cat)) return "ANJ-K";
            if ("Sedang".equals(cat)) return "ANJ-S";
            if ("Besar".equals(cat)) return "ANJ-B";
            return "ANJ-X"; // Default
        } 
        else if (type.equals("Kucing")) {
            if ("Standar".equals(cat)) return "KUC-ST";
            if ("Premium".equals(cat)) return "KUC-PR";
            if ("VIP".equals(cat)) return "KUC-VP";
            return "KUC-X";
        }
        else if (type.equals("Burung")) return "BRG";
        else if (type.equals("Kelinci")) return "KLN";
        else if (type.equals("Hamster") || type.equals("Marmut")) return "HMR";
        
        // Lainnya / Reptil
        return "RPT"; 
    }

    public PetService(PetRepository petRepository, FileStorageService fileStorageService) {
        this.petRepository = petRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Pet> getAllPetsByUser(UUID userId) {
        return petRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Pet getPetById(UUID id) {
        return petRepository.findById(id).orElse(null);
    }

    @Transactional
    public Pet createPet(UUID userId, Pet pet) {
        pet.setUserId(userId);
        return petRepository.save(pet);
    }

    @Transactional
    public Pet updatePet(UUID petId, Pet updatedData) {
        Pet existing = getPetById(petId);
        if (existing != null) {
            existing.setPetType(updatedData.getPetType());
            existing.setQuantity(updatedData.getQuantity());
            existing.setDescription(updatedData.getDescription());
            existing.setOwnerName(updatedData.getOwnerName());
            existing.setOwnerPhone(updatedData.getOwnerPhone());
            return petRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public Pet updatePetImage(UUID petId, MultipartFile file) throws IOException {
        Pet existing = getPetById(petId);
        if (existing != null && !file.isEmpty()) {
            // Hapus file lama jika ada
            if (existing.getImagePath() != null) {
                fileStorageService.deleteFile(existing.getImagePath());
            }
            // Simpan file baru
            String filename = fileStorageService.storeFile(file, petId);
            existing.setImagePath(filename);
            return petRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public boolean deletePet(UUID petId, String verificationPhone) {
        Pet existing = getPetById(petId);
        if (existing != null) {
            // Validasi: No HP harus cocok dengan pemilik
            if (existing.getOwnerPhone().equals(verificationPhone)) {
                if (existing.getImagePath() != null) {
                    fileStorageService.deleteFile(existing.getImagePath());
                }
                petRepository.delete(existing);
                return true;
            }
        }
        return false;
    }

    // Untuk Chart: Menghitung jumlah hewan berdasarkan jenisnya
    public Map<String, Long> getPetTypeStats(UUID userId) {
        List<Pet> pets = getAllPetsByUser(userId);
        return pets.stream()
                .collect(Collectors.groupingBy(pet -> pet.getPetType().toUpperCase(), Collectors.counting()));
    }
}