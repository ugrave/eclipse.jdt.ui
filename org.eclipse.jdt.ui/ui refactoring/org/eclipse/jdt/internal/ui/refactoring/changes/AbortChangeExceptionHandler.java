/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.refactoring.changes;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jdt.core.refactoring.ChangeAbortException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.IChangeExceptionHandler;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A default implementation of <code>IChangeExceptionHandler</code> which
 * always aborts an change if an exception is caught.
 */
public class AbortChangeExceptionHandler implements IChangeExceptionHandler {
	
	public void handle(ChangeContext context, IChange change, Exception e) {
		JavaPlugin.log(e);
		throw new ChangeAbortException(e);
	}
}
