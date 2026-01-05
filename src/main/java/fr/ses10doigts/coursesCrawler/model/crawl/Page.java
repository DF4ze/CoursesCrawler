package fr.ses10doigts.coursesCrawler.model.crawl;

import fr.ses10doigts.coursesCrawler.service.scrap.TypePage;
import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
public class Page implements Comparable<Page> {

    private String	url;
    private Set<String>	urlsContained = new HashSet<>();
    private Integer	hop;
    private Integer	nbRetry;
    private TypePage type;

    @Override
    public int compareTo(Page page) {
	return getUrl().compareTo(page.getUrl());
    }

    @Override
    public int hashCode() {
	return Objects.hash(url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Page other = (Page) obj;
        return Objects.equals(url, other.url);
    }

}
