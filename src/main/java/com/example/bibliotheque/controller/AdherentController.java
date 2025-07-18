package com.example.bibliotheque.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bibliotheque.model.Adherent;
import com.example.bibliotheque.model.DemandeEmprunt;
import com.example.bibliotheque.model.Livre;
import com.example.bibliotheque.service.AdherentService;
import com.example.bibliotheque.service.DemandeEmpruntService;
import com.example.bibliotheque.service.EmpruntService;
import com.example.bibliotheque.service.LivreService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdherentController {

    private final AdherentService service;
    private final LivreService livreService;
    private final DemandeEmpruntService demandeService;
    private final EmpruntService empruntService;

    @GetMapping("/login-adherent")
    public String showLogin() {
        return "adherent/login";
    }

    @PostMapping("/login-adherent")
    public String login(@RequestParam String email,
                        @RequestParam String motDePasse,
                        HttpSession session,
                        Model model) {
        Adherent adherent = service.authentifier(email, motDePasse);
        if (adherent != null) {
            session.setAttribute("adherent", adherent);
            return "redirect:/dashboard-adherent";
        } else {
            model.addAttribute("error", "Email ou mot de passe invalide.");
            return "adherent/login";
        }
    }

    @GetMapping("/register-adherent")
    public String showRegister(Model model) {
        model.addAttribute("adherent", new Adherent());
        return "adherent/register";
    }

    @PostMapping("/register-adherent")
    public String register(@ModelAttribute Adherent adherent) {
        service.inscrire(adherent);
        return "redirect:/login-adherent";
    }

    @GetMapping("/dashboard-adherent")
    public String dashboard(HttpSession session, Model model) {
        Adherent adherent = (Adherent) session.getAttribute("adherent");
        if (adherent == null) return "redirect:/login-adherent";

        model.addAttribute("emprunts", empruntService.getEmpruntsEnCours(adherent));
        return "adherent/dashboard-adherent";
    }

    @GetMapping("/adherent/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/catalogue")
    public String afficherLivres(Model model) {
        model.addAttribute("livres", livreService.getAllLivres());
        return "adherent/catalogue";
    }

    @PostMapping("/demande-action")
    public String creerDemande(@RequestParam Integer livreId,
                            @RequestParam String typeAction,
                            @RequestParam(required = false)
                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateEmprunt,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Adherent adherent = (Adherent) session.getAttribute("adherent");
        if (adherent == null) {
            return "redirect:/login-adherent";
        }

        if (adherent.getDateDeblocage() != null && adherent.getDateDeblocage().isAfter(LocalDate.now())) {
            redirectAttributes.addAttribute("error", "Vous êtes bloqué jusqu'au " + adherent.getDateDeblocage() + ". Vous ne pouvez pas faire de demande.");
            return "redirect:/catalogue";
        }

        try {
            Livre livre = livreService.getLivreById(livreId);
            DemandeEmprunt.TypeAction action = DemandeEmprunt.TypeAction.valueOf(typeAction);
            demandeService.creerDemande(adherent, livre, action, dateEmprunt); // 👉 nouvelle méthode
            redirectAttributes.addAttribute("success", "Votre demande a été envoyée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }

        return "redirect:/catalogue";
    }


    @GetMapping("/mes-demandes")
    public String mesDemandes(HttpSession session, Model model) {
        Adherent adherent = (Adherent) session.getAttribute("adherent");
        if (adherent == null) {
            return "redirect:/login-adherent";
        }
        
        List<DemandeEmprunt> demandes = demandeService.getDemandesParAdherent(adherent.getId());
        model.addAttribute("demandes", demandes);
        return "adherent/mes-demandes";
    }

    // Méthode modifiée pour accepter une date de retour personnalisée
    @PostMapping("/adherent/rendre-livre/{id}")
    public String rendreLivre(@PathVariable Integer id, 
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateRetour,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Adherent adherent = (Adherent) session.getAttribute("adherent");
        if (adherent == null) return "redirect:/login-adherent";

        try {
            empruntService.rendreLivre(id, adherent, dateRetour);
            redirectAttributes.addFlashAttribute("success", "Livre rendu avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard-adherent";
    }


    @PostMapping("/adherent/prolonger-emprunt/{idEmprunt}")
    public String prolongerEmprunt(@PathVariable Integer idEmprunt,
                                @RequestParam("nouvelleDateRetour") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nouvelleDateRetour,
                                RedirectAttributes redirectAttributes) {
        try {
            empruntService.prolongerDateRetour(idEmprunt, nouvelleDateRetour);
            redirectAttributes.addFlashAttribute("success", "Prolongation enregistrée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la prolongation : " + e.getMessage());
        }
        return "redirect:/dashboard-adherent";
    }

}