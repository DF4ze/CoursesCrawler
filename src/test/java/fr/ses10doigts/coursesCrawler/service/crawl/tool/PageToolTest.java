package fr.ses10doigts.coursesCrawler.service.crawl.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import jakarta.annotation.Generated;

@Generated(value = "org.junit-tools-1.1.0")
public class PageToolTest {

    private PageTool createTestSubject() {
	return new PageTool();
    }

    @Test
    public void testFindUrl() throws Exception {
	PageTool testSubject;
	Page seed = new Page("http://test.fr", 1);
	List<String> authorised = new ArrayList<>();
	List<Page> result;

	authorised.add("foo");
	authorised.add("bar");

	// @formatter:off
	String textContent = "<head>injection of some script <script href=\"http://scripts.com\"/></head>" +
		"<body>"+
		"<a href=\"http://undesiredlink.com\">" +
		"<a href=\"http://desired_foo_link.com\">"+
		"<a href=\"http://desired_bar_link.com\">" +
		"</body>";
	// @formatter:on

	// default test
	testSubject = createTestSubject();
	result = testSubject.findUrlsInContent(seed, textContent, authorised);

	assertEquals(result.size(), 2);
	for (Page page : result) {
	    assertEquals(page.getHop(), seed.getHop() + 1);
	}

    }

    @Test
    public void testUrl2Pages() throws Exception {
	PageTool tool;
	List<String> urls = new ArrayList<>();
	urls.add("http://google.fr");

	List<Page> result;

	// default test
	tool = createTestSubject();
	result = tool.url2Pages(urls);

	assertEquals(result.size(), 1);
	for (Page page : result) {
	    assertEquals(page.getHop(), 0);
	    assertEquals(page.getNbRetry(), 0);
	    assertEquals(page.getUrl(), "http://google.fr");
	}
    }

    @Test
    public void testGetBaseUrl() {
	PageTool tool;
	String url = "http://test.fr/folder/subfolder";

	tool = createTestSubject();
	String base = tool.getBaseUrl(url);

	assertEquals(base, "http://test.fr");
    }
}