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
package org.red5.server.net.rtmp.codec;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.AbstractAmfOutput;
import flex.messaging.io.amf.Amf0Output;
import flex.messaging.io.amf.Amf3Output;
import flex.messaging.messages.AsyncMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.Serializer;
import org.red5.server.api.IConnection;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MuleRTMPProtocolEncoder extends org.red5.server.net.rtmp.codec.RTMPProtocolEncoder {

    public static final String NO_TRANSACTION_HEADER = "NO_TRANSACTION";

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPProtocolEncoder.class);
    private Serializer serializer;

    public void setSerializer(Serializer serializer) {
        super.setSerializer(serializer);//
        this.serializer = serializer;
    }

    protected void encodeNotifyOrInvoke(IoBuffer out, Notify invoke, RTMP rtmp) {
        // TODO: tidy up here
        // log.debug("Encode invoke");
        AbstractAmfOutput blazeOutput;
        SerializationContext serializationContext = MuleRTMPAMFEndpoint.getInstance().getSerializationContext();
        Amf3Output blazeAmf3Output = new Amf3Output(serializationContext);
        Amf0Output blazeAmf0Output = new Amf0Output(serializationContext);
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutStream = new DataOutputStream(baOutput);

        org.red5.io.object.Output output = new org.red5.io.amf.Output(out);
        final IServiceCall call = invoke.getCall();
        final boolean isPending = (call.getStatus() == Call.STATUS_PENDING);
//        log.debug("Call: {} pending: {}", call, isPending);

        if (!isPending) {
            log.debug("Call has been executed, send result. success: " + call.isSuccess());
            serializer.serialize(output, call.isSuccess() ? "_result" : "_error"); // seems right
        } else {
            log.debug("This is a pending call, send request");
            // for request we need to use AMF3 for client mode
            // if the connection is AMF3
            if (rtmp.getEncoding() == IConnection.Encoding.AMF3 && rtmp.getMode() == RTMP.MODE_CLIENT) {
                output = new org.red5.io.amf3.Output(out);
            }
            final String action = (call.getServiceName() == null) ? call.getServiceMethodName() : call.getServiceName()
                    + '.' + call.getServiceMethodName();
            serializer.serialize(output, action); // seems right
        }
        if (invoke instanceof Invoke) {
            serializer.serialize(output, invoke.getInvokeId());
            serializer.serialize(output, invoke.getConnectionParams());
        }

        if (call.getServiceName() == null && "connect".equals(call.getServiceMethodName())) {
            // Response to initial connect, always use AMF0
            blazeOutput = new Amf0Output(serializationContext);
            blazeOutput.setOutputStream(dataOutStream);
        } else {
            if (rtmp.getEncoding() == IConnection.Encoding.AMF3) {
                blazeOutput = blazeAmf3Output;
                blazeOutput.setOutputStream(dataOutStream);
                out.put(AMF.TYPE_AMF3_OBJECT);
            } else {
                blazeOutput = blazeAmf0Output;
                blazeOutput.setOutputStream(dataOutStream);
            }
        }
        try {

            if (!isPending && (invoke instanceof Invoke)) {
                IPendingServiceCall pendingCall = (IPendingServiceCall) call;
                if (!call.isSuccess()) {
                    log.debug("Call was not successful");
                    if (call.getException() != null) {
                        StatusObject status = generateErrorResult(StatusCodes.NC_CALL_FAILED, call.getException());
                        pendingCall.setResult(status);
                    }
                }
                Object res = pendingCall.getResult();
                log.debug("Writing result: {}", res);
                if (res != null) {
                    blazeOutput.writeObject(res);
                }
            } else {
                log.debug("Writing params");
                final Object[] args = call.getArguments();
                if (args != null) {
                    for (Object element : args) {
                        blazeOutput.writeObject(element);
                    }
                }
            }

            dataOutStream.flush();

        } catch (IOException ioException) {
            log.error("Upps", ioException);
        }


        // flush to buf


        try {
            out.put(baOutput.toByteArray());
            baOutput.reset();

        } catch (Exception e) {
            log.error("Caught exception", e);
        }

        if (invoke.getData() != null) {
            out.setAutoExpand(true);
            out.put(invoke.getData());
        }

    }
}
