# ---------- Runtime image: Spring Boot backend + Nginx frontend ----------
FROM eclipse-temurin:17-jre

# Install nginx to serve the Angular frontend
RUN apt-get update && \
    apt-get install -y --no-install-recommends nginx && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Backend jar (built by Maven in the pipeline)
COPY backend/target/*.jar app.jar

# Frontend build output (Angular 17+ puts it in dist/frontend/browser/)
COPY frontend/dist/frontend/browser/ /usr/share/nginx/html/

# Nginx config: serve frontend, proxy API to backend
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 8080

# Start backend in background, nginx in foreground
CMD ["sh", "-c", "java -jar app.jar & nginx -g 'daemon off;'"]
