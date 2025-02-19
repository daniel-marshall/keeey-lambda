FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

COPY build/libs/keeey-lambda.jar ./
ENTRYPOINT [ "/usr/bin/java", "-jar", "keeey-lambda.jar" ]
