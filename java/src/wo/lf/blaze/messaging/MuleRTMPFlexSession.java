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

package wo.lf.blaze.messaging;

import flex.messaging.NonHttpFlexSession;
import flex.messaging.messages.Message;
import org.red5.server.api.service.ServiceUtils;
import org.red5.server.net.rtmp.MuleRTMPMinaConnection;
import org.red5.server.net.rtmp.codec.MuleRTMPProtocolEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MuleRTMPFlexSession extends NonHttpFlexSession {

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPFlexSession.class);

    private String id;

    public MuleRTMPFlexSession() {
        id = UUID.randomUUID().toString();
    }


    @Override
    public boolean isPushSupported() {
        return true;
    }

    /**
     * @param message The message to push.
     * @exclude FlexClient invokes this to push a message to a remote client.
     */
    @Override
    public void push(Message message) {
        log.debug("Pushing message " + message + " via connection " + connection);
        message.setHeader(MuleRTMPProtocolEncoder.NO_TRANSACTION_HEADER, true);
        ServiceUtils.invokeOnConnection(connection, "receive", new Object[]{message});
    }

    @Override
    public String getId() {
        return id;
    }

    public MuleRTMPMinaConnection getConnection() {
        return connection;
    }

    public void setConnection(MuleRTMPMinaConnection connection) {
        this.connection = connection;
    }

    MuleRTMPMinaConnection connection;

    @Override
    protected void internalInvalidate() {
        log.debug("Invalidating session");
        if (connection != null) {
            log.debug("Closing connection {}", connection);
            connection.setFlexSession(null);
            connection.close();
            connection = null;
        }
    }
}
