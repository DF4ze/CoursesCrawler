package fr.ses10doigts.coursesCrawler.service.course;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.course.EntitiesList;
import fr.ses10doigts.coursesCrawler.model.course.entity.Arrivee;
import fr.ses10doigts.coursesCrawler.model.course.entity.Cote;
import fr.ses10doigts.coursesCrawler.model.course.entity.Course;
import fr.ses10doigts.coursesCrawler.model.course.entity.Partant;
import fr.ses10doigts.coursesCrawler.model.course.entity.Rapport;
import fr.ses10doigts.coursesCrawler.service.course.tool.XPathTool;

@Component
public class GenyParser implements HtmlParser{

    private String url;
    private String body;
    private Document doc;
    private XPathTool xPathTool;

    private static final Logger	logger = LoggerFactory.getLogger(GenyParser.class);

    GenyParser() {
    }



    @Override
    public EntitiesList parse(String url, String body){
	this.url = url;
	this.body = body;

	EntitiesList beansList = new EntitiesList();
	init();

	EntitiesList beanListCourse = parse_course();
	if( beanListCourse != null) {
	    beansList.addAll( beanListCourse );
	}

	EntitiesList beansListRapport = parse_rapport();
	if( beansListRapport != null ) {
	    beansList.addAll( beansListRapport );
	}

	EntitiesList beansListArrive = parse_arrivee();
	if( beansListArrive != null ) {
	    beansList.addAll( beansListArrive );
	}

	EntitiesList beansListPartant = parse_partant();
	if( beansListPartant != null ) {
	    beansList.addAll( beansListPartant );
	}

	EntitiesList beansListCote = parse_cote();
	if( beansListCote != null ) {
	    beansList.addAll( beansListCote );
	}



	try {
	    Thread.sleep(10l);
	} catch (InterruptedException e) {}

	return beansList;
    }

    private void init(){
	doc = Jsoup.parse(body);
	xPathTool = new XPathTool();
    }

    @Override
    public Long parse_numCourse(){
	// extraction de l'ID de la course
	Pattern p = Pattern.compile(".*_c([0-9]+)$");
	Matcher m = p.matcher(url);
	boolean b = m.matches();
	Long longCourse = null;
	if( b ){
	    String numCourse = m.group(1);
	    try{
		longCourse = Long.parseLong(numCourse);
	    }catch(Exception e){
		logger.debug(e.getMessage());

	    }
	}

	return longCourse;
    }

