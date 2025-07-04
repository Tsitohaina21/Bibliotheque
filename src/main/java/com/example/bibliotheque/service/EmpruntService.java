package com.example.bibliotheque.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bibliotheque.model.Adherent;
import com.example.bibliotheque.model.Emprunt;
import com.example.bibliotheque.repository.AdherentRepository;
import com.example.bibliotheque.repository.EmpruntRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpruntService {
    private final EmpruntRepository empruntRepository;
    private final AdherentRepository adherentRepository;

    public List<Emprunt> getEmpruntsEnCours(Adherent adherent) {
        return empruntRepository.findByAdherent_IdAndRenduFalse(adherent.getId());
    }

    // Méthode modifiée pour accepter une date de retour personnalisée
    public void rendreLivre(Integer empruntId, Adherent adherent, LocalDate dateRetour) {
        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new RuntimeException("Emprunt introuvable"));

        if (!emprunt.getAdherent().getId().equals(adherent.getId())) {
            throw new RuntimeException("Cet emprunt ne vous appartient pas.");
        }

        // Validation de la date de retour
        if (dateRetour.isBefore(emprunt.getDateEmprunt())) {
            throw new RuntimeException("La date de retour ne peut pas être antérieure à la date d'emprunt.");
        }

        emprunt.setRendu(true);
        emprunt.setDateRetourEffective(dateRetour);

        // Vérification du retard basée sur la date de retour effective saisie
        if (dateRetour.isAfter(emprunt.getDateRetourPrevue())) {
            adherent.setStatut(Adherent.Statut.bloque);
            adherent.setDateDeblocage(LocalDate.now().plusDays(10));
            adherentRepository.save(adherent);
        }

        empruntRepository.save(emprunt);
    }

    // Méthode de compatibilité qui utilise la date actuelle
    public void rendreLivre(Integer empruntId, Adherent adherent) {
        rendreLivre(empruntId, adherent, LocalDate.now());
    }
}