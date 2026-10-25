package com.squadup.service;

import com.squadup.dto.AuthDtos.ChangePasswordRequest;
import com.squadup.dto.UpdateProfileRequest;
import com.squadup.dto.UserDto;
import com.squadup.dto.UserSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Player profile operations. Used by the REST {@code UserController} and by the SOAP
 * {@code UserSoapEndpoint}; both depend on this interface, not on the implementation.
 */
public interface UserService {

    UserDto getProfile(Long userId);

    UserDto getByUsername(String username);

    UserDto updateProfile(Long userId, UpdateProfileRequest request);

    /** Changes the password and signs out every other session; {@code currentToken} stays valid. */
    void changePassword(Long userId, ChangePasswordRequest request, String currentToken);

    void deleteAccount(Long userId);

    Page<UserSummary> search(String usernameFragment, Pageable pageable);

    Page<UserSummary> directory(Pageable pageable);

    /** Players of a game, sorted and limited by {@code pageable}; e-mail addresses are left out. */
    List<UserDto> playersOfGame(String game, Pageable pageable);
}
