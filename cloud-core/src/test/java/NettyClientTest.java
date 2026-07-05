import java.net.InetSocketAddress;

public class NettyClientTest {

    public static void main(String[] args) {
        NettyClient client = new NettyClient();
        client.connect(new InetSocketAddress("0.0.0.0", 1913));
    }
}