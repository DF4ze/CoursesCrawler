package fr.ses10doigts.coursesCrawler.service.scrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractEntity;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Arrivee;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Cote;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Rapport;
import fr.ses10doigts.coursesCrawler.repository.course.ArriveeRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CoteRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.repository.course.PartantRepository;
import fr.ses10doigts.coursesCrawler.repository.course.RapportRepository;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.RefactorerReport;

@Component
public class Refactorer implements Runnable {

	@Autowired
	private transient ArriveeRepository arriveeRepository;
	@Autowired
	private transient CoteRepository coteRepository;
	@Autowired
	private transient PartantRepository partantRepository;
	@Autowired
	private transient RapportRepository rapportRepository;
	@Autowired
	private transient CourseRepository courseRepository;
	@Autowired
	private transient RepositoryService repository;

	private static final Logger logger = LoggerFactory.getLogger(Refactorer.class);
	private transient final int cycleStep = 50;
	private static Long from = null;
	private static boolean running = true;
	private Thread friend;
	private RefactorerReport report = RefactorerReport.getInstance();
	private Chrono chrono;

	public static void setFrom(Long from) {
		Refactorer.from = from;
	}

	public Refactorer() {
	}

	public void makeCourseComplete(Long fromCourseId) {
		from = fromCourseId;
		makeCourseComplete();
	}

	@Override
	public void run() {
		try {
			if (friend != null) {
				logger.debug("Refactorer waiting for crawl to end");
				friend.join();
			}
			chrono = new Chrono();
			chrono.pick();
			makeCourseComplete();

		} catch (InterruptedException e) {
			logger.error("Refactorer thread failed to wait for Crawler. Refactoring won't be done");
		}

	}

