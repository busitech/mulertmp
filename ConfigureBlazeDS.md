To add the MuleRTMPChannel/MuleRTMPAMFEndpoint in BlazeDS you need to add it to your services-config.xml
```
<channel-definition id="my-rtmp" class="wo.lf.mule.messaging.channels.MuleRTMPChannel">
    <endpoint url="rtmp://{server.name}/{context.root}/messagebroker/rtmp-amf"
          class="wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint"/>
</channel-definition>
```

to connect to BlazeDS using RTMP you will also need to link the MuleRTMPFlexLib.swc to your flex project.