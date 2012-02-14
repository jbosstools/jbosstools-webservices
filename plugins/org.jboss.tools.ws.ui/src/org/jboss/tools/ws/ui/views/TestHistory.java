package org.jboss.tools.ws.ui.views;

import java.util.Iterator;
import java.util.Stack;

public class TestHistory {
	
	private Stack<TestHistoryEntry> entries = null;
	
	public TestHistory() {
		entries = new Stack<TestHistoryEntry>();
	}

	public Stack<TestHistoryEntry> getEntries() {
		return entries;
	}
	
	public TestHistoryEntry findEntryByURL ( String urlStr ) {
		Iterator<TestHistoryEntry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			TestHistoryEntry entry = entryIter.next();
			if (entry.getUrl().contentEquals(urlStr)) {
				return entry;
			}
		}
		return null;
	}
	
	public void addEntry (TestHistoryEntry newEntry ) {
		this.entries.push(newEntry);
	}
	
	public void replaceEntry ( TestHistoryEntry oldEntry, TestHistoryEntry newEntry) {
		boolean found = entries.remove(oldEntry);
		if (found) {
			addEntry(newEntry);
		}
	}

	@Override
	public String toString() {
		String result = "TestHistory [entries= \n";  //$NON-NLS-1$
		Iterator<TestHistoryEntry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			TestHistoryEntry entry = entryIter.next();
			result = result + entry.toString();
			if (entryIter.hasNext()) {
				result = result + '\n';
			}
		}
		result = result + "]"; //$NON-NLS-1$
		return result;
	}
	
}
