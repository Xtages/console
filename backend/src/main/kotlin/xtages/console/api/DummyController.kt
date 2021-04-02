package xtages.console.api

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DummyController {

    @GetMapping("/api/hello")
    fun sayHi(auth: JwtAuthenticationToken, @AuthenticationPrincipal jwt: Jwt): String {
        return "Hello World! ${jwt.subject}: ${jwt.headers} ${jwt.claims}"
    }
}
