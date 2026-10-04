package com.squadup.repository;

import com.squadup.dto.SquadSearchCriteria;
import com.squadup.entity.Friendship;
import com.squadup.entity.FriendshipStatus;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit III: CRUD, derived queries, named queries, @Query, pagination & sorting,
 * @Modifying updates and the custom repository implementation.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private SquadRepository squads;

    @Autowired
    private FriendshipRepository friendships;

    @Autowired
    private EntityManager em;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = users.save(player("alice", "Valorant", "English"));
        bob = users.save(player("bob", "CS2", "Spanish"));
        carol = users.save(player("carol", "Valorant", "English"));
    }

    @Test
    void crudOperations() {
        User dave = users.save(new User("dave", "dave@test.com", "hash"));
        assertThat(dave.getId()).isNotNull();                        // Create
        assertThat(users.findById(dave.getId())).isPresent();        // Read

        dave.setBio("Updated bio");                                  // Update
        users.saveAndFlush(dave);
        em.clear();
        assertThat(users.findById(dave.getId()).orElseThrow().getBio()).isEqualTo("Updated bio");

        users.deleteById(dave.getId());                              // Delete
        assertThat(users.findById(dave.getId())).isEmpty();
    }

    @Test
    void namedQueryFindsByUsernameOrEmail() {
        assertThat(users.findByLogin("alice")).map(User::getId).contains(alice.getId());
        assertThat(users.findByLogin("ALICE@TEST.COM")).map(User::getId).contains(alice.getId());
        assertThat(users.findByLogin("Alice")).isEmpty(); // usernames are case-sensitive
    }

    @Test
    void derivedQueryWithPaginationAndSorting() {
        Page<User> page = users.findByUsernameContainingIgnoreCase("O", PageRequest.of(0, 1, Sort.by("username")));

        assertThat(page.getTotalElements()).isEqualTo(2); // bob, carol
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(User::getUsername).containsExactly("bob");
    }

    @Test
    void jpqlQueryWithDynamicSort() {
        List<User> players = users.findPlayersOfGame("Valorant", Sort.by(Sort.Direction.DESC, "username"));
        assertThat(players).extracting(User::getUsername).containsExactly("carol", "alice");
    }

    @Test
    void customSortingWithQueryOrdersByMemberCount() {
        Squad small = squads.save(new Squad("Small", "CS2", "English", 5, bob));
        Squad big = new Squad("Big", "Valorant", "English", 5, alice);
        big.getMembers().add(carol);
        squads.save(big);

        Page<Squad> popular = squads.findMostPopular(PageRequest.of(0, 10));

        assertThat(popular.getContent()).extracting(Squad::getName).containsExactly("Big", "Small");
        assertThat(small.getId()).isNotNull();
    }

    @Test
    void customRepositorySearchCombinesOptionalFilters() {
        Squad open = squads.save(new Squad("Night Owls", "Valorant", "English", 5, alice));
        Squad full = new Squad("Full House", "Valorant", "English", 2, bob);
        full.getMembers().add(carol);
        squads.save(full);
        squads.save(new Squad("Los Tacos", "CS2", "Spanish", 5, bob));
        em.flush();

        var valorantOpen = new SquadSearchCriteria(null, "valorant", null, true, null);
        assertThat(squads.search(valorantOpen, PageRequest.of(0, 10)).getContent())
                .extracting(Squad::getName).containsExactly("Night Owls");

        var textSearch = new SquadSearchCriteria("spanish", null, null, false, null);
        assertThat(squads.search(textSearch, PageRequest.of(0, 10)).getContent())
                .extracting(Squad::getName).containsExactly("Los Tacos");

        var notMine = new SquadSearchCriteria(null, null, null, false, alice.getId());
        assertThat(squads.search(notMine, PageRequest.of(0, 10, Sort.by("name"))).getContent())
                .extracting(Squad::getName).containsExactly("Full House", "Los Tacos");
        assertThat(open.isMember(alice.getId())).isTrue();
    }

    @Test
    void modifyingQueryUpdatesFriendshipStatus() {
        Friendship request = friendships.save(new Friendship(alice, bob, FriendshipStatus.PENDING));

        int updated = friendships.updateStatus(request.getId(), FriendshipStatus.ACCEPTED);

        assertThat(updated).isEqualTo(1);
        assertThat(friendships.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(friendships.findAllForUser(bob.getId(), FriendshipStatus.ACCEPTED)).hasSize(1);
        assertThat(friendships.findBetween(bob.getId(), alice.getId())).isPresent();
    }

    @Test
    void nativeModifyingQueryRemovesMemberships() {
        Squad squad = new Squad("Trio", "Valorant", "English", 5, alice);
        squad.getMembers().add(carol);
        squads.saveAndFlush(squad);

        assertThat(squads.removeMemberFromAllSquads(carol.getId())).isEqualTo(1);
        em.clear();
        assertThat(squads.findByMembers_IdOrderByCreatedAtDesc(carol.getId())).isEmpty();
        assertThat(squads.findNotJoinedBy(carol.getId())).extracting(Squad::getName).contains("Trio");
    }

    private static User player(String name, String favoriteGame, String language) {
        User u = new User(name, name + "@test.com", "hash");
        u.setFavoriteGame(favoriteGame);
        u.setLanguage(language);
        return u;
    }
}
