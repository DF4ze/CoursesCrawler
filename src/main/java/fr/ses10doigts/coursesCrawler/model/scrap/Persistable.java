package fr.ses10doigts.coursesCrawler.model.scrap;

import java.io.Serializable;

public interface Persistable <T extends Serializable> {
	public T getId();
}
