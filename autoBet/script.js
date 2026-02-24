const puppeteer = require('puppeteer');
const fs = require('fs');

/////////// Lock system
const LOCK_FILE = 'bet.lock';

function acquireLock() {
    if (fs.existsSync(LOCK_FILE)) {
        console.error("❌ Script déjà en cours d'exécution.");
        process.exit(1);
    }

    fs.writeFileSync(LOCK_FILE, process.pid.toString());
}

function releaseLock() {
    if (fs.existsSync(LOCK_FILE)) {
        fs.unlinkSync(LOCK_FILE);
    }
}

////////// Accounts
function loadAccounts() {
    if (!fs.existsSync('/home/oklm/courses/auto_bet/accounts.json')) {
        throw new Error("accounts.json manquant");
    }

    const raw = fs.readFileSync('/home/oklm/courses/auto_bet/accounts.json');
    return JSON.parse(raw);
}

function getAccountByPseudo(pseudo) {

    const accounts = loadAccounts();

    const account = accounts.find(acc => acc.pseudo === pseudo);

    if (!account) {
        throw new Error(`Compte introuvable pour pseudo: ${pseudo}`);
    }

    if (!account.enable) {
        throw new Error(`Compte désactivé: ${pseudo}`);
    }

    return account;
}

///////// Tools
function delay(time) {
   return new Promise(resolve => setTimeout(resolve, time));
}

//////// Hooks
process.on('SIGINT', () => {
    console.log("SIGINT reçu");
    releaseLock();
    process.exit();
});

process.on('SIGTERM', () => {
    console.log("SIGTERM reçu");
    releaseLock();
    process.exit();
});

process.on('uncaughtException', (err) => {
    console.error("Erreur fatale :", err);
    releaseLock();
    process.exit(1);
});


////////// Parsing external parameters
const [,, courseID, bet, cvlNb, dev, pseudo] = process.argv;

const SAFE_MODE = true;
const GLOBAL_MAX_BET = 50; // sécurité absolue

