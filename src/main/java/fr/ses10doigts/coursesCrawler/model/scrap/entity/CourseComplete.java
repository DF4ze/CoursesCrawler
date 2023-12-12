package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class CourseComplete extends AbstractCourseEntity{

	///////// Info course
	private String dateCourse;
	private Integer numeroReunion;
	private Integer numeroCourse;
	private String hippodrome;
	private String prime;
	private String typeCourse;
	private String autoStart;

	///////// Arrivée
    private Integer nombrePartant;
	private Integer nombreChevauxInfCinqDepart;
	private Integer nombreChevauxInfCinqAvant;
	private Integer nombreChevauxInfCinqProbablePMU;
	private Integer nombreChevauxInfCinqProbableGeny;
	private Integer nombreChvlFavoriPlaceDepart;
	private Integer nombreChvlFavoriPlaceAvant;
	private Integer nombreChvlFavoriPlaceProbablePMU;
	private Integer nombreChvlFavoriPlaceProbableGeny;

	private Integer numeroChvlPremier;
	private Integer numeroChvlDeuxieme;
	private Integer numeroChvlTroisieme;

    private Double  rapGagnantGeny;
    private Double  rapPlacePremierGeny;
    private Double  rapPlaceDeuxiemeGeny;
    private Double  rapPlaceTroisiemeGeny;
    private Double  rapGagnantPmu;
    private Double  rapPlacePremierPmu;
    private Double  rapPlaceDeuxiemePmu;
    private Double  rapPlaceTroisiemePmu;

    private Float   totalPourcent;

	////////////// Prono 1
//    private Float   cotePremierFavoris;
//    private Float   pourcentPremierFavori;
//    private Integer numeroPremierFavori;
//
//    private Float   coteDeuxiemeFavoris;
//    private Float   pourcentDeuxiemeFavori;
//    private Integer numeroDeuxiemeFavori;
//
//    private Float   coteTroisiemeFavoris;
//    private Float   pourcentTroisiemeFavori;
//    private Integer numeroTroisiemeFavori;

	///////////// Prono 2
	private Integer numeroChlPremierAvant;
	private Float cotePremierAvant;
	private Float pourcentPremierAvant;

	private Integer numeroChlPremierDepart;
	private Float cotePremierDepart;
	private Float pourcentPremierDepart;

	private Integer numeroChlPremierProbablePMU;
	private Float rapportPremierProbablePMU;

	private Integer numeroChlPremierProbableGeny;
	private Float rapportPremierProbableGeny;

	private Integer numeroChlDeuxiemeAvant;
	private Float coteDeuxiemeAvant;
	private Float pourcentDeuxiemeAvant;

	private Integer numeroChlDeuxiemeDepart;
	private Float coteDeuxiemeDepart;
	private Float pourcentDeuxiemeDepart;

	private Integer numeroChlDeuxiemeProbablePMU;
	private Float rapportDeuxiemeProbablePMU;

	private Integer numeroChlDeuxiemeProbableGeny;
	private Float rapportDeuxiemeProbableGeny;

	private Integer numeroChlTroisiemeAvant;
	private Float coteTroisiemeAvant;
	private Float pourcentTroisiemeAvant;

	private Integer numeroChlTroisiemeDepart;
	private Float coteTroisiemeDepart;
	private Float pourcentTroisiemeDepart;

	private Integer numeroChlTroisiemeProbablePMU;
	private Float rapportTroisiemeProbablePMU;

	private Integer numeroChlTroisiemeProbableGeny;
	private Float rapportTroisiemeProbableGeny;

	/////////////// Divers
    private String  ageSexChvlPremier;
    private String  musiquePremier;
    private String  nomChvlPremier;
    private Integer gainChvlPremier;
    private String  musiqueMeilleurGains;
    private Integer numeroMeilleurGains;



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CourseComplete other = (CourseComplete) obj;
		return Objects.equals(dateCourse, other.dateCourse) && Objects.equals(numeroCourse, other.numeroCourse)
				&& Objects.equals(numeroReunion, other.numeroReunion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dateCourse, numeroCourse, numeroReunion);
	}



}
