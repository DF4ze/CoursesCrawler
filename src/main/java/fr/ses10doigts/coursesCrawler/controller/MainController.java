package fr.ses10doigts.coursesCrawler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.scrap.RefactorerService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.ExcelExtractorService;

@Controller
public class MainController {

	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private CrawlService crawlService;
	@Autowired
	private RefactorerService refactoService;
	@Autowired
	private ExcelExtractorService excelService;

	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("configuration", configurationService.getConfiguration());
		model.addAttribute("crawlReport", crawlService.getReportCurrentCrawl());
		model.addAttribute("refactReport", refactoService.getReportCurrentRefact());

		return "home";
	}

	@GetMapping("/generate")
	public String generateXls(Model model) {

		excelService.extractCourseCompletes();

		model.addAttribute("configuration", configurationService.getConfiguration());
		model.addAttribute("crawlReport", crawlService.getReportCurrentCrawl());
		model.addAttribute("refactReport", refactoService.getReportCurrentRefact());

		return "redirect:/";
	}

	@PostMapping(value = "/", params = "action=save")
	public ModelAndView saveConfig(@ModelAttribute Configuration dto) {
		logger.info("User ask to save config : " + dto);

		configurationService.saveConfiguration(dto);
		return new ModelAndView("redirect:/");
	}

	@PostMapping(value = "/", params = "action=launch")
	public ModelAndView launchCrawl(@ModelAttribute Configuration dto) {
		logger.info("User ask to launch with config : " + dto);

		configurationService.saveConfiguration(dto);
		Report crawlReport = crawlService.manageLaunch();

		ModelAndView mav = new ModelAndView("home");
		mav.addObject("configuration", configurationService.getConfiguration());
		mav.addObject("crawlReport", crawlReport);
		mav.addObject("refactReport", refactoService.getReportCurrentRefact());

		return mav;
	}

	@PostMapping(value = "/", params = "action=stop")
	public ModelAndView stop() {
		logger.info("User ask to stop ");

		crawlService.stopCurrentCrawl();
		refactoService.stopRefactorer();

		ModelAndView mav = new ModelAndView("home");
		mav.addObject("configuration", configurationService.getConfiguration());
		mav.addObject("crawlReport", crawlService.getReportCurrentCrawl());
		mav.addObject("refactReport", refactoService.getReportCurrentRefact());

		return mav;
	}

	@PostMapping(value = "/", params = "action=test")
	public ModelAndView test() {
		logger.info("Testing connectivity ");

		crawlService.testConnectivity();

		ModelAndView mav = new ModelAndView("home");
		mav.addObject("configuration", configurationService.getConfiguration());
		mav.addObject("crawlReport", crawlService.getReportCurrentCrawl());
		mav.addObject("refactReport", refactoService.getReportCurrentRefact());

		return mav;
	}

	@PostMapping(value = "/", params = "action=generate")
	public ModelAndView generate(@ModelAttribute Configuration dto) {
		logger.info("User ask to generate ");

		String urls = configurationService.generateUrlFromDates(dto.getStartGenDate(), dto.getEndGenDate());

		dto.setTxtSeeds(urls);

		configurationService.saveConfiguration(dto);

		ModelAndView mav = new ModelAndView("home");
		mav.addObject("configuration", dto);
		mav.addObject("crawlReport", crawlService.getReportCurrentCrawl());
		mav.addObject("refactReport", refactoService.getReportCurrentRefact());

		return mav;
	}

}
