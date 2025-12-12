package org.delcom.app.services;

import org.delcom.app.entities.Pet;
import org.delcom.app.entities.User;
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

    // --- UPDATE PET (LOGIKA BARU: RE-GENERATE CODE) ---
    @Transactional
    public Pet updatePet(UUID petId, Pet updatedData, User user) {
        Pet existing = getPetById(petId);
        if (existing != null) {
            
            // Simpan data lama untuk perbandingan
            String oldType = existing.getPetType();
            String oldCategory = existing.getPetCategory();

            // Update data field
            existing.setPetType(updatedData.getPetType());
            existing.setPetCategory(updatedData.getPetCategory());
            existing.setQuantity(updatedData.getQuantity());
            existing.setDescription(updatedData.getDescription());
            existing.setOwnerName(updatedData.getOwnerName());
            existing.setOwnerPhone(updatedData.getOwnerPhone());

            // Handle logic input manual "Lainnya"
            if ("Lainnya".equals(updatedData.getPetType())) {
                 // Jika updatedData punya otherType (biasanya dihandle di controller, 
                 // tapi di sini kita pastikan data yang masuk sudah bersih).
                 // Asumsi: Controller sudah set petType ke nilai manual.
            }

            // --- DETEKSI PERUBAHAN JENIS/KATEGORI ---
            boolean typeChanged = !oldType.equals(existing.getPetType());
            boolean categoryChanged = false;

            if (oldCategory == null && existing.getPetCategory() != null) categoryChanged = true;
            else if (oldCategory != null && existing.getPetCategory() == null) categoryChanged = true;
            else if (oldCategory != null && !oldCategory.equals(existing.getPetCategory())) categoryChanged = true;

            // JIKA BERUBAH -> GENERATE KODE BARU
            if (typeChanged || categoryChanged) {
                String newCode = generatePetCode(existing, user);
                existing.setPetCode(newCode);
            }

            return petRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public Pet updatePetImage(UUID petId, MultipartFile file) throws IOException {
        Pet existing = getPetById(petId);
        if (existing != null && !file.isEmpty()) {
            if (existing.getImagePath() != null) {
                fileStorageService.deleteFile(existing.getImagePath());
            }
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

    public String generatePetCode(Pet pet, User user) {
        String prefix = getPrefix(pet);
        String lastCode = petRepository.findLatestCodeByUser(prefix + "-", user.getId());
        
        int nextNumber = 1;
        if (lastCode != null) {
            String numberPart = lastCode.substring(lastCode.lastIndexOf("-") + 1);
            try {
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                nextNumber = 1;
            }
        }
        return prefix + "-" + String.format("%03d", nextNumber);
    }

    private String getPrefix(Pet pet) {
        String type = pet.getPetType() != null ? pet.getPetType() : "";
        String cat = pet.getPetCategory() != null ? pet.getPetCategory() : "";

        if (type.equals("Anjing")) {
            if ("Kecil".equals(cat)) return "ANJ-K";
            if ("Sedang".equals(cat)) return "ANJ-S";
            if ("Besar".equals(cat)) return "ANJ-B";
            return "ANJ-X";
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
        
        return "RPT"; 
    }

    public Map<String, Long> getPetTypeStats(UUID userId) {
        List<Pet> pets = getAllPetsByUser(userId);
        return pets.stream()
                .collect(Collectors.groupingBy(pet -> pet.getPetType().toUpperCase(), Collectors.counting()));
    }
}