	public void makeCourseComplete() {
		if (!running) {
			return;
		}

		logger.debug("************************* Refactorer start");
		report.startRefacto();

		List<Course> coursesList = null;

		int stepDone = 0;

		Chrono stepChrono = new Chrono();

		stepChrono.pick();

		Collection<AbstractEntity> computedBuffer = new ArrayList<>();
		if (from != null) {
			logger.info("Starting from id " + from);
			coursesList = courseRepository.findAllFrom(from);
		} else {
			coursesList = courseRepository.findAll();
		}

		report.setTotalCourse(coursesList.size());

		for (Course course : coursesList) {
			if (!running) {
				return;
			}

			CourseComplete cc = new CourseComplete();

			///////////////////////////////////
			// Infos Course
			cc.setCourseID(course.getCourseID());
			cc.setDateCourse(course.getDate());
			cc.setNumeroReunion(course.getReunion());
			cc.setNumeroCourse(course.getCourse());
			cc.setHippodrome(course.getHippodrome());
			cc.setPrime(course.getPrime());
			cc.setTypeCourse(course.getType());
			cc.setAutoStart(course.getDepart());

			////////////////////////////////////
			// Infos Rapport
			Set<Rapport> rapportsList = rapportRepository.findAllByCourseID(course.getCourseID());

			if (rapportsList.size() == 0) {
				logger.debug("Skip => Missing rapport for " + course.getCourseID());
				report.addSkipped(1);
				continue;
			}

			boolean isBreak = false;
			for (Rapport unRap : rapportsList) {
				if (!running) {
					return;
				}

				if (unRap.getArrivee() == null) {
					isBreak = true;
					break;
				}

				if (unRap.getArrivee() == 1) {
					cc.setNumeroChvlPremier(unRap.getNumCheval());
					cc.setRapGagnantGeny(unRap.getGagnantGeny());
					cc.setRapPlacePremierGeny(unRap.getPlaceGeny());
					cc.setRapGagnantPmu(unRap.getGagnantPmu());
					cc.setRapPlacePremierPmu(unRap.getPlacePmu());

				} else if (unRap.getArrivee() == 2) {
					cc.setNumeroChvlDeuxieme(unRap.getNumCheval());
					cc.setRapPlaceDeuxiemeGeny(unRap.getPlaceGeny());
					cc.setRapPlaceDeuxiemePmu(unRap.getPlacePmu());

				} else if (unRap.getArrivee() == 3) {
					cc.setNumeroChvlTroisieme(unRap.getNumCheval());
					cc.setRapPlaceTroisiemeGeny(unRap.getPlaceGeny());
					cc.setRapPlaceTroisiemePmu(unRap.getPlacePmu());

				}

			}
			if (isBreak) {
				report.addSkipped(1);
				continue;
			}

			rapportsList.clear();
			rapportsList = null;

			// Quick Break
			if (!running) {
				return;
			}

			////////////////////////////////////
			// Infos Cote
			Set<Cote> cotesList = coteRepository.findByCourseID(course.getCourseID());

			if (cotesList.size() == 0) {
				logger.debug("Skip => Missing cote for " + course.getCourseID());
				report.addSkipped(1);
				continue;
			}

			cc.setNombrePartant(cotesList.size());

			// Définition des meilleures cotes
			cc.setCotePremierAvant(5000f);
			cc.setCotePremierDepart(5000f);
			cc.setCoteDeuxiemeAvant(5000f);
			cc.setCoteDeuxiemeDepart(5000f);
			cc.setCoteTroisiemeAvant(5000f);
			cc.setCoteTroisiemeDepart(5000f);

			for (Cote uneCote : cotesList) {
				// Les cotes avant
				if (uneCote.getCoteAvant() != null && uneCote.getCoteAvant() < cc.getCotePremierAvant()) {
					cc.setCoteTroisiemeAvant(cc.getCoteDeuxiemeAvant());
					cc.setCoteDeuxiemeAvant(cc.getCotePremierAvant());
					cc.setCotePremierAvant(uneCote.getCoteAvant());

					cc.setNumeroChlTroisiemeAvant(cc.getNumeroChlDeuxiemeAvant());
					cc.setNumeroChlDeuxiemeAvant(cc.getNumeroChlPremierAvant());
					cc.setNumeroChlPremierAvant(uneCote.getNumCheval());

					cc.setPourcentTroisiemeAvant(cc.getPourcentDeuxiemeAvant());
					cc.setPourcentDeuxiemeAvant(cc.getPourcentPremierAvant());
					cc.setPourcentPremierAvant(uneCote.getEnjeuxAvant());

				} else if (uneCote.getCoteAvant() != null && uneCote.getCoteAvant() < cc.getCoteDeuxiemeAvant()) {
					cc.setCoteTroisiemeAvant(cc.getCoteDeuxiemeAvant());
					cc.setCoteDeuxiemeAvant(uneCote.getCoteAvant());

					cc.setNumeroChlTroisiemeAvant(cc.getNumeroChlDeuxiemeAvant());
					cc.setNumeroChlDeuxiemeAvant(uneCote.getNumCheval());

					cc.setPourcentTroisiemeAvant(cc.getPourcentDeuxiemeAvant());
					cc.setPourcentDeuxiemeAvant(uneCote.getEnjeuxAvant());

				} else if (uneCote.getCoteAvant() != null && uneCote.getCoteAvant() < cc.getCoteTroisiemeAvant()) {
					cc.setCoteTroisiemeAvant(uneCote.getCoteAvant());

					cc.setNumeroChlTroisiemeAvant(uneCote.getNumCheval());

					cc.setPourcentTroisiemeAvant(uneCote.getEnjeuxAvant());
				}

				// Les cotes Départ
				if (uneCote.getCoteDepart() < cc.getCotePremierDepart()) {
					cc.setCoteTroisiemeDepart(cc.getCoteDeuxiemeDepart());
					cc.setCoteDeuxiemeDepart(cc.getCotePremierDepart());
					cc.setCotePremierDepart(uneCote.getCoteDepart());

					cc.setNumeroChlTroisiemeDepart(cc.getNumeroChlDeuxiemeDepart());
					cc.setNumeroChlDeuxiemeDepart(cc.getNumeroChlPremierDepart());
					cc.setNumeroChlPremierDepart(uneCote.getNumCheval());

					cc.setPourcentTroisiemeDepart(cc.getPourcentDeuxiemeDepart());
					cc.setPourcentDeuxiemeDepart(cc.getPourcentPremierDepart());
					cc.setPourcentPremierDepart(uneCote.getEnjeuxAvant());

				} else if (uneCote.getCoteDepart() < cc.getCoteDeuxiemeDepart()) {
					cc.setCoteTroisiemeDepart(cc.getCoteDeuxiemeDepart());
					cc.setCoteDeuxiemeDepart(uneCote.getCoteDepart());

					cc.setNumeroChlTroisiemeDepart(cc.getNumeroChlDeuxiemeDepart());
					cc.setNumeroChlDeuxiemeDepart(uneCote.getNumCheval());

					cc.setPourcentTroisiemeDepart(cc.getPourcentDeuxiemeDepart());
					cc.setPourcentDeuxiemeDepart(uneCote.getEnjeuxAvant());

				} else if (uneCote.getCoteDepart() < cc.getCoteTroisiemeDepart()) {
					cc.setCoteTroisiemeDepart(uneCote.getCoteDepart());

					cc.setNumeroChlTroisiemeDepart(uneCote.getNumCheval());

					cc.setPourcentTroisiemeDepart(uneCote.getEnjeuxAvant());
				}

				// Cotes < 5
				cc.setNombreChevauxInfCinqAvant(0);
				cc.setNombreChevauxInfCinqDepart(0);
				if (uneCote.getCoteAvant() != null && uneCote.getCoteAvant() < 5)
					cc.setNombreChevauxInfCinqAvant(cc.getNombreChevauxInfCinqAvant() + 1);
				if (uneCote.getCoteDepart() < 5)
					cc.setNombreChevauxInfCinqDepart(cc.getNombreChevauxInfCinqDepart() + 1);

			}

			// Suppression des 5000 si pb
			if (cc.getCotePremierAvant().equals(5000f))
				cc.setCotePremierAvant(null);
			if (cc.getCotePremierDepart().equals(5000f))
				cc.setCotePremierDepart(null);
			if (cc.getCoteDeuxiemeAvant().equals(5000f))
				cc.setCoteDeuxiemeAvant(null);
			if (cc.getCoteDeuxiemeDepart().equals(5000f))
				cc.setCoteDeuxiemeDepart(null);
			if (cc.getCoteTroisiemeAvant().equals(5000f))
				cc.setCoteTroisiemeAvant(null);
			if (cc.getCoteTroisiemeDepart().equals(5000f))
				cc.setCoteTroisiemeDepart(null);

			// Favoris Placés
			List<Integer> cvxArrivees = new ArrayList<>();
			cvxArrivees.add(cc.getNumeroChvlPremier());
			cvxArrivees.add(cc.getNumeroChvlDeuxieme());
			cvxArrivees.add(cc.getNumeroChvlTroisieme());

			cc.setNombreChvlFavoriPlaceAvant(0);
			cc.setNombreChvlFavoriPlaceDepart(0);

			// Cotes Avant
			if (cvxArrivees.contains(cc.getNumeroChlPremierAvant()))
				cc.setNombreChvlFavoriPlaceAvant(cc.getNombreChvlFavoriPlaceAvant() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlDeuxiemeAvant()))
				cc.setNombreChvlFavoriPlaceAvant(cc.getNombreChvlFavoriPlaceAvant() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlTroisiemeAvant()))
				cc.setNombreChvlFavoriPlaceAvant(cc.getNombreChvlFavoriPlaceAvant() + 1);

