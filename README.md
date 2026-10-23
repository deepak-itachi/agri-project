# AgriSetu — Farmer Learning & Marketplace Platform (formerly AgriSaathi)

An all-in-one guided platform that takes a young/beginner farmer from
**"I have some land"** to **"here's what to grow, how to grow it, and where
to sell it for profit"** — for both small-scale/terrace and large-scale
open-field farmers.

## Architecture

| Requirement | Implementation |
|---|---|
| SOAP web service | `FarmerRequirementEndpoint` (Spring-WS) at `/ws`, WSDL auto-published at `/ws/farmerRequirement.wsdl` |
| Multiple external APIs | OpenWeatherMap (weather), ISRIC SoilGrids (soil), data.gov.in Agmarknet (mandi prices) |
| GUI | React frontend (`/frontend`) |
| Database | PostgreSQL via Spring Data JPA (`/database/schema.sql`) |
| Cloud deployment | Dockerfiles for both backend and frontend, deployable to Cloud Run |

## User Journey (matches the project brief)

1. **Onboarding** — farmer enters land size, land type, location, budget, experience → SOAP service classifies them (small/medium/large scale, terrace vs open-field)
2. **Soil & Weather Analysis** — pulled from SoilGrids + OpenWeatherMap APIs
3. **Crop Recommendation** — filtered by scale + land type
4. **Growth Guide** — full stage-by-stage guide (land prep → sowing → watering → fertilizer → pest watch → harvest)
5. **Profit Prediction** — combines crop cost/yield data with live mandi prices
6. **Market Finder** — live mandi prices + internal produce marketplace listings

## Project Structure

```
agri-project/
├── backend/          # Spring Boot (SOAP + REST + JPA)
├── frontend/         # React GUI
└── database/         # schema.sql (reference / manual DB setup)
```

## Running Locally

### 1. Database
```bash
createdb agridb
psql -U postgres -d agridb -f database/schema.sql
```

### 2. Backend
```bash
cd backend
# set your local DB credentials + any API keys as env vars, or edit application.properties
export DB_URL=jdbc:postgresql://localhost:5432/agridb
export DB_USER=postgres
export DB_PASSWORD=yourpassword
export OPENWEATHER_API_KEY=your_key       # free at openweathermap.org
export DATAGOVIN_API_KEY=your_key         # free at data.gov.in
mvn spring-boot:run
```
Backend runs at `http://localhost:8080`. Test the SOAP WSDL at:
`http://localhost:8080/ws/farmerRequirement.wsdl`

### 3. Frontend
```bash
cd frontend
npm install
npm start
```
Frontend runs at `http://localhost:3000` and calls the backend at `http://localhost:8080` by default (see `src/api.js`).

## Deploying to Google Cloud Run

### Prerequisites
- Google Cloud project with billing enabled
- `gcloud` CLI installed and authenticated (`gcloud auth login`)
- Cloud SQL instance created (PostgreSQL) — or use SQLite/in-memory for a quick demo

### 1. Set up Cloud SQL (PostgreSQL)
```bash
gcloud sql instances create agri-db-instance \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=asia-south1

gcloud sql databases create agridb --instance=agri-db-instance
```

### 2. Deploy the backend
```bash
cd backend
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/agri-backend

gcloud run deploy agri-backend \
  --image gcr.io/YOUR_PROJECT_ID/agri-backend \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated \
  --add-cloudsql-instances YOUR_PROJECT_ID:asia-south1:agri-db-instance \
  --set-env-vars DB_URL="jdbc:postgresql:///agridb?cloudSqlInstance=YOUR_PROJECT_ID:asia-south1:agri-db-instance&socketFactory=com.google.cloud.sql.postgres.SocketFactory",DB_USER=postgres,DB_PASSWORD=yourpassword,OPENWEATHER_API_KEY=your_key,DATAGOVIN_API_KEY=your_key
```
Note the resulting service URL, e.g. `https://agri-backend-xxxxx-el.a.run.app`

### 3. Deploy the frontend
```bash
cd ../frontend
# point the build at your live backend URL
echo "REACT_APP_API_BASE=https://agri-backend-xxxxx-el.a.run.app" > .env.production

gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/agri-frontend

gcloud run deploy agri-frontend \
  --image gcr.io/YOUR_PROJECT_ID/agri-frontend \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated
```

Your site is now live on both Cloud Run services. For a single public URL,
optionally put both behind a load balancer or point your frontend calls at
the backend's Cloud Run URL directly (as done above) — that's sufficient for
a project demo.

## Getting the External API Keys (all free tier)
- **OpenWeatherMap**: https://openweathermap.org/api → sign up → free API key
- **data.gov.in (Agmarknet mandi prices)**: https://data.gov.in/ → register → generate API key
- **ISRIC SoilGrids**: no key required, public REST API

## AgriSetu upgrade: how the site works now

Three separate sign-in windows (`/signin/learner`, `/signin/seller`, `/signin/buyer`) plus an admin window (`/signin/admin`).

| Account | Workspace | What it does |
|---|---|---|
| Learner | `/learn` | Land details, soil report / water / land photo upload, crop suggestions with photos, TNAU-based guides, profit estimate |
| Seller | `/seller` | Add products with their own photo, orders (mark shipped / delivered), customer details, net business done, payout details |
| Buyer | `/market`, `/buyer/orders` | Browse, buy, pay (UPI/GPay/PhonePe/card via Razorpay), track orders |
| Admin | `/admin` | Login activity (who is signed in now, per-role lists, every sign-in), edit users, listings, crop images, crop guides, view orders |

Seller photos appear on the buyer marketplace immediately. Buyers only ever see the seller's shop name; phone, email and payout details are never sent to buyers (separate response objects per role, JWT-checked on the server).

### Accounts
- Admin is created on first start: `admin@agrisetu.in` / `Admin@123`. **Change it** with env vars `ADMIN_EMAIL` and `ADMIN_PASSWORD` before first start.
- Demo data (sample seller, buyer, learner, 8 listings; password `Demo@1234`) is created when `SEED_DEMO=true` (default). Set `SEED_DEMO=false` for a real deployment.
- Set `JWT_SECRET` (any long random string) in production.

### Payments
- No Razorpay keys: **demo checkout** (clearly labelled, no real money, no card numbers collected). Good for showing the whole flow.
- With `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET`: the real Razorpay window opens (UPI apps, cards, net banking) and the server verifies the signature before marking an order paid.
- Money first reaches your Razorpay account; sending it on to each seller automatically needs Razorpay Route (business KYC). Until then, settle sellers manually using the payout details on the admin Users page.

### Run
```bash
createdb agridb
psql -U postgres -d agridb -f database/schema.sql    # fresh database; existing databases update automatically
cd backend && mvn spring-boot:run                     # http://localhost:8080  (API docs: /swagger-ui/index.html)
cd frontend && npm install && npm start               # http://localhost:3000
```
Existing databases: just restart the backend, Hibernate adds the new columns and tables.

### Renaming the project
Edit one line in `frontend/src/brand.js` (`name`). Backend API docs title is in `OpenApiConfig.java`.

### Notes
- Uploaded product photos are stored on the backend's disk under `uploads/`. On Cloud Run use a mounted bucket or object storage, because container disks are temporary.
- Crop reference material: TNAU Agritech Portal https://agritech.tnau.ac.in/
