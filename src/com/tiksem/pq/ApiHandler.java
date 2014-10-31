package com.tiksem.pq;

import com.tiksem.pq.data.Success;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.DatabaseManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    @RequestMapping("/login")
    public @ResponseBody Object login(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response){
        User user = DatabaseManager.getInstance().loginOrThrow(login, password);

        response.addCookie(new Cookie("login", login));
        response.addCookie(new Cookie("password", password));
        return user;
    }

    @RequestMapping("/register")
    public @ResponseBody Object register(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);

        return DatabaseManager.getInstance().registerUser(user);
    }

    @RequestMapping("/users")
    public @ResponseBody Object getAllUsers() {
        return DatabaseManager.getInstance().getAllUsers();
    }

    @RequestMapping("/deleteAllUsers")
    public @ResponseBody Object deleteAllUsers() {
        DatabaseManager.getInstance().deleteAllUsers();
        return new Success();
    }
}
