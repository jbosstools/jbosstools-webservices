
package org.example.www.helloworld;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for sayHello complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="sayHello">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sayHelloRequest" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHello", propOrder = {
    "sayHelloRequest"
})
public class SayHello {

    protected String sayHelloRequest;

    /**
     * Gets the value of the sayHelloRequest property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSayHelloRequest() {
        return sayHelloRequest;
    }

    /**
     * Sets the value of the sayHelloRequest property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSayHelloRequest(String value) {
        this.sayHelloRequest = value;
    }

}
