FROM 606626603369.dkr.ecr.us-east-1.amazonaws.com/xtages-corretto:15.0.2

RUN yum install -y shadow-utils
RUN useradd -ms /bin/bash console

ARG ENV=dev
ARG JAR_FILE=build/libs/console*.jar

ENV SPRING_PROFILES_ACTIVE=$ENV
COPY ${JAR_FILE} console.jar
EXPOSE 8080
CMD java -jar console.jar
USER console