			// Cotes Départ
			if (cvxArrivees.contains(cc.getNumeroChlPremierDepart()))
				cc.setNombreChvlFavoriPlaceDepart(cc.getNombreChvlFavoriPlaceDepart() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlDeuxiemeDepart()))
				cc.setNombreChvlFavoriPlaceDepart(cc.getNombreChvlFavoriPlaceDepart() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlTroisiemeDepart()))
				cc.setNombreChvlFavoriPlaceDepart(cc.getNombreChvlFavoriPlaceDepart() + 1);


			cotesList.clear();
			cotesList = null;

			// calcul de la somme des enjeux/prcent
			Float somPrCent = (cc.getPourcentPremierDepart() == null ? 0f : cc.getPourcentPremierDepart())
					+ (cc.getPourcentDeuxiemeDepart() == null ? 0f : cc.getPourcentDeuxiemeDepart())
					+ (cc.getPourcentTroisiemeDepart() == null ? 0f : cc.getPourcentTroisiemeDepart());
			cc.setTotalPourcent(somPrCent);

			///////////////////////////
			// Arrivees
			Set<Arrivee> arrivees = arriveeRepository.findByCourseID(course.getCourseID());

			if (arrivees.isEmpty()) {
				logger.debug("Skip => Missing arrivee for " + course.getCourseID());
				report.addSkipped(1);
				continue;
			}

			// Récupération de l'URL depuis une arrivée
			Object[] array = arrivees.toArray();
			Arrivee uneArrivee = (Arrivee) array[0];
			cc.setUrl(uneArrivee.getUrl());


			//////////////////////////////
			// Info partant
			int ageMin = 30;
			int ageMax = 0;

			Set<Partant> partantsListe = partantRepository.findByCourseID(course.getCourseID());

			if (partantsListe.isEmpty()) {
				logger.debug("Skip => Missing partant for " + course.getCourseID());
				report.addSkipped(1);
				continue;
			}

			Partant ChvBestGains = null;
			if (partantsListe.size() != 0) {
				cc.setRapportPremierProbableGeny(5000f);
				cc.setRapportDeuxiemeProbableGeny(5000f);
				cc.setRapportTroisiemeProbableGeny(5000f);
				cc.setNombreChevauxInfCinqProbableGeny(0);

				cc.setRapportPremierProbablePMU(5000f);
				cc.setRapportDeuxiemeProbablePMU(5000f);
				cc.setRapportTroisiemeProbablePMU(5000f);
				cc.setNombreChevauxInfCinqProbablePMU(0);

				// on fait le tour de tous les partants de cette course
				for (Partant unPart : partantsListe) {
					if (!running) {
						return;
					}

					// prono < 5
					if (unPart.getProbableGeny() != null && unPart.getProbableGeny() <= 5) {
						cc.setNombreChevauxInfCinqProbableGeny(cc.getNombreChevauxInfCinqProbableGeny() + 1);
					}
					if (unPart.getProbablePMU() != null && unPart.getProbablePMU() <= 5) {
						cc.setNombreChevauxInfCinqProbablePMU(cc.getNombreChevauxInfCinqProbablePMU() + 1);
					}

					// si le numero du cheval en cours est celui du 1er
					if (unPart.getNumCheval() == cc.getNumeroChvlPremier()) {
						cc.setMusiquePremier(unPart.getMusique());
						cc.setAgeSexChvlPremier(unPart.getAgeSexe());
						cc.setNomChvlPremier(unPart.getNomCheval());
						cc.setGainChvlPremier(unPart.getIGains());
					}

					// Organisation meilleurs Probables Geny
					if (unPart.getProbableGeny() != null) {
						if (unPart.getProbableGeny() < cc.getRapportPremierProbableGeny()) {
							cc.setRapportTroisiemeProbableGeny(cc.getRapportDeuxiemeProbableGeny());
							cc.setRapportDeuxiemeProbableGeny(cc.getRapportPremierProbableGeny());
							cc.setRapportPremierProbableGeny(unPart.getProbableGeny());

							cc.setNumeroChlTroisiemeProbableGeny(cc.getNumeroChlTroisiemeProbableGeny());
							cc.setNumeroChlDeuxiemeProbableGeny(cc.getNumeroChlPremierProbableGeny());
							cc.setNumeroChlPremierProbableGeny(unPart.getNumCheval());

						} else if (unPart.getProbableGeny() < cc.getRapportDeuxiemeProbableGeny()) {
							cc.setRapportTroisiemeProbableGeny(cc.getRapportDeuxiemeProbableGeny());
							cc.setRapportDeuxiemeProbableGeny(unPart.getProbableGeny());

							cc.setNumeroChlTroisiemeProbableGeny(cc.getNumeroChlTroisiemeProbableGeny());
							cc.setNumeroChlDeuxiemeProbableGeny(unPart.getNumCheval());

						} else if (unPart.getProbableGeny() < cc.getRapportTroisiemeProbableGeny()) {
							cc.setRapportTroisiemeProbableGeny(unPart.getProbableGeny());

							cc.setNumeroChlTroisiemeProbableGeny(unPart.getNumCheval());
						}
					}


					// Organisation meilleurs Probables PMU
					if (unPart.getProbablePMU() != null) {
						if (unPart.getProbablePMU() < cc.getRapportPremierProbablePMU()) {
							cc.setRapportTroisiemeProbablePMU(cc.getRapportDeuxiemeProbablePMU());
							cc.setRapportDeuxiemeProbablePMU(cc.getRapportPremierProbablePMU());
							cc.setRapportPremierProbablePMU(unPart.getProbablePMU());

							cc.setNumeroChlTroisiemeProbablePMU(cc.getNumeroChlTroisiemeProbablePMU());
							cc.setNumeroChlDeuxiemeProbablePMU(cc.getNumeroChlPremierProbablePMU());
							cc.setNumeroChlPremierProbablePMU(unPart.getNumCheval());

						} else if (unPart.getProbablePMU() < cc.getRapportDeuxiemeProbablePMU()) {
							cc.setRapportTroisiemeProbablePMU(cc.getRapportDeuxiemeProbablePMU());
							cc.setRapportDeuxiemeProbablePMU(unPart.getProbablePMU());

							cc.setNumeroChlTroisiemeProbablePMU(cc.getNumeroChlTroisiemeProbablePMU());
							cc.setNumeroChlDeuxiemeProbablePMU(unPart.getNumCheval());

						} else if (unPart.getProbablePMU() < cc.getRapportTroisiemeProbablePMU()) {
							cc.setRapportTroisiemeProbablePMU(unPart.getProbablePMU());

							cc.setNumeroChlTroisiemeProbablePMU(unPart.getNumCheval());
						}
					}

					// min et max age
					try {
						String text = unPart.getAgeSexe().replace("F", "");
						text = text.replace("H", "");
						text = text.replace("M", "");

						int age = Integer.parseInt(text);

						if (age > ageMax) {
							ageMax = age;
						}
						if (age < ageMin) {
							ageMin = age;
						}

					} catch (Exception e) {
						String texte = "Pb parse Age (" + unPart.getAgeSexe() + "), course " + course.getCourseID()
								+ " : " + e.getMessage();
						logger.error(texte);

					}

					// musique du cheval qui a le plus gros gain
					if (ChvBestGains == null && unPart.getIGains() != null) {
						ChvBestGains = unPart.clone();
					} else if (unPart.getIGains() != null) {
						if (unPart.getIGains() > ChvBestGains.getIGains()) {
							ChvBestGains = unPart.clone();
						}
					}

				}
				cc.setAgeSexChvlPremier(ageMin + "-" + ageMax);
				if (ChvBestGains != null) {
					cc.setMusiqueMeilleurGains(ChvBestGains.getMusique());
					cc.setNumeroMeilleurGains(ChvBestGains.getNumCheval());
				}

				// Nettoyage des 5000
				if (cc.getRapportPremierProbableGeny().equals(5000f))
					cc.setRapportPremierProbableGeny(null);
				if (cc.getRapportDeuxiemeProbableGeny().equals(5000f))
					cc.setRapportDeuxiemeProbableGeny(null);
				if (cc.getRapportTroisiemeProbableGeny().equals(5000f))
					cc.setRapportTroisiemeProbableGeny(null);

				if (cc.getRapportPremierProbablePMU().equals(5000f))
					cc.setRapportPremierProbablePMU(null);
				if (cc.getRapportDeuxiemeProbablePMU().equals(5000f))
					cc.setRapportDeuxiemeProbablePMU(null);
				if (cc.getRapportTroisiemeProbablePMU().equals(5000f))
					cc.setRapportTroisiemeProbablePMU(null);

			}
			partantsListe.clear();
			partantsListe = null;



			// Nombre Favoris Probable Geny placés
			cc.setNombreChvlFavoriPlaceProbableGeny(0);

			if (cvxArrivees.contains(cc.getNumeroChlPremierProbableGeny()))
				cc.setNombreChvlFavoriPlaceProbableGeny(cc.getNombreChvlFavoriPlaceProbableGeny() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlDeuxiemeProbableGeny()))
				cc.setNombreChvlFavoriPlaceProbableGeny(cc.getNombreChvlFavoriPlaceProbableGeny() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlTroisiemeProbableGeny()))
				cc.setNombreChvlFavoriPlaceProbableGeny(cc.getNombreChvlFavoriPlaceProbableGeny() + 1);

			// Nombre Favoris Probable PMU placés
			cc.setNombreChvlFavoriPlaceProbablePMU(0);

			if (cvxArrivees.contains(cc.getNumeroChlPremierProbablePMU()))
				cc.setNombreChvlFavoriPlaceProbablePMU(cc.getNombreChvlFavoriPlaceProbablePMU() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlDeuxiemeProbablePMU()))
				cc.setNombreChvlFavoriPlaceProbablePMU(cc.getNombreChvlFavoriPlaceProbablePMU() + 1);
			if (cvxArrivees.contains(cc.getNumeroChlTroisiemeProbablePMU()))
				cc.setNombreChvlFavoriPlaceProbablePMU(cc.getNombreChvlFavoriPlaceProbablePMU() + 1);

			///////////////////////////
			// envoi BDD
			// repository.add(cc);
			computedBuffer.add(cc);
			logger.debug("Computed ok for courseID : " + course.getCourseID());

			cc = null;

			// stepDone++;
			// if (stepDone > cycleStep) {
			// from = course.getCourseID();
			// break;
			// }
			stepDone++;
			report.addTreated(1);
		}

		if (!running) {
			return;
		}
		repository.addAll(computedBuffer);
		computedBuffer.clear();

		logger.info("Computed saved");
		long time = stepChrono.compare();

		logger.info(stepDone + " records in " + (time / cycleStep) + "ms / step");
		report.setTime(time);
		report.stopRefacto();

		coursesList.clear();
		coursesList = null;
		System.gc();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}

		// if( stepDone < cycleStep ) {
		// break;
		// }

		// }

	}

//    private static Map<Integer, String> getNomFromPlace(Set<Arrivee> arrivees) {
//		Map<Integer, String> ret = new HashMap<Integer, String>();
//
//		for (Arrivee arrivee : arrivees) {
//			ret.put(arrivee.getNumArrivee(), arrivee.getNomChv());
//		}
//
//		return ret;
//    }

	public void stop() {
		running = false;

	}

	public void setFriend(Thread t) {
		friend = t;
	}

	public RefactorerReport getReport() {
		return report;
	}

	public boolean getRunning() {

		return running;
	}

	public Chrono getChrono() {
		return chrono;
	}

}
