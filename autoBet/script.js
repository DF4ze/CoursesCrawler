const puppeteer = require('puppeteer');

function delay(time) {
   return new Promise(function(resolve) { 
       setTimeout(resolve, time)
   });
}

const [,, courseID, bet, cvlNb, user, pwd] = process.argv;

(async () => {
	const browser = await puppeteer.launch({
		headless: true,          // cacher le navigateur?
		args: ['--no-sandbox', '--start-maximized'],  // utile sur certains VPS,
	});

	let page = await browser.newPage();

	await page.setViewport({
	  width: 1920,
	  height: 1080
	});  

  try {
    console.log("=== Script démarré ===");
	console.log("Params :");
	console.log("- courseID: "+courseID);
	console.log("- bet: "+bet);
	console.log("- num cvl: "+cvlNb);
	console.log("- user: "+user);
	console.log("- pwd: "+pwd);

    // 1. Login
    console.log("Ouverture page de login...");
    await page.goto('https://www.genybet.fr/', { waitUntil: 'networkidle2' });	
    await await delay(500);    
    await page.screenshot({ path: 'step1_login.png' });

	console.log("Fermeture popup cookies (si présente)");

	try {
		await page.waitForSelector('#didomi-notice-agree-button', {
			timeout: 5000,     // attend jusqu’à 5s
			visible: true
		});
		await page.click('#didomi-notice-agree-button');
		console.log("Popup cookies fermée");
	} catch (e) {
		console.log("Popup cookies absente (ou déjà fermée)");
	}	

    
    // Phone
    var inPhoneLbl = 'input[placeholder="Email"]';
    await page.type(inPhoneLbl, user, { delay: 50 });
    await page.waitForSelector(inPhoneLbl);
    await page.$eval(inPhoneLbl, (el, value) => {
      el.value = value;
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    }, user);
    console.log(inPhoneLbl);
    
    // Password
    var inPwdLbl = 'input[placeholder="Mot de passe"]';
    await page.type(inPwdLbl, pwd, { delay: 50 });
    await page.waitForSelector(inPwdLbl);
    await page.$eval(inPwdLbl, (el, value) => {
      el.value = value;
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    }, pwd);
    console.log(inPwdLbl);
    
    await page.screenshot({ path: 'step1_loggin_written.png' });


	
    console.log("Click signin");
    await page.waitForSelector('#signin', { visible: true });
    await page.click('#signin');
	await await delay(1000);
	
	console.log("Page de la course", courseID);
	await page.goto('https://www.genybet.fr/courses/partants-pronostics/'+courseID, { waitUntil: 'networkidle2' });	
    await await delay(500); 
	
	
	const numero = cvlNb;
	console.log("Select chvl", cvlNb);
	await page.waitForSelector(`table.bet tr#partant-${numero} td.checkbox-SIMPLE_GAGNANT input`);
	await page.click(`table.bet tr#partant-${numero} td.checkbox-SIMPLE_GAGNANT input`);
	
	await await delay(500); 
	
	console.log("betAmount", bet);
	await page.waitForSelector('input.betAmount.ca');
	await page.click('input.betAmount.ca', { clickCount: 3 });
	await page.type('input.betAmount.ca', bet);
	await await delay(500); 

	// Il faut cliquer 2 fois sur le bouton car le 1er retire le focus et met a jour le paris
	await page.waitForSelector('button.bet-button', { visible: true });
	await page.click('button.bet-button');
	await await delay(500); 

	console.log("Validate");
	await page.waitForSelector('button.bet-button', { visible: true });
	await page.click('button.bet-button');
	await await delay(500); 


	
	
	
    console.log("=== Succès: action terminée ===");
	
	// Garde le script ouvert
	//await new Promise(() => {});
	
  } catch (err) {
    console.error("Erreur rencontrée:", err);
    await page.screenshot({ path: 'error.png' });
  } finally {
    await browser.close();
  }
})();
