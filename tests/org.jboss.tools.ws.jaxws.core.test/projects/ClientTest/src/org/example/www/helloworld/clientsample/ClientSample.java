package org.example.www.helloworld.clientsample;

import org.example.www.helloworld.*;

public class ClientSample {

	public static void main(String[] args) {
	        System.out.println("***********************");
	        System.out.println("Create Web Service Client...");
	        HelloWorldService service1 = new HelloWorldService();
	        System.out.println("Create Web Service...");
	        HelloWorld port1 = service1.getHelloWorldPort();
	        System.out.println("Create Web Service Operation...");
	        System.out.println("Server said: " + port1.sayHello(args[0]));
	        System.out.println("Server said: " + port1.sayHello2(args[0]));
	        System.out.println("***********************");
	        System.out.println("Call Over!");
	}
}
