import de.pocketcloud.common.util.FormatUtils;
import de.pocketcloud.common.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

public final class ByteBufTest {

    @Test
    void testByteBufferWriteSpeed() {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();

        String string = StringUtils.generate(32);
        byte[] bytes = string.getBytes(CharsetUtil.UTF_8);
        Instant start = Instant.now();
        for (int i = 0; i < 1_000_000; i++) {

            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }

        System.out.println("ByteBuf generated in " + Duration.between(start, Instant.now()).toMillis() + " ms");
        System.out.println("ByteBuf length: " + byteBuf.readableBytes());
        System.out.println("ByteBuf size (human readable): " + FormatUtils.bytes((long) byteBuf.readableBytes()));

        start = Instant.now();
        ByteBuf copy = Unpooled.buffer(byteBuf.readableBytes());
        byteBuf.readBytes(copy, byteBuf.readableBytes());

        System.out.println("ByteBuf copied in " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }
}