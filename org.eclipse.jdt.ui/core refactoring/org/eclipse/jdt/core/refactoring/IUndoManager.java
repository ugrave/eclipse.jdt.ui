/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.core.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaModelException;

/**
 * An undo manager keeps track of changes performed by refactorings. Use <code>performUndo</code> 
 * and <code>performRedo</code> to undo and redo changes.
 * <p>
 * NOTE: This interface is not intended to be implemented or extended. Use Refactoring.getUndoManager()
 * to access the undo manager. </p>
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface IUndoManager {

	/**
	 * Adds a listener to the undo manager.
	 * 
	 * @param listener the listener to be added to the undo manager
	 */
	public void addListener(IUndoManagerListener listener);
	
	/**
	 * Removes the given listener from this undo manager.
	 * 
	 * @param listener the listener to be removed
	 */
	public void removeListener(IUndoManagerListener listener);

	/**
	 * Adds a new undo change to this undo manager.
	 * 
	 * @param name the name of the refactoring the change was created
	 *  for. The name must not be <code>null</code>
	 * @param change the undo change. The change must not be <code>null</code>
	 */
	public void addUndo(String name, IChange change);

	/**
	 * Returns <code>true</code> if there is anything to undo, otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code> if there is anything to undo, otherwise
	 *  <code>false</code>
	 */
	public boolean anythingToUndo();
	
	/**
	 * Returns the name of the top most undo.
	 * 
	 * @return the top most undo name. The main purpose of the name is to
	 * render it in the UI. Returns <code>null</code> if there are no any changes to undo.
	 */
	public String peekUndoName();
	
	/**
	 * Undo the top most undo change.
	 * 
	 * @param pm a progress monitor to report progress during performing
	 *  the undo change. The progress monitor must not be <code>null</code>
	 */	
	public void performUndo(ChangeContext context, IProgressMonitor pm) throws JavaModelException;

	/**
	 * Redo the top most redo change.
	 * 
	 * @param pm a progress monitor to report progress during performing
	 *  the redo change. The progress monitor must not be <code>null</code>
	 */	
	public void performRedo(ChangeContext context, IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * Returns <code>true</code> if there is anything to redo, otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code> if there is anything to redo, otherwise
	 *  <code>false</code>
	 */
	public boolean anythingToRedo();
	
	/**
	 * Returns the name of the top most redo.
	 * 
	 * @return the top most redo name. The main purpose of the name is to
	 * render it in the UI. Returns <code>null</code> if there are no any changes to redo.
	 */
	public String peekRedoName();
	
	/**
	 * Flushes the undo manager's undo and redo stacks.
	 */	
	public void flush();	
}