package xtages.console

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import xtages.console.config.ConsoleProperties

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = [ConsoleProperties::class])
class ConsoleApplication

fun main(args: Array<String>) {
    // Disable the JOOQ logo from logs
    System.getProperties().setProperty("org.jooq.no-logo", "true")
    runApplication<ConsoleApplication>(*args)
}
