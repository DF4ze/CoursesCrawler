package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.model.paris.BilanParis;
import fr.ses10doigts.coursesCrawler.model.paris.GlobalBilanParis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.concurrent.CompletableFuture;

@Service
public class GlobalBilanAsyncService {
    private static final Logger logger = LoggerFactory.getLogger(GlobalBilanAsyncService.class);

    @Autowired
    private BilanAsyncService bilanAsyncService;

    @Async
    public CompletableFuture<GlobalBilanParis> computeGlobalBilan() {
        LocalDate today = LocalDate.now();

        int annee = today.getYear();
        int mois = today.getMonthValue();
        int semaine = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); // semaine ISO
        int jour = today.getDayOfMonth();

        logger.info("Ask for bilan Y: {}, M: {}, W: {}",annee, mois, semaine);

        CompletableFuture<BilanParis> anneeFuture =
                bilanAsyncService.computeBilanAsync(annee, null, null, null);

        CompletableFuture<BilanParis> moisFuture =
                bilanAsyncService.computeBilanAsync(annee, mois, null, null);

        CompletableFuture<BilanParis> semaineFuture =
                bilanAsyncService.computeBilanAsync(annee, null, semaine, null);

        CompletableFuture<BilanParis> jourFuture =
                bilanAsyncService.computeBilanAsync(annee, mois, null, jour);

        logger.info("Queries launched, waiting for ...");

        return CompletableFuture
                .allOf(anneeFuture, moisFuture, semaineFuture, jourFuture)
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        logger.error("Erreur lors du calcul du bilan global", ex);
                    } else {
                        logger.info("All futures completed successfully");
                    }
                })
                .thenApply(v -> {
                    logger.info("All queries received, aggregating... and returns");
                    GlobalBilanParis global = new GlobalBilanParis();
                    global.setDate(LocalDate.now());
                    global.setBilanAnnee(anneeFuture.join());
                    global.setBilanMois(moisFuture.join());
                    global.setBilanSemaine(semaineFuture.join());
                    global.setBilanJour(jourFuture.join());
                    return global;
                });
    }
}