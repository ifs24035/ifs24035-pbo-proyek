package org.delcom.app.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStorageServiceTests {

    private FileStorageService fileStorageService;
    private MultipartFile mockMultipartFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        fileStorageService = new FileStorageService();
        // Override uploadDir dengan temporary directory
        fileStorageService.uploadDir = tempDir.toString();
        mockMultipartFile = mock(MultipartFile.class);
    }

    @Test
    @DisplayName("Store file berhasil menyimpan file dengan extension")
    void storeFile_berhasil_menyimpan_file_dengan_extension() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        String originalFilename = "image.jpg";
        String expectedFilename = "cover_" + todoId + ".jpg";
        byte[] fileContent = "fake image content".getBytes();

        when(mockMultipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertEquals(expectedFilename, result);

        // Verify file actually exists and content is correct
        Path expectedFile = tempDir.resolve(expectedFilename);
        assertTrue(Files.exists(expectedFile));
        assertArrayEquals(fileContent, Files.readAllBytes(expectedFile));
    }

    @Test
    @DisplayName("Store file berhasil tanpa extension ketika original filename null")
    void storeFile_berhasil_tanpa_extension_ketika_originalFilename_null() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        String expectedFilename = "cover_" + todoId.toString();
        byte[] fileContent = "fake content".getBytes();

        when(mockMultipartFile.getOriginalFilename()).thenReturn(null);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertEquals(expectedFilename, result);
        assertTrue(Files.exists(tempDir.resolve(expectedFilename)));
    }

    @Test
    @DisplayName("Store file berhasil tanpa extension ketika tidak ada dot")
    void storeFile_berhasil_tanpa_extension_ketika_tidak_ada_dot() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        String expectedFilename = "cover_" + todoId.toString();
        byte[] fileContent = "fake content".getBytes();

        when(mockMultipartFile.getOriginalFilename()).thenReturn("filename");
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertEquals(expectedFilename, result);
        assertTrue(Files.exists(tempDir.resolve(expectedFilename)));
    }

    @Test
    @DisplayName("Store file berhasil dengan complex extension")
    void storeFile_berhasil_dengan_complex_extension() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        String originalFilename = "document.final.pdf";
        String expectedFilename = "cover_" + todoId + ".pdf";
        byte[] fileContent = "fake pdf content".getBytes();

        when(mockMultipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertEquals(expectedFilename, result);
        assertTrue(Files.exists(tempDir.resolve(expectedFilename)));
    }

    @Test
    @DisplayName("Store file membuat directory ketika belum ada")
    void storeFile_membuat_directory_ketika_belum_ada() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        Path customUploadDir = tempDir.resolve("custom-upload");
        fileStorageService.uploadDir = customUploadDir.toString();

        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertTrue(Files.exists(customUploadDir));
        assertTrue(Files.isDirectory(customUploadDir));
        assertTrue(Files.exists(customUploadDir.resolve(result)));
    }

    @Test
    @DisplayName("Store file melemparkan exception ketika IOException terjadi")
    void storeFile_melemparkan_exception_ketika_ioexception_terjadi() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();

        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockMultipartFile.getInputStream()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            fileStorageService.storeFile(mockMultipartFile, todoId);
        });
    }

    @Test
    @DisplayName("Delete file berhasil menghapus file yang ada")
    void deleteFile_berhasil_menghapus_file_yang_ada() throws Exception {
        // Arrange
        String filename = "test-file.txt";
        Path testFile = tempDir.resolve(filename);
        Files.write(testFile, "content".getBytes());
        assertTrue(Files.exists(testFile));

        // Act
        boolean result = fileStorageService.deleteFile(filename);

        // Assert
        assertTrue(result);
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("Delete file return false ketika file tidak ada")
    void deleteFile_return_false_ketika_file_tidak_ada() {
        // Arrange
        String nonExistentFilename = "non-existent-file.txt";

        // Act
        boolean result = fileStorageService.deleteFile(nonExistentFilename);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Delete file return false ketika IOException terjadi")
    void deleteFile_return_false_ketika_ioexception() throws Exception {
        // Arrange
        String filename = "test-file.txt";
        Path filePath = Paths.get(fileStorageService.uploadDir).resolve(filename);

        // Mock Files class untuk melemparkan IOException
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(filePath))
                    .thenThrow(new IOException("Permission denied"));

            // Act
            boolean result = fileStorageService.deleteFile(filename);

            // Assert
            assertFalse(result);
        }
    }

    @Test
    @DisplayName("Load file return path yang benar")
    void loadFile_return_path_yang_benar() {
        // Arrange
        String filename = "test-file.txt";
        Path expectedPath = tempDir.resolve(filename);

        // Act
        Path result = fileStorageService.loadFile(filename);

        // Assert
        assertEquals(expectedPath, result);
    }

    @Test
    @DisplayName("File exists return true ketika file ada")
    void fileExists_return_true_ketika_file_ada() throws Exception {
        // Arrange
        String filename = "existing-file.txt";
        Path existingFile = tempDir.resolve(filename);
        Files.write(existingFile, "content".getBytes());

        // Act
        boolean result = fileStorageService.fileExists(filename);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("File exists return false ketika file tidak ada")
    void fileExists_return_false_ketika_file_tidak_ada() {
        // Arrange
        String nonExistentFilename = "non-existent-file.txt";

        // Act
        boolean result = fileStorageService.fileExists(nonExistentFilename);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Store file menggantikan file yang sudah ada")
    void storeFile_menggantikan_file_yang_sudah_ada() throws Exception {
        // Arrange
        UUID todoId = UUID.randomUUID();
        String originalFilename = "test.txt";
        String expectedFilename = "cover_" + todoId + ".txt";

        // Create existing file with different content
        Path existingFile = tempDir.resolve(expectedFilename);
        Files.write(existingFile, "old content".getBytes());

        byte[] newContent = "new content".getBytes();

        when(mockMultipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));

        // Act
        String result = fileStorageService.storeFile(mockMultipartFile, todoId);

        // Assert
        assertEquals(expectedFilename, result);
        assertArrayEquals(newContent, Files.readAllBytes(existingFile));
    }
}