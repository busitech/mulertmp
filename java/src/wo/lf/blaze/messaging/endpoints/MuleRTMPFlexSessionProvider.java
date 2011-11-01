package wo.lf.blaze.messaging.endpoints;

import org.red5.server.net.rtmp.MuleRTMPMinaConnection;

import wo.lf.blaze.messaging.MuleRTMPFlexSession;
import flex.messaging.AbstractFlexSessionProvider;

public class MuleRTMPFlexSessionProvider extends AbstractFlexSessionProvider {
	
	public MuleRTMPFlexSession createSession(MuleRTMPMinaConnection connection) {
		MuleRTMPFlexSession session = new MuleRTMPFlexSession(this);
		session.setConnection(connection);
		getFlexSessionManager().registerFlexSession(session);
		return session;
	}
}
