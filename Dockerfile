# ---------- 1) Compilar con Gradle (JDK 21) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
# Las pruebas se corren en local (./gradlew test); aquí solo se arma el .jar.
RUN ./gradlew --no-daemon bootJar -x test && cp build/libs/*-SNAPSHOT.jar app.jar

# ---------- 2) Imagen liviana solo con Java 21 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
# Render gratis tiene 512 MB de RAM: se limita la memoria de Java.
ENV JAVA_OPTS="-Xmx320m -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Duser.timezone=America/Lima"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
