FROM amazoncorretto:17

COPY ./target/SEMCode-1.0-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]