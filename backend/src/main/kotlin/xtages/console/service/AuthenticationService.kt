package xtages.console.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import xtages.console.config.CognitoUserId

@Service
class AuthenticationService {

    /**
     * Returns the cognito id of the currently signed-in user.
     */
    val currentCognitoUserId: CognitoUserId
        get() {
            return CognitoUserId((SecurityContextHolder.getContext().authentication.principal as Jwt).subject)
        }
}
