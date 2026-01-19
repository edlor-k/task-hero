package ru.taskhero.userservice.service;

import ru.taskhero.userservice.dto.ChildLoginRequestDto;
import ru.taskhero.userservice.dto.LoginResponseDto;
import ru.taskhero.userservice.dto.UserLoginRequestDto;

public interface AuthService {

    /**
     * Авторизация родителя или администратора.
     */
    LoginResponseDto loginUser(UserLoginRequestDto request);

    /**
     * Авторизация ребёнка по loginToken.
     */
    LoginResponseDto loginChild(ChildLoginRequestDto request);
}
