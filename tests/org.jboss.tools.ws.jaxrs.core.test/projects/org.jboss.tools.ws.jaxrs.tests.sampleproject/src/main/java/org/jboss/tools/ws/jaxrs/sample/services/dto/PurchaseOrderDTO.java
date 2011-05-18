package org.jboss.tools.ws.jaxrs.sample.services.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.jboss.tools.ws.jaxrs.sample.domain.PurchaseOrder;

@XmlRootElement(name = "order")
@XmlType(propOrder = { "creationDate", "totalPrice", "cancelled", "selfLinks", "customer", "products" })
public class PurchaseOrderDTO {

	private Integer id;

	private Date creationDate;

	private Float totalPrice;

	private Boolean cancelled = false;

	private List<Link> selfLinks = null;

	private CustomerDTO customer = null;

	private List<ProductDTO> products = null;

	public PurchaseOrderDTO() {

	}

	public PurchaseOrderDTO(PurchaseOrder order) {
		/*BeanUtils.copyProperties(order, this,
		// ignored properties
				new String[] { "customer", "products" });*/

	}

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElementWrapper(name = "products")
	@XmlElements({ @XmlElement(name = "book", type = BookDTO.class), @XmlElement(name = "game", type = GameDTO.class) })
	public List<ProductDTO> getProducts() {
		return products;
	}

	public void addProduct(ProductDTO productDTO) {
		if(products == null) {
			products = new ArrayList<ProductDTO>();
		}
		products.add(productDTO);
	}

	@XmlElement(name = "customer")
	public CustomerDTO getCustomer() {
		return customer;
	}

	public void setCustomer(CustomerDTO customer) {
		this.customer = customer;
	}

	@XmlElement(name = "link")
	public List<Link> getSelfLinks() {
		return selfLinks;
	}

	public void addSelfLink(Link self) {
		if(selfLinks == null) {
			selfLinks = new ArrayList<Link>();
		}
		selfLinks.add(self);
	}

	public Float getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(Float totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Boolean getCancelled() {
		return cancelled;
	}

	public void setCancelled(Boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * @param creationDate
	 *            the creationDate to set
	 */
	public void setCreationDate(Date date) {
		this.creationDate = date;
	}

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

}
