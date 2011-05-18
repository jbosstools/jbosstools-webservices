/**
 * 
 */
package org.jboss.tools.ws.jaxrs.sample.services.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.tools.ws.jaxrs.sample.domain.Game;

/**
 * @author xcoulon
 * 
 */
@XmlRootElement(name = "game")
@XmlType(propOrder = { "name", "partNumber", "price", "selfLinks" })
public class GameDTO extends ProductDTO {

	private String name = null;

	public GameDTO() {
		super();
	}

	public GameDTO(Game game) {
		/*BeanUtils.copyProperties(game, this);*/
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	@XmlElement(name = "link")
	public List<Link> getSelfLinks() {
		return super.getSelfLinks();
	}

	@Override
	@XmlElement(name="price")
	public Float getPrice() {
		return super.getPrice();
	}
	
	@Override
	@XmlElement(name="partNumber")
	public String getPartNumber() {
		return super.getPartNumber();
	}

}
