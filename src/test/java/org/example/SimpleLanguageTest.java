package org.example;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.support.LRUCache;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleLanguageTest extends CamelTestSupport {
    @EndpointInject("mock:result")
    private MockEndpoint mockResult;

    @EndpointInject("mock:fail")
    private MockEndpoint mockWithFailure;

    @Test
    public void whenSimpleLanguageNotUseCachedEntriesItWillNotFail() throws Exception {

        mockResult.await(20, TimeUnit.SECONDS);
        mockResult.expectedMessageCount(102);
        MockEndpoint.assertIsSatisfied(mockResult);
        List<Exchange> exchanges = mockResult.getExchanges();
        exchanges.stream()
                .forEach(exchange -> {
                    //System.out.println("Evaluated: "+exchange.getMessage().getHeader("#CustomHeader", String.class));
                    //System.out.println("-Expected: This is a test a with startLabel: `Document` endLabel: `Document` and label: `ALabel`");
                    assertEquals("This is a test a with startLabel: `Document` endLabel: `Document` and label: `ALabel`", exchange.getMessage().getHeader("#CustomHeader", String.class));
                });


    }

    @Test
    public void whenSimpleLanguageUseCachedEntriesItWillFail() throws Exception {
        mockWithFailure.expectedMessageCount(1002);
        MockEndpoint.assertIsSatisfied(mockWithFailure);
        List<Exchange> exchanges = mockWithFailure.getExchanges();
        exchanges.stream()
                .forEach(exchange -> assertEquals("This is a test a with startLabel: `Document` endLabel: `Document` and label: `ALabel`", exchange.getMessage().getHeader("#CustomHeader", String.class)));


    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
           /* Map body = new HashMap() {{
                put("label", "ALabel");
                put("startLabel", "Document");
                put("endLabel", "Document");
            }};*/
            Map body = Map.of("label", "ALabel", "startLabel", "Document", "endLabel", "Document");


            String simpleTemplate = "This is a test a with startLabel: `${body.get('startLabel')}` endLabel: `${body.get('endLabel')}` and label: `${body.get('label')}`";


            @Override
            public void configure() throws Exception {
                from("timer://test-timer?fixedRate=true&period=10&delay=1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplateNoCache(simpleTemplate, exchange));
                            }
                        })
                        .to("mock:result");

               /*from("timer://test-timer1?fixedRate=true&period=10&delay=1")

                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplateNoCache(simpleTemplate, exchange));
                            }
                        })
                        .to("mock:result");

                from("timer://test-timer2?fixedRate=true&period=10&delay=1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplateNoCache(simpleTemplate, exchange));
                            }
                        })
                        .to("mock:result");*/

                from("timer://test-timer3?fixedRate=true&period=10&delay=1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplate(simpleTemplate, exchange));
                            }
                        })
                        //.setHeader("#CustomHeader",simple(simpleTemplate,String.class))
                        .to("mock:fail");

                from("timer://test-timer4?fixedRate=true&period=10&delay=1")

                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplate(simpleTemplate, exchange));
                            }
                        })
                        //.setHeader("#CustomHeader",simple(simpleTemplate,String.class))
                        .to("mock:fail");

                from("timer://test-timer5?fixedRate=true&period=10&delay=1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getMessage().setBody(body);
                                exchange.getMessage().setHeader("#CustomHeader", resolveTemplate(simpleTemplate, exchange));
                            }
                        })
                        //.setHeader("#CustomHeader",simple(simpleTemplate,String.class))
                        .to("mock:fail");
            }

        };
    }

    public String resolveTemplate(String template, Exchange exchange) {
       // synchronized (SimpleLanguage.class) {
            var simpleExpression = new SimpleExpression();
            simpleExpression.setExpression(template);
            return simpleExpression.evaluate(exchange, String.class);
        //}

    }


    public String resolveTemplateNoCache(String template, Exchange exchange) {
        var simpleExpression = new SimpleExpression();
        //This will force to create a new entry in the cache in the SimpleLanguage class
        String nocache = String.join("-", "-nocache", UUID.randomUUID().toString());
        simpleExpression.setExpression("%s%s".formatted(template, nocache));
        return simpleExpression.evaluate(exchange, String.class).replace(nocache, "");

    }
}
