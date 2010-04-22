/*
 * MuleRTMP - use red5's rtmp handler in blaze ds
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.red5.server.net.rtmp;

import flex.messaging.messages.AsyncMessage;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.codec.MuleRTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import wo.lf.blaze.messaging.MuleRTMPFlexSession;

public class MuleRTMPMinaConnection extends RTMPMinaConnection {
    public MuleRTMPFlexSession getFlexSession() {
        return flexSession;
    }

    public void setFlexSession(MuleRTMPFlexSession flexSession) {
        this.flexSession = flexSession;
    }

    private MuleRTMPFlexSession flexSession;

    @Override
    public void close() {
        super.close();
        if (flexSession != null) {
            flexSession.invalidate();
            flexSession = null;
        }
    }

    @Override
    public void invoke(org.red5.server.api.service.IServiceCall call, int channel) {
            // We need to use Invoke for all calls to the client
            Invoke invoke = new Invoke();
            invoke.setCall(call);
            Object[] args = call.getArguments();
            if (args != null && args.length == 1 && args[0] instanceof AsyncMessage && ((AsyncMessage) args[0]).headerExists(MuleRTMPProtocolEncoder.NO_TRANSACTION_HEADER)) {
                invoke.setInvokeId(0);
            }else{
                invoke.setInvokeId(getInvokeId());
            }
            if (call instanceof IPendingServiceCall && invoke.getInvokeId()!=0) {
                registerPendingCall(invoke.getInvokeId(), (IPendingServiceCall) call);
            }
            getChannel(channel).write(invoke);
    }
}
