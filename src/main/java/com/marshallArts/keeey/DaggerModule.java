package com.marshallArts.keeey;

import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marshallArts.keeey.LambdaActual.GetHandler;
import com.marshallArts.keeey.LambdaActual.PutHandler;
import dagger.Provides;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@dagger.Module
public class DaggerModule {

    @Provides
    final RequestHandler<APIGatewayV2HTTPEvent, String> lambda(
            final ObjectMapper objectMapper,
            final PutHandler putDelegate,
            final GetHandler getDelegate) {
        return new LambdaActual(
                objectMapper,
                putDelegate,
                getDelegate
        );
    }

    @Provides
    final GetHandler getHandler(
            final ObjectMapper objectMapper,
            final DynamoDbClient dynamoClient,
            @Named("DYNAMO_TABLE_NAME") final String tableName) {
        return new GetHandler(
                objectMapper,
                dynamoClient,
                tableName
        );
    }

    @Provides
    final PutHandler putHandler(
            final DynamoDbClient dynamoClient,
            @Named("DYNAMO_TABLE_NAME") final String tableName) {
        return new PutHandler(
                dynamoClient,
                tableName
        );
    }

    @Provides
    final ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Provides
    final DynamoDbClient dynamoClient() {
        return DynamoDbClient.builder().build();
    }

    @Provides
    @Named("DYNAMO_TABLE_NAME")
    final String tableName() {
        return System.getenv("DYNAMO_TABLE_NAME");
    }
}
