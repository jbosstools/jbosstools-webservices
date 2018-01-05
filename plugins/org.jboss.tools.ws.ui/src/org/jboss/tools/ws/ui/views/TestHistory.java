/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.ui.views;

import java.util.Iterator;
import java.util.Stack;

public class TestHistory {
	
	private Stack<TestEntry> entries = null;
	
	public TestHistory() {
		entries = new Stack<TestEntry>();
	}

	public Stack<TestEntry> getEntries() {
		return entries;
	}
	
	/**
	 * @since 2.0
	 */
	public TestEntry findEntryByURL ( String urlStr ) {
		Iterator<TestEntry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			TestEntry entry = entryIter.next();
			if (entry.getUrl().contentEquals(urlStr)) {
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * @since 2.0
	 */
	public void addEntry (TestEntry newEntry ) {
		this.entries.push(newEntry);
	}
	
	/**
	 * @since 2.0
	 */
	public void replaceEntry ( TestEntry oldEntry, TestEntry newEntry) {
		boolean found = entries.remove(oldEntry);
		if (found) {
			addEntry(newEntry);
		}
	}

	@Override
	public String toString() {
		String result = "TestHistory [entries= \n";  //$NON-NLS-1$
		Iterator<TestEntry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			TestEntry entry = entryIter.next();
			result = result + entry.toString();
			if (entryIter.hasNext()) {
				result = result + '\n';
			}
		}
		result = result + "]"; //$NON-NLS-1$
		return result;
	}
	
}
