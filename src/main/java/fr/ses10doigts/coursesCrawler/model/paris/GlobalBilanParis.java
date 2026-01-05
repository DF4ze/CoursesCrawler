package fr.ses10doigts.coursesCrawler.model.paris;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GlobalBilanParis {
    private BilanParis BilanAnnee;
    private BilanParis BilanMois;
    private BilanParis BilanSemaine;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *Bilan des Paris*\n\n");

        sb.append(formatBilan("Année 🌟", BilanAnnee));
        sb.append(formatBilan("Mois 📅", BilanMois));
        sb.append(formatBilan("Semaine 🗓️", BilanSemaine));

        return sb.toString();
    }

    private String formatBilan(String titre, BilanParis bilan) {
        if (bilan == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("🔹 *").append(titre).append("*\n");
        sb.append("🏇 Courses jouées : ").append(bilan.getNbCourses()).append("\n");
        sb.append("✅ Gains : ").append(bilan.getNbWin())
                .append(" (💰 ").append(bilan.getAmountWin()).append(")\n");
        sb.append("❌ Pertes : ").append(bilan.getNbLoose())
                .append(" (💸 ").append(bilan.getAmountLoose()).append(")\n");

        // Emoji selon bénéfice positif ou négatif
        BigDecimal benefits = bilan.getBenefits();
        String emoji = benefits.signum() >= 0 ? "🎉" : "😢";
        sb.append("⚖️ Bénéfices : ").append(benefits).append(" ").append(emoji).append("\n\n");

        return sb.toString();
    }
}
