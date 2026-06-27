
# 🌾 SmartAgriChat — AI-Powered Farmer Assistant

A Java Swing desktop application that gives farmers access to an intelligent 
agricultural chatbot, crop disease detection, fertilizer calculators, and 
a feedback system — all backed by MySQL.

## Features
- 🔐 Secure farmer login, registration, and password reset
- 💬 Agricultural chatbot with 35+ knowledge topics
- 🧮 NPK fertilizer calculator (regex-parsed natural language input)
- 💧 Crop water requirement estimator
- 🖼️ Crop image upload with disease diagnosis
- ⭐ 5-star feedback system (stored in database)
- ⬇️ Chat history export to .txt

## Requirements
- Java JDK 8 or higher
- MySQL Server 8.x
- MySQL Connector/J JAR (mysql-connector-j-x.x.x.jar)

## Database Setup
```sql
CREATE DATABASE farmer_db;
USE farmer_db;
CREATE TABLE farmer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
CREATE TABLE feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    rating INT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Installation
1. Clone the repository: `git clone https://github.com/yourusername/SmartAgriChat`
2. Add `mysql-connector-j.jar` to your project classpath
3. Update DB credentials in `FarmerLoginApp.java`, `SmartAgriChat.java`, etc. 
   (or better: create a `config.properties` file)
4. Update image paths in each file to match your local `resources/` folder
5. Compile and run `FarmerLoginApp.java`

## Technologies Used
Java SE · Java Swing · JDBC · MySQL · SwingWorker · java.util.regex

## Future Enhancements
- BCrypt password hashing
- Real image classification (TensorFlow Lite)
- Live weather API integration
- Connection pooling (HikariCP)
- Role-based access (Admin / Farmer)

## 👨‍💻 Author

**SU Gowtham** · [GitHub]https://github.com/gowthamsu0309
