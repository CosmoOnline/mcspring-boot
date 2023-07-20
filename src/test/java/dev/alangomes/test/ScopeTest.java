package dev.alangomes.test;

import kr.chuyong.springspigot.context.Context;
import kr.chuyong.springspigot.event.SpringEventExecutor;
import kr.chuyong.springspigot.scope.SenderContextScope;
import kr.chuyong.springspigot.scope.SenderScoped;
import dev.alangomes.test.util.SpringSpigotTestInitializer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {TestApplication.class, ScopeTest.CounterService.class},
        initializers = SpringSpigotTestInitializer.class
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ScopeTest {

    @Autowired
    private CounterService counterService;

    @Autowired
    private Context context;

    @Autowired
    private SpringEventExecutor springEventExecutor;

    @Autowired
    private SenderContextScope senderContextScope;

    @Autowired
    private Server server;

    @Mock
    private Player player1, player2;

    private EventExecutor eventExecutor;

    @Before
    @SneakyThrows
    public void setup() {
        when(player1.getName()).thenReturn("test_player");
        when(player2.getName()).thenReturn("test_player2");
        when(server.getPlayer("test_player")).thenReturn(player1);
        when(server.getPlayer("test_player2")).thenReturn(player2);
        eventExecutor = springEventExecutor.create(SenderContextScope.class.getDeclaredMethod("onQuit", PlayerQuitEvent.class));
    }

    @Test
    public void shouldIsolateScopeForEachSender() {
        context.runWithSender(player1, () -> counterService.setCounter(2));
        context.runWithSender(player2, () -> counterService.setCounter(3));

        Integer counter1 = context.runWithSender(player1, counterService::getCounter);
        Integer counter2 = context.runWithSender(player2, counterService::getCounter);

        assertEquals(Integer.valueOf(2), counter1);
        assertEquals(Integer.valueOf(3), counter2);
    }

    @Test
    @SneakyThrows
    public void shouldClearScopeAfterPlayerQuit() {
        context.runWithSender(player1, () -> counterService.setCounter(2));
        context.runWithSender(player2, () -> counterService.setCounter(3));

        PlayerQuitEvent event = new PlayerQuitEvent(player1, "");
        eventExecutor.execute(senderContextScope, event);

        Integer counter1 = context.runWithSender(player1, counterService::getCounter);
        Integer counter2 = context.runWithSender(player2, counterService::getCounter);
        assertEquals(Integer.valueOf(0), counter1);
        assertEquals(Integer.valueOf(3), counter2);
    }

    @Getter
    @Setter
    @Service
    @SenderScoped
    static class CounterService {

        private Integer counter = 0;

    }


}
