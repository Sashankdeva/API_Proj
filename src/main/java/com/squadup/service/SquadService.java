package com.squadup.service;

import com.squadup.dto.SquadDtos.CreateSquadRequest;
import com.squadup.dto.SquadDtos.SquadDto;
import com.squadup.dto.SquadDtos.SquadMatch;
import com.squadup.dto.SquadSearchCriteria;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.matching.SquadMatcher;
import com.squadup.repository.MessageRepository;
import com.squadup.repository.SquadRepository;
import com.squadup.repository.UserRepository;
import com.squadup.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class SquadService {

    private static final Logger log = LoggerFactory.getLogger(SquadService.class);

    /** Upper bound of squads scored per recommendation request. */
    private static final int MAX_CANDIDATES = 500;

    private final SquadRepository squads;
    private final UserRepository users;
    private final MessageRepository messages;
    private final ObjectProvider<SquadMatcher> matchers;
    private final RateLimiter rateLimiter;

    /**
     * {@code ObjectProvider} gives lazy access to a bean. Because {@link SquadMatcher} is
     * prototype-scoped, every {@code matchers.getObject()} call returns a new instance.
     */
    public SquadService(SquadRepository squads, UserRepository users, MessageRepository messages,
                        ObjectProvider<SquadMatcher> matchers, RateLimiter rateLimiter) {
        this.squads = squads;
        this.users = users;
        this.messages = messages;
        this.matchers = matchers;
        this.rateLimiter = rateLimiter;
    }

    public List<SquadDto> squadsOf(Long userId) {
        return squads.findByMembers_IdOrderByCreatedAtDesc(userId).stream().map(SquadDto::from).toList();
    }

    public List<SquadDto> notJoinedBy(Long userId) {
        return squads.findNotJoinedBy(userId).stream().map(SquadDto::from).toList();
    }

    /**
     * The mapper runs inside the read-only transaction, while lazy associations (members,
     * owner) can still be loaded, so v1 and v2 of the API can each pick their own DTO.
     */
    public <T> Page<T> search(SquadSearchCriteria criteria, Pageable pageable, Function<Squad, T> mapper) {
        return squads.search(criteria, pageable).map(mapper);
    }

    public <T> Page<T> mostPopular(Pageable pageable, Function<Squad, T> mapper) {
        return squads.findMostPopular(pageable).map(mapper);
    }

    public <T> T get(Long squadId, Function<Squad, T> mapper) {
        return mapper.apply(load(squadId));
    }

    /** Only the newest open squads are scored, so one request never loads the whole table. */
    public List<SquadMatch> recommend(Long userId, int limit) {
        User player = loadUser(userId);
        SquadMatcher matcher = matchers.getObject().forPlayer(player);
        squads.search(new SquadSearchCriteria(null, null, null, true, userId), PageRequest.of(0, MAX_CANDIDATES))
                .forEach(matcher::consider);
        return matcher.top(limit);
    }

    @Transactional
    public SquadDto create(Long ownerId, CreateSquadRequest req) {
        rateLimiter.check(RateLimiter.Rule.SQUAD_CREATE, ownerId);
        User owner = loadUser(ownerId);
        String language = (req.language() == null || req.language().isBlank()) ? "English" : req.language().trim();
        int maxMembers = req.maxMembers() == null ? 5 : req.maxMembers();

        Squad squad = squads.save(new Squad(req.name().trim(), req.game().trim(), language, maxMembers, owner));
        log.info("'{}' created squad '{}' ({})", owner.getUsername(), squad.getName(), squad.getGame());
        return SquadDto.from(squad);
    }

    /**
     * The squad row is locked (SELECT ... FOR UPDATE) until the transaction commits, so two
     * players joining at the same moment cannot overfill the squad.
     */
    @Transactional
    public SquadDto join(Long squadId, Long userId) {
        Squad squad = squads.findByIdForUpdate(squadId)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found."));
        if (squad.isMember(userId)) {
            throw ApiException.conflict("Already a member.");
        }
        if (squad.isFull()) {
            throw ApiException.conflict("Squad is full.");
        }
        squad.getMembers().add(loadUser(userId));
        return SquadDto.from(squad);
    }

    /** The owner deletes the whole squad; any other member just leaves it. */
    @Transactional
    public String leaveOrDelete(Long squadId, Long userId) {
        Squad squad = load(squadId);
        if (squad.isOwnedBy(userId)) {
            messages.deleteByRoomId(squad.getRoomId());
            squads.delete(squad);
            return "Squad deleted.";
        }
        if (!squad.isMember(userId)) {
            throw ApiException.badRequest("You are not a member of this squad.");
        }
        squad.getMembers().removeIf(m -> m.getId().equals(userId));
        return "Left squad.";
    }

    private Squad load(Long squadId) {
        return squads.findById(squadId).orElseThrow(() -> new ResourceNotFoundException("Squad not found."));
    }

    private User loadUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
