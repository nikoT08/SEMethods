FROM amazoncorretto:17
COPY ./target/SEMCode-1.0-SNAPSHOT.jar /tmp
WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "SEMCode-1.0-SNAPSHOT.jar"]