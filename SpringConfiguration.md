To use MuleRTMP with BlazeDS, bootstrap the red5 and MuleRTMP classes using spring.
You will find a sample configuration in the java-sample-web/web/WEB-INF directory.

```

<?xml version="1.0" encoding="UTF-8"?>

<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:flex="http://www.springframework.org/schema/flex"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-2.5.xsd
       http://www.springframework.org/schema/flex
       http://www.springframework.org/schema/flex/spring-flex-1.5.xsd">

    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="locations" value="WEB-INF/red5.properties"/>
    </bean>
    <bean id="schedulingService" class="org.red5.server.scheduling.QuartzSchedulingService"/>
    <!-- Maps request paths at /* to the BlazeDS MessageBroker -->
    <flex:message-broker id="_messageBroker" services-config-path="/WEB-INF/flex/services-config.xml" />

    <bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
        <property name="mappings">
            <value>
                /*=_messageBroker
            </value>
        </property>
    </bean>

    <bean id="global.handler" class="org.red5.server.MuleRTMPCoreHandler"/>

    <bean id="mule.serviceInvoker" class="wo.lf.red5.server.service.MuleRTMPServiceInvoker">
        <property name="messageBroker" ref="_messageBroker" />  
    </bean>

    <bean id="clientRegistry" class="org.red5.server.ClientRegistry" />
    <bean id="globalContext" class="org.red5.server.MuleRTMPGlobalContext">
        <property name="clientRegistry" ref="clientRegistry" />
        <property name="serviceInvoker" ref="mule.serviceInvoker" />
    </bean>

    <bean id="globalScope" class="org.red5.server.scope.Scope" init-method="init">
        <property name="context" ref="globalContext"/>
        <property name="handler" ref="global.handler" />
    </bean>

    <bean id="rtmpMinaConnManager" class="org.red5.server.net.rtmp.RTMPConnManager"/>

    <bean id="rtmpHandler" class="org.red5.server.net.rtmp.MuleRTMPHandler">
        <property name="statusObjectService">
            <bean class="org.red5.server.net.rtmp.status.StatusObjectService">
                <property name="serializer" ref="serializer" />
            </bean>
        </property>
        
        <property name="globalScope" ref="globalScope"/>

    </bean>

    <!-- Serializes objects to AMF binary -->
    <bean id="serializer" class="org.red5.io.object.Serializer"/>
    <!-- Deserializes objects from AMF binary -->
    <bean id="deserializer" class="org.red5.io.object.Deserializer"/>

    <bean id="minaEncoder" class="org.red5.server.net.rtmp.codec.MuleRTMPMinaProtocolEncoder">
        <property name="serializer">
            <ref bean="serializer"/>
        </property>
    </bean>
    <bean id="minaDecoder" class="org.red5.server.net.rtmp.codec.MuleRTMPMinaProtocolDecoder">
        <property name="deserializer">
            <ref bean="deserializer"/>
        </property>
    </bean>


    <!-- RTMP codec factory -->
    <bean id="rtmpCodecFactory" class="org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory" autowire="byType" />
   
    <!-- RTMP Mina IO Handler -->
    <bean id="rtmpMinaIoHandler" class="org.red5.server.net.rtmp.RTMPMinaIoHandler">
        <property name="handler" ref="rtmpHandler"/>
        <property name="rtmpConnManager" ref="rtmpMinaConnManager"/>
    </bean>

    <!-- RTMP Mina Transport -->
    <bean id="rtmpTransport" class="org.red5.server.net.rtmp.RTMPMinaTransport" init-method="start"
          destroy-method="stop">
        <property name="ioHandler" ref="rtmpMinaIoHandler"/>
        <property name="connectors">
            <list>
                <bean class="java.net.InetSocketAddress">
                    <constructor-arg index="0" type="java.lang.String" value="${rtmp.host}"/>
                    <constructor-arg index="1" type="int" value="${rtmp.port}"/>
                </bean>
                <!-- You can now add additional ports and ip addresses
                <bean class="java.net.InetSocketAddress">
                    <constructor-arg index="0" type="java.lang.String" value="${rtmp.host}" />
                    <constructor-arg index="1" type="int" value="1936" />
                </bean>
                 -->
            </list>
        </property>
        <!--<property name="receiveBufferSize" value="${rtmp.receive_buffer_size}"/>-->
        <!-- This is the interval at which the sessions are polled for stats. If mina monitoring is not
                  enabled, polling will not occur. -->
        <property name="minaPollInterval" value="1000"/>
        <property name="tcpNoDelay" value="${rtmp.tcp_nodelay}"/>
    </bean>

    <!-- RTMP Mina Connection -->
    <bean id="rtmpMinaConnection" scope="prototype"
          class="org.red5.server.net.rtmp.MuleRTMPMinaConnection">
        <!-- Ping clients every X ms. Set to 0 to disable ghost detection code. -->
        <property name="pingInterval" value="${rtmp.ping_interval}"/>
        <!-- Disconnect client after X ms of not responding. -->
        <property name="maxInactivity" value="${rtmp.max_inactivity}"/>
        <!-- Max. time in milliseconds to wait for a valid handshake. -->
        <property name="maxHandshakeTimeout" value="5000"/>
    </bean>
</beans>

```