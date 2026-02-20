package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.model.paris.BilanParis;
import fr.ses10doigts.coursesCrawler.model.paris.GlobalBilanParis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GlobalBilanAsyncService {
    @Autowired
    private BilanAsyncService bilanAsyncService;

    @Async
    public CompletableFuture<GlobalBilanParis> computeGlobalBilan() {
        LocalDate today = LocalDate.now();

        int annee = today.getYear();
        int mois = today.getMonthValue();
        int semaine = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); // semaine ISO
        int jour = today.getDayOfMonth();

        log.info("Ask for bilan Y: {}, M: {}, W: {}",annee, mois, semaine);

        CompletableFuture<BilanParis> anneeFuture =
                bilanAsyncService.computeBilanAsync(annee, null, null, null);

        CompletableFuture<BilanParis> moisFuture =
                bilanAsyncService.computeBilanAsync(annee, mois, null, null);

        CompletableFuture<BilanParis> semaineFuture =
                bilanAsyncService.computeBilanAsync(annee, null, semaine, null);

        CompletableFuture<BilanParis> jourFuture =
                bilanAsyncService.computeBilanAsync(annee, mois, null, jour);

        log.debug("Queries launched, waiting for ...");

        return CompletableFuture
                .allOf(anneeFuture, moisFuture, semaineFuture, jourFuture)
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        log.error("Erreur lors du calcul du bilan global", ex);
                    } else {
                        log.debug("All futures completed successfully");
                    }
                })
                .thenApply(v -> {
                    log.info("All queries received, aggregating... and returns");
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