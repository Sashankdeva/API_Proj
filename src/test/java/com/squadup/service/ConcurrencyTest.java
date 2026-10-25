package com.squadup.service;

import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.SquadDtos.CreateSquadRequest;
import com.squadup.dto.SquadDtos.SquadDto;
import com.squadup.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Operations that race each other must still leave the data consistent. */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    private record Player(long id, String username) {
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private SquadService squadService;

    @Autowired
    private FriendService friendService;

    @Test
    void onlyOneOfManySimultaneousPlayersTakesTheLastSlot() throws Exception {
        Player owner = newPlayer("owner");
        SquadDto squad = squadService.create(owner.id(), new CreateSquadRequest("Last Slot", "Valorant", "English", 2));
        List<Player> racers = IntStream.range(0, 6).mapToObj(i -> newPlayer("racer" + i)).toList();

        List<Object> outcomes = runTogether(racers.stream()
                .<Callable<Object>>map(p -> () -> squadService.join(squad.id(), p.id())).toList());

        assertThat(outcomes.stream().filter(o -> !(o instanceof Throwable)).toList()).hasSize(1);
        assertThat(outcomes.stream().filter(o -> o instanceof Throwable).toList())
                .hasSize(5)
                .allSatisfy(o -> assertThat(((ApiException) o).getStatus().value()).isEqualTo(409));
        int members = squadService.get(squad.id(), s -> s.getMembers().size());
        assertThat(members).isEqualTo(2);
    }

    @Test
    void oppositeFriendRequestsAtTheSameMomentCreateOnlyOneRequest() throws Exception {
        for (int round = 0; round < 8; round++) {
            Player a = newPlayer("pair_a" + round);
            Player b = newPlayer("pair_b" + round);

            List<Object> outcomes = runTogether(List.of(
                    () -> friendService.sendRequest(a.id(), b.username()),
                    () -> friendService.sendRequest(b.id(), a.username())));

            assertThat(outcomes.stream().filter(o -> !(o instanceof Throwable)).toList())
                    .as("round %d: exactly one request goes through", round).hasSize(1);
            assertThat(outcomes.stream().filter(o -> o instanceof Throwable).toList())
                    .allSatisfy(o -> assertThat(((ApiException) o).getStatus().value()).isEqualTo(409));
        }
    }

    private Player newPlayer(String prefix) {
        String name = prefix + "_" + System.nanoTime() % 1_000_000;
        long id = authService.register(new RegisterRequest(name, name + "@test.com", "secret123")).id();
        return new Player(id, name);
    }

    /** Starts all tasks at the same moment; a task's result is its return value or the exception it threw. */
    private static List<Object> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    go.await();
                    try {
                        return task.call();
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }
            go.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(20, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
