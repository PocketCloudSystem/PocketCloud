import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MapperTest {

    @Test
    void testMapperSpeed() {
        int entries = 10000;
        List<ICloudPlayer> players = new ArrayList<>();

        UUID uuid = UUID.randomUUID();
        String namePrefix = StringUtils.generate(16);
        String addressPrefix = StringUtils.generate(16);
        String xboxId = StringUtils.generate(32);

        System.out.println("Preparing n=" + entries + " entries...");

        for (int i = 0; i < entries; i++) {
            CloudPlayer player = new CloudPlayer(namePrefix + i, addressPrefix + i, xboxId + i, uuid, 1001, "1.26.40");
            players.add(player);
        }

        System.out.println("Starting mapper speed test... (" + entries + "x entries will be tested)");
        Instant start = Instant.now();

        List<Duration> times = new ArrayList<>();

        for (ICloudPlayer player : players) {
            long nanos = System.nanoTime();
            MapperUtils.toMap(player);
            times.add(Duration.ofNanos(System.nanoTime() - nanos));
        }

        System.out.println("Test ended");
        System.out.println("Mapped objects: " + entries);
        System.out.println("Time taken: " + Duration.between(start, Instant.now()).toMillis() + " ms");
        System.out.println("Average time: " + (times.stream().mapToLong(Duration::toMillis).average().getAsDouble() * 1000D) + " µs");
        System.out.println("First run: " + (times.getFirst().toMillis()) + " ms");
        System.out.println("Second run: " + (times.get(1).toNanos() / 1000) + " µs");
        System.out.println("Last run: " + (times.getLast().toNanos() / 1000) + " µs");
    }
}