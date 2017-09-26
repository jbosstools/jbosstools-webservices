
package org.example.www.helloworld;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(name = "HelloWorld", targetNamespace = "http://www.example.org/HelloWorld")
public class HelloWorld{


    @WebMethod(action = "http://www.example.org/HelloWorld/sayHello")
    @WebResult(name = "sayHelloResponse", partName = "sayHelloResponse")
    public String sayHello(@WebParam(name = "sayHelloRequest", partName = "sayHelloRequest") String str){
    	return "Hello World";
    }
    
    @WebMethod(action = "http://www.example.org/HelloWorld/sayHello2")
    @WebResult(name = "sayHelloResponse2", partName = "sayHelloResponse2")
    public String sayHello2(@WebParam(name = "sayHelloRequest2", partName = "sayHelloRequest2") String str){
    	return "Hello qq";
    }

}
