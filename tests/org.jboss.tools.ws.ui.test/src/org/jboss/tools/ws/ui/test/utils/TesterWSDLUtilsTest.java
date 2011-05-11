/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.ui.test.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.jboss.tools.ws.ui.utils.SchemaUtils;
import org.jboss.tools.ws.ui.utils.TesterWSDLUtils;
import org.junit.Assert;
import org.junit.Test;

public class TesterWSDLUtilsTest {

	@Test
	public void testGetNSServiceNameAndMessageNameArray() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String[] r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertArrayEquals(new String[] {"http://webservices.samples.jboss.org/", "HelloWorldService", "HelloWorldPort"}, r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertArrayEquals(new String[] {"http://www.ecubicle.net/webservices", "gsearch_rss", "gsearch_rssSoap"}, r);

		r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertArrayEquals(new String[] {"http://webservices.www.ecubicle.net/", "EchoService", "EchoPort"}, r);
	}

	@Test
	public void testGetEndpointURL() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String r = TesterWSDLUtils.getEndpointURL(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertEquals("http://localhost:8080/ws/HelloWorld", r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getEndpointURL(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertEquals("http://www.ecubicle.net/gsearch_rss.asmx", r);

		r = TesterWSDLUtils.getEndpointURL(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertEquals("http://localhost:8080/webws/EchoPortType", r);
	}

	@Test
	public void testGetActionURL() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String r = TesterWSDLUtils.getActionURL(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertEquals("", r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getActionURL(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertEquals("http://www.ecubicle.net/webservices/GetSearchResults", r);

		r = TesterWSDLUtils.getActionURL(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertEquals("", r);
	}

	@Test
	public void testJBIDE6497() {
		String s1 = getSampleMessage("/jbide6497/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertTrue("was: '" + s1, s1.contains("<tns:GetSearchResults xmlns:tns=\"http://www.ecubicle.net/webservices\">"));
		Assert.assertTrue("was: '" + s1, s1.contains("<tns:gQuery>?</tns:gQuery>"));

		s1 = getSampleMessage("/jbide6593/original.wsdl", "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertTrue("was: '" + s1, s1.contains("<ns:p>\n<age>?</age>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<male>?</male>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<tax>?</tax>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<ns:echo xmlns:ns=\"http://test.jboss.org/ns\">"));
	}

	@Test
	public void testJBIDE6558() {
		String s1 = getSampleMessage("/jbide6558/x.wsdl", "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertTrue(s1.contains("xmlns:tns=\"http://webservices.samples.jboss.org/\""));
		Assert.assertTrue(s1.contains("<tns:arg0>?</tns:arg0>"));
		String s2 = getSampleMessage("/jbide6558/y.wsdl", "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertTrue(s2.contains("xmlns:webs=\"http://webservices.samples.jboss.org/\""));
		Assert.assertTrue(s2.contains("<arg0>?</arg0>"));
		// won't be equal due to the namespacing
		//Assert.assertEquals(s1, s2); 
	}

	@Test
	public void testJBIDE6593() {
		String s1 = getSampleMessage("/jbide6497/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertTrue(s1.contains("<tns:GetSearchResults xmlns:tns=\"http://www.ecubicle.net/webservices\">"));
		Assert.assertTrue(s1.contains("<tns:gQuery>?</tns:gQuery>"));

		String s2 = getSampleMessage("/jbide6593/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
//		Assert.assertTrue(s2.contains("<tns:GetSearchResults xmlns:tns=\"http://www.ecubicle.net/webservices\">"));
//		Assert.assertTrue(s2.contains("<tns:gQuery>?</tns:gQuery>"));
		Assert.assertEquals(s1, s2);
	}

	@Test
	public void testJBIDE6694() {
		String s1 = getSampleMessage("/jbide6694/ConverterPortType.wsdl", "ConverterPortType", "ConverterPortTypeImplPort", "ConverterPortTypeBinding", "convert");
		Assert.assertTrue(s1.contains("<tns:ChangeUnit xmlns:tns=\"http://test.jboss.org/ns\">"));
		Assert.assertTrue(s1.contains("<tns:value>?</tns:value>"));
		Assert.assertTrue(s1.contains("<tns:fromUnit>?</tns:fromUnit>"));
		Assert.assertTrue(s1.contains("<tns:toUnit>?</tns:toUnit>"));

		String s2 = getSampleMessage("/jbide6694/jbide6694.wsdl", "Converter", "ConverterPort", "ConverterBinding", "convert");
		Assert.assertTrue(s2.contains("<tns:ChangeUnit xmlns:tns=\"http://test.jboss.org/ns\">"));
		Assert.assertTrue(s2.contains("<tns:value>?</tns:value>"));
		Assert.assertTrue(s2.contains("<tns:fromUnit>?</tns:fromUnit>"));
		Assert.assertTrue(s2.contains("<tns:toUnit>?</tns:toUnit>"));
	}

	@Test
	public void testJBIDE6865() {
		String s1 = getSampleMessage("/jbide6865/wsdl1.wsdl", "DirectFlight", "DirectFlightSoap", "FlightAwareDirectFlight:DirectFlightSoap", "AirportInfo");
		Assert.assertTrue(s1.contains("<airportCode>?</airportCode>"));
	}
	
	@Test
	public void testJBDS1602() {
		String s1 = getSampleMessage("/jbds1602/StockQuoteService.wsdl", "StockQuoteService", "StockQuoteServicePort", "tns:StockQuoteServiceBinding", "getStockQuoteBySymbol");
		Assert.assertTrue(s1.contains("<stoc:getStockQuoteBySymbol xmlns:stoc=\"http://www.jboss.com/webservices/StockQuoteService\">"));
		Assert.assertTrue(s1.contains("<arg0>?</arg0>"));

		String s2 = getSampleMessage("/jbds1602/jb/SampleWS.wsdl", "SampleWSService", "SampleWSPort", "tns:SampleWSServiceSoapBinding", "echo");
		Assert.assertTrue(s2.contains("<test:echo xmlns:test=\"http://test/\">"));
		Assert.assertTrue(s2.contains("<test:arg0  xmlns:x=\"http://example.com/attr/x\" x:C=\"?\"  xmlns:y=\"http://example.com/attr/y\" y:D=\"?\" >"));
		Assert.assertTrue(s2.contains("<othe:OtherType  xmlns:othe=\"http://example.com/attr/other\" othe:myid=\"?\"  x:Y=\"?\" >"));
		Assert.assertTrue(s2.contains("<b:description>?</b:description>"));
	}
	
	@Test
	public void testJBIDE8770() {
		String s1 = getSampleMessage("/jbide8770/parts.wsdl", "basic", "minusPort", "minusSOAP", "minusOperation");
		Assert.assertTrue(s1.contains("<basi:operationRequest xmlns:basi=\"http://www.example.org/ws/basic/\">"));
		Assert.assertTrue(s1.contains("<a>?</a>"));
		
		String s2 = getSampleMessageHeader("/jbide8770/parts.wsdl", "basic", "minusPort", "minusSOAP", "minusOperation");
		Assert.assertTrue(s2.contains("<squa:storeHeader xmlns:squa=\"http://www.example.org/ws/square/\">"));
		Assert.assertTrue(s2.contains("<timestamp>?</timestamp>"));
	}

	private String getSampleMessage(String res, String service, String port, String binding, String operation) {
		Definition def = readWSDL(res);
		return SchemaUtils.getSampleSOAPInputMessage(def, service, port, binding, operation);
	}

	private String getSampleMessageHeader(String res, String service, String port, String binding, String operation) {
		Definition def = readWSDL(res);
		return SchemaUtils.getSampleSOAPMessageHeader(def, service, port, binding, operation);
	}

	private Definition readWSDL(String path) {
		try {
			URL url = SchemaUtils.class.getResource(path).toURI().toURL();
			return SchemaUtils.readWSDLURL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (WSDLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}
}
