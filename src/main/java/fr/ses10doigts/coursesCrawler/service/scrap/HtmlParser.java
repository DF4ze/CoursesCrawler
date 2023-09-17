package fr.ses10doigts.coursesCrawler.service.scrap;

import fr.ses10doigts.coursesCrawler.model.scrap.EntitiesList;

public interface HtmlParser {


    public EntitiesList parse( String url, String body );

}
