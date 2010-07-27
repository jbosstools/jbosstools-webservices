package org.jboss.tools.ws.ui.views;

import java.util.Stack;

public class TestHistory {
	
	private Stack<TestHistoryEntry> entries = null;
	
	public TestHistory() {
		entries = new Stack<TestHistoryEntry>();
	}

	public Stack<TestHistoryEntry> getEntries() {
		return entries;
	}
}
