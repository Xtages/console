package xtages.console.controllers

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * A @{@link Controller} that handles all requests that are not matched by an existing
 * controller and responds with the contents of `public/index.html`. This is necessary
 * to enable client side routing of the React frontend app.
 */
@Controller
class FallbackController {
    companion object {
        val classPathResource = ClassPathResource("public/index.html")
    }

    @ResponseBody
    @RequestMapping("/*")
    fun rootHandler(): ClassPathResource {
        return classPathResource
    }
}
