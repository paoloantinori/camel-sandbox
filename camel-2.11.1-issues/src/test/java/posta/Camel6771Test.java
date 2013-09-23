/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package posta;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class Camel6771Test extends CamelTestSupport{

    class DummyClass {
        String test = "test";
    }

    @Test
    public void testForceConcurrentModException() throws InterruptedException {
        final HashMap<String, DummyClass> records = new HashMap<String, DummyClass>();
        records.put("key", new DummyClass());

        for (int i = 0; i < 10000; i++) {
            records.put("key" + i, new DummyClass());
        }

        ExecutorService ser = Executors.newFixedThreadPool(1);
        ExecutorService ser2 = Executors.newFixedThreadPool(1);
        ser.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1);
                        new HashMap<String, DummyClass>(records);
                    } catch (InterruptedException e) {
                        System.out.println("unexpected interrupt..");
                    }
                }
            }
        });

        ser2.execute(new Runnable() {
            private boolean bool = false;
            @Override
            public void run() {
                while(true) {
                    try {
                        records.get("key").test = bool ? "blue" : "red";
                        Thread.sleep(1);

                        if (bool) {
                            records.remove("key-2");
                        } else {
                            records.put("key-2", new DummyClass());
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Unexpected interrupt part 2..");
                    }
                }
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testRun() throws InterruptedException {
        int multiplier = 10 * 1000;
        MockEndpoint mock = getMockEndpoint("mock:end");

        mock.expectedMessageCount(2 * multiplier);

        List<String> body = null;
        for (int i = 0; i < 1 * multiplier; i++) {
            body = new ArrayList<String>();

            body.add("one" + i);
            body.add("two" + i);
            template.sendBody("direct:data", body);

        }

        assertMockEndpointsSatisfied(3, TimeUnit.MINUTES);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:data").to("seda:filteraccept?concurrentConsumers=8");

                from("seda:filteraccept?concurrentConsumers=8")
                        .split(body())
                        .to("direct:wind-aggregator");

                from("direct:wind-aggregator")
                        .aggregate(header(Exchange.CORRELATION_ID), new MyAggregator())
                        .eagerCheckCompletion()
                        .completionPredicate(header(Exchange.SPLIT_COMPLETE))
                        .split()
                        .method(body())
//                        .process(new Processor() {
//                            @Override
//                            public void process(Exchange exchange) throws Exception {
//                                Map<?, ?> records = (Map<?, ?>) exchange.getProperty(Exchange.AGGREGATION_STRATEGY);
//                                System.out.println("before size for exchange=: " + exchange.getIn().getHeader("breadcrumbId")
//                                        + " size=" + records.size());
//                            }
//                        })
                        .to("seda:output");

                from("seda:output")
//                        .process(new Processor() {
//                            @Override
//                            public void process(Exchange exchange) throws Exception {
//                                Map<?, ?> records = (Map<?, ?>) exchange.getProperty(Exchange.AGGREGATION_STRATEGY);
//                                System.out.println("after size for exchange=: " + exchange.getIn().getHeader("breadcrumbId")
//                                        + " size=" + records.size());
//                            }
//                        })
                        .to("mock:end");
            }
        };
    }
}
