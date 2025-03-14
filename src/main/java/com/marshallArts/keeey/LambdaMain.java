package com.marshallArts.keeey;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LambdaMain {

    public static void main(final String[] args) {
        AWSLambda.main(new String[] { Handler.class.getName() + "::handleRequest" });
    }

    public static final class Handler implements RequestHandler<APIGatewayV2HTTPEvent, String> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private static final DynamoDbClient DYNAMO_CLIENT = DynamoDbClient.builder()
                .build();
        private static final String DYNAMO_TABLE_NAME = System.getenv("DYNAMO_TABLE_NAME");
        public record PutEvent(String key, String value) { }
        public record GetEvent(String key) { }

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

        public String handlePutRequest(final PutEvent putEvent, final APIGatewayV2HTTPEvent event, final Context context) {
            DYNAMO_CLIENT.putItem(PutItemRequest.builder()
                    .tableName(DYNAMO_TABLE_NAME)
                    .item(Map.of(
                            "key", AttributeValue.builder().s(putEvent.key()).build(),
                            "value", AttributeValue.builder()
                                    .m(EnhancedDocument.fromJson(putEvent.value()).toMap())
                                    .build()
                    ))
                    .build()
            );
            return "Success";
        }

        @SneakyThrows
        private String handleGetRequest(final GetEvent getEvent, final APIGatewayV2HTTPEvent event, final Context context) {
            final GetItemResponse item = DYNAMO_CLIENT.getItem(GetItemRequest.builder()
                    .tableName(DYNAMO_TABLE_NAME)
                    .key(Map.of(
                            "key", AttributeValue.builder().s(getEvent.key()).build()
                    ))
                    .build()
            );
            final ObjectNode jsonNode = OBJECT_MAPPER.readValue(
                    EnhancedDocument.fromAttributeValueMap(item.item()).toJson(),
                    ObjectNode.class
            );

            return OBJECT_MAPPER.writeValueAsString(jsonNode.get("value"));
        }
    }
}
