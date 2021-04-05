FROM amazoncorretto:15.0.2

#RUN addgroup -S xtages && adduser -S xtages -G xtages
#USER xtages:xtages


ARG JAR_FILE=build/libs/console*.jar
COPY ${JAR_FILE} console.jar
EXPOSE 8080
CMD java -jar console.jar