
package org.fcrepo.apix.poc.route.triplestore;

import org.apache.camel.spring.boot.CamelSpringBootApplicationController;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@EnableAutoConfiguration
@Configuration
@ImportResource({"classpath*:applicationContext.xml"})
public class Main {

    public static void main(String[] args) {

        SpringApplication.run(Main.class, args)
                .getBean(CamelSpringBootApplicationController.class).run();
    }
}
