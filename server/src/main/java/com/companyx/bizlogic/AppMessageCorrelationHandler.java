package com.companyx.bizlogic;

import com.companyx.domain.AppMessage;
import com.companyx.domain.AppMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives the message and will send it back via the reply-to outbound gateway
 *
 * Simulates the Raptor application consuming from a common request queue
 * and replying based on the reply to field which is transparent
 * to the developer.
 *
 */
@Component
public class AppMessageCorrelationHandler {

    static Logger log = LoggerFactory.getLogger(AppMessageCorrelationHandler.class);

    ConcurrentHashMap<String, Message> correlationMap;

    public AppMessageCorrelationHandler() {
        correlationMap = new ConcurrentHashMap<String, Message>();
    }

    /**
     * Just a pass through from the Queue mechanism to capture the reply to and Message ID which
     * is stored into a ConcurrentHashMap
     *
     * NOTE: Input is a Spring Integration Message used to depict what can be done with the original
     * Message.  Headers can be pulled and other data in case some other correlation needs to transpire
     *
     * @param message
     * @return
     */
    public AppMessage processMessage(Message message) {
        AppMessage appMessage = (AppMessage)message.getPayload();

        log.info("\nIn process AppMessage()");
        log.debug("message headers: MessageHeaders.ID:" + message.getHeaders().get(MessageHeaders.ID));
        log.debug("jms_messageId=" + message.getHeaders().get("jms_messageId"));
        log.debug("jms_correlationId=" + message.getHeaders().get("jms_correlationId"));

        /* Store data in a ConcurrentHashMap or other structure here.
         Keyed by a String which is the Message Id from the business transaction and not Spring Integration Message ID.
         Value is the Spring Message which contains the information needed to set the return properties.
        */
        correlationMap.put(appMessage.getId(), message);

        log.debug("\nReceived AppMessage from Amq with id: " + appMessage.getId());

        return appMessage;
    }

    /**
     * This will pass message from the tcp Channel back to the Queue mechanism
     * while setting the replyTo and original Message headers needed.  The item will also be
     * removed the appropriate entry from the Map.
     * @return
     */
    public Message sendMessage(Message message) {

        // Rebuild the AppResponse from the paypload of the Spring Integration Message based on contents returned from netty
        AppMessageResponse appMessageResponse = new AppMessageResponse();
        if ( message.getPayload() != null && (message.getPayload().toString().contains(","))) {
            String[] values = message.getPayload().toString().split(",");
            appMessageResponse.setId(values[0]);
            appMessageResponse.setMessage(values[1]);
        }

        // Pull the original SI Message from the Map
        Message originalMessage = correlationMap.get(appMessageResponse.getId());
        correlationMap.remove(appMessageResponse.getId());

        // Create a new Spring IntegrationMessage to hold the original headers and a new AppMessageResponse paypload
        Message<AppMessageResponse> newMessage = MessageBuilder.withPayload(appMessageResponse).copyHeaders(originalMessage.getHeaders()).build();
        log.debug("jms_correlationId in newMessage.getHeaders():" + newMessage.getHeaders().get("jms_correlationId"));
        log.debug("Sending reply to Amq with id: " + message.getPayload().toString());
        return newMessage;
    }
}
