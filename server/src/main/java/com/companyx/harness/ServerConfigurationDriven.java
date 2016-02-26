package com.companyx.harness;

import com.companyx.bizlogic.AppMessageCorrelationHandler;
import com.companyx.domain.AppMessage;
import com.companyx.netty.NettyStringClient;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

@EnableAutoConfiguration
@EnableIntegration
@IntegrationComponentScan
@ComponentScan("com.companyx.harness")
@Configuration
public class ServerConfigurationDriven {

    static Logger log = LoggerFactory.getLogger(ServerConfigurationDriven.class);

    @Value("${request.queue}")
    String requestQueue;

    @Value("${broker.url}")
    String brokerUrl;
    public static void main(String[] args) {
        log.info("Starting Raptor ServerXmlDriven");
        final ConfigurableApplicationContext ctx = new SpringApplication(ServerConfigurationDriven.class).run();
        for (String bean : ctx.getBeanDefinitionNames() )
            log.debug("bean: " + bean.toString());
    }

    @Bean
    public JmsMessageDrivenEndpoint jmsMDEPForQueueRequestChannel()
    {
        ChannelPublishingJmsMessageListener channelPubJmsMsgLstnr = new ChannelPublishingJmsMessageListener();
        channelPubJmsMsgLstnr.setRequestChannel(this.queueRequestChannel());

        DefaultMessageListenerContainer defaultMsgLsnrCtr = new DefaultMessageListenerContainer();
        defaultMsgLsnrCtr.setConcurrentConsumers(5);
        defaultMsgLsnrCtr.setConnectionFactory(this.getConnectionFactory());
        defaultMsgLsnrCtr.setDestination(this.requestQueue());
        defaultMsgLsnrCtr.setMaxConcurrentConsumers(10);
        defaultMsgLsnrCtr.start();

        return new JmsMessageDrivenEndpoint(defaultMsgLsnrCtr,channelPubJmsMsgLstnr);
    }

    @Bean
    @ServiceActivator(inputChannel="queueRequestChannel", outputChannel="nettyOutChannel")
    public ServiceActivatingHandler serviceActivator()
    {
        Class[] parameterTypes = new Class[1];
        parameterTypes[0] = Message.class;
        ServiceActivatingHandler srvActivatingHandler = null;
        try {
            srvActivatingHandler = new ServiceActivatingHandler(this.appMessageCorrelationHandler(), AppMessageCorrelationHandler.class.getMethod("processMessage", parameterTypes));
            srvActivatingHandler.setOutputChannel(nettyOutChannel());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return srvActivatingHandler;
    }

    @Bean
    public DirectChannel nettyOutChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator (inputChannel="nettyOutChannel")
    public ServiceActivatingHandler serviceActivatorToNetty()
    {
        System.out.println("serviceActivatorToNetty() called");
        Class[] parameterTypes = new Class[1];
        parameterTypes[0] = AppMessage.class;
        ServiceActivatingHandler srvActivatingHandler = null;
        try {

            //Todo refactor to use bean
            srvActivatingHandler = new ServiceActivatingHandler(this.switchNettyHandler(), NettyStringClient.class.getMethod("sendMessage", parameterTypes));
            srvActivatingHandler.setRequiresReply(false);
            srvActivatingHandler.setOutputChannel(new NullChannel());
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return srvActivatingHandler;
    }

    @Bean
    public NettyStringClient switchNettyHandler() {
        return new NettyStringClient();
    }

    @Bean
    public DirectChannel cardNetworkInChannel()
    {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator (inputChannel="cardNetworkInChannel", outputChannel="queueReplyChannel")
    public ServiceActivatingHandler serviceActivatorFromNetty()
    {
        System.out.println("serviceActivatorFromNetty() called");
        Class[] parameterTypes = new Class[1];
        parameterTypes[0] = Message.class;
        ServiceActivatingHandler srvActivatingHandler = null;
        try {
            srvActivatingHandler = new ServiceActivatingHandler(this.appMessageCorrelationHandler(), AppMessageCorrelationHandler.class.getMethod("sendMessage", parameterTypes));
            srvActivatingHandler.setRequiresReply(false);
            srvActivatingHandler.setOutputChannel(queueReplyChannel());
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return srvActivatingHandler;
    }

    @Bean
    public NettyStringClient NettyStringClient() {
        return new NettyStringClient();
    }

    @Bean
    public AppMessageCorrelationHandler appMessageCorrelationHandler()
    {
        return new AppMessageCorrelationHandler();
    }

    @Bean
    public DirectChannel queueReplyChannel() {
        return new DirectChannel();
    }

    @Bean
    public EventDrivenConsumer outBoundChannelAdapter()
    {
        SpelExpression spelExpression = (new SpelExpressionParser()).parseRaw("headers['jms_replyTo']");

        JmsTemplate jmsTemplateToSendToResponseQueue = new JmsTemplate(this.getConnectionFactory());
        JmsSendingMessageHandler messageHandler = new JmsSendingMessageHandler(jmsTemplateToSendToResponseQueue);
        messageHandler.setDestinationExpression(spelExpression);

        EventDrivenConsumer outboundChannelAdapter = new EventDrivenConsumer(this.queueReplyChannel(),messageHandler);

        return outboundChannelAdapter;
    }

    @Bean
    public Queue requestQueue()
    {
        return new ActiveMQQueue(requestQueue);
    }

    @Bean
    public MessageChannel queueRequestChannel()
    {
        return new DirectChannel();
    }

    @Bean
    public ConnectionFactory getConnectionFactory()
    {
        ActiveMQConnectionFactory activeMQConnectionFactory = new
                ActiveMQConnectionFactory();
        activeMQConnectionFactory.setBrokerURL(brokerUrl);

        CachingConnectionFactory cachingConnectionFactory = new
                CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setReconnectOnException(true);
        cachingConnectionFactory.setSessionCacheSize(300);
        return cachingConnectionFactory;
    }


}