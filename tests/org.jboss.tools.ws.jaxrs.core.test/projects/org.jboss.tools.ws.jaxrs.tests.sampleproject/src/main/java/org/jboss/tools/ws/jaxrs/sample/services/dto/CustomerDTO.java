package org.jboss.tools.ws.jaxrs.sample.services.dto;


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.jboss.tools.ws.jaxrs.sample.domain.Address;
import org.jboss.tools.ws.jaxrs.sample.domain.Customer;

@XmlRootElement(name = "customer")
@XmlType(propOrder = { "firstName", "lastName", "address", "orders", "selfLinks" })
public class CustomerDTO {

	private Integer id;

	private String firstName;

	private String lastName;

	private Address address;

	private final List<PurchaseOrderDTO> orders = new ArrayList<PurchaseOrderDTO>();

	private final List<Link> selfLinks = new ArrayList<Link>();

	public CustomerDTO() {

	}

	public CustomerDTO(Customer customer) {
		/*BeanUtils.copyProperties(customer, this);*/
	}

	public Customer toCustomer() {
		Customer customer = new Customer();
		/*BeanUtils.copyProperties(this, customer, new String[] { "orders",
				"selfLinks" });*/
		return customer;
	}

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	
	@XmlElementWrapper(name="orders")
	@XmlElements(@XmlElement(name = "order"))
	public List<PurchaseOrderDTO> getOrders() {
		return orders;
	}

	public void addOrder(PurchaseOrderDTO order) {
		orders.add(order);
	}
	
	@XmlElement(name = "link")
	public List<Link> getSelfLinks() {
		return selfLinks;
	}

	public void addSelfLink(Link link) {
		selfLinks.add(link);
	}
	
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

}
