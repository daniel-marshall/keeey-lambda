package com.marshallArts.keeey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class LambdaMain {

    public static void main(final String[] args) {
        AWSLambda.main(new String[] { Handler.class.getName() + "::handleRequest" });
    }

    public static final class Handler implements RequestHandler<APIGatewayV2HTTPEvent, String> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        public record PutEvent(String name, Integer age) { }
        public record GetEvent(String name, Integer age) { }

        @Override
        @SneakyThrows
        public String handleRequest(final APIGatewayV2HTTPEvent event, final Context context) {
            context.getLogger().log(String.valueOf(event));
            return switch (event.getRequestContext().getHttp().getMethod()) {
                case "PUT" -> handlePutRequest(
                        OBJECT_MAPPER.readValue(event.getBody(), PutEvent.class),
                        event,
                        context
                );
                case "GET" -> handleGetRequest(
                        OBJECT_MAPPER.readValue(event.getBody(), GetEvent.class),
                        event,
                        context
                );
                default -> throw new IllegalArgumentException("Unrecognised Event");
            };
        }

        public String handlePutRequest(final PutEvent put, final APIGatewayV2HTTPEvent event, final Context context) {
            return "ABC";
        }

        private String handleGetRequest(final GetEvent getEvent, final APIGatewayV2HTTPEvent event, final Context context) {
            return "123";
        }
    }
}
