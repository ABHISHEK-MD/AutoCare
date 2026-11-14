-- ==============================================================================
-- AutoCare – Complete Database Setup Script
-- Database: MariaDB / MySQL
--
-- This script is completely safe and idempotent:
-- - It creates the database, user, tables, and demonstration seed data.
-- - Uses 'IF NOT EXISTS' and 'WHERE NOT EXISTS' for safe repeat execution.
-- - NEVER drops, truncates, or overwrites existing tables or user data.
-- ==============================================================================

-- 1. DATABASE CREATION
CREATE DATABASE IF NOT EXISTS autocare_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

-- 2. APPLICATION USER CREATION & PRIVILEGES
-- Default credentials expected by backend application.properties:
-- User: autocare_app | Password: root | Database: autocare_db
CREATE USER IF NOT EXISTS 'autocare_app'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON autocare_db.* TO 'autocare_app'@'localhost';
FLUSH PRIVILEGES;

USE autocare_db;

-- ==============================================================================
-- 3. TABLE DEFINITIONS
-- ==============================================================================

-- CUSTOMERS TABLE
CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    phone       VARCHAR(15)  NOT NULL,
    email       VARCHAR(150),
    address     VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- VEHICLES TABLE
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id         BIGINT       NOT NULL,
    registration_number VARCHAR(20)  NOT NULL,
    brand               VARCHAR(50)  NOT NULL,
    model               VARCHAR(50)  NOT NULL,
    variant             VARCHAR(50),
    manufacturing_year  INT,
    fuel_type           VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_registration_number (registration_number),

    CONSTRAINT fk_vehicle_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- SERVICE RECORDS TABLE
