-- Run this AFTER creating the database itself, e.g.:
--   createdb agridb
--   psql -U postgres -d agridb -f schema.sql

CREATE TABLE farmers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(150),
    location VARCHAR(150),
    land_size_sqft DOUBLE PRECISION,
    land_type VARCHAR(30),         -- OPEN_FIELD, TERRACE, BACKYARD, ROOFTOP_CONTAINER
    experience_level VARCHAR(20),  -- BEGINNER, HOBBYIST, SERIOUS
    budget_in_rupees DOUBLE PRECISION,
    scale VARCHAR(30),             -- SMALL_SCALE_URBAN, SMALL_SCALE, MEDIUM_SCALE, LARGE_SCALE
    suggested_farming_method VARCHAR(150)
);

CREATE TABLE crop_guides (
    id BIGSERIAL PRIMARY KEY,
    crop_name VARCHAR(100),
    suitable_land_types VARCHAR(150),   -- comma separated
    suitable_scale VARCHAR(30),
    duration_days INT,
    land_preparation_steps TEXT,
    sowing_instructions TEXT,
    watering_schedule TEXT,
    fertilizer_plan TEXT,
    pest_watch TEXT,
    harvest_guide TEXT,
    estimated_cost_per_acre_or_unit DOUBLE PRECISION,
    estimated_yield_per_acre_or_unit DOUBLE PRECISION
);

CREATE TABLE growth_logs (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT REFERENCES farmers(id),
    crop_guide_id BIGINT REFERENCES crop_guides(id),
    sowing_date DATE,
    current_stage VARCHAR(30),   -- LAND_PREP, SOWN, GROWING, FLOWERING, HARVEST_READY, HARVESTED
    notes VARCHAR(500)
);


-- ==========================================================================
-- Accounts (separate login for learning farmers, sellers, and buyers/traders)
-- ==========================================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    city VARCHAR(100),
    business_name VARCHAR(150),         -- sellers: the only name buyers see
    role VARCHAR(20) NOT NULL,          -- ADMIN, FARMER (learner), SELLER, BUYER
    upi_id VARCHAR(100),                -- seller payout destination
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    farmer_id BIGINT REFERENCES farmers(id),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now(),
    last_login_at TIMESTAMP,
    login_count INTEGER DEFAULT 0,
    UNIQUE (email, role)
);

-- Every sign-in, registration, sign-out and failed attempt (shown on the admin Login activity page)
CREATE TABLE login_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(150),
    full_name VARCHAR(100),
    role VARCHAR(20),
    event_type VARCHAR(20),             -- LOGIN, REGISTER, LOGOUT, LOGIN_FAILED
    ip_address VARCHAR(64),
    user_agent VARCHAR(300),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE produce_listings (
    id BIGSERIAL PRIMARY KEY,
    seller_user_id BIGINT REFERENCES users(id),
    crop_name VARCHAR(100),
    quantity_kg DOUBLE PRECISION,
    expected_price_per_kg DOUBLE PRECISION,
    location VARCHAR(150),
    available_from DATE,
    description VARCHAR(1000),
    image_url VARCHAR(300),             -- seller's own photo; empty = crop dataset photo is used
    status VARCHAR(20) DEFAULT 'AVAILABLE',  -- AVAILABLE, SOLD_OUT, HIDDEN
    created_at TIMESTAMP DEFAULT now()
);

-- ==========================================================================
-- Improved analysis inputs: soil test report, water resources, land photo
-- ==========================================================================
CREATE TABLE soil_reports (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT REFERENCES farmers(id),
    file_name VARCHAR(255),
    stored_path VARCHAR(500),
    content_type VARCHAR(100),
    ph_level DOUBLE PRECISION,
    nitrogen_kg_per_ha DOUBLE PRECISION,
    phosphorus_kg_per_ha DOUBLE PRECISION,
    potassium_kg_per_ha DOUBLE PRECISION,
    organic_carbon_percent DOUBLE PRECISION,
    soil_texture VARCHAR(50),
    lab_name VARCHAR(150),
    tested_on TIMESTAMP,
    uploaded_at TIMESTAMP DEFAULT now()
);

