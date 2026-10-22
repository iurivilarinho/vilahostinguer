package com.bancada.turnstile;

import java.net.http.HttpClient;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Wires the Turnstile check. The filter is added to the security chains, so it is not a servlet filter here. */
@Configuration
@EnableConfigurationProperties(TurnstileProperties.class)
public class TurnstileConfiguration {

	private static final Logger log = LoggerFactory.getLogger(TurnstileConfiguration.class);

	@Bean
	public TurnstileVerifier turnstileVerifier(TurnstileProperties properties) {
		if (properties.partiallyConfigured()) {
			log.warn("Turnstile needs TURNSTILE_SITE_KEY and TURNSTILE_SECRET_KEY together; the check stays off");
		} else if (properties.enabled()) {
			log.info("Turnstile check on for {}", properties.protectedPaths());
		}
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return new TurnstileVerifier(properties, RestClient.builder().requestFactory(factory).build());
	}

	@Bean
	public TurnstileFilter turnstileFilter(TurnstileVerifier verifier, TurnstileProperties properties,
			List<TurnstileFailureHandler> failureHandlers) {
		return new TurnstileFilter(verifier, properties, failureHandlers);
	}

	/** Keeps Spring Boot from also registering the filter in the servlet chain. */
	@Bean
	public FilterRegistrationBean<TurnstileFilter> turnstileFilterRegistration(TurnstileFilter filter) {
		FilterRegistrationBean<TurnstileFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
