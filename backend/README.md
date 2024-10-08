# Read Me First
The following was discovered as part of building this project:

* The JVM level was changed from '16' to '11', review the [JDK Version Range](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions#jdk-version-range) on the wiki for more details.

# Getting Started

## Local testing

Assuming that we have a GitHub App for testing purposes add a [`smee`](https://smee.io/) endpoint pointing to it so it can listen to
calls for the app and direct it to your host. 
For example: 
```shell
smee --url https://smee.io/z98JIvw6K0SQtSSp --target http://localhost:8080/api/v1/github/webhook
```

After building the app with `gradle bootJar` you can run the following command to start the backend:
```shell
java -jar build/libs/console-0.0.1-SNAPSHOT.jar 
```


### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.4.4/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.4.4/gradle-plugin/reference/html/#build-image)
* [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring/docs/5.3.5/spring-framework-reference/languages.html#coroutines)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/2.4.4/reference/htmlsingle/#using-boot-devtools)
* [JOOQ Access Layer](https://docs.spring.io/spring-boot/docs/2.4.4/reference/htmlsingle/#boot-features-jooq)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/2.4.4/reference/htmlsingle/#production-ready)
* [Liquibase Migration](https://docs.spring.io/spring-boot/docs/2.4.4/reference/htmlsingle/#howto-execute-liquibase-database-migrations-on-startup)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

