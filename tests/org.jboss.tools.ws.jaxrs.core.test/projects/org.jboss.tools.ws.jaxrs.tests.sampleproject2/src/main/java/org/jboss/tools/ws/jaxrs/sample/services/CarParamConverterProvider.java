/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.jboss.tools.ws.jaxrs.sample.domain.Car;

/**
 * @author xcoulon
 *
 */
@Provider
public class CarParamConverterProvider implements ParamConverterProvider {

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> arg0, Type arg1, Annotation[] arg2) {
		if (arg0.getName().equals(Car.class.getName())) {

			return new ParamConverter<T>() {

				@Override
				public T fromString(String arg0) {
					Car car = new Car();
					car.setColor(arg0);
					return null;
				}

				@Override
				public String toString(T arg0) {
					// TODO Auto-generated method stub
					return null;
				}

			};
		}
		return null;
	}

}
