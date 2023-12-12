package fr.ses10doigts.coursesCrawler.repository.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebCrawlingProxy {

	private static final Logger logger = LoggerFactory.getLogger(WebCrawlingProxy.class);

	public String getRawPage(String url) {

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null,
				new ParameterizedTypeReference<String>() {
				});
		String body = response.getBody();
		return body;
	}

	public String getUrlContents(String theUrl) {
		StringBuilder content = new StringBuilder();
		// Use try and catch to avoid the exceptions
		try {
			URL url = new URL(theUrl); // creating a url object
			URLConnection urlConnection = url.openConnection(); // creating a urlconnection object

			// wrapping the urlconnection in a bufferedreader
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			// reading from the urlconnection using the bufferedreader
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line + "\n");
			}
			bufferedReader.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		// System.out.println(content);
		return content.toString();
	}

	public Boolean connectivityTest() {

		Boolean connectivityTest = connectivityTest(0);
		// if no connexion, sleep 1 min and test again
		if (!connectivityTest) {
			logger.warn("Internet connexion lost");

		}

		while (!connectivityTest) {
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
			}
			connectivityTest = connectivityTest(0);
		}
		if (connectivityTest) {
			logger.info("Internet connexion is up");

		}

		return true;
	}

	private Boolean connectivityTest(int already) {
		List<String> urls = new ArrayList<>();
		urls.add("https://google.fr");
		urls.add("https://www.facebook.com/");
		urls.add("https://amazon.fr");
		urls.add("https://www.microsoft.com/fr-fr");
		urls.add("https://www.youtube.com/");

		String url = urls.get((int) (Math.random() * urls.size()));

		String urlContents = getUrlContents(url);
		boolean connected = false;
		if (!urlContents.isBlank()) {
			logger.debug("connexion vérifiée via " + url);
			connected = true;
		} else {
			// logger.warn("non connecté à internet via " + url);
			if (already < urls.size()) {
				connected = connectivityTest(++already);
			}
		}

		return connected;
	}
}