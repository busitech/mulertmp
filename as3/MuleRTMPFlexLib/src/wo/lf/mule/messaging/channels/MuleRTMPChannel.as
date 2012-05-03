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
	import mx.messaging.FlexClient;
	import mx.messaging.channels.NetConnectionChannel;
	import mx.messaging.config.LoaderConfig;
	import mx.messaging.config.ServerConfig;
	import mx.messaging.events.ChannelFaultEvent;
	import mx.messaging.messages.CommandMessage;
	import mx.netmon.NetworkMonitor;
	
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
						var flexClient:FlexClient = FlexClient.getInstance();
						if (flexClient.id == null) {
							flexClient.id = event.info.id;							
						}
						connectSuccess();
						break;
					case "NetConnection.Connect.Rejected":
						disconnectSuccess(true);
						break;
					case "NetConnection.Connect.Failed":
						var faultEvent:ChannelFaultEvent = ChannelFaultEvent.createEvent(this, false, null, "Channel.Connect.Failed", event.info + " url: '" + endpoint + "'");
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
		
		override protected function internalConnect():void {
			// If the NetConnection has a non-null uri the Player will close() it automatically
			// as part of its connect() processing below. Pre-emptively close the connection while suppressing
			// NetStatus event handling to avoid spurious events.
			if (_nc.uri != null && _nc.uri.length > 0 && _nc.connected)
			{
				_nc.removeEventListener(NetStatusEvent.NET_STATUS, statusHandler);
				_nc.close();
			}
					
			_nc.addEventListener(NetStatusEvent.NET_STATUS, statusHandler);
			_nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, securityErrorHandler);
			_nc.addEventListener(IOErrorEvent.IO_ERROR, ioErrorHandler);
			_nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, asyncErrorHandler);
			
			var msg:CommandMessage = new CommandMessage();
			if (credentials != null) {
				msg.operation = CommandMessage.LOGIN_OPERATION;
				msg.body = credentials;
			}
			else {
				msg.operation = CommandMessage.CLIENT_PING_OPERATION;
			}
			
			// Report the messaging version for this Channel.
			msg.headers[CommandMessage.MESSAGING_VERSION] = messagingVersion;
			
			// Indicate if requesting the dynamic configuration from the server.
			if (ServerConfig.needsConfig(this))
				msg.headers[CommandMessage.NEEDS_CONFIG_HEADER] = true;
			
			// Add the FlexClient id header.
			setFlexClientIdOnMessage(msg);
			
			try {
				if (NetworkMonitor.isMonitoring()) {
					var redirectedUrl:String = NetworkMonitor.adjustNetConnectionURL(LoaderConfig.url, url);
					if(redirectedUrl != null){
						url = redirectedUrl;
					}
				}				
				_nc.connect(url, msg);
			}
			catch(e:Error) {
				e.message += "  url: '" + url + "'";
				throw e;
			}
		}
		
		public function onBWCheck(... rest):Number
		{
			trace("Checking Bandwidth");
			return 0;
		}
		
		public function onBWDone(... rest):void
		{
			var object:Object = rest[0];
			trace("KBDown: " + object.kbitDown + " Delta Down: " + object.deltaDown + " Delta Time: " + object.deltaTime + " Latency: " + object.latency);		
		}
		
	}
}