/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaElementImplementationHyperlink;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;


/**
 * The action allows to open the implementation for a method in its hierarchy.
 * <p>
 * The action is applicable to selections containing elements of type <code>
 * IMethod</code>.
 * </p>
 * 
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenImplementationAction extends SelectionDispatchAction {

	/**
	 * The Java editor.
	 */
	private JavaEditor fEditor;


	/**
	 * Creates an <code>OpenImplementationAction</code>.
	 * 
	 * @param site the workbench site
	 */
	protected OpenImplementationAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenImplementationAction_label);
		setDescription(ActionMessages.OpenImplementationAction_description);
		setToolTipText(ActionMessages.OpenImplementationAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_IMPLEMENTATION_ACTION);
	}

	/**
	 * Creates an <code>OpenImplementationAction</code>. Note: This constructor is for internal use
	 * only. Clients should not call this constructor.
	 * 
	 * @param editor the editor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public OpenImplementationAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor) && fEditor.getSelectionProvider().getSelection() instanceof ITextSelection);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(false);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor))
			return;
		IJavaElement element= elementAtOffset();
		if (element == null) {
			return;
		}
		run(element);
	}

	/**
	 * Returns the dialog title.
	 * 
	 * @return the dialog title
	 */
	private String getDialogTitle() {
		return ActionMessages.OpenImplementationAction_error_title;
	}

	/**
	 * Returns the java element corresponding to the selection offset or <code>null</code> if no
	 * java element was found.
	 * 
	 * @return the java element that corresponds to the selection, <code>null</code> if no java
	 *         element was found
	 */
	private IJavaElement elementAtOffset() {
		try {
			return SelectionConverter.getElementAtOffset(fEditor);
		} catch (JavaModelException e) {
		}
		return null;
	}

	/**
	 * Checks if the selected java element is an overridable method, and finds the implementations
	 * for the method.
	 * 
	 * @param element the java element
	 */
	private void run(IJavaElement element) {
		if (!((element instanceof IMethod) && canBeOverriddenMethod((IMethod)element))) {
			MessageDialog.openInformation(getShell(), getDialogTitle(),
					Messages.format(ActionMessages.OpenImplementationAction_not_applicable, BasicElementLabels.getJavaElementName(element.getElementName())));
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		SelectionDispatchAction openAction= (SelectionDispatchAction)fEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (openAction == null)
			return;

		ISourceRange nameRange;
		try {
			nameRange= ((IMethod)element).getNameRange();
			if (nameRange == null) // Panic code (should not happen for methods)
				return;
		} catch (JavaModelException e) {
			return;
		}

		IRegion region= new Region(nameRange.getOffset(), nameRange.getLength());
		JavaElementImplementationHyperlink.openImplementations(fEditor, region, element, openAction);
	}

	/**
	 * Checks whether a method can be overridden.
	 * 
	 * @param method the method
	 * @return <code>true</code> if the method can be overridden, <code>false</code> otherwise
	 */
	private boolean canBeOverriddenMethod(IMethod method) {
		try {
			return !(JdtFlags.isPrivate(method) || JdtFlags.isFinal(method) || JdtFlags.isStatic(method) ||
					method.isConstructor() || JdtFlags.isFinal((IMember)method.getParent()));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}
}