
# CoursesCrawler

## 🚀 Description

CoursesCrawler est un outil d’automatisation dédié aux courses hippiques.
Il permet de récupérer les prévisions et résultats depuis le site Geny, d’appliquer des filtres intelligents et de suivre les actions en temps réel via Telegram.
L’objectif : transformer des données brutes en décisions exploitables, de façon autonome et pilotable.

---

## ✨ Fonctionnalités

* 📥 Récupération automatique des courses du jour
* 📊 Extraction des prévisions et résultats depuis Geny
* 🎯 Application de filtres personnalisés pour sélectionner les courses
* 🤖 Automatisation des actions (ex : suivi / mise basée sur critères)
* 🔔 Système d’alertes via Telegram pour suivi en temps réel
* 📅 Extraction historique des courses entre deux dates
* 📁 Export des données au format Excel

---

## 📸 Aperçu

> ⚠️ Screenshot à ajouter (ex: logs, dashboard, export Excel, alert Telegram)

---

## ⚙️ Installation

- Binaries
  - mkdir courses
  - copy CourseCrawler-X.X.X.jar
  - ln -nfs CourseCrawler-X.X.X.jar CourseCrawler.jar
  - copy scripts
  - modify scripts to fit your path
  - create system service with .sh files


- Config
  - ! Read [application-yourProfile.properties](src/main/resources/application-yourProfile.properties) for property info !
  - mkdir config
  - nano application.properties
  - it's here to write property overrides as DB config...


- Log
  - mkdir log
  - override logging.file.name property


- Install MariaDB
  - create 'courses' db
  - create user & password
  - specify login in properties


- Install NodeJs
  - mkdir auto_bet
  - copy autoBet/script.js in auto_bet
  - init NodeJs
  - specify path in properties


- Telegram
  - write your key in application.properties


- mkdir export & specify path in properties
---

## ▶️ Utilisation

### Mode automatique (quotidien)

* Récupère les courses du jour
* Applique les filtres définis
* Déclenche les actions correspondantes
* Envoie des notifications via Telegram

### Mode manuel (historique)

* L’utilisateur fournit une plage de dates
* Le système récupère les courses correspondantes
* Génère un export Excel exploitable


---

## 🧱 Architecture 

Architecture supposée :

* **Crawler** : récupération des données depuis Geny
* **Filter Engine** : application des règles métier
* **Automation Layer** : exécution des actions (ex : mise)
* **Alerting Service** : intégration Telegram
* **Export Module** : génération Excel

---

## 🗺️ Roadmap

* Amélioration des stratégies de filtrage
* Ajout de nouvelles sources de données hippiques
* Interface utilisateur pour pilotage simplifié
* Backtesting des stratégies
* Gestion multi-utilisateur

---

## 🤝 Contribution

Les contributions sont les bienvenues :

1. Fork du projet
2. Création d’une branche (`feature/xxx`)
3. Pull Request claire et documentée

---

## 📄 Licence

This project is licensed under the Apache License 2.0 with Commons Clause.

✔ You are allowed to:

* Use the software for personal or internal business use
* Modify and distribute it
* Contribute to the project

❌ You are NOT allowed to:

* Sell this software
* Sell a product or service primarily based on this software

See the LICENSE file for full details.

---

## 🛠️ À compléter par l’utilisateur

* ✅ Version exacte de Java / outils de build
* ✅ Commandes d’installation confirmées
* ✅ Exemples réels de filtres utilisés
* ✅ Screenshots (Telegram, Excel, logs)
* ✅ Description précise des actions automatisées (mise réelle ? simulation ?)
* ⚠️ Vérifier la structure de configuration
* ⚠️ Clarifier les limites d’utilisation vis-à-vis du site Geny

