package org.com.repair.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class SpaRoutingConfig {

    @RequestMapping(
            value = {
                    "/{path:^(?!api|v3|swagger-ui|swagger-ui\\.html)[^\\.]*$}",
                    "/{path:^(?!api|v3|swagger-ui|swagger-ui\\.html)[^\\.]*$}/{nestedPath:[^\\.]*}"
            },
            method = {RequestMethod.GET, RequestMethod.HEAD}
    )
    public String forwardFrontendRoute() {
        return "forward:/index.html";
    }
}
