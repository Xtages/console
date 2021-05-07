package xtages.console.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.Scheduler
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.cognitoidentity.model.Credentials
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun userAwsSessionCredentialsCache(): Cache<String, Credentials> {
        return Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(500)
            .expireAfter(object : Expiry<String, Credentials> {
                override fun expireAfterCreate(
                    key: String?,
                    value: Credentials,
                    currentTime: Long
                ): Long {
                    // Evict entries 5 minutes before the credentials expire so we don't have problems with
                    // clock drift between Amazon and us.
                    val expiration = value.expiration()
                        .minus(System.currentTimeMillis(), ChronoUnit.MILLIS)
                        .minus(Duration.ofMinutes(5))
                    return TimeUnit.MILLISECONDS.toNanos(expiration.toEpochMilli())
                }

                override fun expireAfterUpdate(
                    key: String?,
                    value: Credentials,
                    currentTime: Long,
                    currentDuration: Long
                ): Long = currentDuration

                override fun expireAfterRead(
                    key: String?,
                    value: Credentials,
                    currentTime: Long,
                    currentDuration: Long
                ): Long = currentDuration

            })
            .scheduler(Scheduler.systemScheduler())
            .build()
    }
}
