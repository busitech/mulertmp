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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.ICommand;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;
import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.MessageSerializer;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.TypeMarshallingContext;
import flex.messaging.io.amf.AmfTrace;
import flex.messaging.log.Log;
import flex.messaging.log.LogCategories;
import flex.messaging.util.ClassUtil;

public class MuleRTMPProtocolEncoder extends org.red5.server.net.rtmp.codec.RTMPProtocolEncoder {

    public static final String NO_TRANSACTION_HEADER = "NO_TRANSACTION";

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPProtocolEncoder.class);

    protected void encodeCommand(IoBuffer out, ICommand command) {
        // TODO: tidy up here
        SerializationContext serializationContext = MuleRTMPAMFEndpoint.getInstance().getSerializationContext();
        TypeMarshallingContext.setTypeMarshaller(MuleRTMPAMFEndpoint.getInstance().getTypeMarshaller());
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutStream = new DataOutputStream(baOutput);

        AmfTrace trace = null;
        if (Log.isDebug())
            trace = new AmfTrace();

        MessageSerializer blazeSerializer = (MessageSerializer)ClassUtil.createDefaultInstance(serializationContext.getSerializerClass(), MessageSerializer.class);

        org.red5.io.object.Output output = new org.red5.io.amf.Output(out);
        final IServiceCall call = command.getCall();
        final boolean isPending = (call.getStatus() == Call.STATUS_PENDING);
        log.debug("Call: {} pending: {}", call, isPending);
        if (!isPending) {
            log.debug("Call has been executed, send result");
            Serializer.serialize(output, call.isSuccess() ? "_result" : "_error");
        } else {
            log.debug("This is a pending call, send request");
            final String action = (call.getServiceName() == null) ? call.getServiceMethodName() : call.getServiceName() + '.' + call.getServiceMethodName();
            Serializer.serialize(output, action); // seems right
        }
        if (command instanceof Invoke) {
            Serializer.serialize(output, Integer.valueOf(command.getTransactionId()));
            Serializer.serialize(output, command.getConnectionParams());
        }

        if (call.getServiceName() == null && "connect".equals(call.getServiceMethodName())) {
            // response to initial connect, always use AMF0
            output = new org.red5.io.amf.Output(out);
        } else {
            if (Red5.getConnectionLocal().getEncoding() == IConnection.Encoding.AMF3) {
                output = new org.red5.io.amf3.Output(out);
                blazeSerializer.setVersion(MessageIOConstants.AMF3);
            } else {
                output = new org.red5.io.amf.Output(out);
            }
        }
        try {
            blazeSerializer.initialize(serializationContext, dataOutStream, trace);
        if (!isPending && (command instanceof Invoke)) {
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
                blazeSerializer.writeObject(res);
            }
        } else {
            log.debug("Writing params");
            final Object[] args = call.getArguments();
            if (args != null) {
                for (Object element : args) {
                    blazeSerializer.writeObject(element);
                    /*if (element instanceof ByteBuffer) {
                        // a byte buffer indicates that serialization is already complete, send raw
                        final ByteBuffer buf = (ByteBuffer) element;
                        buf.mark();
                        try {
                            out.put(buf);
                        } finally {
                            buf.reset();
                        }
                    } else {
                        // standard serialize
                        Serializer.serialize(output, element);
                    }
                    */
                }
            }
        }

        dataOutStream.flush();
        if (Log.isDebug())
            Log.getLogger(LogCategories.ENDPOINT_AMF).debug(trace.toString());
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

        if (command.getData() != null) {
            out.setAutoExpand(true);
            out.put(command.getData());
        }

    }
}
