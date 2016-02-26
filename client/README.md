# Spring Integration Sample

This represent the client for async request/reply usage of Spring Integration communication to ActiveMQ.

The business logic (AppMessageService) is an interface that is proxied within Spring via the Gateway:

    <int:gateway id="appMessageService"
                 service-interface="com.companyx.service.AppMessageServiceSync"
                 default-request-channel="requestChannel"/>

Run the application via either the ClientAsyncConfigurationDriven or the ClientAsyncXmlDriven classes:
application.properties should be edited for broker.url pointing to correct ActiveMQ.