CREATE TABLE water_resources (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT REFERENCES farmers(id),
    source VARCHAR(30),                 -- BOREWELL, OPEN_WELL, CANAL, RAINFED_ONLY, MUNICIPAL, POND_TANK
    availability_liters_per_day DOUBLE PRECISION,
    irrigation_method VARCHAR(30),      -- DRIP, SPRINKLER, FLOOD, MANUAL
    water_tested_for_quality BOOLEAN,
    water_ph DOUBLE PRECISION,
    salinity_ec DOUBLE PRECISION,
    notes VARCHAR(500)
);

CREATE TABLE land_photos (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT REFERENCES farmers(id),
    file_name VARCHAR(255),
    stored_path VARCHAR(500),
    content_type VARCHAR(100),
    caption VARCHAR(200),
    uploaded_at TIMESTAMP DEFAULT now()
);

-- ==========================================================================
-- Crop images (shown when a crop is recommended - see CropImageController)
-- ==========================================================================
CREATE TABLE crop_images (
    id BIGSERIAL PRIMARY KEY,
    crop_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    source_credit VARCHAR(255)
);

INSERT INTO crop_images (crop_name, image_url, source_credit) VALUES
('Tomato', '/images/crops/tomato.jpg', NULL),
('Paddy (Rice)', '/images/crops/paddy.jpg', NULL),
('Rose', '/images/crops/rose.jpg', NULL),
('Lotus', '/images/crops/lotus.jpg', NULL);

-- ==========================================================================
-- Marketplace orders + payments (buyer purchases produce, pays seller)
-- ==========================================================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT REFERENCES produce_listings(id),
    buyer_user_id BIGINT REFERENCES users(id),
    seller_user_id BIGINT REFERENCES users(id),
    crop_name VARCHAR(100),
    quantity_kg DOUBLE PRECISION,
    price_per_kg DOUBLE PRECISION,
    total_amount_rupees DOUBLE PRECISION,
    status VARCHAR(20) DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED
    created_at TIMESTAMP DEFAULT now(),
    paid_at TIMESTAMP
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    method VARCHAR(20),                 -- CARD, UPI, NETBANKING, WALLET
    amount_rupees DOUBLE PRECISION,
    status VARCHAR(20) DEFAULT 'CREATED', -- CREATED, SUCCESS, FAILED, REFUNDED
    created_at TIMESTAMP DEFAULT now(),
    settled_at TIMESTAMP
);

-- Sample seed data for a couple of crops so the demo works out of the box
INSERT INTO crop_guides (crop_name, suitable_land_types, suitable_scale, duration_days,
    land_preparation_steps, sowing_instructions, watering_schedule, fertilizer_plan,
    pest_watch, harvest_guide, estimated_cost_per_acre_or_unit, estimated_yield_per_acre_or_unit)
VALUES
('Tomato', 'TERRACE,BACKYARD,ROOFTOP_CONTAINER', 'SMALL_SCALE_URBAN', 90,
 'Fill grow bags/containers with 3:1:1 soil-compost-sand mix.',
 'Sow seeds 0.5cm deep, transplant seedlings after 3 weeks.',
 'Water every 2 days; increase during flowering.',
 'Apply compost at planting, NPK 19:19:19 every 15 days after transplant.',
 'Watch for aphids and fruit borer; use neem oil spray weekly.',
 'Harvest when fruit is firm and fully colored, typically day 75-90.',
 1500, 40),
('Paddy (Rice)', 'OPEN_FIELD', 'LARGE_SCALE', 120,
 'Plough field 2-3 times, level and prepare nursery beds.',
 'Transplant 21-day old seedlings at 20x15cm spacing.',
 'Maintain 2-5cm standing water through vegetative stage.',
 'Apply urea in 3 split doses; basal DAP at transplanting.',
 'Monitor for stem borer and blast disease.',
 'Harvest when 80% grains turn golden yellow.',
 25000, 2200);
