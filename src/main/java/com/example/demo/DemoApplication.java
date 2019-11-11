package com.example.demo;

import java.io.File;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


import static java.lang.Math.random;
import static java.net.URI.create;
import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.boundedElastic;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        log.info("CAC");
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    RouterFunction r() {
        log.info("routing setup");
        return route(
                GET("/blocked"), this::blah).andRoute(
                GET("/unblocked"), this::blahUnblocked).andRoute(
                GET("/upload"), req -> ServerResponse.temporaryRedirect(create("/static/upload.html")).build()).andRoute(
                POST("/upload"), this::upload).andOther(
                resources("/**", new ClassPathResource("/static"))
        );
    }

    @Bean
    RouterFunction<ServerResponse> staticResourceRouter(){
        return resources("/**", new ClassPathResource("static/"));
    }

    Mono<ServerResponse> blah(ServerRequest req) {
        log.info("blah");
        return ok().body(just(longRunningMethod()), String.class);
    }

    Mono<ServerResponse> blahUnblocked(ServerRequest req) {
        log.info("blahUnblocked");
        return ok().body(
                fromCallable(this::longRunningMethod).subscribeOn(boundedElastic()),
                String.class
        );
    }

    String longRunningMethod() {
//		log.info("longRunningMethod");
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return String.valueOf(random());
    }

    Mono<ServerResponse> upload(ServerRequest req) {
        log.info("upload");
        return ok().body(save2(req), String.class);
    }


    Mono<String> save(ServerRequest request) {
        return request.body(toMultipartData()).map(
                parts -> {
                    log.info("saving file....");
                    Map<String, Part> map = parts.toSingleValueMap();
                    FilePart filePart = (FilePart) map.get("file");
                    String fileName = filePart.filename();
                    File target = new File("/tmp/" + fileName);
                    filePart.transferTo(target);
                    return target.getAbsolutePath();
                }
        );
    }

    Mono<String> save2(ServerRequest request) {
        return request.body(toMultipartData()).flatMap(
                parts -> {
                    log.info("saving2 file....");
                    FilePart filePart = (FilePart) parts.toSingleValueMap().get("file");
                    return fromCallable(() -> blockingIO(filePart)).subscribeOn(boundedElastic());
                }
        );
    }

    String blockingIO(FilePart filePart) {
        log.info("blockingIO....");
        String fileName = filePart.filename();
        File target = new File("/tmp/" + fileName);
        filePart.transferTo(target);
        return target.getAbsolutePath();
    }

}

