package org.jboss.tools.ws.jaxrs.sample.services.dto;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.ws.jaxrs.sample.domain.Product;

public abstract class ProductDTO {

	private Float price;

	private String partNumber;

	private List<Link> selfLinks = new ArrayList<Link>();

	public ProductDTO() {

	}

	public ProductDTO(Product product) {
		/*BeanUtils.copyProperties(product, this);*/
	}

	public List<Link> getSelfLinks() {
		return selfLinks;
	}
	
	public void addSelfLink(Link link) {
		selfLinks.add(link);
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

}