CREATE TABLE IF NOT EXISTS service_records (
    service_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id         BIGINT        NOT NULL,
    service_date       DATE          NOT NULL,
    odometer_reading   INT           NOT NULL,
    service_type       VARCHAR(100)  NOT NULL,
    customer_complaint VARCHAR(500),
    work_performed     VARCHAR(1000) NOT NULL,
    parts_replaced     VARCHAR(500),
    remarks            VARCHAR(500),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ==============================================================================
-- 4. SAMPLE DEMONSTRATION SEED DATA
-- Safe: Inserts only if the records do not already exist.
-- ==============================================================================

-- A. Customers
INSERT INTO customers (full_name, phone, email, address)
SELECT 'Priya Sundaram', '9845123456', 'priya.sundaram@gmail.com', '78 Koramangala 4th Block, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE phone = '9845123456');

INSERT INTO customers (full_name, phone, email, address)
SELECT 'Vikram Malhotra', '9711234567', 'vikram.m@outlook.com', '12 Richmond Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE phone = '9711234567');

INSERT INTO customers (full_name, phone, email, address)
SELECT 'Ananya Patel', '9900887766', 'ananya.patel@yahoo.com', '45 Whitefield Main Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE phone = '9900887766');

-- B. Vehicles
-- Priya owns 2 vehicles: Hyundai Creta (Petrol) and Tata Nexon EV (Electric)
INSERT INTO vehicles (customer_id, registration_number, brand, model, variant, manufacturing_year, fuel_type)
SELECT (SELECT customer_id FROM customers WHERE phone = '9845123456' LIMIT 1), 'KA03MN4567', 'Hyundai', 'Creta', 'SX(O) Turbo', 2023, 'Petrol'
WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA03MN4567')
  AND EXISTS (SELECT 1 FROM customers WHERE phone = '9845123456');

INSERT INTO vehicles (customer_id, registration_number, brand, model, variant, manufacturing_year, fuel_type)
SELECT (SELECT customer_id FROM customers WHERE phone = '9845123456' LIMIT 1), 'KA04EV8899', 'Tata', 'Nexon EV', 'Empowered Plus', 2024, 'Electric'
WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA04EV8899')
  AND EXISTS (SELECT 1 FROM customers WHERE phone = '9845123456');

-- Vikram owns Mahindra XUV700 (Diesel)
INSERT INTO vehicles (customer_id, registration_number, brand, model, variant, manufacturing_year, fuel_type)
SELECT (SELECT customer_id FROM customers WHERE phone = '9711234567' LIMIT 1), 'KA02GH3344', 'Mahindra', 'XUV700', 'AX7L AWD', 2022, 'Diesel'
WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA02GH3344')
  AND EXISTS (SELECT 1 FROM customers WHERE phone = '9711234567');

-- Ananya owns Maruti Suzuki Baleno (Petrol)
INSERT INTO vehicles (customer_id, registration_number, brand, model, variant, manufacturing_year, fuel_type)
SELECT (SELECT customer_id FROM customers WHERE phone = '9900887766' LIMIT 1), 'KA51JK1212', 'Maruti Suzuki', 'Baleno', 'Alpha AGS', 2021, 'Petrol'
WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA51JK1212')
  AND EXISTS (SELECT 1 FROM customers WHERE phone = '9900887766');

-- C. Service Records
-- Records for KA03MN4567 (Creta)
INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA03MN4567' LIMIT 1), '2023-08-15', 5000, 'Periodic Maintenance', 'First complimentary 5000 km checkup', 'General inspection, tire pressure adjustment, fluid top-up, car wash', NULL, 'All systems normal'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA03MN4567' AND sr.service_date = '2023-08-15' AND sr.odometer_reading = 5000
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA03MN4567');

INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA03MN4567' LIMIT 1), '2024-02-20', 12400, 'Oil & Filter Change', 'Engine feeling slightly rough at idle', 'Replaced engine oil and filter, cleaned spark plugs, throttle body service', 'Shell Helix 5W-30 (3.6L), Hyundai OEM Filter', 'Idle vibration resolved. Engine runs smooth.'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA03MN4567' AND sr.service_date = '2024-02-20' AND sr.odometer_reading = 12400
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA03MN4567');

-- Record for KA02GH3344 (XUV700)
INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA02GH3344' LIMIT 1), '2023-10-10', 15000, 'Periodic Maintenance', '15,000 km service and AC not cooling effectively', 'Cabin air filter replaced, AC condenser cleaned, engine oil replaced, wheel alignment', 'Cabin Filter, Engine Oil 6L, Oil Filter', 'AC cooling verified at 6.5 degrees C. Alignment calibrated.'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA02GH3344' AND sr.service_date = '2023-10-10' AND sr.odometer_reading = 15000
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA02GH3344');

-- Records for KA51JK1212 (Baleno)
INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA51JK1212' LIMIT 1), '2022-09-05', 10000, 'General Service', 'Scheduled annual maintenance', 'Engine oil change, brake inspection, battery test', 'Engine Oil, Filter', 'Battery health 92%'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA51JK1212' AND sr.service_date = '2022-09-05' AND sr.odometer_reading = 10000
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA51JK1212');

INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA51JK1212' LIMIT 1), '2023-10-12', 21500, 'Brake System Service', 'Slight brake pedal shudder at high speeds', 'Front brake discs skimmed, new brake pads installed, brake fluid renewed', 'Brembo Brake Pads, DOT 4 Fluid', 'Braking performance restored to factory spec.'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA51JK1212' AND sr.service_date = '2023-10-12' AND sr.odometer_reading = 21500
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA51JK1212');

INSERT INTO service_records (vehicle_id, service_date, odometer_reading, service_type, customer_complaint, work_performed, parts_replaced, remarks)
SELECT (SELECT vehicle_id FROM vehicles WHERE registration_number = 'KA51JK1212' LIMIT 1), CURRENT_DATE(), 32000, 'Periodic Maintenance', '30,000 km major service', 'Full fluid replacement, spark plugs replaced, coolant flushed, transmission oil changed', 'NGK Iridium Spark Plugs, Coolant (4L), Synthetic Oil', 'Vehicle delivered in pristine condition.'
WHERE NOT EXISTS (
    SELECT 1 FROM service_records sr 
    JOIN vehicles v ON sr.vehicle_id = v.vehicle_id 
    WHERE v.registration_number = 'KA51JK1212' AND sr.odometer_reading = 32000
) AND EXISTS (SELECT 1 FROM vehicles WHERE registration_number = 'KA51JK1212');
