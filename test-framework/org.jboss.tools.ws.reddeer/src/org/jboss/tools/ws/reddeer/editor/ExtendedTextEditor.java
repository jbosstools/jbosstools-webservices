/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.editor;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.eclipse.reddeer.swt.keyboard.Keyboard;
import org.eclipse.reddeer.swt.keyboard.KeyboardFactory;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;

public class ExtendedTextEditor extends TextEditor {

	private final Keyboard keyboard = KeyboardFactory.getKeyboard();

	public ExtendedTextEditor() {
		super();
	}

	public ExtendedTextEditor(String editorTitle) {
		super(editorTitle);
	}

	public void removeLine(String contains) {
		int lineNumber = getLineNum(StringContains.containsString(contains));
		
		selectLine(lineNumber);
		
		keyboard.type(" ");

		save();
	}

	public void replaceLine(String contains, String replacement) {
		int lineNumber = getLineNum(StringContains.containsString(contains));

		selectLine(lineNumber);

		keyboard.type(replacement + "\n");

		save();
	}

	public void replace(String regex, String replacement) {
		setText(getText().replace(regex, replacement));
		save();
	}

	public int getLineNum(Matcher<String> matcher) {
		for (int lineNum = 0; lineNum < getNumberOfLines(); lineNum++) {
			String lineText = getTextAtLine(lineNum);
			if (matcher.matches(lineText)) {
				return lineNum;
			}
		}
		throw new IllegalArgumentException();
	}

	public void insertBeforeLine(String insertText, String afterTextContains) {
		int lineNumber = getLineNum(StringContains
				.containsString(afterTextContains));
		insertLine(lineNumber, insertText);
		save();
	}
}