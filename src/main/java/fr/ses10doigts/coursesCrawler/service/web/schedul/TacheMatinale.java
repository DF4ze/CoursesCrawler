package fr.ses10doigts.coursesCrawler.service.web.schedul;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({ "dev", "telegram" }) // Exécuté uniquement si le profil actif est "prod"
public class TacheMatinale {

    @Scheduled(cron = "0 0 7 * * *") // Tous les jours à 7h00 du matin
    public void executer() {
        System.out.println("Tâche automatique lancée à 7h (prod uniquement)");
        // Ton traitement ici
    }
}