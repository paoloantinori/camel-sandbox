This shows an example maven pom.xml with minimal dependencies 
to be able to bootstrap an embedded ActiveMQ broker with 
the KahaDB message store.

The following are the only required dependencies to make this happen:

activemq-broker
activemq-kahadb-store
activemq-client
slf4j-api
hawtbuf
geronimo-jms_1.1_spec
geronimo-j2ee-management_1.1_spec
