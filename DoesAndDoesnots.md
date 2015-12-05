# What MuleRTMP does and what it does not #

**BlazeDS**

Does:

Provides the MuleRTMPAMFEndpoint class, so you can use BlazeDs with an rtmp endpoint.
This is achieved by using the rtmp protocol handler from red5 server.
Adds a new MuleRTMPFlexSession to enable push capabilities to flex clients over rtmp
protocol.
It should work with versions 3.x and 4.x of BlazeDS.

Does not:
Add any data management capabilities like LCDS.
(You might want to look into direction of the Clear toolkit http://sourceforge.net/projects/cleartoolkit/ i guess you might even use it in conjunction with MuleRTMP)

**Red5**

Does:

MuleRTMP uses some classes from red5, to use the rtmp protocol in BlazeDS.
Some methods have been replaced with derived code, to be able to bootstrap minimal set of classes. Also some adjustments have been done to use the amf deserializer/serializer of BlazeDS instead of the red5 version for compability with BlazeDS.

Does not:
Provide anything else then the RTMP protocol and the possibility to make rpc calls/use messaging in BlazeDS. No video/audio streaming, remote shared objects, or other features of red5 are supported.


WARNING:
Although MuleRTMP has been serving thousands of users every day for the last couple of month, it cannot be guarantueed that there are no problems hidden or exposed by other use cases. So use at your own risk.

Final note:
MuleRTMP is only a really small adapater between red5 and BlazeDS so lots of credits goes to the Red5 project at http://code.google.com/p/red5/ for providing an opensource solution for the rtmp protocol and adobe for opensourcing BlazeDS at http://opensource.adobe.com/wiki/display/blazeds/BlazeDS.