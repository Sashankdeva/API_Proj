package com.squadup.controller;

import com.squadup.aop.TrackExecutionTime;
import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.ApiResponses.UserEnvelope;
import com.squadup.dto.ApiResponses.UserUpdated;
import com.squadup.dto.AuthDtos.ChangePasswordRequest;
import com.squadup.dto.PageResponse;
import com.squadup.dto.UpdateProfileRequest;
import com.squadup.dto.UserDto;
import com.squadup.dto.UserSummary;
import com.squadup.security.AccessGuard;
import com.squadup.security.AuthUser;
import com.squadup.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Set<String> SORTABLE =
            Set.of("username", "displayName", "createdAt", "totalHours", "gamesPlayed");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Paginated player directory, e.g. {@code GET /api/v1/users?page=0&size=10&sort=username,asc}.
     * {@code Pageable} is built by Spring from the page/size/sort request parameters.
     */
    @GetMapping
    public PageResponse<UserSummary> directory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(userService.directory(capped(pageable)), s -> s);
    }

    /** {@code GET /api/v1/users/search?username=nin&page=0&size=10} */
    @GetMapping("/search")
    public PageResponse<UserSummary> search(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return PageResponse.of(userService.search(username, PageRequest.of(page, size, Sort.by("username"))), s -> s);
    }

    /** Players of a game, e.g. {@code GET /api/v1/users/players?game=Valorant&sortBy=totalHours&dir=desc}. */
    @GetMapping("/players")
    public List<UserDto> playersOfGame(
            @RequestParam String game,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String dir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = SORTABLE.contains(sortBy) ? sortBy : "username";
        return userService.playersOfGame(game, Sort.by(direction, property));
    }

    /** Public profile: any logged-in player can view it. */
    @GetMapping("/{id}")
    public UserEnvelope get(@PathVariable Long id) {
        return new UserEnvelope(userService.getProfile(id));
    }

    @PutMapping("/{id}")
    @TrackExecutionTime
    public UserUpdated update(@PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request,
                              @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, id);
        return new UserUpdated(true, userService.updateProfile(id, request));
    }

    @PutMapping("/{id}/password")
    public Ack changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request,
                              @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, id);
        userService.changePassword(id, request);
        return Ack.of("Password updated successfully.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Ack> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, id);
        userService.deleteAccount(id);
        return ResponseEntity.ok(Ack.of("Account deleted."));
    }

    /** Caps the page size and drops sort properties that are not on the allow-list. */
    private static Pageable capped(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> SORTABLE.contains(order.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }
}
