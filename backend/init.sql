CREATE DATABASE IF NOT EXISTS psm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE psm_db;

CREATE TABLE IF NOT EXISTS users (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  fullName    VARCHAR(100) NOT NULL,
  email       VARCHAR(100) UNIQUE NOT NULL,
  password    VARCHAR(255) NOT NULL,
  studentId   VARCHAR(20),
  phone       VARCHAR(20),
  birthDate   VARCHAR(20),
  createdAt   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  userId          INT NOT NULL,
  title           VARCHAR(200) NOT NULL,
  location        VARCHAR(200),
  note            TEXT,
  startTime       BIGINT NOT NULL,
  endTime         BIGINT NOT NULL,
  color           VARCHAR(20) DEFAULT '#A855F7',
  priority        INT DEFAULT 1,
  isRepeat        TINYINT(1) DEFAULT 0,
  repeatDays      VARCHAR(50),
  isCompleted     TINYINT(1) DEFAULT 0,
  reminderMinutes INT DEFAULT 0,
  createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
);
