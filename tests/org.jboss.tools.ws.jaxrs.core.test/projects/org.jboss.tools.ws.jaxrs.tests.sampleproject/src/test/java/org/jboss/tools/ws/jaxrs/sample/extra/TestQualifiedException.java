package org.jboss.tools.ws.jaxrs.sample.extra;


public class TestQualifiedException {

	public class TestException extends Exception {
		
		private static final long serialVersionUID = -1090006116533108548L;

		public TestException(String message) {
			super(message);
		}
	}
}
