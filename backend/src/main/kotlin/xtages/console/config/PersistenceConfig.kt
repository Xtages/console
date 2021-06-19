package xtages.console.config

import mu.KotlinLogging
import org.jooq.ExecuteContext
import org.jooq.ExecuteListenerProvider
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


private val logger = KotlinLogging.logger { }

@Configuration
class PersistenceConfig {

    @Bean
    fun executeListenerProvider(): ExecuteListenerProvider? {
        return DefaultExecuteListenerProvider(JooqSqlLogger())
    }
}

/**
 * [DefaultExecuteListener] that logs the queries that are executed by JOOQ.
 */
private class JooqSqlLogger : DefaultExecuteListener() {
    override fun executeStart(ctx: ExecuteContext) {
        // Create a new DSLContext for logging rendering purposes
        // This DSLContext doesn't need a connection, only the SQLDialect
        val prettyPrinter = DSL.using(
            ctx.dialect(),  // ... and the flag for pretty-printing
            Settings().withRenderFormatted(true)
        )

        // If we're executing a query
        if (ctx.query() != null) {
            logger.debug { prettyPrinter.renderInlined(ctx.query()) }
        } else if (ctx.routine() != null) {
            logger.debug { prettyPrinter.renderInlined(ctx.routine()) }
        }
    }
}
