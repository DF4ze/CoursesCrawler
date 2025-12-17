package fr.ses10doigts.coursesCrawler.service.web;

import fr.ses10doigts.coursesCrawler.service.scheduler.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile({ "dev", "telegram" })
public class TelegramService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private TelegramClient telegramClient = null;

    // Lien vers les emoji
    // https://emojicopy.com/

    @Autowired
    public TelegramService(ConfigurationService configurationService) {
        telegramClient = new OkHttpTelegramClient(configurationService.getConfiguration().getBotToken());
        logger.info("Telegram service loaded");
    }

    public void sendMessage(Long chatId, String text) throws TelegramApiException {
        int maxTries = 3;
        boolean error = false;
        int count = 0;
        TelegramApiException exception = null;
        do {
            try {
                SendMessage sendMessage = new SendMessage(chatId + "", escapeMarkdownPreservingLinks(text));
                sendMessage.setParseMode("MarkdownV2");
                telegramClient.execute(sendMessage);
                if( error )
                    logger.info("Message well sent");
                error = false;

            }catch (TelegramApiException e){
                error = true;
                count++;
                exception = e;

                if( count < maxTries ){
                    logger.warn("Error sending Telegram message, sleep 1min then try again... Try {}", count);
                    try {
                        Thread.sleep(60*1000);
                    } catch (InterruptedException ignore) {
                        ;
                    }
                }
            }
        }while( error && count < maxTries );

        if( error ){
            logger.error("Unable to send Telegram message after {} tries : \n{}", count, escapeMarkdownPreservingLinks(text));
            throw exception;
        }
    }

    public static boolean isWebhookActive(String token) {
        try {
            String url = "https://api.telegram.org/bot" + token + "/getWebhookInfo";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            Scanner scanner = new Scanner(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }

            scanner.close();

            // Vérifie s'il y a une URL dans la réponse
            return response.toString().contains("\"url\":\"http");
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification du webhook : " + e.getMessage());
            return false;
        }
    }

    // Liste des caractères spéciaux à échapper
    private static final String ESCAPE_CHARS = "_*[]()~`>#+-=|{}.!";

    public static String escapeMarkdownPreservingLinks(String input) {
        if (input == null) return null;

        Pattern linkPattern = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");
        Matcher matcher = linkPattern.matcher(input);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Partie avant le lien : à échapper
            String before = input.substring(lastEnd, matcher.start());
            result.append(escapeText(before));

            // Partie du lien : ne PAS échapper
            result.append(matcher.group());

            lastEnd = matcher.end();
        }

        // Fin du texte après le dernier lien
        result.append(escapeText(input.substring(lastEnd)));

        return result.toString();
    }

    private static String escapeText(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (ESCAPE_CHARS.indexOf(c) != -1) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
