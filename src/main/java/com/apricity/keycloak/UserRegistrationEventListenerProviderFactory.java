package com.apricity.keycloak;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserRegistrationEventListenerProviderFactory implements EventListenerProviderFactory {

    private Connection connection;
    private Channel channel;
    private final String EXCHANGE_NAME = "user-registration-events";

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UserRegistrationEventListenerProvider(keycloakSession, channel, EXCHANGE_NAME);
    }

    @Override
    public void init(Config.Scope scope) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq"));
            connectionFactory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "guest"));
            connectionFactory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "guest"));

            this.connection = connectionFactory.newConnection();
            this.channel = connection.createChannel();
            this.channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
            System.out.println("APRICITY-FACTORY: Connected to RabbitMQ successfully.");

        } catch (Exception e) {
            System.err.println("APRICITY-FACTORY: Failed to connect to RabbitMQ: " + e.getMessage());
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {}

    @Override
    public void close() {
        try {
            if (channel != null) channel.close();
            if (connection != null) connection.close();
        } catch (Exception ignored) {}
    }

    @Override
    public String getId() {
        return "apricity-rabbitmq-listener";
    }
}