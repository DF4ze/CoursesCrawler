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

////////// Utils
function delay(time) {
	return new Promise(resolve => setTimeout(resolve, time));
}

async function safeScreenshot(page, path) {
	try {
		if (page && !page.isClosed()) {
			await page.screenshot({ path });
		}
	} catch (e) {
		console.warn("Screenshot failed:", e.message);
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

	if (!account) throw new Error(`Compte introuvable: ${pseudo}`);
	if (!account.enable) throw new Error(`Compte désactivé: ${pseudo}`);

	return account;
}

//////// Hooks
process.on('SIGINT', () => { releaseLock(); process.exit(); });
process.on('SIGTERM', () => { releaseLock(); process.exit(); });
process.on('uncaughtException', (err) => {
	console.error("Erreur fatale :", err);
	releaseLock();
	process.exit(1);
});

//////// Params
const [,, courseID, bet, cvlNb, dev, pseudo] = process.argv;

const SAFE_MODE = true;
const GLOBAL_MAX_BET = 50;

(async () => {
	acquireLock();

	let browser = null;
	let page = null;
	let account = null;

	try {
		const betAmount = parseInt(bet);

		if (isNaN(betAmount) || betAmount <= 0) {
			throw new Error("Bet invalide");
		}

		if (!pseudo) {
			throw new Error("Pseudo manquant");
		}

		account = getAccountByPseudo(pseudo);

		console.log("\n===============================");
		console.log("Compte :", account.user);
		console.log("===============================");
		
		if (betAmount > GLOBAL_MAX_BET) {
			throw new Error("Bet > limite globale");
		}

		if (SAFE_MODE && betAmount > account.maxBet) {
			throw new Error("Bet > limite compte");
		}

		browser = await puppeteer.launch({
			headless: dev === "true" ? false : true,
			args: ['--no-sandbox', '--start-maximized'],
		});

		page = await browser.newPage();

		await page.setViewport({ width: 1920, height: 1080 });

		try {
			console.log("=== Script démarré ===");
			console.log("- courseID:", courseID);
			console.log("- bet:", betAmount);
			console.log("- num cvl:", cvlNb);
			console.log("- user:", account.user);
			console.log("- dev:", dev);

			await page.goto('https://www.genybet.fr/', { waitUntil: 'networkidle2' });
			await delay(500);
			await safeScreenshot(page, "lastAction.png");

			// Cookies
			try {
				await page.waitForSelector('#didomi-notice-agree-button', { timeout: 5000 });
				await page.click('#didomi-notice-agree-button');
			} catch {}

			await safeScreenshot(page, "lastAction.png");

			// Login
			const email = 'input[placeholder="Email"]';
			await page.waitForSelector(email);
			await page.type(email, account.user);

			const pwd = 'input[placeholder="Mot de passe"]';
			await page.waitForSelector(pwd);
			await page.type(pwd, account.pwd);

			await safeScreenshot(page, "lastAction.png");

			await page.click('#signin');
			await delay(5000);

			// 1ère fermeture popup notifications
			try {
				await page.waitForSelector('button.button.blue[onclick="closeNotifications();"]', {
					timeout: 3000,
					visible: true
				});
				await page.click('button.button.blue[onclick="closeNotifications();"]');
				console.log("1st : Win Popup notifications fermée");
			} catch {
				console.log("1st : Win Popup notifications absente");
			}

			// Course
			await page.goto(`https://www.genybet.fr/courses/partants-pronostics/${courseID}`);
			await delay(500);
			await safeScreenshot(page, "lastAction.png");

			// 2ème fermeture popup notifications
			try {
				await page.waitForSelector('button.button.blue[onclick="closeNotifications();"]', {
					timeout: 3000,
					visible: true
				});
				await page.click('button.button.blue[onclick="closeNotifications();"]');
				console.log("2nd : Win Popup notifications fermée");
			} catch {
				console.log("2nd : Win Popup notifications absente");
			}


			// Select cheval
			await page.click(`table.bet tr#partant-${cvlNb} td.checkbox-SIMPLE_GAGNANT input`);
			await safeScreenshot(page, "lastAction.png");

			// Bet input sécurisé
			const inputSelector = 'input.betAmount.ca';
			console.log("waiting for inputSelector");
			await page.waitForSelector(inputSelector);

			let isOk = false;
			let tries = 0;

			while (!isOk && tries < 3) {
				tries++;

				await page.click(inputSelector, { clickCount: 3 });
				await page.keyboard.press('Backspace');
				console.log("input erased");

				await page.type(inputSelector, betAmount.toString());
				await delay(300);
				console.log("inputSelector filed : "+betAmount.toString());

				const real = await page.$eval(inputSelector, el => el.value);
				console.log("inputSelector readed : "+real);

				if (parseFloat(real) === betAmount) {
					console.log("Correspondance filled/readed ok");
					isOk = true;
				} else {
					console.log("Correspondance filled/readed is not ok! Wait and retry ("+tries+"/3)");
					await delay(1000);
				}

				await safeScreenshot(page, "lastAction.png");
			}

			if (!isOk) throw new Error("Montant incorrect");

			// Validation
			console.log("Waiting for bet button");
			await page.waitForSelector('button.bet-button');
			console.log("Click on bet button");
			await page.click('button.bet-button');
			await safeScreenshot(page, "lastAction.png");

			console.log("=== SUCCESS ===");

		} catch (err) {
			console.error("Erreur métier :", err);

			await safeScreenshot(page, `error_${account?.pseudo || 'unknown'}.png`);
			await safeScreenshot(page, "error_last.png");

		}

	} catch (err) {
		console.error("Erreur globale :", err);

		await safeScreenshot(page, `error_${account?.pseudo || 'unknown'}.png`);
		await safeScreenshot(page, "error_last.png");

	} finally {
		if (browser) {
			try { await browser.close(); } catch {}
		}
		releaseLock();
		console.log("Lock libéré");
	}

})();