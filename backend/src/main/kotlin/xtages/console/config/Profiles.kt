package xtages.console.config

/**
 * Enum of profiles corresponding to our environments.
 * Set the active profile by setting `spring.profiles.active`
 * in `application.properties` or setting the `sprint_profiles_active`
 * environment variable.
 */
enum class Profiles {
    PROD,
    STAGING,
    DEV,
    TEST;
}
