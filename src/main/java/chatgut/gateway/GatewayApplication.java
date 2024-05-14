package chatgut.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/principal").permitAll()
                        .anyExchange().authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

}

@RestController
class MyController {

    @GetMapping("/principal")
    public Mono<ResponseEntity<String>> getPrincipal(ServerWebExchange exchange) {
        return exchange.getPrincipal().map(Principal::getName)
                .map(principalName -> new ResponseEntity<>(principalName, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));
    }
}


@Configuration
class SimpleGateway {
    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(rs -> rs
                        .path("/posts/**")
                        .filters(f -> f
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        .uri("http://localhost:8000"))
                .route(rs -> rs
                        .path("/users/**")
                        .filters(f -> f
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        .uri("http://localhost:8002"))
                .route(rs -> rs
                        .path("/like/**")
                        .filters(f -> f
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        .uri("http://localhost:8005/"))
                .route(rs -> rs
                        .path("/images/**")
                        .filters(f -> f
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        .uri("http://localhost:8001/"))
                .route(rs -> rs
                        .path("/short/**")
                        .filters(f -> f
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        .uri("http://localhost:8004/"))
                .route(rs -> rs
                        .path("/**")
                        .filters(f -> f
                                //.stripPrefix(1)
                                .filter(new PreGatewayFilterFactory().apply(new PreGatewayFilterFactory.Config()))
                                .tokenRelay())
                        //.filters(f -> f.rewritePath("/api/(?<path>.*)", "/${path}"))
                        .uri("http://localhost:8080"))
                .build();
    }
}

@Component
class PreGatewayFilterFactory extends AbstractGatewayFilterFactory<PreGatewayFilterFactory.Config> {

    public PreGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .map(Principal::getName)
                .defaultIfEmpty("Default User")
                .flatMap(userName -> {
                    //adds header to proxied request
                    ServerHttpRequest request = exchange.getRequest().mutate().header("userId", userName).build();
                    //use builder to manipulate the request
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    public static class Config {
        //Put the configuration properties for your filter here
    }
}

//@Component
//class GlobalPreGatewayFilter implements GlobalFilter, Ordered {
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        return exchange.getPrincipal()
//                .map(Principal::getName)
//                .defaultIfEmpty("Default User")
//                .map(userName -> {
//                    //adds header to proxied request
//                    ServerHttpRequest request = exchange.getRequest().mutate().header("userId", userName).build();
//                    return exchange.mutate().request(request).build();
//                })
//                .flatMap(chain::filter);
//    }
//
//    @Override
//    public int getOrder() {
//        // The order of precedence for this filter (lower value means higher precedence).
//        return -1;
//    }
//}
