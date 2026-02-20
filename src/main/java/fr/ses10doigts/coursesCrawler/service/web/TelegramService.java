package fr.ses10doigts.coursesCrawler.service.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Profile({ "devWithTelegram", "telegram", "prod" })
public class TelegramService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final TelegramClient telegramClient;

    // Lien vers les emoji
    // https://emojicopy.com/

    private enum SendType {
        TEXT, PHOTO, DOCUMENT
    }

    @Autowired
    public TelegramService(ConfigurationService configurationService) {
        telegramClient = new OkHttpTelegramClient(configurationService.getConfiguration().getBotToken());
        logger.info("Telegram service loaded");
    }

    public void sendMessage(Long chatId, String text) {
        send(chatId, text, null, SendType.TEXT);
    }

    public void sendPhoto(Long chatId, String photoPath)  {
        sendPhoto(chatId, photoPath, null);
    }

    public void sendPhoto(Long chatId, String photoPath, String caption)  {
        send(chatId, caption, photoPath, SendType.PHOTO);
    }

    public void sendFile(Long chatId, String documentPath)  {
        sendFile(chatId, documentPath, null);
    }

    public void sendFile(Long chatId, String documentPath, String caption)  {
        send(chatId, caption, documentPath, SendType.DOCUMENT);
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
            logger.error("Erreur lors de la vérification du webhook : {}", e.getMessage());
            return false;
        }
    }

    private void send( Long chatId, String text, String filePath, SendType type ){

        if( filePath != null && !isExistingFile(filePath) ){
            logger.warn("File {} not found, skipping message {}", filePath, text!=null?text:"");
            return;
        }

        int maxTries = 3;
        boolean error = false;
        int count = 0;
        do {
            try {
                switch (type) {
                    case TEXT:
                        SendMessage sendMessage = new SendMessage(chatId + "", escapeMarkdownPreservingLinks(text));
                        sendMessage.setParseMode("MarkdownV2");
                        telegramClient.execute(sendMessage);
                        break;

                    case PHOTO:
                        SendPhoto sendPhoto = new SendPhoto(
                                chatId.toString(),
                                new InputFile(new File(filePath))
                        );
                        sendPhoto.setCaption(escapeMarkdownPreservingLinks(text));

                        telegramClient.execute(sendPhoto);
                        break;

                    case DOCUMENT:
                        SendDocument sendDocument = new SendDocument(
                                chatId.toString(),
                                new InputFile(new File(filePath))
                        );
                        sendDocument.setCaption(escapeMarkdownPreservingLinks(text));

                        telegramClient.execute(sendDocument);
                        break;
                }


                if( error )
                    logger.info("Message well sent");
                error = false;

            }catch (TelegramApiException e){
                error = true;
                count++;

                if( count < maxTries ){
                    logger.warn("Error sending Telegram message, sleep 1min then try again... Try {}", count);
                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException ignore) {
                        //
                    }
                }
            }
        }while( error && count < maxTries );

        if( error ){
            logger.error("Unable to send Telegram message after {} tries : \n{}", count, escapeMarkdownPreservingLinks(text));

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

    private static boolean isExistingFile(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return false;
        }

        Path path = Paths.get(pathStr);
        return Files.exists(path) && Files.isRegularFile(path);
    }
}
