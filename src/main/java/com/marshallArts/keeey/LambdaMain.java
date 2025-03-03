package com.marshallArts.keeey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class LambdaMain {

    public static void main(final String[] args) {
        AWSLambda.main(new String[] {Handler.class.getName() + "::handleRequest"});
    }

    public static final class Handler implements RequestHandler<APIGatewayV2HTTPEvent, String> {

        public record Input(String name, Integer age) { }

        @Override
        public String handleRequest(final APIGatewayV2HTTPEvent event, final Context context) {
            context.getLogger().log(String.valueOf(event).toString());
            return "Success";
        }
    }
}
