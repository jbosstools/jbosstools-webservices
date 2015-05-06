package websockets;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.OnMessage;

@ServerEndpoint("/path2")
public class MyServer2Endpoint {

	@OnMessage
	public void onMessage(String string) {
	
	}
	
	
}
