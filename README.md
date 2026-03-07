
# CourseCrawler
Crawl geny.fr and Auto Bet on genynet.fr

## Installation guide
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