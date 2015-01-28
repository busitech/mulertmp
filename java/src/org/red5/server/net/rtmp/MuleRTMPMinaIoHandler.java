package org.red5.server.net.rtmp;

import org.apache.mina.core.session.IoSession;
import org.red5.server.net.filter.TrafficShapingFilter;

/**
 * Created by mfrederes on 1/27/15.
 */
public class MuleRTMPMinaIoHandler extends org.red5.server.net.rtmp.RTMPMinaIoHandler {
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        //session.getFilterChain().addFirst("trafficShapingFilter", new TrafficShapingFilter(512000,512000));
    }
}
