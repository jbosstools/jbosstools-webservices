/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class CarFeature implements Feature {

	@Override
	public boolean configure(FeatureContext ctx) {
		return true;
	}
}
