package xtages.console

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import xtages.console.config.ConsoleProperties

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = [ConsoleProperties::class])
class ConsoleApplication

fun main(args: Array<String>) {
    runApplication<ConsoleApplication>(*args)
}
