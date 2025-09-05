package uk.gov.companieshouse.accounts.association.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;

@Configuration
public class ReactorRequestContextPropagation {
    private static final String HOOK_KEY = "request-context-propagation";

    @PostConstruct
    public void init() {
        Schedulers.onScheduleHook(HOOK_KEY, original -> {
            final var captured = RequestContext.getRequestContext();
            return () -> {
                final var previous = RequestContext.getRequestContext();
                if (captured != null) {
                    RequestContext.setRequestContext(captured);
                } else {
                    RequestContext.clear();
                }
                try {
                    original.run();
                } finally {
                    if (previous != null) {
                        RequestContext.setRequestContext(previous);
                    } else {
                        RequestContext.clear();
                    }
                }
            };
        });
    }
}
