package xtages.console.controller.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import javax.validation.Valid

/**
 * An Organization
 * @param name 
 * @param subscriptionStatus 
 */
data class Organization(

    @get:Size(min=1)
    @field:JsonProperty("name") val name: kotlin.String? = null,

    @field:JsonProperty("subscription_status") val subscriptionStatus: Organization.SubscriptionStatus? = null
) {

    /**
    * 
    * Values: UNCONFIRMED,ACTIVE,SUSPENDED,PENDING_CANCELLATION,CANCELLED
    */
    enum class SubscriptionStatus(val value: kotlin.String) {
    
        @JsonProperty("UNCONFIRMED") UNCONFIRMED("UNCONFIRMED"),
    
        @JsonProperty("ACTIVE") ACTIVE("ACTIVE"),
    
        @JsonProperty("SUSPENDED") SUSPENDED("SUSPENDED"),
    
        @JsonProperty("PENDING_CANCELLATION") PENDING_CANCELLATION("PENDING_CANCELLATION"),
    
        @JsonProperty("CANCELLED") CANCELLED("CANCELLED");
    
    }

}

