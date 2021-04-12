FROM amazoncorretto:15.0.2

#RUN addgroup -S xtages && adduser -S xtages -G xtages
#USER xtages:xtages

ARG ENV=dev
ARG JAR_FILE=build/libs/console*.jar

ENV SPRING_PROFILES_ACTIVE=$ENV
COPY ${JAR_FILE} console.jar
EXPOSE 8080
CMD java -jar console.jar