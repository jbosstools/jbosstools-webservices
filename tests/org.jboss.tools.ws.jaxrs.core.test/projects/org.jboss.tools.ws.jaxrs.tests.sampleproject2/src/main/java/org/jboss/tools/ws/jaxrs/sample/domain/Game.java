/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author xcoulon
 * 
 */
@Entity
@DiscriminatorValue(value = "game")
public class Game extends Product {

	private String name = null;

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
