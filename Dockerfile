FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY standalone/MappingServer.java /app/standalone/MappingServer.java
RUN mkdir -p /app/out && javac -encoding UTF-8 -d /app/out /app/standalone/MappingServer.java

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/out /app/out
COPY standalone/web /app/standalone/web

EXPOSE 8080
CMD ["sh", "-c", "SERVER_PORT=${PORT:-8080} java -cp /app/out MappingServer"]
