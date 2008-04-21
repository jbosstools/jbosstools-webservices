package org.jboss.tools.ws.creation.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Ff {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 
		    
		// TODO Auto-generated method stub
		try {
		
			InputStream in = Runtime.getRuntime().exec("ls").getInputStream();
			byte[] b = new byte[100000]; 
			in.read(b,0,99999);
			System.out.print(new String(b));
			
		
		        
			
		} catch (IOException e) {        
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
