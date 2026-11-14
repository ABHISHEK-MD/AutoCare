# AutoCare – Vehicle Service Management System
### Windows 10/11 Beginner Setup Guide

AutoCare is an internal vehicle service and customer management application built with **Java 21 / Spring Boot 3**, **Angular 22**, and **MariaDB**.

This guide provides simple, step-by-step instructions to install the required software, initialize the database using `database/setup.sql`, and run the application on **Windows 10 or Windows 11**.

---

## 1. Prerequisites Installation (Windows 10/11)

Install the following software on your Windows computer:

### A. Java Development Kit (JDK 21)
1. Download **Eclipse Temurin JDK 21 (LTS)** Windows MSI installer from:
   **[https://adoptium.net/](https://adoptium.net/)**
2. Run the installer. On the "Custom Setup" screen, make sure to enable:
   - **"Set JAVA_HOME variable"**
   - **"Add to PATH"**
3. Complete the installation.

### B. Apache Maven
1. Download the **Binary zip archive** of Apache Maven from:
   **[https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)** (e.g., `apache-maven-3.9.9-bin.zip`).
2. Extract the zip folder to: `C:\Program Files\Maven\apache-maven-3.9.9`.
3. Add Maven to your system PATH:
   - Press `Win + S`, type **env**, and select **Edit the system environment variables**.
   - Click **Environment Variables...**
   - Under **System variables**, select **Path** and click **Edit**.
   - Click **New** and add: `C:\Program Files\Maven\apache-maven-3.9.9\bin`
   - Click **OK** on all dialogs.

### C. Node.js 22 LTS & npm
> **Note**: Angular 22 strictly requires **Node.js 22 LTS (v22.22.x+)** or Node.js 24 LTS. Older versions like Node 18 or 20 are incompatible with Angular 22.
1. Download **Node.js 22 LTS** Windows 64-bit MSI installer from:
   **[https://nodejs.org/](https://nodejs.org/)**
2. Run the installer with default settings (npm is included automatically).

### D. MariaDB Server
1. Download the **MariaDB Community Server 11.x** Windows 64-bit MSI installer from:
   **[https://mariadb.org/download/](https://mariadb.org/download/)**
2. During setup:
   - Set an administrative password for the `root` user (we recommend `root` for simplicity).
   - Leave the port as default: `3306`.
   - Keep the checkbox **"Install as service"** checked (Service name: `MariaDB`).
3. Add MariaDB to your system PATH:
   - In **Environment Variables** $\rightarrow$ **Path**, click **New** and add:
     `C:\Program Files\MariaDB 11.8\bin` *(adjust folder version to match your installed version)*.

---

## 2. Verify Software Installations

Open a new **Windows PowerShell** window and run each verification command:

```powershell
java -version
mvn -version
node -v
npm -v
mariadb --version
```

### Expected Output:
- `java -version`: OpenJDK 64-Bit Server VM (build 21.x.x)
- `mvn -version`: Apache Maven 3.9.x
- `node -v`: v22.x.x (or v24.x.x)
- `npm -v`: 10.x.x (or 11.x.x)
- `mariadb --version`: mariadb from 11.x.x-MariaDB

*(If any command says "not recognized", close and reopen PowerShell so it picks up your updated PATH)*.

---

## 3. Download the Project & Open PowerShell

1. Download and extract the AutoCare project folder, or clone it using Git:
   ```powershell
   git clone <repository-url>
   ```
2. Open PowerShell inside the project directory:
   ```powershell
   cd C:\path\to\AutoCare
   ```

---

## 4. Database Setup (`database/setup.sql`)

The project includes a single setup file: **`database/setup.sql`**.

This file automatically:
- Creates the `autocare_db` database.
- Creates the application database user `autocare_app` with password `root`.
- Grants all required privileges on `autocare_db` to `autocare_app`.
- Creates the `customers`, `vehicles`, and `service_records` tables with all constraints and foreign keys.
- Inserts demonstration sample records safely.
- Uses `IF NOT EXISTS` and `WHERE NOT EXISTS`, making it safe to run multiple times without duplicating or overwriting data.

### Step 4.1: Execute `setup.sql`
In PowerShell at the project root:

```powershell
Get-Content database\setup.sql -Raw | mariadb -u root -p
```
*(Enter your MariaDB root administrator password when prompted)*

> **Alternative method via MariaDB console**:
> ```powershell
> mariadb -u root -p
> ```
> Enter your root password, then inside the MariaDB prompt:
> ```sql
> SOURCE database/setup.sql;
> EXIT;
> ```

### Step 4.2: Verify Database & Tables
Run this quick query to verify that user `autocare_app` can log in and view the tables:

```powershell
mariadb -u autocare_app -proot autocare_db -e "SHOW TABLES; SELECT COUNT(*) AS total_customers FROM customers; SELECT COUNT(*) AS total_vehicles FROM vehicles;"
```

You should see the 3 tables: `customers`, `vehicles`, `service_records`, and the initial customer count.

---

## 5. Start the Application

Open **two separate Windows PowerShell windows** in the `AutoCare` root directory:

### Terminal 1 – Start the Backend Server (Spring Boot)
```powershell
cd backend
mvn spring-boot:run
```
- **Backend API URL**: `http://localhost:8080`
- **Health Check**: Open [http://localhost:8080/api/dashboard/stats](http://localhost:8080/api/dashboard/stats) in your browser to see live JSON stats.
- To stop the backend: Press `Ctrl + C`.

---

### Terminal 2 – Start the Frontend Web App (Angular)
```powershell
cd frontend
npm install
npm start
```
> **Note**: `npm install` is only required the **first time** you set up the project on your machine to download the dependencies. For all future runs, simply run `npm start`!

- **Frontend Application URL**: **[http://localhost:4200](http://localhost:4200)**
- To stop the frontend: Press `Ctrl + C`.

---

## 6. Accessing & Using the Application

Open your browser and navigate to:
**[http://localhost:4200](http://localhost:4200)**

### Main Modules to Explore:
1. **Dashboard (`/dashboard`)**: Displays live summary statistics (total customers, total vehicles, total service visits, visits this month) and a table of the 10 most recent service visits.
2. **Vehicles Registry (`/vehicles`)**: Search vehicles across 5 fields (registration number, owner name, owner phone, vehicle brand, or model).
3. **Register Vehicle (`/vehicles/add`)**: Combined atomic form to register a new owner and vehicle in a single transaction, or select an existing customer via phone lookup.
4. **Service History Search (`/service-history`)**:
   - Lookup existing vehicle `KA03MN4567` $\rightarrow$ displays vehicle specifications, registered owner details, and complete chronological maintenance records sorted newest-first.
   - Lookup an unregistered vehicle (e.g. `KA99ZZ9999`) $\rightarrow$ displays **"Vehicle Not Found"** with a **"Register Vehicle"** button that opens the registration form with the searched registration number prefilled and editable.

---

## 7. Troubleshooting

- **`mvn : The term 'mvn' is not recognized`**:
  Make sure Maven's `bin` folder is added to your System Environment Variables `Path`. Close and reopen your PowerShell window.
- **`Port 8080 or Port 4200 already in use`**:
  Another application is using the port. Stop it in PowerShell:
  ```powershell
  Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
  ```
- **`MariaDB service not running (Connection refused on port 3306)`**:
  Open PowerShell as Administrator and run:
  ```powershell
  Start-Service -Name MariaDB
  ```
- **Custom MariaDB Password**:
  If you changed the `autocare_app` user password, update `spring.datasource.password` in `backend/src/main/resources/application.properties` or set the environment variable `$env:DB_PASSWORD="your_password"` in PowerShell before starting the backend.
