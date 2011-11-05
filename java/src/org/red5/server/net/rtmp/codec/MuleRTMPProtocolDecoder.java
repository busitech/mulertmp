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

import flex.messaging.io.MessageDeserializer;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.TypeMarshallingContext;
import flex.messaging.io.amf.AmfTrace;
import flex.messaging.log.Log;
import flex.messaging.log.LogCategories;
import flex.messaging.util.ClassUtil;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.server.net.rtmp.event.FlexMessage;
import org.red5.server.service.PendingCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wo.lf.blaze.messaging.endpoints.MuleRTMPAMFEndpoint;

import java.io.IOException;
import java.util.ArrayList;

public class MuleRTMPProtocolDecoder extends RTMPProtocolDecoder {

    private static final Logger log = LoggerFactory.getLogger(MuleRTMPMinaProtocolDecoder.class);

    private Deserializer deserializer;

    public void setDeserializer(Deserializer deserializer) {
        super.setDeserializer(deserializer);//
        this.deserializer = deserializer;
    }

    /**
     * Decodes FlexMessage event.
     *
     * @param in   IoBuffer
     * @param rtmp RTMP protocol state
     * @return FlexMessage event
     */
    public FlexMessage decodeFlexMessage(IoBuffer in, RTMP rtmp) {
    	AmfTrace trace = null;
    	 if (Log.isDebug()) 
    		 trace = new AmfTrace();
    	TypeMarshallingContext.setTypeMarshaller(MuleRTMPAMFEndpoint.getInstance().getTypeMarshaller());
        SerializationContext serializationContext = MuleRTMPAMFEndpoint.getInstance().getSerializationContext();
        MessageDeserializer blazeDeserializer = (MessageDeserializer)ClassUtil.createDefaultInstance(serializationContext.getDeserializerClass(), MessageDeserializer.class);
        blazeDeserializer.initialize(serializationContext, in.asInputStream(),  trace);

        // TODO: Unknown byte, probably encoding as with Flex SOs?
        in.skip(1);
        // Encoding of message params can be mixed - some params may be in AMF0, others in AMF3,
        // but according to AMF3 spec, we should collect AMF3 references
        // for the whole message body (through all params)
        org.red5.io.amf3.Input.RefStorage refStorage = new org.red5.io.amf3.Input.RefStorage();

        Input input = new org.red5.io.amf.Input(in);
        String action = deserializer.deserialize(input, String.class);
        int invokeId = deserializer.<Number>deserialize(input, Number.class).intValue();
        FlexMessage msg = new FlexMessage();
        msg.setInvokeId(invokeId);
        Object[] params = new Object[]{};

        if (in.hasRemaining()) {
            ArrayList<Object> paramList = new ArrayList<Object>();

            final Object obj = deserializer.deserialize(input, Object.class);
            if (obj != null) {
                paramList.add(obj);
            }

            while (in.hasRemaining()) {
                try {
                    paramList.add(blazeDeserializer.readObject());
                } catch (IOException ioException) {

                } catch (ClassNotFoundException noClassException) {

                }
            }
            params = paramList.toArray();
            if (log.isDebugEnabled()) {
                log.debug("Num params: {}", paramList.size());
                for (int i = 0; i < params.length; i++) {
                    log.debug(" > {}: {}", i, params[i]);
                }
            }
        }
        /*    we need to change this because action is actually null for some reason
       final int dotIndex = action.lastIndexOf('.');
       String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
       String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
        */
        String serviceName = null;
        String serviceMethod = "mule";

        PendingCall call = new PendingCall(serviceName, serviceMethod, params);
        msg.setCall(call);
        if (Log.isDebug())
        	Log.getLogger(LogCategories.ENDPOINT_AMF).debug(trace.toString());
        return msg;
        
    }
}
