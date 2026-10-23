# Deploying AgriSetu on Microsoft Azure

Result: a public website such as `https://agrisetu-web.azurestaticapps.net`, which anyone can open.

| Part | Azure service | Notes |
|---|---|---|
| Website (React) | Static Web Apps, Free plan | HTTPS link included |
| API (Spring Boot) | App Service, Linux, Java 17, B1 plan | seller photos saved under `/home/uploads`, which survives restarts |
| Database | Azure Database for PostgreSQL Flexible Server, B1ms | tables are created automatically |

## Cost
B1 App Service + B1ms PostgreSQL cost roughly ₹2,000 to ₹3,500 per month if left running. With **Azure for Students** (free credit, sign up at azure.microsoft.com/free/students with your college email, no card) the credit covers a project demo. When the demo is over, delete everything with one command (below).

## Steps
1. Install the Azure CLI: `winget install Microsoft.AzureCLI`. Java 17, Maven and Node are already on your PC.
2. Open PowerShell, run `az login`, and sign in with the student account.
3. In the project folder run: `.\deploy\azure-deploy.ps1`
   - Edit `$Prefix` first (for example `agrisetu` plus your initials). It must be unique in Azure.
   - It asks for a database password and the website admin password.
   - It takes about 15 minutes (the database is the slow part).
4. When it finishes it prints your website link. Open it, then sign in at `/signin/admin`.
5. If PowerShell blocks the script: `Set-ExecutionPolicy -Scope Process Bypass` and run it again.

## After it is live
- **Demo data:** the script seeds sample accounts (`seller@demo.in`, `buyer@demo.in`, `learner@demo.in`, password `Demo@1234`). For a public site set `SEED_DEMO=false` in the script, or in the portal: App Service > Environment variables.
- **Payments:** the checkout runs in clearly labelled demo mode until you add `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` as App Service environment variables (use Razorpay test keys first).
- **Weather and mandi prices:** add `OPENWEATHER_API_KEY` and `DATAGOVIN_API_KEY` the same way.
- **Custom name:** Static Web App > Custom domains. Free `*.azurestaticapps.net` works without one.
- **Logs:** App Service > Log stream. The first request after a deploy can take about a minute.

## Updating later
- Backend: `cd backend; mvn clean package -DskipTests` then `az webapp deploy -g <prefix>-rg -n <prefix>-api --src-path target/agri-backend-1.0.0.jar --type jar`
- Frontend: `cd frontend; $env:REACT_APP_API_BASE="https://<prefix>-api.azurewebsites.net"; npm run build; npx @azure/static-web-apps-cli deploy ./build --deployment-token <token>` (get the token with `az staticwebapp secrets list`).

## Remove everything (stops all charges)
`az group delete -n <prefix>-rg --yes`

## Known limits for a real launch
- One backend instance. Seller photos live on its disk (`/home`), so do not scale to several instances without moving photos to Blob Storage.
- Money from buyers lands in your Razorpay account; paying each seller automatically needs Razorpay Route (business verification).
- Change the admin password and use a strong `JWT_SECRET` (the script generates one).
