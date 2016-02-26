# Spring Integration Sample

This represents the server for async request/reply usage of Spring Integration.
The business logic (SwitchMessageHandler) uses Spring Integration annotations (@MessagingEndpoint and
@ServiceActivator).  The important consideration is that the remoting interface and
implementation (in this case JMS) are abstracted behind Spring Integration Channel and
Gateway semantics.


To run the application:

application.properties should be edited for broker.url pointing to correct ActiveMQ.
Run the main method within the com.companyz.harness.ServerXmlDriven class.
Start the client application. Follow the console logs.
