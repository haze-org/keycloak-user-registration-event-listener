package com.haze.keycloak;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;

public class UserRegistrationEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final Channel channel;
    private final String exchange;
    private final JsonMapper jsonMapper = new JsonMapper();

    public UserRegistrationEventListenerProvider(KeycloakSession session, Channel channel, String exchange) {
        this.session = session;
        this.channel = channel;
        this.exchange = exchange;
    }

    @Override
    public void onEvent(Event event) {
        if ("REGISTER".equals(event.getType().toString())) {

            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, event.getUserId());

            String routingKey = "user.created";

            Map<String, Object> messageBody = new HashMap<>();
            Map<String, String> details = event.getDetails();
            messageBody.put("userId", event.getUserId());
            messageBody.put("email", details.get("email"));
            messageBody.put("firstName", user.getFirstName());
            messageBody.put("lastName", user.getLastName());
            messageBody.put("username", details.get("username"));

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .build();

            try {
                byte[] body = jsonMapper.writeValueAsBytes(messageBody);
                channel.basicPublish(exchange, routingKey, props, body);
                System.out.println("HAZE-WORKER: Sent message to RabbitMQ for user: " + event.getUserId());
            } catch (Exception e) {
                System.err.println("HAZE-WORKER: Failed to publish event: " + e.getMessage());
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
    }

    @Override
    public void close() {
    }
}