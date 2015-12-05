# Howto build and deploy the demo #

To build and deploy the MuleRTMP demo application, checkout the source, import into Intellij Idea and add a run configuration for the MuleRTMPSampleWeb:Web artifact.

The MuleRTMP project includes 4 modules:

**java/** the MuleRTMP sourcecode to wire Red5 and BlazeDS up and dependency pom.xml.

**as3/MuleRTMPFlexLib** the MuleRMPFlexLib to connect to the MuleRTMPChannel.

**java-sample-web** contains a small demo service and samples for configuration of BlazeDS and spring configuration for the red5 part

**as3/MuleRTMPFlexSample** a flex sample application to connect to the sample java service and messaging