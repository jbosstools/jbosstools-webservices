/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.container.ResourceInfo;

@Provider
public class CarDynamicFeature implements DynamicFeature {

	@Override
	public void configure(ResourceInfo info, FeatureContext ctx) {
	}
}
