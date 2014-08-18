package org.jboss.tools.ws.jaxrs.sample.services;
 
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding; // do not remove
import org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding; // do not remove

@SuppressWarnings("unused")
@ApplicationPath("/app")
public class RestApplication extends Application {

}
