package com.marshallArts.keeey;

import javax.inject.Named;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marshallArts.keeey.LambdaMain.HandlerDelegate;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public final class LambdaActual implements RequestHandler<APIGatewayV2HTTPEvent, String> {
    private final ObjectMapper objectMapper;
    private final HandlerDelegate<PutEvent> putDelegate;
    private final HandlerDelegate<GetEvent> getDelegate;

    public record PutEvent(String key, String value) { }
    public record GetEvent(String key) { }

    @Override
    @SneakyThrows
    public String handleRequest(final APIGatewayV2HTTPEvent event, final Context context) {
        context.getLogger().log(String.valueOf(event));
        return switch (event.getRequestContext().getHttp().getMethod()) {
            case "PUT" -> putDelegate.handle(
                    objectMapper.readValue(event.getBody(), PutEvent.class),
                    event,
                    context
            );
            case "GET" -> getDelegate.handle(
                    objectMapper.readValue(event.getBody(), GetEvent.class),
                    event,
                    context
            );
            default -> throw new IllegalArgumentException("Unrecognised Event");
        };
    }

    @AllArgsConstructor
    public static final class PutHandler implements HandlerDelegate<PutEvent> {

        private final DynamoDbClient dynamoClient;

        @Named("DYNAMO_TABLE_NAME")
        private final String tableName;

        @Override
        public String handle(
                final PutEvent event,
                final APIGatewayV2HTTPEvent rawEvent,
                final Context context) {
            dynamoClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "key", AttributeValue.builder().s(event.key()).build(),
                            "value", AttributeValue.builder()
                                    .m(EnhancedDocument.fromJson(event.value()).toMap())
                                    .build()
                    ))
                    .build()
            );

            return "Success";
        }
    }

    @AllArgsConstructor
    public static final class GetHandler implements HandlerDelegate<GetEvent> {

        private final ObjectMapper objectMapper;

        private final DynamoDbClient dynamoClient;

        @Named("DYNAMO_TABLE_NAME")
        private final String tableName;

        @Override
        public String handle(
                final GetEvent event,
                final APIGatewayV2HTTPEvent rawEvent,
                final Context context)
                throws Exception {
            final GetItemResponse item = dynamoClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "key", AttributeValue.builder().s(event.key()).build()
                    ))
                    .build()
            );

            final ObjectNode jsonNode = objectMapper.readValue(
                    EnhancedDocument.fromAttributeValueMap(item.item()).toJson(),
                    ObjectNode.class
            );

            return objectMapper.writeValueAsString(jsonNode.get("value"));
        }
    }
}