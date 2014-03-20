/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

/**
 * @author xcoulon
 *
 */
@Target({ ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@NameBinding
public @interface AnotherCustomInterceptorBinding {

}
