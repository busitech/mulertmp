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
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;
import flex.messaging.util.UUIDUtils;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.rtmp.MuleRTMPMinaConnection;
import org.red5.server.service.Call;
import org.red5.server.service.ServiceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wo.lf.blaze.messaging.MuleRTMPFlexSession;
import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;

public class MuleRTMPServiceInvoker extends ServiceInvoker {

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPServiceInvoker.class);

    private MessageBroker messageBroker;

    private ExceptionTranslator exceptionTranslator = new ExceptionTranslator() {
        @Override
        public boolean handles(Class<?> clazz) {
            return false;
        }

        @Override
        public MessageException translate(Throwable t) {
            return null;
        }
    };

    /**
     * {@inheritDoc}
     */
    public boolean invoke(IServiceCall call, Object service) {
        IConnection conn = Red5.getConnectionLocal();

        // create a flex session if needed and associate flex session with connection for pushing messages to the client
        // finally set the flexSesssion as threadlocal
        if (conn instanceof MuleRTMPMinaConnection) {
            MuleRTMPFlexSession flexSession;
            if (((MuleRTMPMinaConnection) conn).getFlexSession() == null) {
                flexSession = MuleRTMPAMFEndpoint.getInstance().sessionProvider.createSession((MuleRTMPMinaConnection) conn);
                ((MuleRTMPMinaConnection) conn).setFlexSession(flexSession);
            } else {
                flexSession = ((MuleRTMPMinaConnection) conn).getFlexSession();
            }

            FlexContext.setThreadLocalSession(flexSession);

        } else {
            throw new RuntimeException();
        }

        Message message = (Message) call.getArguments()[0];
        try {
            Object bResult;

            Object messageClientId = message.getClientId();
            if (messageClientId == null) {
            	message.setClientId(UUIDUtils.createUUID());
            }
            
            if (message instanceof CommandMessage) {
                // handle command message
                CommandMessage commandMessage = (CommandMessage) message;
                MuleRTMPAMFEndpoint.getInstance().setupFlexClient(commandMessage);
                // route command to service
                bResult = messageBroker.routeCommandToService((CommandMessage) message, MuleRTMPAMFEndpoint.getInstance()).getSmallMessage();
                

            } else if(message instanceof Message) {
            	MuleRTMPAMFEndpoint.getInstance().setupFlexClient(message);
                // route async message to service
                bResult = messageBroker.routeMessageToService(message, MuleRTMPAMFEndpoint.getInstance()).getSmallMessage();
            }else{
				bResult = null;
			}
			

            call.setStatus(bResult == null ? Call.STATUS_SUCCESS_NULL : Call.STATUS_SUCCESS_RESULT);
            if (call instanceof IPendingServiceCall) {
                ((IPendingServiceCall) call).setResult(bResult);
            }

        } catch (MessageException e) {
            if (call instanceof IPendingServiceCall) {
                ErrorMessage errorMessage = e.getErrorMessage();
                errorMessage.setCorrelationId(message.getMessageId());

                ((IPendingServiceCall) call).setResult(errorMessage);
                call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
            }
            return false;

        } catch (Throwable e) {
            if (call instanceof IPendingServiceCall) {
                MessageException messageException;
                if (exceptionTranslator.handles(e.getClass())) {
                    messageException = exceptionTranslator.translate(e);

                } else {
                    messageException = new MessageException(e);
                }

                ErrorMessage errorMessage = messageException.getErrorMessage();
                errorMessage.setCorrelationId(message.getMessageId());

                ((IPendingServiceCall) call).setResult(errorMessage);
                call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
            }
            return false;

        }
        return true;
    }

    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void setExceptionTranslator(ExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
    }
}
