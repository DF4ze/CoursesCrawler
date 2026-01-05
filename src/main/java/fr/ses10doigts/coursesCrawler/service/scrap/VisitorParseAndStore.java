package fr.ses10doigts.coursesCrawler.service.scrap;


import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.scrap.EntitiesList;

@Component
public class VisitorParseAndStore implements HtmlVisitor {

    @Autowired
    RepositoryService repository;

    @Autowired
    HtmlParser parser;


    @Override
    public EntitiesList indexify(Page page, String archiveBody) {

        EntitiesList bl = parser.parse(page, archiveBody);

        if (bl != null && !bl.get().isEmpty()) {
            repository.addAll(bl);
        }

        return bl;
    }
}
