# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- Serve stage ----
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
# Cloud Run expects the container to listen on $PORT (nginx default 80 is fine
# since Cloud Run maps PORT=8080 by default for nginx images, but override below)
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