(async () => {
	acquireLock();
	try{
		const betAmount = parseInt(bet);
		
		if (isNaN(betAmount) || betAmount <= 0) {
			throw new Error("Bet invalide");
		}

		if (!pseudo) {
			throw new Error("Pseudo manquant en argument");
		}

		const account = getAccountByPseudo(pseudo);

		console.log("\n===============================");
		console.log("Compte :", account.user);
		console.log("===============================");
		
		if (betAmount > GLOBAL_MAX_BET) {
			throw new Error("Montant de mise ("+betAmount+") dépasse la limite globale de sécurité ("+GLOBAL_MAX_BET+")");
		}

		if (SAFE_MODE && betAmount > account.maxBet) {
			throw new Error("Montant de mise ("+betAmount+") > max autorisé pour ce compte ("+account.maxBet+")→ SKIP");
		}

		const browser = await puppeteer.launch({
			headless: dev === "true" ? false : true,
			args: ['--no-sandbox', '--start-maximized'],
		});

		let page = await browser.newPage();

		await page.setViewport({
			width: 1920,
			height: 1080
		});

		try {
			console.log("=== Script démarré ===");
			console.log("- courseID:", courseID);
			console.log("- bet:", betAmount);
			console.log("- num cvl:", cvlNb);
			console.log("- user:", account.user);
			console.log("- dev:", dev);

			// =========================
			// LOGIN
			// =========================

			console.log("Ouverture page login...");
			await page.goto('https://www.genybet.fr/', { waitUntil: 'networkidle2' });
			await delay(500);

			console.log("Fermeture popup cookies (si présente)");
			try {
				await page.waitForSelector('#didomi-notice-agree-button', {
					timeout: 5000,
					visible: true
				});
				await page.click('#didomi-notice-agree-button');
				console.log("Popup cookies fermée");
			} catch {
				console.log("Popup cookies absente");
			}

			// Email
			const inPhoneLbl = 'input[placeholder="Email"]';
			await page.waitForSelector(inPhoneLbl);
			await page.type(inPhoneLbl, account.user, { delay: 50 });

			await page.$eval(inPhoneLbl, (el, value) => {
				el.value = value;
				el.dispatchEvent(new Event('input', { bubbles: true }));
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}, account.user);

			// Password
			const inPwdLbl = 'input[placeholder="Mot de passe"]';
			await page.waitForSelector(inPwdLbl);
			await page.type(inPwdLbl, account.pwd, { delay: 50 });

			await page.$eval(inPwdLbl, (el, value) => {
				el.value = value;
				el.dispatchEvent(new Event('input', { bubbles: true }));
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}, account.pwd);

			console.log("Click signin");
			await page.waitForSelector('#signin', { visible: true });
			await page.click('#signin');
			await delay(5000);

			// 1ère fermeture popup notifications
			try {
				await page.waitForSelector('button.button.blue[onclick="closeNotifications();"]', {
					timeout: 3000,
					visible: true
				});
				await page.click('button.button.blue[onclick="closeNotifications();"]');
				console.log("1st : Popup notifications fermée");
			} catch {
				console.log("1st : Popup notifications absente");
			}

			// =========================
			// PAGE COURSE
			// =========================

			console.log("Page course", courseID);
			await page.goto(
				'https://www.genybet.fr/courses/partants-pronostics/' + courseID,
				{ waitUntil: 'networkidle2' }
			);
			await delay(500);

			// 2ème fermeture popup notifications
			try {
				await page.waitForSelector('button.button.blue[onclick="closeNotifications();"]', {
					timeout: 3000,
					visible: true
				});
				await page.click('button.button.blue[onclick="closeNotifications();"]');
				console.log("2nd : Popup notifications fermée");
			} catch {
				console.log("2nd : Popup notifications absente");
			}

			// =========================
			// SELECTION CHEVAL
			// =========================

			console.log("Select cheval", cvlNb);
			await page.waitForSelector(`table.bet tr#partant-${cvlNb} td.checkbox-SIMPLE_GAGNANT input`);
			await page.click(`table.bet tr#partant-${cvlNb} td.checkbox-SIMPLE_GAGNANT input`);
			await delay(500);

			// =========================
			// SAISIE MONTANT SECURISEE
			// =========================

			console.log("betAmount", betAmount);
			var nbRetryMax = 3;
			var nbTry = 0;
			var isOk = false;

			const inputSelector = 'input.betAmount.ca';

			do{
				nbTry ++;
				await page.waitForSelector(inputSelector);

				// triple select + clear
				await page.click(inputSelector, { clickCount: 3 });
				await page.keyboard.press('Backspace');
				
/*				var test = betAmount;
				if( nbTry != 3 )
					test = betAmount+1;
*/
				await page.type(inputSelector, test.toString());
				await delay(500);

				// 🔒 Vérification réelle
				const realValue = await page.$eval(inputSelector, el => el.value);

				if (parseFloat(realValue) !== betAmount) {
					console.log( "Try N°"+nbTry+": Value in field ("+realValue+") is different than bet asked ("+betAmount+")" );
					if( nbTry >= nbRetryMax )
						throw new Error(`SECURITY STOP: montant incohérent (${realValue})`);
					
					await delay(1000);
					
				}else{
					isOk = true;
					console.log( "Try N°"+nbTry+": Bet writen is ok");
				}
			}while( !isOk );
			
			// 🔒 LOG AVANT VALIDATION
			fs.appendFileSync('/home/oklm/courses/logs/bets.log',
				`${new Date().toISOString()} | ${account.user} | course=${courseID} | cheval=${cvlNb} | montant=${betAmount}\n`
			);

			// =========================
			// VALIDATION
			// =========================

			console.log("Validate");
			await page.waitForSelector('button.bet-button', { visible: true });
			await page.click('button.bet-button');
			await delay(500);

			// Double clic sécurité (comme ton script)
			try {
				await page.waitForSelector('button.bet-button', { visible: true });
				await page.click('button.bet-button');
				await delay(500);
			} catch {}

			console.log("=== Succès ===");

			if (dev === "true") {
				console.log("Close browser to end script...");
				releaseLock();
				await new Promise(() => {});
			}

		} catch (err) {

			console.error("=== ERREUR ===", err);
			await page.screenshot({ path: `error_${account.pseudo}.png` });

		} finally {
			await browser.close();
			releaseLock();
		}
		
	}catch( err ){
		console.error("Erreur globale :", err);
		
	}finally {

        releaseLock();
        console.log("Lock libéré");

    }

})();