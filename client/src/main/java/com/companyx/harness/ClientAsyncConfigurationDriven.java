package com.companyx.harness;

import com.companyx.domain.AppMessage;
import com.companyx.domain.AppMessageResponse;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.messaging.MessageChannel;

import javax.jms.ConnectionFactory;
import java.util.UUID;


@EnableAutoConfiguration
@EnableIntegration
@IntegrationComponentScan
@Configuration
public class ClientAsyncConfigurationDriven
{

    static Logger log = LoggerFactory.getLogger(ClientAsyncConfigurationDriven.class);

    @Value("${request.queue}")
    private String requestQueueName;

    @Value("${reply.queue}")
    private String replyQueueName;

    @Value("${broker.url}")
    private String brokerUrl;

    public static void main(final String[] args)
	{
		log.info("Starting ClientAsyncConfigurationDriven");

		final ConfigurableApplicationContext ctx = new SpringApplication(ClientAsyncConfigurationDriven.class).run();
		final ClientGateway gateway = ctx.getBean(ClientGateway.class);
		// Send Messages
		AppMessage message = null;
		for (int i = 0; i < 50; i++)
		{
			message = new AppMessage();
			message.setId(UUID.randomUUID().toString());
			message.setMessage("Request message with index = " + i);

			final AppMessageResponse response = gateway.sendAndReceive(message);
			log.info("Recieved response id: " + response.getId() + " Message: " + response.getMessage());
		}

		ctx.close();
	}

	@MessagingGateway(defaultRequestChannel = "requestChannel")
	public interface ClientGateway
	{
		AppMessageResponse sendAndReceive(AppMessage appMessage);
	}

	@Bean
	public MessageChannel requestChannel()
	{
		return new DirectChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "requestChannel")
	public JmsOutboundGateway jmsGateway()
	{
		final JmsOutboundGateway jmsOutboundGateway = new JmsOutboundGateway();
		jmsOutboundGateway.setConnectionFactory(this.getConnectionFactory());
		jmsOutboundGateway.setRequestDestinationName(requestQueueName);
		jmsOutboundGateway.setReplyDestinationName(replyQueueName);
		jmsOutboundGateway.setCorrelationKey("JMSCorrelationID");
		jmsOutboundGateway.setExtractRequestPayload(true);
		return jmsOutboundGateway;
	}

	@Bean
	public ConnectionFactory getConnectionFactory()
	{
		final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(brokerUrl);

		final CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
		cachingConnectionFactory.setReconnectOnException(true);
		cachingConnectionFactory.setSessionCacheSize(300);
		return cachingConnectionFactory;
	}

}