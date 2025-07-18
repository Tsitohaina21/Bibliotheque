package com.example.bibliotheque.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bibliotheque.model.Adherent;
import com.example.bibliotheque.model.Emprunt;
import com.example.bibliotheque.model.Livre;
import com.example.bibliotheque.repository.AdherentRepository;
import com.example.bibliotheque.repository.EmpruntRepository;
import com.example.bibliotheque.repository.JourFerieRepository;
import com.example.bibliotheque.repository.LivreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpruntService {
    private final EmpruntRepository empruntRepository;
    private final AdherentRepository adherentRepository;
    private final LivreRepository livreRepository;

    private final JourFerieRepository jourFerieRepository;

    public List<Emprunt> getEmpruntsEnCours(Adherent adherent) {
        return empruntRepository.findByAdherent_IdAndRenduFalse(adherent.getId());
    }

    // Méthode modifiée pour accepter une date de retour personnalisée
    public void validerNouvelEmprunt(Adherent adherent, Emprunt emprunt) {
        LocalDate today = LocalDate.now();

        if (jourFerieRepository.existsByDateFerie(today)) {
            throw new RuntimeException("Impossible d'emprunter un livre pendant un jour férié.");
        }
        emprunt.setDateEmprunt(today);
        emprunt.setDateRetourPrevue(today.plusDays(15));
        emprunt.setRendu(false);

        empruntRepository.save(emprunt);
    }

    public void rendreLivre(Integer empruntId, Adherent adherent, LocalDate dateRetour) {
        if (jourFerieRepository.existsByDateFerie(dateRetour)) {
            throw new RuntimeException("Impossible de rendre un livre pendant un jour férié.");
        }

        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new RuntimeException("Emprunt introuvable"));

        if (!emprunt.getAdherent().getId().equals(adherent.getId())) {
            throw new RuntimeException("Cet emprunt ne vous appartient pas.");
        }

        if (dateRetour.isBefore(emprunt.getDateEmprunt())) {
            throw new RuntimeException("Date de retour invalide.");
        }

        emprunt.setDateRetourEffective(dateRetour);
        emprunt.setRendu(true);

        if (dateRetour.isAfter(emprunt.getDateRetourPrevue())) {
            adherent.setStatut(Adherent.Statut.bloque);
            adherent.setDateDeblocage(LocalDate.now().plusDays(10));
            adherentRepository.save(adherent);
        }

        Livre livre = emprunt.getLivre();
        livre.setNbExemplaires(livre.getNbExemplaires() + 1);
        livre.setStatut(Livre.Statut.DISPONIBLE); // facultatif si tu veux changer automatiquement
        livreRepository.save(livre);
        
        empruntRepository.save(emprunt);
    }

    // Méthode de compatibilité qui utilise la date actuelle
    public void rendreLivre(Integer empruntId, Adherent adherent) {
        rendreLivre(empruntId, adherent, LocalDate.now());
    }

    public void prolongerDateRetour(Integer idEmprunt, LocalDate nouvelleDateRetour) {
        Emprunt emprunt = empruntRepository.findById(idEmprunt)
            .orElseThrow(() -> new RuntimeException("Emprunt non trouvé"));

        if (nouvelleDateRetour.isAfter(emprunt.getDateRetourPrevue())) {
            emprunt.setDateRetourPrevue(nouvelleDateRetour);
            emprunt.setProlonge(true);  // on marque l'emprunt comme prolongé
            empruntRepository.save(emprunt);
        } else {
            throw new RuntimeException("La nouvelle date doit être après la date actuelle de retour prévue.");
        }
    }

}