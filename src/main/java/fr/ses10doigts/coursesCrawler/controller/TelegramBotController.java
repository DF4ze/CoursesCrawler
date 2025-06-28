package fr.ses10doigts.coursesCrawler.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;

@Component
@Profile({ "dev", "telegram" })
public class TelegramBotController implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final Logger logger = LoggerFactory.getLogger(TelegramBotController.class);

	private ConfigurationService configurationService;
	@Autowired
	private TelegramService telegramService;

	private List<Long> authorizedChat = new ArrayList<>();
	{
		authorizedChat.add(1595302518L);
		authorizedChat.add(-4706435457L);
	}


	public TelegramBotController(ConfigurationService configurationService) {
		this.configurationService = configurationService;
		logger.info("Telegram controller loaded");
    }

    @Override
    public String getBotToken() {
		return configurationService.getConfiguration().getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {
			Message message = update.getMessage();
			String userMessage = message.getText();
			boolean startAndStop = false;
			
			logger.debug("Message id : " + message.getChatId());

			try{
				if (!authorizedChat.contains(message.getChatId())) {
					telegramService.sendMessage(message.getChatId(),
							"Vous n'êtes pas authorisé à échanger avec ce bot");
					return;
				}

				if (userMessage.startsWith("/")) {
					if (userMessage.startsWith("/timebefore")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							TelegramService.setTimeBefore(Integer.parseInt(param[1]));
							telegramService.sendMessage(message.getChatId(),
									"Temps de check avant l'heure de la course définit à " + param[1] + " minutes");
						} else {
							telegramService.sendMessage(message.getChatId(),
									"Temps de check avant l'heure de la course actuellement définit à "
											+ TelegramService.getTimeBefore() + " minutes");
						}

					} else if (userMessage.startsWith("/nbpartant")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							TelegramService.setNbPartant(Integer.parseInt(param[1]));
							telegramService.sendMessage(message.getChatId(),
									"Check courses avec >= " + param[1] + " partants");
						} else {
							telegramService.sendMessage(message.getChatId(),
									"Check courses avec >= " + TelegramService.getNbPartant() + " partants");
						}

					} else if (userMessage.startsWith("/go")) {

						String startDay = dayDate();
						String endDay = startDay;
						String[] dates = userMessage.split(" ");
						if (dates.length == 3) {
							if (dates[1].equals("stop")) {
								startAndStop = true;
								startDay = dates[2];
								endDay = dates[2];
							} else {
								startDay = dates[1];
								endDay = dates[2];
							}
						} else if (dates.length == 2) {
							if (dates[1].equals("stop"))
								startAndStop = true;
							else {
								startDay = dates[1];
								endDay = startDay;
							}
						}

						if (!isValidDate(startDay) || !isValidDate(endDay)) {
							telegramService.sendMessage(message.getChatId(),
									"Paramètres non reconnus après /go.\nDoit être soit une date, soit 2 dates (début/fin).\nAu format jj/mm/aaaa.");
							return;
						}

						telegramService.launchMainScheduledCrawl(startDay, endDay, startAndStop, message.getChatId());

//						crawlService.launchSurveyCrawl(startDay, endDay);
//
//						telegramService.manageEndOfCrawl(telegramClient, crawlService.getTreatment(),
//								message.getChatId(), startDay, endDay);
//
//						telegramService.sendMessage(message.getChatId(),
//								"Crawl du " + startDay + (startDay.equals(endDay) ? "" : " au " + endDay)
//										+ " lancé. Résultat dans env. 5-10 min.");
//
//						if (startAndStop)
//							crawlService.stopCurrentCrawl();
					}
				}
			} catch (TelegramApiException e) {
				logger.error("Error while sending message : " + e.getMessage());
			}
		}

    }

	private String dayDate() {
		LocalDate day = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return day.format(formatter);
	}


	private boolean isValidDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
		try {
			LocalDate.parse(dateStr, formatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}
}