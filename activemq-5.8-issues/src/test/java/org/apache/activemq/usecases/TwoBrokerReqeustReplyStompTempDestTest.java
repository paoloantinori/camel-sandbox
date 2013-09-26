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
package org.apache.activemq.usecases;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.JmsMultipleBrokersTestSupport;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.broker.region.policy.VMPendingQueueMessageStoragePolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.network.DiscoveryNetworkConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.store.kahadb.KahaDBStore;
import org.apache.activemq.transport.stomp.Stomp;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.activemq.transport.stomp.StompFrame;
import org.junit.Test;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class TwoBrokerReqeustReplyStompTempDestTest extends JmsMultipleBrokersTestSupport {


    private static final String REQUEST_QUEUE_NAME = "request.queue";
    private static final String RESPONSE_QUEUE_NAME = "response.queue";

    final CountDownLatch serverShutDown = new CountDownLatch(1);

    @Test
    public void testStompRequestReplySameConnection() throws Exception {
        bridgeBrokers("spoke", "hub");
        startAllBrokers();

//        setupJmsRespondent();
        setupStompRespondent();

        StompConnection stompConnection = new StompConnection();
        stompConnection.open("localhost", 61613);
        stompConnection.sendFrame("CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL);

        StompFrame frame = stompConnection.receive();
        assertTrue(frame.toString().startsWith("CONNECTED"));

        stompConnection.subscribe("/queue/response.queue", "auto");

        String message = generateMessage();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(Stomp.Headers.Send.REPLY_TO, "/queue/" + RESPONSE_QUEUE_NAME);

        for (int i = 0; i < 1000000; i++) {
            stompConnection.send("/queue/" + REQUEST_QUEUE_NAME, message, null, headers);
            if (i % 10000 == 0) {
                System.out.println("making progress " + (i / 10000));
            }
        }

        serverShutDown.countDown();

        stompConnection.close();

    }

    private void setupStompRespondent() {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    StompConnection stompConnection = new StompConnection();
                    stompConnection.open("localhost", 61614);
                    stompConnection.sendFrame("CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL);

                    StompFrame frame = stompConnection.receive();
                    assertTrue(frame.toString().startsWith("CONNECTED"));

                    stompConnection.subscribe("/queue/response.queue", "auto");
                    String responseMessage = "A-well-a, everybody's heard about the bird. Bird, bird, bird, b-bird's the word";

                    while (serverShutDown.getCount() == 1) {
                        frame = stompConnection.receive(1000);
                        if (frame == null) {
                            System.err.println("Received a null message? Not expecting that");
                        }
                        else {
                            stompConnection.send(frame.getHeaders().get("reply-to"), responseMessage);
                        }
                    }

                    stompConnection.close();

                } catch (Exception e) {
                    System.err.println("You had an issue sending back messages!");
                }
            }
        });
    }

    private String generateMessage() {
        return "I know a song that'll get on your nerves\n";
    }

    private void setupJmsRespondent() throws JMSException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61617");
                    Connection connection = factory.createConnection();
                    final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Destination requestDest = session.createQueue(REQUEST_QUEUE_NAME);
                    MessageConsumer consumer = session.createConsumer(requestDest);
                    final MessageProducer producer = session.createProducer(null);
                    consumer.setMessageListener(new MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            try {
                                producer.send(message.getJMSReplyTo(), message);
                            } catch (JMSException e) {
                                System.out.println("Had issues trying to send back response");
                            }
                        }
                    });

                    serverShutDown.await();

                    consumer.close();
                } catch (Exception e) {
                    System.err.println("You had an issue sending back messages!");
                }
            }
        });

    }

    @Override
    protected NetworkConnector bridgeBrokers(BrokerService localBroker, BrokerService remoteBroker, boolean dynamicOnly, int networkTTL, boolean conduit, boolean failover) throws Exception {
        String uri = "static:(tcp://localhost:61616)";
        NetworkConnector queueConnector = new DiscoveryNetworkConnector(new URI(uri));
        queueConnector.setName("queues-to-" + remoteBroker.getBrokerName());
        queueConnector.setDynamicOnly(true);
        queueConnector.setNetworkTTL(2);
        queueConnector.setConduitSubscriptions(false);
        queueConnector.setDecreaseNetworkConsumerPriority(true);
        queueConnector.setDuplex(true);
        queueConnector.addExcludedDestination(new ActiveMQTopic(">"));
        localBroker.addNetworkConnector(queueConnector);
        System.out.println("Broker bridged queues...");

        NetworkConnector topicConnector = new DiscoveryNetworkConnector(new URI(uri));
        topicConnector.setName("topics-to-" + remoteBroker.getBrokerName());
        topicConnector.setDynamicOnly(true);
        topicConnector.setNetworkTTL(2);
        topicConnector.setConduitSubscriptions(false);
        topicConnector.setDecreaseNetworkConsumerPriority(true);
        topicConnector.setDuplex(true);
        topicConnector.addExcludedDestination(new ActiveMQQueue(">"));
        localBroker.addNetworkConnector(topicConnector);
        System.out.println("Broker bridged topics...");

        // we have to return something, so just return the first conenctor.. shouldn't be used anymore though
        return queueConnector;

    }

    @Override
    public void setUp() throws Exception {
            super.setUp();
        String options = new String("?deleteAllMessagesOnStartup=true");
        createAndConfigureBroker(new URI("broker:(tcp://localhost:61616,stomp://localhost:61613)/hub" + options));
        createAndConfigureBroker(new URI("broker:(tcp://localhost:61617,stomp://localhost:61614)/spoke" + options));
    }

    private BrokerService createAndConfigureBroker(URI uri) throws Exception {
        BrokerService broker = createBroker(uri);
        configurePersistenceAdapter(broker);
        configureMemorySettings(broker);
        configureDestinationPolicy(broker);
        return broker;
    }

    private void configureDestinationPolicy(BrokerService broker) {
        PolicyMap map = new PolicyMap();
        PolicyEntry entry = new PolicyEntry();
        entry.setQueue(">");
        entry.setProducerFlowControl(false);
        entry.setPendingQueuePolicy(new VMPendingQueueMessageStoragePolicy());
        entry.setGcInactiveDestinations(true);
        entry.setInactiveTimoutBeforeGC(300000);
        entry.setQueuePrefetch(1);
        map.put(new ActiveMQQueue(">"), entry);

        broker.setDestinationPolicy(map);
    }

    private void configureMemorySettings(BrokerService broker) {
        broker.getSystemUsage().getMemoryUsage().setLimit(2 * 1024 * 1024 * 1024);
    }

    protected void configurePersistenceAdapter(BrokerService broker) throws IOException {
        File dataFileDir = new File("target/test-amq-data/kahadb/" + broker.getBrokerName());
        KahaDBStore kaha = new KahaDBStore();
        kaha.setDirectory(dataFileDir);
        broker.setPersistenceAdapter(kaha);
    }

}
