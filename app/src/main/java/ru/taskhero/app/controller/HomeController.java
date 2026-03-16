package ru.taskhero.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для публичных страниц.
 */
@Controller
public class HomeController {

    /**
     * Главная страница.
     */
    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    /**
     * Страница "О проекте".
     */
    @GetMapping("/about")
    public String about() {
        return "about";
    }

    /**
     * Страница "Доступ запрещён".
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
