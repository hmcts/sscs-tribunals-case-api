package uk.gov.hmcts.reform.sscs.servicebus;

import org.junit.rules.ExternalResource;

public class EmbeddedInMemoryQpidBrokerRule extends ExternalResource {
    private EmbeddedInMemoryQpidBroker broker;

    @Override
    protected void before() throws Throwable {
        this.broker = new EmbeddedInMemoryQpidBroker();
        this.broker.start();
    }

    @Override
    protected void after() {
        this.broker.shutdown();
    }
}
