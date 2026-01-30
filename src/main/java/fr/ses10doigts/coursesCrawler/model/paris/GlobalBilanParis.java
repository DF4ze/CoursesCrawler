package fr.ses10doigts.coursesCrawler.model.paris;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
public class GlobalBilanParis {


    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalDate date;

    private BilanParis BilanAnnee;
    private BilanParis BilanMois;
    private BilanParis BilanSemaine;
    private BilanParis BilanJour;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Bilan des Paris ");
        if( date != null ){
            sb.append(date.format(formatter));
        }
        sb.append("\n\n");

        sb.append(formatBilan("Année", BilanAnnee));
        sb.append(formatBilan("Mois", BilanMois));
        sb.append(formatBilan("Semaine", BilanSemaine));
        sb.append(formatBilan("Jour", BilanJour));

        return sb.toString();
    }

    private String formatBilan(String titre, BilanParis bilan) {
        if (bilan == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("🔹 ").append(titre).append("\n");
        sb.append("Courses jouées : ").append(bilan.getNbCourses()).append("\n");
        sb.append("Gains : ").append(bilan.getNbWin())
                .append(" (").append(fmt(bilan.getAmountWin())).append("e)\n");
        sb.append("Pertes : ").append(bilan.getNbLoose())
                .append(" (").append(fmt(bilan.getAmountLoose())).append("e)\n");

        // Emoji selon bénéfice positif ou négatif
        BigDecimal benefits = bilan.getBenefits();
        String emoji = benefits.signum() >= 0 ? "🎉" : "😢";
        sb.append("⚖️ Bénéfices : ").append(fmt(benefits)).append(" ").append(emoji).append("\n\n");

        return sb.toString();
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "0.00" :
                v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
