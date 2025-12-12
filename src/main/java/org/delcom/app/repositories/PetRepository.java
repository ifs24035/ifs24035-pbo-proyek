package org.delcom.app.repositories;

import org.delcom.app.entities.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {
    
    // Menampilkan daftar hewan berdasarkan user, diurutkan dari yang terbaru
    List<Pet> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // UPDATE: Mencari kode terakhir HANYA milik User tertentu
    @Query(value = "SELECT pet_code FROM pets WHERE user_id = :userId AND pet_code LIKE CONCAT(:prefix, '%') ORDER BY length(pet_code) DESC, pet_code DESC LIMIT 1", nativeQuery = true)
    String findLatestCodeByUser(@Param("prefix") String prefix, @Param("userId") UUID userId);
}