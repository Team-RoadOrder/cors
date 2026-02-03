package dev.gmpark.cors.controllers;



import dev.gmpark.cors.entities.RegisterEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequestMapping(value = "/")
public class AdminController {


    @RequestMapping(value = "/admin")
    public String getAdmin(
            @SessionAttribute(value = "sessionUser",required = false) RegisterEntity sessionUser,
            Model model
    ) {
        if ( sessionUser == null) {
            return "redirect:/login";
        }
        if (!"admin".equalsIgnoreCase(sessionUser.getUsertype())) {

            if ("customer".equalsIgnoreCase(sessionUser.getUsertype())) {
                return "redirect:/main";
            } else if ("owner".equalsIgnoreCase(sessionUser.getUsertype())) {
                return "redirect:/owner";
            } else {
                return "redirect:/main";
            }
        }
        return "admin/admin";

    }
}
