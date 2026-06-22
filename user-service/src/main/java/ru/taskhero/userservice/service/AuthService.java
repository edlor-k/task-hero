package ru.taskhero.userservice.service;

import ru.taskhero.userservice.dto.ChildLoginRequestDto;
import ru.taskhero.userservice.dto.LoginResponseDto;
import ru.taskhero.userservice.dto.SocialLoginRequestDto;
import ru.taskhero.userservice.dto.UserLoginRequestDto;

public interface AuthService {

    LoginResponseDto loginUser(UserLoginRequestDto request);

    LoginResponseDto loginChild(ChildLoginRequestDto request);

    /**
     * Вход через SSO: ищет пользователя по email или создаёт нового родителя.
     */
    LoginResponseDto socialLogin(SocialLoginRequestDto request);
}
