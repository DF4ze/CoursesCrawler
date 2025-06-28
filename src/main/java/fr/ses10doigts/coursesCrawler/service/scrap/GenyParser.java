package fr.ses10doigts.coursesCrawler.service.scrap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractEntity;
import fr.ses10doigts.coursesCrawler.model.scrap.EntitiesList;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Arrivee;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Cote;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Rapport;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.XPathTool;

@Component
public class GenyParser implements HtmlParser {

	private String url;
	private String body;
	private Document doc;
	private XPathTool xPathTool;

	@Autowired
	private CourseRepository courseRepository;

	private static final Logger logger = LoggerFactory.getLogger(GenyParser.class);

	GenyParser() {
	}

	@Override
	public EntitiesList parse(String url, String body) {
		this.url = url;
		this.body = body == null ? "" : body;

		EntitiesList beansList = new EntitiesList();
		init();

		EntitiesList beanListCourse = parse_course();
		if (beanListCourse != null) {
			beansList.addAll(beanListCourse);
		}

		EntitiesList beansListRapport = parse_rapport();
		if (beansListRapport != null) {
			beansList.addAll(beansListRapport);
		}

		EntitiesList beansListArrive = parse_arrivee();
		if (beansListArrive != null) {
			beansList.addAll(beansListArrive);
		}

		EntitiesList beansListPartant = parse_partant();
		if (beansListPartant != null) {
			beansList.addAll(beansListPartant);
		}

		EntitiesList beansListCote = parse_cote();
		if (beansListCote != null) {
			beansList.addAll(beansListCote);
		}

		try {
			Thread.sleep(10l);
		} catch (InterruptedException e) {
		}

		return beansList;
	}

	private void init() {
		doc = Jsoup.parse(body);
		xPathTool = new XPathTool();
	}

	private Long parse_numCourse() {
		Long longCourse = null;
		// extraction de l'ID de la course
		Pattern p = Pattern.compile(".*_c([0-9]+)$");
		Matcher m = p.matcher(url);

		boolean isMatching = true;
		if (!m.matches()) {
			p = Pattern.compile(".*course=([0-9]+).*");
			m = p.matcher(url);
			if (!m.matches()) {
				isMatching = false;
			}
		}

		if (isMatching) {
			String numCourse = m.group(1);
			try {
				longCourse = Long.parseLong(numCourse);
			} catch (Exception e) {
				logger.debug("Unable to parse num course : " + e.getMessage());

			}
		}

		return longCourse;
	}

