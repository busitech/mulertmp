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

package wo.lf.red5.server.service;

import flex.messaging.FlexContext;
import flex.messaging.MessageBroker;
import flex.messaging.MessageException;
import flex.messaging.client.FlexClient;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;
import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.rtmp.MuleRTMPMinaConnection;
import org.red5.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wo.lf.blaze.messaging.MuleRTMPFlexSession;
import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;

import java.io.*;

public class MuleServiceInvoker extends ServiceInvoker{

    public static MuleRTMPAMFEndpoint endpoint;
    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(ServiceInvoker.class);

    public MessageBroker getMessageBroker() {
        return messageBroker;
    }

    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    MessageBroker messageBroker;

    /**
     * {@inheritDoc}
     */
    public boolean invoke(IServiceCall call, Object service) {
        IConnection conn = Red5.getConnectionLocal();
        String methodName = call.getServiceMethodName();

        Object[] args = call.getArguments();
        Object[] argsWithConnection;
        Object arg0 = args[0];
        Object bResult = null;

        // create a flex session if needed and associate flex session with connection for pushing messages to the client
        // finally set the flexSesssion as threadlocal
        if (conn instanceof MuleRTMPMinaConnection) {
            MuleRTMPFlexSession flexSession;
            if (((MuleRTMPMinaConnection) conn).getFlexSession() == null) {
                flexSession = new MuleRTMPFlexSession();
                ((MuleRTMPMinaConnection) conn).setFlexSession(flexSession);
                flexSession.setConnection(((MuleRTMPMinaConnection) conn));
            }else{
                flexSession = ((MuleRTMPMinaConnection) conn).getFlexSession();
            }
            FlexContext.setThreadLocalSession(flexSession);
        }

        try{
            if(arg0 instanceof flex.messaging.messages.CommandMessage) {
                // handle command message
                flex.messaging.messages.CommandMessage commandMessage = (flex.messaging.messages.CommandMessage) arg0;
                FlexClient client = endpoint.setupFlexClient((String) commandMessage.getClientId());
                // set the clientid into the message
                commandMessage.setClientId(client.getId());
                // set the endpoint id into the message
                commandMessage.setHeader(Message.ENDPOINT_HEADER,endpoint.getId());
                // route command to service
                bResult = messageBroker.routeCommandToService((flex.messaging.messages.CommandMessage) arg0, endpoint).getSmallMessage();
            } else if (arg0 instanceof Message){
                // route async message to service
                bResult = messageBroker.routeMessageToService((Message) arg0, endpoint).getSmallMessage();
            }

            call.setStatus(bResult == null ? Call.STATUS_SUCCESS_NULL
                    : Call.STATUS_SUCCESS_RESULT);
            if (call instanceof IPendingServiceCall) {
                ((IPendingServiceCall) call).setResult(bResult);
            }
        }catch(MessageException e){

            if (call instanceof IPendingServiceCall) {
                ErrorMessage errorMessage = e.getErrorMessage();
                errorMessage.setCorrelationId(((Message) arg0).getMessageId());

                ((IPendingServiceCall) call).setResult(errorMessage);
                call.setStatus(Call.STATUS_GENERAL_EXCEPTION);

                return false;
            }
        }
        return true;
    }
}
