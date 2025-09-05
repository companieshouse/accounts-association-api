package uk.gov.companieshouse.accounts.association.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;

class ReactorRequestContextPropagationTest {

    @BeforeAll
    static void setupHook() {
        new ReactorRequestContextPropagation().init();
    }

    private static RequestContextData makeContext(String requestId) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        return new RequestContextData.RequestContextDataBuilder().setXRequestId(mockRequest).build();
    }

    @Test
    void contextShouldSurviveFluxPipeline() {
        final var requestId = "req-" + UUID.randomUUID();
        RequestContext.setRequestContext(makeContext(requestId));

        final var seenIds = Flux.fromIterable(IntStream.range(0, 10).boxed().toList()).flatMapSequential(
                i -> Flux.defer(() -> {
                    var ctx = RequestContext.getRequestContext();
                    return Flux.just(ctx != null ? ctx.getXRequestId() : "missing");
                }).subscribeOn(Schedulers.boundedElastic())).collectList().block();

        RequestContext.clear();

        assertThat(seenIds).containsOnly(requestId);
    }

    @Test
    void manyConcurrentRequestsShouldNotLeakContexts() {
        final var numberOfRequests = 500;
        final var numberOfThreads = 50;

        try (ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads)) {
            List<CompletableFuture<Boolean>> futures = IntStream.range(0, numberOfRequests).mapToObj(
                    i -> CompletableFuture.supplyAsync(() -> {
                        final var requestId = "req-" + UUID.randomUUID();
                        RequestContext.setRequestContext(makeContext(requestId));
                        try {
                            List<String> seenIds = Flux.range(1, 30).flatMap(j -> Flux.defer(() -> {
                                final var ctx = RequestContext.getRequestContext();
                                return Flux.just(ctx != null ? ctx.getXRequestId() : "missing");
                            }).subscribeOn(Schedulers.boundedElastic())).collectList().block();

                            return seenIds.stream().allMatch(requestId::equals);
                        } finally {
                            RequestContext.clear();
                        }
                    }, pool)).toList();

            final var results = futures.stream().map(CompletableFuture::join).toList();

            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(r -> r);
        }
    }
}