	private EntitiesList parse_course() {
		logger.debug("=================================== Course infos");
		logger.info(url);

		Long longCourse = parse_numCourse();
		EntitiesList bl = null;
		List<Course> findByCourseID = courseRepository.findByCourseID(longCourse);
		// si la course a déjà été parsée on ne la renseigne pas de nouveau pour éviter
		// les doublons
		if (!findByCourseID.isEmpty()) {
			return bl;
		}

		// extraction de l'ID de la date
		Pattern p = Pattern.compile(".*([0-9]{4}-[0-9]{2}-[0-9]{2})-.*");
		Matcher m = p.matcher(url);
		boolean b = m.matches();
		String date = null;
		if (b) {
			date = m.group(1);
		} else {
			Elements elements = xPathTool.getElements(doc, "/div[@class='yui-u']/div/span");
			if (elements != null && elements.size() > 0) {
				String txt = elements.get(0).text();
				p = Pattern.compile(".*([0-9]{2}/[0-9]{2}/[0-9]{2}).*");
				m = p.matcher(txt);
				if (m.matches()) {
					date = m.group(1);
					SimpleDateFormat sdf2DYear = new SimpleDateFormat("dd/MM/yy");
					SimpleDateFormat sdf4DYear = new SimpleDateFormat("yyyy/MM/dd");
					try {
						Date parse = sdf2DYear.parse(date);
						date = sdf4DYear.format(parse);
					} catch (ParseException e) {
						logger.error("Error parsing date : " + date);
					}
				}
			}
		}
		logger.debug("Date : " + date);

		// extraction de la reunion
		String hippodrome = null;
		String reunion = null;
		Integer intReunion = null;
		Elements elements = xPathTool.getElements(doc, "/div[@class='nomReunion']");
		if (elements != null && elements.size() > 0) {
			String txt = elements.get(0).text();
			logger.debug("nomReunion : " + txt);

			// hippodrome
			p = Pattern.compile(".*:(.*)\\(R[0-9]+\\)");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				hippodrome = m.group(1);
				hippodrome = hippodrome.trim();
			}
			logger.debug("hippodrome : " + hippodrome);

			// N� reunion
			p = Pattern.compile(".*:.*\\(R([0-9]+)\\)");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				reunion = m.group(1);
				try {
					intReunion = Integer.parseInt(reunion);
				} catch (Exception e) {
					logger.debug(e.getMessage());
				}
			}
			logger.debug("reunion : " + intReunion);

		}

		// Extraction de la course
		Integer numCourse = null;
		String prix = null;
		elements = xPathTool.getElements(doc, "/div[@class='nomCourse']");
		if (elements != null && elements.size() > 0) {
			String txt = elements.get(0).text();

			p = Pattern.compile("([0-9]+).*-.*");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				String sNumCourse = m.group(1);
				try {
					numCourse = Integer.parseInt(sNumCourse);
				} catch (Exception e) {
					logger.error("!!!!!!!! error parsing numCourse : " + e.getMessage());
				}
			}
			logger.debug("numCourse : " + numCourse);

			p = Pattern.compile("[0-9]+.*- (.*)");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				prix = m.group(1);
				prix = prix.trim();
			}
			logger.debug("prix : " + prix);

		}

		// extract info course // Mont�|Steeple-chase|Attel�|Plat
		String type = null;
		String prime = null;
		String depart = "non";
		String heures = null;
		String minutes = null;
		elements = xPathTool.getElements(doc, "/span[@class='infoCourse']");
		if (elements != null && elements.size() > 0) {
			String txt = elements.get(0).text();
			logger.debug("infoCourse : " + txt);

			p = Pattern.compile(".*(Monté|Steeple-chase|Attelé|Plat).*");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				type = m.group(1);
				type = type.trim();
			}
			logger.debug("type : " + type);

			// somme des enjeux
			p = Pattern.compile(".*-.(([0-9]+.)*[0-9]{3})€.*");
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				prime = m.group(1);
				prime = prime.replaceAll(" ", "");
				// try{
				// intReunion = Integer.parseInt(prime);
				// }catch(Exception e){}
			} else {
				logger.debug("prime no match ");

			}
			logger.debug("prime : " + prime);

			// AutoStart
			p = Pattern.compile(".*autostart.*", Pattern.CASE_INSENSITIVE);
			m = p.matcher(txt);
			b = m.matches();
			if (b) {
				depart = "oui";
			}
			logger.debug("Autostart : " + depart);

			// Heure
			p = Pattern.compile("Départ\\s+(\\d{1,2})h(\\d{2})");
			m = p.matcher(txt);
			if (m.find()) {
				heures = m.group(1);
				minutes = m.group(2);
			}
			logger.debug("heure : " + heures + ":" + minutes);

		}

		Course course = null;
	// @formatter:off
	if( longCourse 		!= null &&
		numCourse 	!= null &&
		date		!= null &&
		hippodrome 	!= null &&
		prix 		!= null &&
		intReunion 	!= null &&
		depart	 	!= null &&
		type 		!= null ){
	    // @formatter:on

			course = new Course();
			course.setId(longCourse);
			course.setCourseID(longCourse);
			course.setCourse(numCourse);
			course.setDate(date);
			course.setHippodrome(hippodrome);
			course.setPrix(prix);
			course.setReunion(intReunion);
			course.setType(type);
			course.setPrime(prime);
			course.setDepart(depart);
			course.setUrl(url);
			course.setHeures(heures);
			course.setMinutes(minutes);
		}

		if (course != null) {
			bl = new EntitiesList();
			bl.add(course);
			logger.info("Adding course : " + course);
		}

		return bl;
	}

	private EntitiesList parse_rapport() {
		Long longCourse = parse_numCourse();

		EntitiesList listeRapports = null;
		if (url.indexOf("arrivee-et-rapports") != -1 && longCourse != null) {
			logger.debug("=================================== Rapports");
			logger.info(url);

			listeRapports = new EntitiesList();

			// Recherche tableau
			Elements tables = xPathTool.getElements(doc, "/table[@id='lesSolos']/tbody");

			String isRigthTable = "";

			// TODO tout casser et refaire!
			// faire une premiere phase de recherche des tableaux
			// puis parsing

			table: for (int iTable = 1; iTable < tables.size(); iTable++) {
				// si c'est encore du Geny... faut passer encore au tableau suivant
				if (isRigthTable.equals("geny")) {
					isRigthTable = "";
					continue;
				}
				Element table = tables.get(iTable);
				Elements lignes = table.select("tr");
				if (lignes == null || lignes.size() == 0) {
					continue;
				}

				Rapport rapport = new Rapport();
				rapport.setUrl(url);
				line: for (int iLigne = 0; iLigne < lignes.size(); iLigne++) {
					Element uneLigne = lignes.get(iLigne);
					Elements cellules = uneLigne.select("td");
					if (cellules == null || cellules.size() == 0) {
						continue;
					}

					Integer numCheval = null;
					Double gain = null;
					try {
						boolean firstLine = false;
						for (int jCellule = 0; jCellule < cellules.size(); jCellule++) {

							Element uneCellule = cellules.get(jCellule);
							// C'est la 1ere cell du tab qui indique s'il s'agit du bon tab
							Elements discrimGeny = uneCellule.getElementsByAttributeValueMatching("alt", "solo");
							Elements discrimPmu = uneCellule.getElementsByAttributeValueMatching("alt", "eSimple");

							if (discrimGeny.size() > 0) {
								isRigthTable = "geny";
								firstLine = true;
								// on dédie le traitement Spécial! des Geny à une fonction et on passe au
								// tableau suivant
								listeRapports.addAll(parseGenyRapport(tables, longCourse));
								continue table;

							} else if (discrimPmu.size() > 0) {
								isRigthTable = "pmu";
								firstLine = true;
							}
							// si on n'est pas sur le bon tableau, on passe au tableau suivant
							if (isRigthTable.isEmpty()) {
								continue table;
							}
							// si on est sur le bon tableau mais 1ere ligne, on passe à la ligne suivante
							if (firstLine) {
								continue line;
							}

							if (jCellule == 0) {

								String txt = uneCellule.text().trim();
								txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");
								numCheval = Integer.parseInt(txt);

							} else if (jCellule == 1) {
								String txt = uneCellule.text().trim();
								txt = txt.replace(",", ".");
								txt = txt.replace("<b>", "");
								txt = txt.replace("</b>", "");
								txt = txt.replace(" ", "");
								txt = txt.replace("€", "");
								txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");

								gain = Double.parseDouble(txt);
							}

						} // for cellules

						if (isRigthTable.isEmpty()) {
							continue;
						}
						if (iLigne == 1) {
							rapport = new Rapport();
							rapport.setUrl(url);
							rapport.setNumCheval(numCheval);
							rapport.setCourseID(longCourse);
							rapport.setGagnantPmu(gain);
							rapport.setArrivee(1);

						} else if (iLigne == 2) {
							rapport.setPlacePmu(gain);

							listeRapports.add(rapport);
							logger.debug("PMU : 1er => N" + rapport.getNumCheval() + " G:" + rapport.getGagnantPmu()
									+ " P:" + rapport.getPlacePmu());

						} else {
							rapport = new Rapport();
							rapport.setUrl(url);
							rapport.setCourseID(longCourse);
							rapport.setNumCheval(numCheval);
							rapport.setArrivee(iLigne - 1);
							rapport.setPlacePmu(gain);

							listeRapports.add(rapport);
							logger.debug("PMU : " + rapport.getArrivee() + "eme => N" + rapport.getNumCheval() + " P:"
									+ rapport.getPlacePmu());
						}

					} catch (Exception e) { // pour les Parses Exceptions
						logger.error("Erreur RAPPORT : " + e.getMessage());
					}

				} // for lignes

			} // for Tables

			// need to mix all result (can't save same courseid/numCheval in db)
			Map<String, Rapport> mixer = new HashMap<>();
			for (AbstractEntity entity : listeRapports.get()) {
				if (entity instanceof Rapport) {
					Rapport rapport = (Rapport) entity;
					String key = rapport.getCourseID() + "-" + rapport.getNumCheval();
					// si on a déjà stocké
					if (mixer.containsKey(key)) {
						Rapport old = mixer.get(key);
						if (rapport.getGagnantGeny() != null) {
							old.setGagnantGeny(rapport.getGagnantGeny());
						}
						if (rapport.getPlaceGeny() != null) {
							old.setPlaceGeny(rapport.getPlaceGeny());
						}
						if (rapport.getGagnantPmu() != null) {
							old.setGagnantPmu(rapport.getGagnantPmu());
						}
						if (rapport.getPlacePmu() != null) {
							old.setPlacePmu(rapport.getPlacePmu());
						}
						mixer.put(key, old);

					} else {
						mixer.put(key, rapport);
					}
				}
			}

			// et on recréé la liste
			listeRapports.get().clear();
			for (Entry<String, Rapport> entry : mixer.entrySet()) {
				listeRapports.add(entry.getValue());
			}

			if (listeRapports.size() == 0) {
				logger.warn("!!!!!!!!!!!!!!!!!!!! Page sans rapports parsés : " + url);
			} else {
				logger.info("Added " + listeRapports.size() + " rapports");
			}
		}

		return listeRapports;
	}

	public EntitiesList parseGenyRapport(Elements tables, Long longCourse) {

		Rapport rapport = new Rapport();
		rapport.setUrl(url);
		EntitiesList listeRapports = new EntitiesList();

		table: for (int iTable = 1; iTable < tables.size(); iTable++) {
			Element table = tables.get(iTable);
			Elements lignes = table.select("tr");
			if (lignes == null || lignes.size() == 0) {
				continue;
			}
			if (iTable == 3) {
				break table;
			}

			for (int iLigne = (iTable == 1 ? 1 : 0); iLigne < lignes.size(); iLigne++) {
				Element uneLigne = lignes.get(iLigne);
				Elements cellules = uneLigne.select("td");
				if (cellules == null || cellules.size() == 0) {
					continue;
				}

				Integer numCheval = null;
				Double gain = null;
				try {
					for (int jCellule = 0; jCellule < cellules.size(); jCellule++) {
						Element uneCellule = cellules.get(jCellule);

						if (jCellule == 0) {
							String txt = uneCellule.text().trim();
							txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");
							numCheval = Integer.parseInt(txt);

						} else if (jCellule == 1) {
							String txt = uneCellule.text().trim();
							txt = txt.replace(",", ".");
							txt = txt.replace("<b>", "");
							txt = txt.replace("</b>", "");
							txt = txt.replace(" ", "");
							txt = txt.replace("€", "");
							txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");

							gain = Double.parseDouble(txt);
						}

					} // for cellules

					// Creating records from parsed
					if (iLigne == 1 && iTable == 1) {
						rapport = new Rapport();
						rapport.setUrl(url);
						rapport.setNumCheval(numCheval);
						rapport.setCourseID(longCourse);
						rapport.setGagnantGeny(gain);
						rapport.setArrivee(1);

						// Soit 2eme ligne soit cas spécial de la partie GENY qui est sur 2
						// tableaux.........
					} else if (iLigne == 0 && iTable == 2) {
						rapport.setPlaceGeny(gain);
						rapport.setNumCheval(numCheval);
						rapport.setCourseID(longCourse);

						Rapport clone = new Rapport();
						clone.setUrl(url);
						clone.setArrivee(rapport.getArrivee());
						clone.setCourseID(rapport.getCourseID());
						clone.setGagnantGeny(
								rapport.getGagnantGeny() != null ? Double.valueOf(rapport.getGagnantGeny()) : null);
						clone.setGagnantPmu(rapport.getGagnantPmu());
						clone.setNumCheval(rapport.getNumCheval());
						clone.setPlaceGeny(rapport.getPlaceGeny());
						clone.setPlacePmu(rapport.getPlacePmu());
						listeRapports.add(clone);
						rapport.setGagnantGeny(null);

						logger.debug("Geny : 1er => N" + clone.getNumCheval() + " G:" + clone.getGagnantGeny() + " P:"
								+ clone.getPlaceGeny());

					} else if (iTable == 2) {
						rapport = new Rapport();
						rapport.setUrl(url);

						rapport.setCourseID(longCourse);
						rapport.setNumCheval(numCheval);
						rapport.setArrivee(iLigne + 1);
						rapport.setPlaceGeny(gain);

						logger.debug("Geny : " + rapport.getArrivee() + "eme => N" + rapport.getNumCheval() + " P:"
								+ rapport.getPlaceGeny());

						listeRapports.add(rapport);
					}

				} catch (Exception e) { // pour les Parses Exceptions
					logger.error("Erreur RAPPORT : " + e.getMessage());
				}

			} // for lignes
		} // for Tables

		if (listeRapports.size() == 0) {
			logger.warn("!!!!!!!!!!!!!!!!!!!!!! page sans rapport eGeny : " + url);
		} else {
			logger.info("Geny Added " + listeRapports.size() + " rapports");
		}

		return listeRapports;
	}

	private EntitiesList parse_arrivee() {
		Long longCourse = parse_numCourse();

		EntitiesList listeArrivees = null;
		if (url.indexOf("arrivee-et-rapports") != -1 && longCourse != null) {
			logger.debug("=================================== Arrivee");
			logger.info(url);
			Elements nbTableaux = xPathTool.getElements(doc, "/table[@id='arrivees']/tbody");

			Elements lignes = null;
			if (nbTableaux != null && nbTableaux.size() > 0) {
				lignes = xPathTool.getElements(doc, "/table[@id='arrivees']/tbody/tr");
			}

			if (lignes != null) {
				for (int i = 1; i < lignes.size(); i++) {
					Element uneLigne = lignes.get(i);
					Elements cellules = uneLigne.select("td");

					String nomCheval = null;
					Integer numCheval = null;
					Integer placeCheval = null;
					try {
						if (cellules != null && cellules.size() > 0) {
							for (int j = 0; j < cellules.size(); j++) {
								Element uneCellule = cellules.get(j);

								if (j == 0) {
									String content = uneCellule.text();
									placeCheval = Integer.parseInt(content);

								} else if (j == 1) {
									String content = uneCellule.text();
									numCheval = Integer.parseInt(content);

								} else if (j == 2) {
									Elements justeNom = uneCellule.select("a");
									nomCheval = justeNom.text().trim();

								} else {
									break;
								}
							}

							if (placeCheval != null && numCheval != null) {
								if (listeArrivees == null) {
									listeArrivees = new EntitiesList();
								}
								logger.debug("Course : " + longCourse + " Place : " + placeCheval + " Numero : "
										+ numCheval + " Nom : " + nomCheval);

								listeArrivees.add(new Arrivee(url, longCourse, placeCheval, numCheval, nomCheval));
							}
						}
					} catch (Exception e) {
						// logger.debug("Ligne N " + i + " Error: " + e.getMessage());

					}
				}

			}
			if (listeArrivees.size() == 0) {
				logger.warn("!!!!!!!!!!!!!!!!!!!! Page sans arrivées : " + url);
			} else {
				logger.info("Adding " + listeArrivees.size() + " arrivees");
			}
		}

		return listeArrivees;
	}

	private EntitiesList parse_cote() {
		Long longCourse = parse_numCourse();

		EntitiesList cotesCourse = null;

		if (url.indexOf("/cotes/") != -1 && longCourse != null) {
			logger.debug("=================================== Cote");
			logger.info(url);

			Elements lignes = xPathTool.getElements(doc, "/div[@id='div_tableau_cotes']/table/tbody/tr");

			if (lignes != null && lignes.size() > 0) {

				for (int i = 0; i < lignes.size(); i++) {
					Element uneLigne = lignes.get(i);
					Elements cellules = uneLigne.select("td");

					// logger.debug("Line N°" + i + " content : " + cellules.text());

					Integer numCheval = null;
					Float enjeuxDepart = null;
					Float enjeuxAvant = null;
					Float coteAvant = null;
					Float coteDepart = null;
					if (cellules != null && cellules.size() > 0 && !cellules.text().isBlank()) {
						for (int j = 0; j < cellules.size(); j++) {
							Element uneCellule = cellules.get(j);

							// logger.debug("Treating cell N°" + j + " content : " + uneCellule.text());
							try {
								switch (j) {
								case 0:
									numCheval = Integer.parseInt(uneCellule.text().trim());
									break;
								case 6:
									String txt = uneCellule.text().trim();
									txt = txt.replace(",", ".");
									coteAvant = Float.parseFloat(txt);
									break;
								case 7:
									txt = uneCellule.text().trim();
									txt = txt.replace(",", ".");
									coteDepart = Float.parseFloat(txt);
									break;
								case 9:
									txt = uneCellule.text().trim();
									txt = txt.replace(",", ".");
									txt = txt.replace(" ", "");
									txt = txt.replace("%", "");
									txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");

									enjeuxAvant = Float.parseFloat(txt);
									break;
								case 10:
									txt = uneCellule.text().trim();
									txt = txt.replace(",", ".");
									txt = txt.replace(" ", "");
									txt = txt.replace("%", "");
									txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");

									enjeuxDepart = Float.parseFloat(txt);
									break;
								default:
									break;
								}

							} catch (Exception e) {
								// Trop de spam si affiché
								// logger.error("Erreur sur la ligne 'cote' : " + uneLigne.text() + " Message :"
								// + e.getMessage());

							}

						} // fin for cellules

						if (numCheval != null) {
							logger.debug("Cvl : " + numCheval + " cote : " + coteDepart + " enjeux : " + enjeuxDepart);
						}

						if (numCheval != null && coteDepart != null && enjeuxDepart != null) {
							if (cotesCourse == null) {
								cotesCourse = new EntitiesList();
							}

							Cote cote = new Cote();
							cote.setUrl(url);
							cote.setCourseID(longCourse);
							cote.setNumCheval(numCheval);
							cote.setCoteDepart(coteDepart);
							cote.setCoteAvant(coteAvant);
							cote.setEnjeuxDepart(enjeuxDepart);
							cote.setEnjeuxAvant(enjeuxAvant);

							cotesCourse.add(cote);

						}
					}
				} // fin for lignes

				if (cotesCourse.size() == 0) {
					logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!! Page sans Cote : " + url);
				} else {
					logger.info("Adding " + cotesCourse.size() + " cotes");
				}

			}
		}

		return cotesCourse;
	}

	private EntitiesList parse_partant() {
		Long longCourse = parse_numCourse();

		EntitiesList partantsCourse = null;

		if (url.indexOf("/partants-pmu") != -1 && longCourse != null) {
			logger.debug("=================================== Partants");
			logger.info(url);

			Elements celz = xPathTool.getElements(doc, "/div[@id='dt_partants']/table/thead/tr/th");
			int colMusique = 6;
			int colAgeSexe = 2;
			int colNom = 1;
			int colGains = 8;
			int colProbPMU = 9;
			int colProbGeny = 10;

			for (int i = 0; i < celz.size(); i++) {
				Element oneCel = celz.get(i);
				if (oneCel.text().trim().toLowerCase().equals("musique")) {
					colMusique = i;

				} else if (oneCel.text().trim().toLowerCase().equals("sa")) {
					colAgeSexe = i;

				} else if (oneCel.text().trim().toLowerCase().equals("cheval")) {
					colNom = i;

				} else if (oneCel.text().trim().toLowerCase().equals("gains")
						|| oneCel.text().trim().toLowerCase().equals("valeur")) {
					colGains = i;

				} else if (oneCel.text().trim().toLowerCase().equals("cotes références")) {
					colProbPMU = i;

				} else if (oneCel.text().trim().toLowerCase().equals("dernières cotes")) {
					colProbGeny = i;
				}
			}

			Elements lignes = xPathTool.getElements(doc, "/div[@id='dt_partants']/table/tbody/tr");
			if (lignes != null && lignes.size() > 0) {

				for (int i = 0; i < lignes.size(); i++) {
					Element uneLigne = lignes.get(i);
					Elements cellules = uneLigne.select("td");

					Integer numCheval = null;
					String ageSexe = null;
					String musique = null;
					String nom = null;
					String gains = null;
					Float probPMU = null;
					Float probGeny = null;

					if (cellules != null && cellules.size() > 0) {
						for (int j = 0; j < cellules.size(); j++) {
							Element uneCellule = cellules.get(j);

							try {
								if (j == 0) {
									numCheval = Integer.parseInt(uneCellule.text().trim());
								} else if (j == colNom) {
									nom = uneCellule.text().trim();

								} else if (j == colAgeSexe) {
									ageSexe = uneCellule.text().trim().replaceAll("\\W", "");
									if (ageSexe.isBlank()) {
										ageSexe = cellules.get(j + 3).text().trim().replaceAll("\\W", "");
									}

								} else if (j == colMusique) {
									musique = uneCellule.text().trim();
									if (!musique.matches("^[a-zA-z][0-9].*")) {
										musique = cellules.get(j + 3).text().trim();
									}

								} else if (j == colGains) {
									gains = uneCellule.text().trim().replace(",", ".");
									try {
										Float.parseFloat(gains);
									} catch (Exception e) {
										gains = cellules.get(j + 3).text().trim().replace(",", ".");
									}

								} else if (j == colProbPMU) {
									String sProbPMU = uneCellule.text().trim().replace(",", ".");
									try {
										probPMU = Float.parseFloat(sProbPMU);
									} catch (Exception e) {
										sProbPMU = cellules.get(j + 3).text().trim().replace(",", ".");
										try {
											probPMU = Float.parseFloat(sProbPMU);
										} catch (Exception e2) {
											logger.debug("Error parsing probPMU : " + sProbPMU);
										}
									}

								} else if (j == colProbGeny) {
									String sProbGeny = uneCellule.text().trim().replace(",", ".");
									try {
										probGeny = Float.parseFloat(sProbGeny);
									} catch (Exception e) {
										sProbGeny = cellules.get(j + 3).text().trim().replace(",", ".");
										try {
											probGeny = Float.parseFloat(sProbGeny);
										} catch (Exception e2) {
											logger.debug("Error parsing probGeny : " + sProbGeny);
										}
									}

								}
							} catch (Exception e) {
								// logger.error("Erreur sur une ligne 'Partant' : " + e.getMessage());

							}

						} // fin for cellules

						logger.debug("Cvl : " + nom + " Num : " + numCheval + " ageSexe : " + ageSexe + " musique : "
								+ musique + " musique : " + musique + " gains : " + gains);

						if (numCheval != null) {
							if (partantsCourse == null) {
								partantsCourse = new EntitiesList();
							}

							Partant partantsBean = new Partant();
							partantsBean.setUrl(url);
							partantsBean.setCourseID(longCourse);
							partantsBean.setNumCheval(numCheval);
							partantsBean.setAgeSexe(ageSexe);
							partantsBean.setMusique(musique);
							partantsBean.setNomCheval(nom);
							partantsBean.setGains(gains);
							partantsBean.setProbablePMU(probPMU);
							partantsBean.setProbableGeny(probGeny);

							partantsCourse.add(partantsBean);
						}
					}
				} // fin for lignes

			}
			if (partantsCourse.size() == 0) {
				logger.warn("!!!!!!!!!!!!!!!!!!! Page sans partants : " + url);
			} else {
				logger.info("Adding " + partantsCourse.size() + " partants");
			}
		}

		return partantsCourse;
	}

}
