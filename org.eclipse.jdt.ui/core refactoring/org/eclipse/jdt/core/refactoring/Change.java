/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.core.refactoring;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.JavaModelException;

/**
 * An abstract default implementation for a change object - suitable for subclassing. This class manages
 * the change's active status.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class Change implements IChange {

	private boolean fIsActive= true;

	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void aboutToPerform() {
		// do nothing.
	}
	 
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void performed() {
		// do nothing.
	} 
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void setActive(boolean active) {
		fIsActive= active;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public boolean isActive() {
		return fIsActive;
	}
	
	/**
	 * Handles the given exception using the <code>IChangeExceptionHandler</code> provided by
	 * the given change context. If the execution of the change is to be aborted than
	 * this method throws a corresponding <code>JavaModelException</code>. The exception
	 * is either the given exception if it is an instance of <code>JavaModelException</code> or
	 * a new one created by calling <code>new JavaModelException(exception, code)</code>.
	 * 
	 * @param context the change context used to retrieve the exception handler
	 * @param exception the exception caugth during change execution
	 * @exception <code>ChangeAbortException</code> if the execution is to be aborted
	 */
	protected void handleException(ChangeContext context, Exception exception) throws ChangeAbortException {
		if (exception instanceof ChangeAbortException)
			throw (ChangeAbortException)exception;
			
		context.getExceptionHandler().handle(context, this, exception);
	}	
}