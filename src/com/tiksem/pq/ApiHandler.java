package com.tiksem.pq;

import com.tiksem.pq.db.DatabaseManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    @RequestMapping("/greeting")
    public @ResponseBody
    Object greeting(
            @RequestParam(value="name", required=false, defaultValue="World") String name) {
        return new Object();
    }

    @RequestMapping("/addUser")
    public @ResponseBody
    Object addUser(
            @RequestParam(value="login", required=true) String login,
            @RequestParam(value="password", required=true) String password) {
        return DatabaseManager.getInstance().addUser(login, password);
    }
}
