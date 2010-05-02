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
package wo.lf.mule.messaging.channels{
	import flash.events.AsyncErrorEvent;
	import flash.events.IOErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.events.SecurityErrorEvent;
	
	import mx.core.mx_internal;
	import mx.messaging.MessageAgent;
	import mx.messaging.MessageResponder;
	import mx.messaging.channels.NetConnectionChannel;
	import mx.messaging.events.ChannelFaultEvent;
	import mx.messaging.messages.CommandMessage;
	
	use namespace mx_internal;
	
	public class MuleRTMPChannel extends NetConnectionChannel
	{
		public function MuleRTMPChannel(id:String=null, uri:String=null)
		{
			super(id, uri);
			internalPollingEnabled = false;
		}
		
		override public function enablePolling():void
		{
		}

        override public function disablePolling():void
		{
		}

		override protected function timerRequired():Boolean
		{
			return false;
		}
		
		override public function poll() : void
		{
			
		}
		
		override public function get protocol():String
		{
			return "rtmp";
		}
		
		override mx_internal function get realtime():Boolean
		{
			return true;
		}
		
		override protected function statusHandler(event:NetStatusEvent):void
		{
			if(event.info){
				
				switch(event.info.code){
					case "NetConnection.Connect.Success":
						connectSuccess();
						break;
					case "NetConnection.Connect.Rejected":
						disconnectSuccess(true);
						break;
					case "NetConnection.Connect.Failed":
						var faultEvent:ChannelFaultEvent = ChannelFaultEvent.createEvent(this, false, null, "error", event.info + " url: '" + endpoint + "'");
						faultEvent.rootCause = event;
						connectFailed(faultEvent);
						break;
					case "NetConnection.Connect.Closed":
						disconnectSuccess(false);
						break;
					case "NetConnection.Connect.AppShutdown":
					case "NetConnection.Connect.InvalidApp":
						disconnectSuccess(true);
						break;
					default:
						trace(event.info.code);
						break;
				}
				
			}else{
				
			}
		}
	}
}