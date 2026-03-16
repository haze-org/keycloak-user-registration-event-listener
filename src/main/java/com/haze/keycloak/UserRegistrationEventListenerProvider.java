package com.haze.keycloak;

import com.rabbitmq.client.Channel;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.nio.charset.StandardCharsets;

public class UserRegistrationEventListenerProvider implements EventListenerProvider{

    private final Channel channel;
    private final String exchange;

    public UserRegistrationEventListenerProvider(Channel channel, String exchange) {
        this.channel = channel;
        this.exchange = exchange;
    }

    @Override
    public void onEvent(Event event) {
        if ("REGISTER".equals(event.getType().toString())){
            String routingKey = "user.created";
            String message = String.format("{\\\"userId\\\":\\\"%s\\\", \\\"email\\\":\\\"%s\\\"}",
                    event.getUserId(), event.getDetails().get("email"));

            try{
                channel.basicPublish(exchange, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("HAZE-WORKER: Sent message to RabbitMQ for user: " + event.getUserId());
            }catch (Exception e) {
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