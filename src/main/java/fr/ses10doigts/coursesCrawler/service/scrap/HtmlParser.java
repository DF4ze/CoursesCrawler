package fr.ses10doigts.coursesCrawler.service.scrap;

import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import fr.ses10doigts.coursesCrawler.model.scrap.EntitiesList;

public interface HtmlParser {


    EntitiesList parse(Page page, String body );

}
