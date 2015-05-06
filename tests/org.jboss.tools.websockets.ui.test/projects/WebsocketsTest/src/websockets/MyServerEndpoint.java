package websockets;

import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/path")
public class MyServerEndpoint {

	public String toString() {
		return super.toString();
	}
	
	
}
