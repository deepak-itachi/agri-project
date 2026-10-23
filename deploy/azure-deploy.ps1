<#
  Deploys AgriSetu to Azure from Windows PowerShell.
    Database : Azure Database for PostgreSQL Flexible Server
    Backend  : Azure App Service (Linux, Java 17)
    Frontend : Azure Static Web Apps (Free)
  Before running:  winget install Microsoft.AzureCLI   then   az login
  Run from the project root:   .\deploy\azure-deploy.ps1
  Edit the values in the first block. Names marked UNIQUE must be unique across all of Azure.
#>
$ErrorActionPreference = "Stop"

# ------------------------------ edit these ------------------------------
$Prefix      = "agrisetu"          # UNIQUE: lowercase letters and numbers only, e.g. agrisetu42
$Location    = "centralindia"      # if the database is refused here, try southindia or eastasia
$DbPassword  = Read-Host "Choose a database password (12+ chars, letters, numbers, symbol)"
$AdminEmail  = "admin@agrisetu.in"
$AdminPass   = Read-Host "Choose the website ADMIN password"
$SeedDemo    = "true"              # "false" removes demo sellers/buyers/products
$RazorpayId  = ""                  # optional; leave empty for demo checkout
$RazorpaySec = ""
# ------------------------------------------------------------------------

$Rg      = "$Prefix-rg"
$Db      = "$Prefix-db"
$Plan    = "$Prefix-plan"
$Api     = "$Prefix-api"           # backend  -> https://$Api.azurewebsites.net
$Web     = "$Prefix-web"           # frontend
$JwtSecret = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })

Write-Host "1/7 Resource group" -ForegroundColor Green
az group create -n $Rg -l $Location -o none

Write-Host "2/7 PostgreSQL (takes 5-10 minutes)" -ForegroundColor Green
az postgres flexible-server create -g $Rg -n $Db -l $Location --admin-user agriadmin --admin-password $DbPassword `
  --sku-name Standard_B1ms --tier Burstable --storage-size 32 --version 16 --public-access 0.0.0.0 --yes -o none
az postgres flexible-server db create -g $Rg -s $Db -d agridb -o none

Write-Host "3/7 Backend app service" -ForegroundColor Green
az appservice plan create -g $Rg -n $Plan --is-linux --sku B1 -o none
az webapp create -g $Rg -p $Plan -n $Api --runtime "JAVA:17-java17" -o none

$DbUrl = "jdbc:postgresql://$Db.postgres.database.azure.com:5432/agridb?sslmode=require"
$WebUrl = "https://$Web.azurestaticapps.net"   # replaced below with the real address
az webapp config appsettings set -g $Rg -n $Api -o none --settings `
  DB_URL=$DbUrl DB_USER=agriadmin DB_PASSWORD=$DbPassword `
  JWT_SECRET=$JwtSecret ADMIN_EMAIL=$AdminEmail ADMIN_PASSWORD=$AdminPass SEED_DEMO=$SeedDemo `
  RAZORPAY_KEY_ID=$RazorpayId RAZORPAY_KEY_SECRET=$RazorpaySec `
  FILE_UPLOAD_DIR=/home/uploads SERVER_PORT=8080 WEBSITES_PORT=8080 CORS_ORIGINS="*"
az webapp config set -g $Rg -n $Api --always-on true --http20-enabled true -o none
az webapp update -g $Rg -n $Api --https-only true -o none

Write-Host "4/7 Build and upload backend" -ForegroundColor Green
Push-Location backend
mvn -q clean package -DskipTests
Pop-Location
az webapp deploy -g $Rg -n $Api --src-path backend/target/agri-backend-1.0.0.jar --type jar -o none

Write-Host "5/7 Static web app" -ForegroundColor Green
az staticwebapp create -g $Rg -n $Web -l eastasia --sku Free -o none
$WebHost = az staticwebapp show -g $Rg -n $Web --query defaultHostname -o tsv
$Token   = az staticwebapp secrets list -g $Rg -n $Web --query properties.apiKey -o tsv

Write-Host "6/7 Build and upload frontend" -ForegroundColor Green
Push-Location frontend
$env:REACT_APP_API_BASE = "https://$Api.azurewebsites.net"
npm install
npm run build
npx --yes @azure/static-web-apps-cli deploy ./build --deployment-token $Token --env production
Pop-Location

Write-Host "7/7 Lock the backend to your website address" -ForegroundColor Green
az webapp config appsettings set -g $Rg -n $Api -o none --settings CORS_ORIGINS="https://$WebHost"

Write-Host ""
Write-Host "DONE. Open your website:  https://$WebHost" -ForegroundColor Cyan
Write-Host "Backend health / API docs: https://$Api.azurewebsites.net/swagger-ui/index.html"
Write-Host "Admin sign in: https://$WebHost/signin/admin  ($AdminEmail)"
Write-Host "First start takes about a minute while tables are created."
Write-Host "To stop all charges later:  az group delete -n $Rg --yes"
