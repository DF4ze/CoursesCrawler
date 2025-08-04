CREATE DATABASE `courses` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE courses;

CREATE TABLE `partant_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `partant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `age_sexe` varchar(255) DEFAULT NULL,
  `gains` varchar(255) DEFAULT NULL,
  `i_gains` int(11) DEFAULT NULL,
  `musique` varchar(255) DEFAULT NULL,
  `nom_cheval` varchar(255) DEFAULT NULL,
  `num_cheval` int(11) DEFAULT NULL,
  `probable_geny` float DEFAULT NULL,
  `probablepmu` float DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3g7crxs4pufukc6u8tkvlp3as` (`courseid`,`num_cheval`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `course_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `course_complete_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `course_complete` (
  `id` bigint(20) NOT NULL,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `age_sex_chvl_premier` varchar(255) DEFAULT NULL,
  `auto_start` varchar(255) DEFAULT NULL,
  `cote_deuxieme_avant` float DEFAULT NULL,
  `cote_deuxieme_depart` float DEFAULT NULL,
  `cote_premier_avant` float DEFAULT NULL,
  `cote_premier_depart` float DEFAULT NULL,
  `cote_troisieme_avant` float DEFAULT NULL,
  `cote_troisieme_depart` float DEFAULT NULL,
  `date_course` varchar(255) DEFAULT NULL,
  `gain_chvl_premier` int(11) DEFAULT NULL,
  `heures` varchar(255) DEFAULT NULL,
  `hippodrome` varchar(255) DEFAULT NULL,
  `minutes` varchar(255) DEFAULT NULL,
  `musique_meilleur_gains` varchar(255) DEFAULT NULL,
  `musique_premier` varchar(255) DEFAULT NULL,
  `nom_chvl_premier` varchar(255) DEFAULT NULL,
  `nombre_chevaux_inf_cinq_avant` int(11) DEFAULT NULL,
  `nombre_chevaux_inf_cinq_depart` int(11) DEFAULT NULL,
  `nombre_chevaux_inf_cinq_probable_geny` int(11) DEFAULT NULL,
  `nombre_chevaux_inf_cinq_probablepmu` int(11) DEFAULT NULL,
  `nombre_chvl_favori_place_avant` int(11) DEFAULT NULL,
  `nombre_chvl_favori_place_depart` int(11) DEFAULT NULL,
  `nombre_chvl_favori_place_probable_geny` int(11) DEFAULT NULL,
  `nombre_chvl_favori_place_probablepmu` int(11) DEFAULT NULL,
  `nombre_partant` int(11) DEFAULT NULL,
  `numero_chl_deuxieme_avant` int(11) DEFAULT NULL,
  `numero_chl_deuxieme_depart` int(11) DEFAULT NULL,
  `numero_chl_deuxieme_probable_geny` int(11) DEFAULT NULL,
  `numero_chl_deuxieme_probablepmu` int(11) DEFAULT NULL,
  `numero_chl_premier_avant` int(11) DEFAULT NULL,
  `numero_chl_premier_depart` int(11) DEFAULT NULL,
  `numero_chl_premier_probable_geny` int(11) DEFAULT NULL,
  `numero_chl_premier_probablepmu` int(11) DEFAULT NULL,
  `numero_chl_troisieme_avant` int(11) DEFAULT NULL,
  `numero_chl_troisieme_depart` int(11) DEFAULT NULL,
  `numero_chl_troisieme_probable_geny` int(11) DEFAULT NULL,
  `numero_chl_troisieme_probablepmu` int(11) DEFAULT NULL,
  `numero_chvl_deuxieme` int(11) DEFAULT NULL,
  `numero_chvl_premier` int(11) DEFAULT NULL,
  `numero_chvl_troisieme` int(11) DEFAULT NULL,
  `numero_course` int(11) DEFAULT NULL,
  `numero_meilleur_gains` int(11) DEFAULT NULL,
  `numero_reunion` int(11) DEFAULT NULL,
  `pourcent_deuxieme_avant` float DEFAULT NULL,
  `pourcent_deuxieme_depart` float DEFAULT NULL,
  `pourcent_premier_avant` float DEFAULT NULL,
  `pourcent_premier_depart` float DEFAULT NULL,
  `pourcent_troisieme_avant` float DEFAULT NULL,
  `pourcent_troisieme_depart` float DEFAULT NULL,
  `prime` varchar(255) DEFAULT NULL,
  `rap_gagnant_geny` double DEFAULT NULL,
  `rap_gagnant_pmu` double DEFAULT NULL,
  `rap_place_deuxieme_geny` double DEFAULT NULL,
  `rap_place_deuxieme_pmu` double DEFAULT NULL,
  `rap_place_premier_geny` double DEFAULT NULL,
  `rap_place_premier_pmu` double DEFAULT NULL,
  `rap_place_troisieme_geny` double DEFAULT NULL,
  `rap_place_troisieme_pmu` double DEFAULT NULL,
  `rapport_deuxieme_probable_geny` float DEFAULT NULL,
  `rapport_deuxieme_probablepmu` float DEFAULT NULL,
  `rapport_premier_probable_geny` float DEFAULT NULL,
  `rapport_premier_probablepmu` float DEFAULT NULL,
  `rapport_troisieme_probable_geny` float DEFAULT NULL,
  `rapport_troisieme_probablepmu` float DEFAULT NULL,
  `total_pourcent` float DEFAULT NULL,
  `type_course` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UniqueCourse` (`date_course`,`numero_reunion`,`numero_course`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `course` (
  `id` bigint(20) NOT NULL,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `course` int(11) DEFAULT NULL,
  `date` varchar(255) DEFAULT NULL,
  `depart` varchar(255) DEFAULT NULL,
  `heures` varchar(255) DEFAULT NULL,
  `hippodrome` varchar(255) DEFAULT NULL,
  `minutes` varchar(255) DEFAULT NULL,
  `prime` varchar(255) DEFAULT NULL,
  `prix` varchar(255) DEFAULT NULL,
  `reunion` int(11) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cote_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cote` (
  `id` bigint(20) NOT NULL,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `cote_avant` float DEFAULT NULL,
  `cote_depart` float DEFAULT NULL,
  `enjeux_avant` float DEFAULT NULL,
  `enjeux_depart` float DEFAULT NULL,
  `num_cheval` int(11) DEFAULT NULL,
  `rapport_probable_geny` float DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKarhlqsj8gg2sj2y3b9hb207bt` (`courseid`,`num_cheval`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `arrivee_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `arrivee` (
  `id` bigint(20) NOT NULL,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `nom_chv` varchar(255) DEFAULT NULL,
  `num_arrivee` int(11) DEFAULT NULL,
  `num_chv` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK92wb4feqqfyeto0sa8u72vwob` (`courseid`,`num_chv`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rapport` (
  `id` bigint(20) NOT NULL,
  `courseid` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `arrivee` int(11) DEFAULT NULL,
  `gagnant_geny` double DEFAULT NULL,
  `gagnant_pmu` double DEFAULT NULL,
  `num_cheval` int(11) DEFAULT NULL,
  `place_geny` double DEFAULT NULL,
  `place_pmu` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsrvql9utxrox80heak3w6ggny` (`courseid`,`num_cheval`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rapport_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `scheduled_task` (
  `id` bigint(20) NOT NULL,
  `course_description` varchar(255) DEFAULT NULL,
  `course_url` varchar(255) DEFAULT NULL,
  `creation_date` datetime DEFAULT NULL,
  `cron_expression` varchar(255) DEFAULT NULL,
  `error_message` varchar(255) DEFAULT NULL,
  `last_execution` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `planned_execution` datetime DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `telegram_message_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `scheduled_task_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

#
# Quartz seems to work best with the driver mm.mysql-2.0.7-bin.jar
#
# PLEASE consider using mysql with innodb tables to avoid locking issues
#
# In your Quartz properties file, you'll need to set
# org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
#

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;


CREATE TABLE QRTZ_JOB_DETAILS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(10) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(200) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    SCHED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

INSERT INTO arrivee_seq (next_val) VALUES (1);
INSERT INTO cote_seq (next_val) VALUES (1);
INSERT INTO course_seq (next_val) VALUES (1);
INSERT INTO course_complete_seq (next_val) VALUES (1);
INSERT INTO partant_seq (next_val) VALUES (1);
INSERT INTO rapport_seq (next_val) VALUES (1);
INSERT INTO scheduled_task_seq (next_val) VALUES (1);




commit;