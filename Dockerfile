# ---------- Stage 1: split the jar into layers ----------
FROM eclipse-temurin:17-jre AS extract
WORKDIR /x
COPY backend/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------- Runtime image: Spring Boot backend + Nginx frontend ----------
FROM eclipse-temurin:17-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends nginx && \
    rm -f /etc/nginx/sites-enabled/default && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Least to most frequently changing, so the big layer stays cached
COPY --from=extract /x/extracted/dependencies/ ./
COPY --from=extract /x/extracted/spring-boot-loader/ ./
COPY --from=extract /x/extracted/snapshot-dependencies/ ./
COPY --from=extract /x/extracted/application/ ./

# Frontend build output (Angular 17+ puts it in dist/frontend/browser/)
COPY frontend/dist/frontend/browser/ /usr/share/nginx/html/

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 8080

CMD ["sh", "-c", "java -jar app.jar & nginx -g 'daemon off;'"]
