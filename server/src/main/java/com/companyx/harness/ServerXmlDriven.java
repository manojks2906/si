package com.companyx.harness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerXmlDriven {

    static Logger log = LoggerFactory.getLogger(ServerXmlDriven.class);

    public static void main(String[] args) {
        log.info("Starting ServerXmlDriven");
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-server.xml");

        for (String bean : ctx.getBeanDefinitionNames() )
            log.info("bean: " + bean.toString());
    }
}
