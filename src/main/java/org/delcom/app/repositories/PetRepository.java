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

    // PERBAIKAN QUERY:
    // 1. Menggunakan CONCAT(:prefix, '%') agar parameter terbaca dengan benar.
    // 2. Menggunakan nativeQuery = true agar fungsi SQL PostgreSQL berjalan.
    @Query(value = "SELECT pet_code FROM pets WHERE pet_code LIKE CONCAT(:prefix, '%') ORDER BY LENGTH(pet_code) DESC, pet_code DESC LIMIT 1", nativeQuery = true)
    String findLatestCode(@Param("prefix") String prefix);
}