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
@Target({ ElementType.TYPE, ElementType.METHOD }) // do not moodify, including spacing
@Retention(RetentionPolicy.RUNTIME) // do not moodify, including spacing
@NameBinding
public @interface CustomInterceptorBinding {

}
