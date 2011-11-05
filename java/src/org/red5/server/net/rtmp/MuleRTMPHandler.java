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

import org.red5.compatibility.flex.messaging.messages.CommandMessage;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.StreamAction;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.Call;
import org.red5.server.stream.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import flex.messaging.FlexContext;
import flex.messaging.client.FlexClient;
import flex.messaging.messages.Message;

import wo.lf.blaze.messaging.MuleRTMPFlexSession;
import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;

import java.util.HashMap;
import java.util.Map;

import static org.red5.server.api.ScopeUtils.getScopeService;

public class MuleRTMPHandler extends RTMPHandler {

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPHandler.class);

    private IScope scope;

    public void setGlobalScope(IScope scope) {
        this.scope = scope;
    }

    /**
     * Remoting call invocation handler.
     *
     * @param conn RTMP connection
     * @param call Service call
     */
    protected void invokeCall(RTMPConnection conn, IServiceCall call) {
        final IScope scope = conn.getScope();
        /*
         if (scope.hasHandler()) {
             final IScopeHandler handler = scope.getHandler();
             log.debug("Scope: {}", scope);
             log.debug("Handler: {}", handler);
             if (!handler.serviceCall(conn, call)) {
                 // XXX: What to do here? Return an error?
                 return;
             }
         }
        */
        final IContext context = scope.getContext();
        log.debug("Context: {}", context);
        context.getServiceInvoker().invoke(call, scope);
    }

    /**
     * Remoting call invocation handler.
     *
     * @param conn    RTMP connection
     * @param call    Service call
     * @param service Server-side service object
     * @return <code>true</code> if the call was performed, otherwise
     *         <code>false</code>
     */
    private boolean invokeCall(RTMPConnection conn, IServiceCall call, Object service) {
        final IScope scope = conn.getScope();
        final IContext context = scope.getContext();
        log.debug("Scope: {}", scope);
        log.debug("Service: {}", service);
        log.debug("Context: {}", context);
        return context.getServiceInvoker().invoke(call, service);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void onInvoke(RTMPConnection conn, Channel channel, Header source, Notify invoke, RTMP rtmp) {

        // Get call
        final IServiceCall call = invoke.getCall();
        // method name
        final String action = call.getServiceMethodName();

        log.debug("Invoke: {}", invoke);


        // If it's a callback for server remote call then pass it over to
        // callbacks handler and return
        if ("_result".equals(action) || "_error".equals(action)) {
            handlePendingCallResult(conn, invoke);
            return;
        }

        boolean disconnectOnReturn = false;

        // If this is not a service call then handle connection...
        if (call.getServiceName() == null) {
            if (!conn.isConnected() && StreamAction.CONNECT.equals(action)) {
                // Handle connection
                log.debug("connect");
                // Get parameters passed from client to
                // NetConnection#connection
                final Map<String, Object> params = invoke.getConnectionParams();
                // Get hostname
                String host = getHostname((String) params.get("tcUrl"));
                // Check up port
                if (host.endsWith(":1935")) {
                    // Remove default port from connection string
                    host = host.substring(0, host.length() - 5);
                }
                // App name as path, but without query string if there is
                // one
                String path = (String) params.get("app");
                if (path.indexOf("?") != -1) {
                    int idx = path.indexOf("?");
                    params.put("queryString", path.substring(idx));
                    path = path.substring(0, idx);
                }
                params.put("path", path);

                final String sessionId = null;
                conn.setup(host, path, sessionId, params);
                try {
                    // Lookup server scope when connected
                    // Use host and application name


                    if (scope != null) {
                        log.info("Connecting to: {}", scope);
                        boolean okayToConnect;
                        try {
                            log.debug("Conn {}, scope {}, call {}", new Object[]{conn, scope, call});
                            log.debug("Call args {}", call.getArguments());
                            if (call.getArguments() != null) {
                                okayToConnect = conn.connect(scope, call.getArguments());
                            } else {
                                okayToConnect = conn.connect(scope);
                            }
                            if (okayToConnect) {
                                log.debug("Connected - Client: {}", conn.getClient());
                                call.setStatus(Call.STATUS_SUCCESS_RESULT);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    //send fmsver and capabilities
                                    StatusObject result = getStatus(NC_CONNECT_SUCCESS);
                                    result.setAdditional("fmsVer", Red5.getFMSVersion());
                                    result.setAdditional("capabilities", Integer.valueOf(31));
                                    pc.setResult(result);
                                }
                                // Measure initial roundtrip time after connecting
                                conn.ping(new Ping(Ping.STREAM_BEGIN, 0, -1));
                                conn.startRoundTripMeasurement();
                            } else {
                                log.debug("Connect failed");
                                call.setStatus(Call.STATUS_ACCESS_DENIED);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    pc.setResult(getStatus(NC_CONNECT_REJECTED));
                                }
                                disconnectOnReturn = true;
                            }
                        } catch (ClientRejectedException rejected) {
                            log.debug("Connect rejected");
                            call.setStatus(Call.STATUS_ACCESS_DENIED);
                            if (call instanceof IPendingServiceCall) {
                                IPendingServiceCall pc = (IPendingServiceCall) call;
                                StatusObject status = getStatus(NC_CONNECT_REJECTED);
                                Object reason = rejected.getReason();
                                if (reason != null) {
                                    status.setApplication(reason);
                                    //should we set description?
                                    status.setDescription(reason.toString());
                                }
                                pc.setResult(status);
                            }
                            disconnectOnReturn = true;
                        }
                    }
                } catch (RuntimeException e) {
                    call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
                    if (call instanceof IPendingServiceCall) {
                        IPendingServiceCall pc = (IPendingServiceCall) call;
                        pc.setResult(getStatus(NC_CONNECT_FAILED));
                    }
                    log.error("Error connecting {}", e);
                    disconnectOnReturn = true;
                }

                // Evaluate request for AMF3 encoding
                if (Integer.valueOf(3).equals(params.get("objectEncoding")) && call instanceof IPendingServiceCall) {
                    Object pcResult = ((IPendingServiceCall) call).getResult();
                    FlexClient flexClient = null;
                    String clientId = null;
                    Object[] args = call.getArguments();
                    if(args!=null) {
                    	Object arg1 = args[0];
                    	if(arg1 instanceof CommandMessage) {
                    		
                    		MuleRTMPFlexSession flexSession;
                            if (((MuleRTMPMinaConnection) conn).getFlexSession() == null) {
                                flexSession = MuleRTMPAMFEndpoint.getInstance().sessionProvider.createSession((MuleRTMPMinaConnection) conn);
                                ((MuleRTMPMinaConnection) conn).setFlexSession(flexSession);
                            } else {
                                flexSession = ((MuleRTMPMinaConnection) conn).getFlexSession();
                            }
                            FlexContext.setThreadLocalSession(flexSession);
                    		
                        	clientId = (String)((CommandMessage) arg1).getHeader(Message.FLEX_CLIENT_ID_HEADER);
                        	
                        	 // This indicates that we're dealing with a non-legacy client that hasn't been
                            // assigned a FlexClient Id yet. Reset to null to generate a fresh Id.
                            if (clientId != null && clientId.equals("nil"))
                            	clientId = null;
                        	
                        	if(clientId==null) {                        		
                        		flexClient = MuleRTMPAMFEndpoint.getInstance().setupFlexClient(clientId);
                        		clientId = flexClient.getId();
                        	}                        	
                    	}
                    }
                    
                    Map<String, Object> result;
                    if (pcResult instanceof Map) {
                        result = (Map<String, Object>) pcResult;
                        result.put("objectEncoding", 3);
                        result.put("id", clientId);
                    } else if (pcResult instanceof StatusObject) {
                        result = new HashMap<String, Object>();
                        StatusObject status = (StatusObject) pcResult;
                        result.put("code", status.getCode());
                        result.put("description", status.getDescription());
                        result.put("application", status.getApplication());
                        result.put("level", status.getLevel());
                        result.put("objectEncoding", 3);
                        result.put("id", clientId);
                        ((IPendingServiceCall) call).setResult(result);
                    }

                    rtmp.setEncoding(IConnection.Encoding.AMF3);
                }
            } else {
                //log.debug("Enum value of: {}", StreamAction.getEnum(action));
                StreamAction streamAction = StreamAction.getEnum(action);
                //if the "stream" action is null, we assume that it is an "invoke"
                if (null == streamAction) {
                    invokeCall(conn, call);
                } else {
                    switch (streamAction) {
                        case DISCONNECT:
                            conn.close();
                            break;
                        case CREATE_STREAM:
                        case DELETE_STREAM:
                        case RELEASE_STREAM:
                        case PUBLISH:
                        case PLAY:
                        case SEEK:
                        case PAUSE:
                        case PAUSE_RAW:
                        case CLOSE_STREAM:
                        case RECEIVE_VIDEO:
                        case RECEIVE_AUDIO:
                            IStreamService streamService = (IStreamService) getScopeService(conn.getScope(),
                                    IStreamService.class, StreamService.class);
                            Status status = null;
                            try {
                                if (!invokeCall(conn, call, streamService)) {
                                    status = getStatus(NS_INVALID_ARGUMENT).asStatus();
                                    status.setDescription(String.format("Failed to %s (stream id: %d)", action, source
                                            .getStreamId()));
                                }
                            } catch (Throwable err) {
                                log.error("Error while invoking {} on stream service. {}", action, err);
                                status = getStatus(NS_FAILED).asStatus();
                                status.setDescription(String.format("Error while invoking %s (stream id: %d)", action,
                                        source.getStreamId()));
                                status.setDetails(err.getMessage());
                            }
                            if (status != null) {
                                channel.sendStatus(status);
                            }
                            break;
                        default:
                            invokeCall(conn, call);
                    }
                }
            }
        } else if (conn.isConnected()) {
            // Service calls, must be connected.
            invokeCall(conn, call);
        } else {
            // Warn user attempts to call service without being connected
            log.warn("Not connected, closing connection");
            conn.close();
        }

        if (invoke instanceof Invoke) {
            if ((source.getStreamId() != 0)
                    && (call.getStatus() == Call.STATUS_SUCCESS_VOID || call.getStatus() == Call.STATUS_SUCCESS_NULL)) {
                // This fixes a bug in the FP on Intel Macs.
                log.debug("Method does not have return value, do not reply");
                return;
            }

            boolean sendResult = true;
            if (call instanceof IPendingServiceCall) {
                IPendingServiceCall psc = (IPendingServiceCall) call;
                Object result = psc.getResult();
                if (result instanceof DeferredResult) {
                    // Remember the deferred result to be sent later
                    DeferredResult dr = (DeferredResult) result;
                    dr.setServiceCall(psc);
                    dr.setChannel(channel);
                    dr.setInvokeId(invoke.getInvokeId());
                    conn.registerDeferredResult(dr);
                    sendResult = false;
                }
            }

            if (sendResult) {
                log.debug("Sending response via conn " + conn);
                // The client expects a result for the method call.
                Invoke reply = new Invoke();
                reply.setCall(call);
                reply.setInvokeId(invoke.getInvokeId());
                channel.write(reply);
                if (disconnectOnReturn) {
                    conn.close();
                }
            }
        }
    }
}
