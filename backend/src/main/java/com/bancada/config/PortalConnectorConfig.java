package com.bancada.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Second local port, only for the customer panel. The gateway publishes this one on the internet;
 * {@link PortalAccessFilter} makes sure nothing of the administrator API answers through it.
 */
@Configuration
public class PortalConnectorConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> portalConnector(@Value("${bancada.portal.port}") int port) {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(port);
            connector.setProperty("address", "127.0.0.1");
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
