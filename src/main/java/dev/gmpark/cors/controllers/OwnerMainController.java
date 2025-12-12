package dev.gmpark.cors.controllers;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/")
public class OwnerMainController {
    @RequestMapping(value = "/owner", method = RequestMethod.GET, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public String getOwnerMain() {
        return "ownermain/ownermain";
    }
}
