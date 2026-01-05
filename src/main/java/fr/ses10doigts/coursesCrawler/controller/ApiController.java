package fr.ses10doigts.coursesCrawler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.scrap.RefactorerService;

@RestController
public class ApiController {

    @Autowired
    private CrawlService crawlService;
    @Autowired
    private RefactorerService refactoService;


    @GetMapping("/launchRefacto")
    public void launchRefacto() {
	refactoService.launchRefactorer();
    }


    @GetMapping("/report")
    public Report reportCrawl() {

	return crawlService.getReportCurrentCrawl();

    }



}