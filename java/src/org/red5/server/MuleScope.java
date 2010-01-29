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

package org.red5.server;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.*;
import org.springframework.context.ApplicationContextAware;


public class MuleScope extends Scope {

    

    @Override
    public boolean connect(IConnection conn, Object[] params) {
        /*
        String id = conn.getSessionId();
        // Use client registry from scope the client connected to.
        IScope connectionScope = Red5.getConnectionLocal().getScope();
        log.debug("Connection scope: {}", (connectionScope == null ? "is null" : "not null"));

        // when the scope is null bad things seem to happen, if a null scope is OK then
        // this block will need to be removed - Paul
        if (connectionScope == null) {
            return false;
        }

        // Get client registry for connection scope
        //IClientRegistry clientRegistry = new ClientRegistry();//connectionScope.getContext().getClientRegistry();
        log.debug("Client registry: {}", (clientRegistry == null ? "is null" : "not null"));

        // Get client from registry by id or create a new one
        IClient client = clientRegistry.hasClient(id) ? clientRegistry.lookupClient(id) : clientRegistry.newClient(params);
        //IClient client = clientRegistry.newClient(params);

        // We have a context, and a client object.. time to init the connection.
        conn.initialize(client);
              */
        return super.connect(conn, params);    //To change body of overridden methods use File | Settings | File Templates.
    }

    

}
