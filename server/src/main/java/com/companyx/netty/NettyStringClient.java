package com.companyx.netty;

import com.companyx.domain.AppMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
@Component
public class NettyStringClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    @Autowired
    ApplicationContext applicationContext;

    private Channel channel;

    public NettyStringClient() {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
                          b.group(group)
                          .channel(NioSocketChannel.class)
                          .handler(new NettyStringClientInitializer(this));
                                      // Start the connection attempt.
                         this.channel = b.connect(HOST, PORT).sync().channel();

        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    /**
     * Outbound message via Netty to tcp
     */
    public void sendMessage(AppMessage appMessage) {
        ChannelFuture channelFuture = channel.writeAndFlush(appMessage.toString() + "\r\n");
    }

    /**
     * Inbound message via Netty from tcp calling Spring Integration DirectChannel
     * @param message
     */
    public void receiveMessage(String message) {
        DirectChannel directChannel = (DirectChannel)applicationContext.getBean("cardNetworkInChannel");
        Message<String> intMessage = MessageBuilder.withPayload(message).build();
        directChannel.send(intMessage);
    }
}