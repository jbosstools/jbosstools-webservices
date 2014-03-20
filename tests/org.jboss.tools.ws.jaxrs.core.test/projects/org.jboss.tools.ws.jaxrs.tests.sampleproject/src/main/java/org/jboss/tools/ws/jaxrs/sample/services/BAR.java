package org.jboss.tools.ws.jaxrs.sample.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
@HttpMethod("BAR")
@SuppressWarnings("unused") // keep it for some junit tests
public @interface BAR {

}
