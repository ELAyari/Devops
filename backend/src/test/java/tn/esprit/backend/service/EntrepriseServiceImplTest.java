package tn.esprit.backend.service.impl;

import org.junit.jupiter.api.Test;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.repository.EntrepriseRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntrepriseServiceImplTest {

    private final EntrepriseRepository repository = mock(EntrepriseRepository.class);
    private final EntrepriseServiceImpl service = new EntrepriseServiceImpl(repository);

    @Test
    void addEntrepriseSavesAndReturnsEntity() {
        Entreprise entreprise = Entreprise.builder().nom("ESPRIT").adresse("Tunis").build();
        when(repository.save(entreprise)).thenReturn(entreprise);

        Entreprise result = service.addEntreprise(entreprise);

        assertSame(entreprise, result);
        verify(repository).save(entreprise);
    }

    @Test
    void getAllEntreprisesDelegatesToRepository() {
        Entreprise entreprise = Entreprise.builder().nom("ESPRIT").build();
        when(repository.findAll()).thenReturn(List.of(entreprise));

        List<Entreprise> result = service.getAllEntreprises();

        assertEquals(List.of(entreprise), result);
        verify(repository).findAll();
    }

    @Test
    void deleteEntrepriseDelegatesToRepository() {
        service.deleteEntreprise(5L);

        verify(repository).deleteById(5L);
    }
}
