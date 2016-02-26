package com.companyx.harness;


import com.companyx.domain.AppMessage;
import com.companyx.domain.AppMessageResponse;
import com.companyx.service.AppMessageServiceAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientAsyncXmlDriven {

    static Logger log = LoggerFactory.getLogger(ClientAsyncConfigurationDriven.class);

    public static void main(String[] args) {
        log.info("Starting ClientAsyncXmlDriven");
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-client-async.xml");

        for (String bean : ctx.getBeanDefinitionNames() )
            System.out.println("bean: " + bean.toString());

        final AppMessageServiceAsync messageService = (AppMessageServiceAsync)ctx.getBean("appMessageServiceAsync");

        // Number of threads
        int threads = 5;
        // Number of Message / Thread
        final int messages = 5;
        for (int n = 0; n < threads; n++) {
            new Thread() {
                public void run() {
                    AppMessage message = null;

                    // Warmup to avoid initial TimeoutException
                    try { Thread.sleep(500); } catch (InterruptedException e) { }

                    for (int i = 0; i < messages; i++) {
                        message = new AppMessage();
                        message.setId(UUID.randomUUID().toString());
                        message.setMessage("A message");

                        Future<AppMessageResponse> future = messageService.sendMessage(message);
                        try {
                            // Add timeout to meet SLAs
                            AppMessageResponse appMessageResponse = future.get(5000, TimeUnit.MILLISECONDS);
                            log.debug("Thread id: " + Thread.currentThread().getId() + " Received request id: " + message.getId() + " response id: " + appMessageResponse.getId() + " Message: " + appMessageResponse.getMessage());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("Thread id: " + Thread.currentThread().getId() + " Completed");
                }
            }.start();
        }
    }
}
