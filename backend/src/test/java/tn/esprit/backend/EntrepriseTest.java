package tn.esprit.backend;

import org.junit.jupiter.api.Test;
import tn.esprit.backend.entity.Entreprise;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntrepriseTest {

    @Test
    void builderSetsNomAndAdresse() {
        Entreprise entreprise = Entreprise.builder()
                .nom("ESPRIT")
                .adresse("Tunis")
                .build();

        assertEquals("ESPRIT", entreprise.getNom());
        assertEquals("Tunis", entreprise.getAdresse());
    }
}
