package com.example.demo;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@Slf4j
public class AsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncApplication.class, args);
    }

    WebClient webClient = WebClient.create("http://localhost:8080");

    private String processRequest() {
        log.info("Start processing request");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Completed processing request");
        return UUID.randomUUID().toString();
    }

    private String reverseString(String s) {
        log.info("Start reversing string");
        String reversed = new StringBuilder(s).reverse().toString();
        log.info("Completed reversing string");
        return reversed;
    }

    @RequestMapping(path = "/sync", method = RequestMethod.GET)
    public String getValueSync() {

        log.info("Request received");

        return processRequest();

    }

    @RequestMapping(path = "/asyncDeferred", method = RequestMethod.GET)
    public DeferredResult<String> getValueAsyncUsingDeferredResult() {

        log.info("Request received");

        DeferredResult<String> deferredResult = new DeferredResult<>();

        ForkJoinPool.commonPool()
                    .submit(() -> deferredResult.setResult(processRequest()));

        log.info("Servlet thread released");

        return deferredResult;

    }

    @RequestMapping(path = "/asyncCompletable", method = RequestMethod.GET)
    public CompletableFuture<String> getValueAsyncUsingCompletableFuture() {

        log.info("asyncCompletable Request received");

        CompletableFuture<String> completableFuture
                = CompletableFuture.supplyAsync(this::processRequest);

        log.info("asyncCompletable Servlet thread released");

        return completableFuture;

    }

    @RequestMapping(path = "/asyncCompletableComposed", method = RequestMethod.GET)
    public CompletableFuture<String> getValueAsyncUsingCompletableFutureComposed() {

        return CompletableFuture
                .supplyAsync(this::processRequest)
                .thenApplyAsync(this::reverseString);

    }

    @RequestMapping(path = "/asyncMono", method = RequestMethod.GET)
    public Mono<String> getValueAsyncUsingMono() {
        log.info("@ Request received");

        Mono<String> result = webClient.get().uri("/asyncCompletable").retrieve().bodyToMono(String.class);

        log.info("@ Servlet thread released");

        return result.map(str -> str.substring(0, 4));
    }

    @RequestMapping(path = "/syncMono", method = RequestMethod.GET)
    public String getValueSyncUsingMono() {
        log.info("@ Request received");

        String result = webClient.get().uri("/asyncCompletable").retrieve().bodyToMono(String.class).block();

        log.info("@ Servlet thread released");

        return result.substring(0, 4);
    }

    @RequestMapping(path = "/mock", method = RequestMethod.GET)
    public String mockGet() {
        return "OK";
    }

    @RequestMapping(path = "/mock", method = RequestMethod.POST)
    public String mockPost() {
        return "OK";
    }
}