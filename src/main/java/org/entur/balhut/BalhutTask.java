package org.entur.balhut;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BalhutTask implements CommandLineRunner {

    private final CamelContext camelContext;

    public BalhutTask(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void run(String... args) {
        FluentProducerTemplate fluentProducerTemplate = camelContext.createFluentProducerTemplate();
        fluentProducerTemplate.to("direct:makeCSV").request();
    }
}
