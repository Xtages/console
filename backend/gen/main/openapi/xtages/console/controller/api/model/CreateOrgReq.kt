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
 * Request made to POST /organization
 * @param organizationName 
 * @param ownerCognitoUserId 
 */
data class CreateOrgReq(

    @get:Size(min=1)
    @field:JsonProperty("organizationName", required = true) val organizationName: kotlin.String,

    @get:Size(min=1)
    @field:JsonProperty("ownerCognitoUserId", required = true) val ownerCognitoUserId: kotlin.String
) {

}

