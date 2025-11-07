package com.imbilalbutt.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


// 1. We have created a Filter class
// Filter class in Spring boot is a custom class that allows us to intercept http request
// apply custom logic and then decide whether to continue proceesing the request or not
@Component
public class JwtValidationGatewayFilterFactory
//    3. By extending from the AbstractGatewayFilterFactory, we are telling Spring boot and spring boot cloud gateway
//    dependencies that we want to add this filter to the request life cycle
        extends AbstractGatewayFilterFactory<Object> {

//        5. First of all we need a request to validate the end point and for this we need Webclient
    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder
//      6. Purpose of having value holder is because
//         in docker auth-service:4005
//         in cloud ecs.aws.SDASasa:5000
    , @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }


//     2. We can use a filter to call Auth-service validation endpoint to check if token is valid or not
//     based on that we handle the request in appropriate way
    @Override
    public GatewayFilter apply(Object config) {
//        7. exchange variable here is an object that gets passed to us by Spring Gateway that holds all the properties
//        for the current request
//        Chain variable is a variable that manages the chain of filters that currently exists in the filter chain
        return (exchange, chain) -> {
//         8. first, we are going to check the request for an authorization token
            String token =  exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION); // also contains Bearer work
//            9. Next we are going to check if token exists and it is in correct format before we call auth-service
            if(token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return webClient.get()
                    .uri("/validate")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .toBodilessEntity()
                    .then(chain.filter(exchange));
        };
    }
}

// 4. By extending AbstractGatewayFilterFactory and implementing the apply() method, it measn that
//spring cloud gateway will automatically apply our filter to all the request.
// ALl we have to do is implement business logic in the apply method for how to process and handle the request