/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.configuration;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

/**
 * Utility class to manipulate WTP project facets .
 * 
 * @author xcoulon
 */
public final class ProjectBuilderUtils {

	public static final String VALIDATOR_BUILDER_ID = "org.eclipse.wst.validation.validationbuilder";
	
	/** Hidden constructor of the utility method. Prevents instantiation. */
	private ProjectBuilderUtils() {
		super();
	}

	/**
	 * Check if a builder identified by its ID is installed on a given project.
	 * 
	 * @param project
	 *            the project to look into
	 * @param builderId
	 *            the Metamodel Builder ID to look up in the project's builders
	 * @return true if the Metamodel Builder is installed (ie, declared)
	 * @throws CoreException
	 *             in case of exception
	 */
	public static boolean isProjectBuilderInstalled(final IProject project, final String builderId)
			throws CoreException {
		ICommand[] commands = project.getDescription().getBuildSpec();
		for (ICommand command : commands) {
			if (command.getBuilderName().equals(builderId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Install the given builder identified by its ID on the given project as
	 * the first builder of the project, to be sure it is always called before
	 * the validation builder.
	 * 
	 * @param project
	 *            the project on which the facet should be installed
	 * @param builderId
	 *            the id of the builder to install
	 * @return true if the facet was installed
	 * @throws CoreException
	 *             in case of exception
	 */
	public static boolean installProjectBuilder(final IProject project, final String builderId) throws CoreException {
		final IProjectDescription desc = project.getDescription();
		final ICommand[] commands = desc.getBuildSpec();
		if (isProjectBuilderInstalled(project, builderId)) {
			return false;
		}
		// prepare a long array to store the JAX-RS builder id
		final ICommand jaxrsBuilderCommand = desc.newCommand();
		jaxrsBuilderCommand.setBuilderName(builderId);
		final int validatorBuilderIdIndex = locatorValidatorBuilderId(commands);
		if(validatorBuilderIdIndex == -1) {
			final ICommand[] newCommands = new ICommand[commands.length + 2];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			newCommands[newCommands.length - 2] = jaxrsBuilderCommand;
			final ICommand validationBuilderCommand = desc.newCommand();
			validationBuilderCommand.setBuilderName(VALIDATOR_BUILDER_ID);
			newCommands[newCommands.length - 1] = validationBuilderCommand;
			desc.setBuildSpec(newCommands);
		} else {
			final ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, validatorBuilderIdIndex);
			System.arraycopy(commands, validatorBuilderIdIndex, newCommands, validatorBuilderIdIndex + 1,
					commands.length - validatorBuilderIdIndex);
			newCommands[validatorBuilderIdIndex] = jaxrsBuilderCommand;
			desc.setBuildSpec(newCommands);
		}
		project.setDescription(desc, null);
		return true;
	}

	/**
	 * Locates the validator builder command (using its known Id) in the given array of {@link ICommand}s
	 * @param commands the builder commands
	 * @return the location in the array or -1 if not found.
	 * @see ProjectBuilderUtils#VALIDATOR_BUILDER_ID
	 */
	private static int locatorValidatorBuilderId(final ICommand[] commands) {
		for(int i = 0; i < commands.length; i++) {
			if(commands[i].getBuilderName().equals(VALIDATOR_BUILDER_ID)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the builder position in the indexed list of builders of the given
	 * project, starting from 0
	 * 
	 * @param project
	 *            the project
	 * @param builderId
	 *            the builder ID
	 * @return the index or -1 if not found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static int getBuilderPosition(final IProject project, final String builderId) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].getBuilderName().equals(builderId)) {
				return i;
			}
		}
		// not found
		return -1;
	}

	/**
	 * Remove the given builder identified by its ID on the given project.
	 * 
	 * @param project
	 *            the project from which the facet should be removed
	 * @param builderId
	 *            the id of the builder to remove
	 * @return true if the facet was removed
	 * @throws CoreException
	 *             in case of exception
	 */
	public static boolean uninstallProjectBuilder(final IProject project, final String builderId) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; i++) {
			if (commands[i].getBuilderName().equals(builderId)) {
				// remove builder from project
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				desc.setBuildSpec(newCommands);
				project.setDescription(desc, null);
				return true;
			}
		}
		return false;
	}

}
