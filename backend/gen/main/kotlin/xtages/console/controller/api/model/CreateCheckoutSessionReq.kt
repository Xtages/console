package xtages.console.controller.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import javax.validation.Valid

/**
 * Request made to POST /checkout/session
 * @param organizationName 
 * @param priceIds 
 */
data class CreateCheckoutSessionReq(

    @get:Size(min=1)
    @field:JsonProperty("organizationName", required = true) val organizationName: kotlin.String,

    @field:JsonProperty("priceIds", required = true) val priceIds: kotlin.collections.List<kotlin.String>
) {

}

