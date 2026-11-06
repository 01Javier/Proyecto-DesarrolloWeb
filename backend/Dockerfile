FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY libs-repo libs-repo
RUN sed -i 's/\r$//' mvnw && sh mvnw -q dependency:go-offline -DskipTests || true
COPY src src
RUN sed -i 's/\r$//' mvnw && sh mvnw -q -DskipTests package && mv target/*.jar app.jar

FROM eclipse-temurin:25-jdk
RUN useradd --system --create-home guali
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER guali
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseSerialGC", "-Xss512k", "-jar", "app.jar"]
