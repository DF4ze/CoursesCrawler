package fr.ses10doigts.coursesCrawler.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import fr.ses10doigts.coursesCrawler.service.misc.LogAccessService;
import fr.ses10doigts.coursesCrawler.service.scheduler.CrawlJobCheckerService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
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

import fr.ses10doigts.coursesCrawler.service.scheduler.SchedulerService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;

@Component
@Profile({ "dev", "telegram" })
public class TelegramBotController implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final Logger logger = LoggerFactory.getLogger(TelegramBotController.class);

	private final ConfigurationService configurationService;
	@Autowired
	private SchedulerService schedulerService;
	@Autowired
	private TelegramService telegramService;
	@Autowired
	private LogAccessService logService;

	private final List<Long> authorizedChat = new ArrayList<>();
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

					boolean isValueChanged = false;
					if (userMessage.startsWith("/help")) {

							telegramService.sendMessage(message.getChatId(),
									"Une commande commence toujours par un '/'. Elle est suivie d'un mot clé. Elle peut prendre un ou plusieurs paramètres (indiqué entre [ ]).\n" +
											"La norme suivie est : \n" +
											"* Si pas de paramètre, affiche simplement la/les valeur(s) actuelle(s)\n" +
											"* Si un ou plusieurs paramètres, enregistrement de la/les nouvelle(s) valeur(s)\n\n" +

											"/help\n" +
											"➡\uFE0F Affiche cette aide \uD83D\uDE1C\n\n" +
											"/timebefore [temps (en min)]\n" +
											"➡\uFE0F Temps avant la course pour un check\n\n" +
											"/pourcent [X]\n" +
											"➡\uFE0F Somme des 3 meilleurs favoris >= pourcent\n\n" +
											"/type [Plat, Attelé, ...]\n" +
											"➡\uFE0F Type de course\n\n" +
											"/nbpartantmin [X]\n" +
											"➡\uFE0F Nombre partants min\n\n" +
											"/nbpartantmax [X]\n" +
											"➡\uFE0F Nombre partants max\n\n" +
											"/reunionmax [X]\n" +
											"➡\uFE0F N° de réunion max\n\n" +
											"/planned\n" +
											"➡\uFE0F Courses qui vont être vérifiées aujourd'hui\n\n" +
											"/verbose\n" +
											"➡\uFE0F Quantité de messages envoyés :\n" +
											"   * HIGH : Envoi des message à chaque check même quand la course est hors statistique\n" +
											"   * LOW : N'envoi des messages que quand la course est à jouer.\n\n" +
											"/go [jj/mm/aaaa] [jj/mm/aaaa jj/mm/aaaa]\n " +
												"➡\uFE0F Force le lancement de la vérification des courses pour la journée si pas de paramètre.\n" +
												"➡\uFE0F Pour le jour donné si 1 paramètre.\n" +
												"➡\uFE0F Pour toutes les dates comprises entre les 2 dates données\n\n" +
											"/log [X]\n" +
											"➡\uFE0F Affiche les logs (C'est technique, pour moi)");


					} else if (userMessage.startsWith("/timebefore")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							schedulerService.setTimeBefore(Integer.parseInt(param[1]));
							telegramService.sendMessage(message.getChatId(),
									"Temps de check avant l'heure de la course définit à " + param[1] + " minutes");
							isValueChanged = true;
						} else {
							telegramService.sendMessage(message.getChatId(),
									"Temps de check avant l'heure de la course actuellement définit à "
											+ schedulerService.getTimeBefore() + " minutes");
						}

					} else if (userMessage.startsWith("/verbose")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							try {
								Verbose verbose = Verbose.fromString(param[1]);
								configurationService.getConfiguration().setTelegramVerbose(verbose);
								telegramService.sendMessage(message.getChatId(),
										"L'envoi de message Telegram est définit à " + param[1] );
								isValueChanged = true;
							}catch (Exception e){
								telegramService.sendMessage(message.getChatId(),
										"Une erreur s'est produite, certainement " + param[1] + " n'est pas connu.\nUtiliser 'HIGH' ou 'LOW' seulement");
							}

						} else {
							telegramService.sendMessage(message.getChatId(),
									"L'envoi de message Telegram est définit à "
											+ configurationService.getConfiguration().getTelegramVerbose());
						}

					} else if (userMessage.startsWith("/pourcent")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							String f = param[1].replace(",", ".");
							SchedulerService.setPourcentFavoris(Float.parseFloat(f));
							telegramService.sendMessage(message.getChatId(),
									"Somme des 3 favoris définit >= " + param[1] + "%");
							isValueChanged = true;
						} else {
							telegramService.sendMessage(message.getChatId(),
									"Somme des 3 favoris actuellement définit à "
											+ SchedulerService.getPourcentFavoris() + "%");
						}

					} else if (userMessage.startsWith("/type")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							SchedulerService.setTypeCourse(param[1]);
							telegramService.sendMessage(message.getChatId(),
									"Type de course définit à '" + param[1]+"'");
							isValueChanged = true;
						} else {
							telegramService.sendMessage(message.getChatId(),
									"Type de course actuellement définit à '"
											+ SchedulerService.getTypeCourse()+"'");
						}

					} else if (userMessage.startsWith("/nbpartantmin")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							SchedulerService.setNbPartantMin(Integer.parseInt(param[1]));
							isValueChanged = true;
						}
						telegramService.sendMessage(message.getChatId(),
								"Check courses entre " + SchedulerService.getNbPartantMin()
										+ " et " + SchedulerService.getNbPartantMax() + " partants");


					} else if (userMessage.startsWith("/nbpartantmax")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							SchedulerService.setNbPartantMax(Integer.parseInt(param[1]));
							isValueChanged = true;
						}
						telegramService.sendMessage(message.getChatId(),
								"Check courses entre " + SchedulerService.getNbPartantMin()
										+ " et " + SchedulerService.getNbPartantMax() + " partants");

					} else if (userMessage.startsWith("/reunionmax")) {
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							SchedulerService.setNbReunionMax(Integer.parseInt(param[1]));
							isValueChanged = true;
						}
						telegramService.sendMessage(message.getChatId(),
								"Numéro de réunion max : " + SchedulerService.getNbReunionMax()
						);

					} else if (userMessage.startsWith("/log")) {
						int nb = 50;
						String[] param = userMessage.split(" ");
						if (param.length == 2) {
							nb = Integer.parseInt(param[1]);
						}
						List<String> lines = logService.getLastLines(nb);
						StringBuilder msg = new StringBuilder();
						for (String line : lines){
							msg.append("\uD83D\uDD35").append(line).append("\n");
						}
						telegramService.sendMessage(message.getChatId(), msg.toString());

					} else if (userMessage.startsWith("/planned")) {
						List<ScheduledTask> scheduledTasks = schedulerService.getFutureScheduledTasks();

						StringBuilder msg = new StringBuilder();
						if( scheduledTasks.isEmpty() ){
							msg.append("\uD83D\uDC4E  Pas/Plus de courses à venir pour aujourd'hui");

						}else {
							msg.append("\uD83D\uDCC6 Courses qu'il reste à checker aujourd'hui : \n\n");
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
							for (ScheduledTask task : scheduledTasks) {
								msg.append(task.getCourseDescription()).append("\n");
								String hour = task.getPlannedExecution().format(formatter);
								msg.append("⏰ Planifiée à ").append(hour).append("\n\n");
							}
						}

						telegramService.sendMessage(message.getChatId(), msg.toString());

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

						schedulerService.launchMainScheduledCrawl(startDay, endDay, startAndStop, message.getChatId());


					}

					if(isValueChanged){
                        logger.info("User changed value : {}", userMessage);
					}
				}
			} catch (TelegramApiException e) {
                logger.error("Error while sending message : {}", e.getMessage());
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