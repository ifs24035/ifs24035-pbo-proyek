package org.delcom.app.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PetFormTests {

    @Test
    void testPetForm() {
        PetForm form = new PetForm();
        form.setPetType("Anjing");
        form.setOtherType("Sesuatu");
        form.setPetCategory("Besar");
        form.setQuantity(5);
        form.setDescription("Galak");
        form.setOwnerName("Joni");
        form.setOwnerPhone("08111");

        Assertions.assertEquals("Anjing", form.getPetType());
        Assertions.assertEquals("Sesuatu", form.getOtherType());
        Assertions.assertEquals("Besar", form.getPetCategory());
        Assertions.assertEquals(5, form.getQuantity());
        Assertions.assertEquals("Galak", form.getDescription());
        Assertions.assertEquals("Joni", form.getOwnerName());
        Assertions.assertEquals("08111", form.getOwnerPhone());
    }
}