package com.marshallArts.keeey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaMain {

    public static void main(final String[] args) {
        AWSLambda.main(new String[] {Handler.class.getName() + "::handleRequest"});
    }

    public static final class Handler implements RequestHandler<Handler.Input, String> {

        public record Input() { }

        @Override
        public String handleRequest(final Input input, final Context context) {
            return "Success";
        }
    }
}