    @Override
    public EntitiesList parse_course(){
	Long longCourse = parse_numCourse();

	// extraction de l'ID de la date
	Pattern p = Pattern.compile(".*([0-9]{4}-[0-9]{2}-[0-9]{2})-.*");
	Matcher m = p.matcher(url);
	boolean b = m.matches();
	String date = null;
	if( b ){
	    date = m.group(1);
	}
	logger.debug("Date : " + date);

	// extraction de la reunion
	String hippodrome = null;
	String reunion = null;
	Integer intReunion = null;
	Elements elements = xPathTool.getElements(doc, "/div[@class='nomReunion']");
	if( elements!=null && elements.size() > 0 ){
	    String txt = elements.get(0).text();
	    logger.debug("nomReunion : " + txt);

	    // hippodrome
	    p = Pattern.compile(".*:(.*)\\(R[0-9]+\\)");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		hippodrome = m.group(1);
		hippodrome = hippodrome.trim();
	    }
	    logger.debug("hippodrome : " + hippodrome);

	    // N??? reunion
	    p = Pattern.compile(".*:.*\\(R([0-9]+)\\)");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		reunion = m.group(1);
		try{
		    intReunion = Integer.parseInt(reunion);
		}catch(Exception e){
		    logger.debug(e.getMessage());
		}
	    }
	    logger.debug("reunion : " + intReunion);

	}

	// Extraction de la course
	Integer numCourse = null;
	String prix = null;
	elements = xPathTool.getElements(doc, "/div[@class='nomCourse']");
	if( elements!=null && elements.size() > 0 ){
	    String txt = elements.get(0).text();

	    p = Pattern.compile("([0-9]+).*-.*");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		String sNumCourse = m.group(1);
		try{
		    numCourse = Integer.parseInt(sNumCourse);
		}catch(Exception e){
		    logger.debug(e.getMessage());
		}
	    }
	    logger.debug("numCourse : " + numCourse);

	    p = Pattern.compile("[0-9]+.*- (.*)");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		prix = m.group(1);
		prix = prix.trim();
	    }
	    logger.debug("prix : " + prix);


	}

	// extract info course // Mont???|Steeple-chase|Attel???|Plat
	String type = null;
	String prime = null;
	String depart = "non";
	elements = xPathTool.getElements(doc, "/span[@class='infoCourse']");
	if( elements!=null && elements.size() > 0 ){
	    String txt = elements.get(0).text();
	    logger.debug("infoCourse : " + txt);

	    p = Pattern.compile(".*(Mont??|Steeple-chase|Attel??|Plat).*");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		type = m.group(1);
		type = type.trim();
	    }
	    logger.debug("type : " + type);


	    // somme des enjeux
	    p = Pattern.compile(".*-.(([0-9]+.)*[0-9]{3})???.*");
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		prime = m.group(1);
		prime = prime.replaceAll( " ", "" );
		//				try{
		//					intReunion = Integer.parseInt(prime);
		//				}catch(Exception e){}
	    }else	{
		logger.debug("prime no match ");

	    }
	    logger.debug("prime : " + prime);


	    // AutoStart
	    p = Pattern.compile(".*autostart.*", Pattern.CASE_INSENSITIVE);
	    m = p.matcher(txt);
	    b = m.matches();
	    if( b ){
		depart = "oui";
	    }
	    logger.debug("Autostart : " + depart);

	}

	Course course = null;
	if( longCourse 		!= null &&
		numCourse 	!= null &&
		date 		!= null &&
		hippodrome 	!= null &&
		prix 		!= null &&
		intReunion 	!= null &&
		depart	 	!= null &&
		type 		!= null ){

	    course = new Course();
	    course.setId( longCourse );
	    course.setCourse(numCourse);
	    course.setDate(date);
	    course.setHippodrome(hippodrome);
	    course.setPrix(prix);
	    course.setReunion(intReunion);
	    course.setType(type);
	    course.setPrime(prime);
	    course.setDepart(depart);
	}
	EntitiesList bl = null;
	if( course != null){
	    bl = new EntitiesList();
	    bl.add(course);
	    logger.info("Adding course : " + course);
	}

	return bl;
    }

    @Override
    public EntitiesList parse_rapport(){
	Long longCourse = parse_numCourse();

	EntitiesList listeRapports = null;
	if( url.indexOf("arrivee-et-rapports") != -1 && longCourse != null){
	    logger.debug("=================================== Rapports");
	    logger.debug(url);

	    Elements nbTableaux = xPathTool.getElements(doc, "/table[@id='lesSolos']/tbody table");
	    boolean oldStyle = false;
	    if( nbTableaux != null && nbTableaux.size() == 3) {
		oldStyle = true;
	    }

	    // ancien tableau
	    Elements lignes = null;
	    if( oldStyle ){
		lignes = xPathTool.getElements(doc, "/table[@id='lesSolos']/tbody/tr[0]/td[1]/table/tbody/tr");

		if( lignes == null || lignes.size() == 0 ) {
		    lignes = xPathTool.getElements(doc, "/table[@id='lesSolos']/tbody/tr[0]/td[2]/table/tbody/tr");
		}
	    }else{
		// nouveau tableau
		lignes = xPathTool.getElements(doc, "/table[@id='lesSolos']/tbody/tr[0]/td[3]/table/tbody/tr");

	    }

	    if( lignes!=null && lignes.size() > 0 ){

		Rapport rapport = new Rapport();
		listeRapports = new EntitiesList();

		for( int i=1; i< lignes.size(); i++ ){
		    Element uneLigne = lignes.get(i);
		    Elements cellules = uneLigne.select("td");

		    Integer numCheval = null;
		    Double gain = null;
		    try{
			if( cellules!=null && cellules.size() > 0 ){
			    for( int j=0; j < (oldStyle? cellules.size()/*-1*/ : cellules.size()); j++ ){
				Element uneCellule = cellules.get(j);

				if( j == 0 ){

				    String txt = uneCellule.text().trim();
				    txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");
				    numCheval = Integer.parseInt( txt );

				}else if( j == 1 ){
				    String txt = uneCellule.text().trim();
				    txt = txt.replace(",", ".");
				    txt = txt.replace("<b>", "");
				    txt = txt.replace("</b>", "");
				    txt = txt.replace(" ", "");
				    txt = txt.replace("???", "");
				    txt = txt.replaceAll("[^\\d\\.\\,\\-]", "");


				    gain = Double.parseDouble(txt);
				}
			    }// for cellules

			    if( i == 1 ){
				rapport = new Rapport();
				rapport.setNumCheval(numCheval);
				rapport.setCourseID(longCourse);
				rapport.setGagnant(gain);
				rapport.setArrivee(1);

			    }else if( i == 2 ){
				rapport.setPlace(gain);
				logger.debug("1er => N" + rapport.getNumCheval() + " G:" + rapport.getGagnant() + " P:"
					+ rapport.getPlace());

				listeRapports.add(rapport);
				// logger.info("Adding rapport : " + rapport);

			    }else{
				rapport = new Rapport();

				rapport.setCourseID(longCourse);
				rapport.setNumCheval(numCheval);
				rapport.setArrivee(i-1);
				rapport.setPlace(gain);

				logger.debug(rapport.getArrivee() + "eme => N" + rapport.getNumCheval() + " P:"
					+ rapport.getPlace());

				listeRapports.add(rapport);
				// logger.info("Adding rapport : " + rapport);


			    }
			}
		    }catch( Exception e  ){ // pour les Parses Exceptions
			logger.error("Erreur RAPPORT : " + e.getMessage());

		    }
		}// for lignes
		logger.info("Added " + listeRapports.size() + " rapports");

		// nouveau tableau
	    }

	}

	return listeRapports;
    }

    @Override
    public EntitiesList parse_arrivee(){
	Long longCourse = parse_numCourse();

	EntitiesList listeArrivees = null;
	if( url.indexOf("arrivee-et-rapports") != -1 && longCourse != null){
	    logger.debug("=================================== Arrivee");
	    logger.debug(url);
	    Elements nbTableaux = xPathTool.getElements(doc, "/table[@id='arrivees']/tbody");

	    Elements lignes = null;
	    if( nbTableaux != null && nbTableaux.size() > 0 ) {
		lignes = xPathTool.getElements(doc, "/table[@id='arrivees']/tbody/tr");
	    }

	    if( lignes != null ){
		for( int i=1; i< lignes.size(); i++ ){
		    Element uneLigne = lignes.get(i);
		    Elements cellules = uneLigne.select("td");

		    String nomCheval = null;
		    Integer numCheval = null;
		    Integer placeCheval = null;
		    try{
			if( cellules!=null && cellules.size() > 0 ){
			    for( int j=0; j < cellules.size(); j++ ){
				Element uneCellule = cellules.get(j);

				if( j == 0 ){
				    String content = uneCellule.text();
				    placeCheval = Integer.parseInt(content);

				}else if( j==1 ){
				    String content = uneCellule.text();
				    numCheval = Integer.parseInt(content);

				}else if( j==2 ){
				    Elements justeNom = uneCellule.select("a");
				    nomCheval = justeNom.text().trim();

				} else {
				    break;
				}
			    }

			    if( placeCheval != null && numCheval != null){
				if( listeArrivees == null ) {
				    listeArrivees = new EntitiesList();
				}
				logger.debug("Course : " + longCourse + " Place : " + placeCheval + " Numero : "
					+ numCheval + " Nom : " + nomCheval);

				listeArrivees.add(new Arrivee(longCourse, placeCheval, numCheval, nomCheval));
			    }
			}
		    }catch(Exception e){
			// logger.debug("Ligne N " + i + " Error: " + e.getMessage());

		    }
		}

	    }
	    logger.info("Adding " + listeArrivees.size() + " arrivees");
	}

	return listeArrivees;
    }




    @Override
    public EntitiesList parse_cote(){
	Long longCourse = parse_numCourse();

	EntitiesList cotesCourse = null;

	if(  url.indexOf("/cotes/") != -1 && longCourse != null){
	    logger.debug("=================================== Cote");
	    logger.debug(url);

	    Elements lignes = xPathTool.getElements(doc, "/div[@id='div_tableau_cotes']/table/tbody/tr");

	    if( lignes!=null && lignes.size() > 0 ){

		for( int i=0; i< lignes.size(); i++ ){
		    Element uneLigne = lignes.get(i);
		    Elements cellules = uneLigne.select("td");

		    // logger.debug("Line N??" + i + " content : " + cellules.text());

		    Integer numCheval = null;
		    Float enjeuxDepart = null;
		    Float enjeuxAvant = null;
		    Float coteAvant = null;
		    Float coteDepart = null;
		    if (cellules != null && cellules.size() > 0 && !cellules.text().isBlank()) {
			for( int j=0; j < cellules.size(); j++ ){
			    Element uneCellule = cellules.get(j);

			    // logger.debug("Treating cell N??" + j + " content : " + uneCellule.text());
			    try{
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


			    }catch( Exception e ){
				// Trop de spam si affich??
				//				logger.error("Erreur sur la ligne 'cote' : " + uneLigne.text() + " Message :"
				//					+ e.getMessage());

			    }

			}// fin for cellules

			if (numCheval != null) {
			    logger.debug("Cvl : " + numCheval + " cote : " + coteDepart + " enjeux : " + enjeuxDepart);
			}

			if (numCheval != null && coteDepart != null && enjeuxDepart != null) {
			    if( cotesCourse == null ) {
				cotesCourse = new EntitiesList();
			    }

			    Cote cote = new Cote();
			    cote.setCourseID(longCourse);
			    cote.setNumCheval(numCheval);
			    cote.setCoteDepart(coteDepart);
			    cote.setCoteAvant(coteAvant);
			    cote.setEnjeuxDepart(enjeuxDepart);
			    cote.setEnjeuxAvant(enjeuxAvant);

			    cotesCourse.add(cote);


			}
		    }
		}//fin for lignes

		logger.info("Adding " + cotesCourse.size() + " cotes");

	    }
	}

	return cotesCourse;
    }

    @Override
    public EntitiesList parse_partant(){
	Long longCourse = parse_numCourse();

	EntitiesList partantsCourse = null;

	if(  url.indexOf("/partants-pmu/") != -1 && longCourse != null){
	    logger.debug("=================================== Partants");
	    logger.debug(url);

	    Elements celz = xPathTool.getElements(doc, "/div[@id='dt_partants']/table/thead/tr/th");
	    int colMusique = 6;
	    int colAgeSexe = 2;
	    int colNom = 1;
	    int colGains = 1;
	    for(int i = 0; i< celz.size(); i++){
		Element oneCel = celz.get(i);
		if( oneCel.text().trim().toLowerCase().equals( "musique" ) ){
		    colMusique = i;
		}
		if( oneCel.text().trim().toLowerCase().equals( "sa" ) ){
		    colAgeSexe = i;
		}
		if( oneCel.text().trim().toLowerCase().equals( "cheval" ) ){
		    colNom = i;
		}
		if (oneCel.text().trim().toLowerCase().equals("gains")
			|| oneCel.text().trim().toLowerCase().equals("valeur")) {

		    colGains = i;
		}
	    }


	    Elements lignes = xPathTool.getElements(doc, "/div[@id='dt_partants']/table/tbody/tr");
	    if( lignes!=null && lignes.size() > 0 ){

		for( int i=0; i< lignes.size(); i++ ){
		    Element uneLigne = lignes.get(i);
		    Elements cellules = uneLigne.select("td");

		    Integer numCheval = null;
		    String ageSexe = null;
		    String musique = null;
		    String nom = null;
		    String gains = null;
		    if( cellules!=null && cellules.size() > 0 ){
			for( int j=0; j < cellules.size(); j++ ){
			    Element uneCellule = cellules.get(j);

			    try{
				if (j == 0) {
				    numCheval = Integer.parseInt( uneCellule.text().trim() );
				} else if (j == colNom) {
				    nom = uneCellule.text().trim();

				}else if( j == colAgeSexe ){
				    ageSexe = uneCellule.text().trim().replaceAll("\\W", "");
				    if (ageSexe.isBlank()) {
					ageSexe = cellules.get(j + 3).text().trim().replaceAll("\\W", "");
				    }

				}else if( j == colMusique ){
				    musique = uneCellule.text().trim();
				    if (!musique.matches("^[a-zA-z][0-9].*")) {
					musique = cellules.get(j + 3).text().trim();
				    }

				}else if( j == colGains ){
				    gains = uneCellule.text().trim().replace(",", ".");
				    try {
					Float.parseFloat(gains);
				    } catch (Exception e) {
					gains = cellules.get(j + 3).text().trim().replace(",", ".");
				    }

				}
			    }catch( Exception e ){
				// logger.error("Erreur sur une ligne 'Partant' : " + e.getMessage());

			    }

			}// fin for cellules

			logger.debug("Cvl : " + nom + " Num : " + numCheval + " ageSexe : " + ageSexe + " musique : "
				+ musique + " musique : " + musique + " gains : " + gains);

			if( numCheval != null ){
			    if (partantsCourse == null) {
				partantsCourse = new EntitiesList();
			    }

			    Partant partantsBean = new Partant();
			    partantsBean.setCourseID(longCourse);
			    partantsBean.setNumCheval(numCheval);
			    partantsBean.setAgeSexe(ageSexe);
			    partantsBean.setMusique(musique);
			    partantsBean.setNomCheval(nom);
			    partantsBean.setGains(gains);

			    partantsCourse.add(partantsBean);
			}
		    }
		}//fin for lignes
		logger.info("Adding " + partantsCourse.size() + " partants");
	    }
	}


	return partantsCourse;
    }


}
