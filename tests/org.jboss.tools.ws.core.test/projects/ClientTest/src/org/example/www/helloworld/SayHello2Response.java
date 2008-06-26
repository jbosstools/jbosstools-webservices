
package org.example.www.helloworld;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for sayHello2Response complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="sayHello2Response">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sayHelloResponse2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHello2Response", propOrder = {
    "sayHelloResponse2"
})
public class SayHello2Response {

    protected String sayHelloResponse2;

    /**
     * Gets the value of the sayHelloResponse2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSayHelloResponse2() {
        return sayHelloResponse2;
    }

    /**
     * Sets the value of the sayHelloResponse2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSayHelloResponse2(String value) {
        this.sayHelloResponse2 = value;
    }

}
