/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;


public class NLSSearchQuery implements ISearchQuery {

	private NLSSearchResult fResult;
	private IJavaElement[] fWrapperClass;
	private IFile[] fPropertiesFile;
	private IJavaSearchScope fScope;
	private String fScopeDescription;
	
	public NLSSearchQuery(IJavaElement wrapperClass, IFile propertiesFile, IJavaSearchScope scope, String scopeDescription) {
		this(new IJavaElement[] {wrapperClass}, new IFile[] {propertiesFile}, scope, scopeDescription);
	}
	
	public NLSSearchQuery(IJavaElement[] wrapperClass, IFile[] propertiesFile, IJavaSearchScope scope, String scopeDescription) {
		fWrapperClass= wrapperClass;
		fPropertiesFile= propertiesFile;
		fScope= scope;
		fScopeDescription= scopeDescription;
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("", 5 * fWrapperClass.length); //$NON-NLS-1$
		
		try {
			final AbstractTextSearchResult textResult= (AbstractTextSearchResult) getSearchResult();
			textResult.removeAll();
			
			for (int i= 0; i < fWrapperClass.length; i++) {
				IJavaElement wrapperClass= fWrapperClass[i];
				IFile propertieFile= fPropertiesFile[i];
				if (! wrapperClass.exists())
					return JavaUIStatus.createError(0, Messages.format(NLSSearchMessages.NLSSearchQuery_wrapperNotExists, wrapperClass.getElementName()), null); 
				if (! wrapperClass.exists())
					return JavaUIStatus.createError(0, Messages.format(NLSSearchMessages.NLSSearchQuery_propertiesNotExists, propertieFile.getName()), null); 
				
				SearchPattern pattern= SearchPattern.createPattern(wrapperClass, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
				SearchParticipant[] participants= new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
				
				NLSSearchResultRequestor requestor= new NLSSearchResultRequestor(propertieFile, fResult);
				try {
					SearchEngine engine= new SearchEngine();
					engine.search(pattern, participants, fScope, requestor, new SubProgressMonitor(monitor, 4));
					requestor.reportUnusedPropertyNames(new SubProgressMonitor(monitor, 1));
					
					IField[] fields= ((IType)wrapperClass).getFields();
					for (int j= 0; j < fields.length; j++) {
						IField field= fields[j];
						if (isUndefinedKey(field, requestor)) {
							ISourceRange sourceRange= field.getSourceRange();
							if (sourceRange != null)
								fResult.addMatch(new Match(field, sourceRange.getOffset(), sourceRange.getLength()));	
						}
					}
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			}
		} finally {
			monitor.done();
		}
		return 	Status.OK_STATUS;
	}

	private boolean isUndefinedKey(IField field, NLSSearchResultRequestor requestor) throws JavaModelException {
		int flags= field.getFlags();
		if (!Flags.isPublic(flags))
			return false;
		
		if (!Flags.isStatic(flags))
			return false;
		
		String fieldName= field.getElementName();
		if (NLSRefactoring.BUNDLE_NAME.equals(fieldName))
			return false;
		
		if ("RESOURCE_BUNDLE".equals(fieldName)) //$NON-NLS-1$
			return false;
		
		if (requestor.hasPropertyKey(fieldName))
			return false;
		
		return true;		
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return NLSSearchMessages.NLSSearchQuery_label; 
	}

	public String getResultLabel(int nMatches) {
		if (fWrapperClass.length == 1) {
			if (nMatches == 1) {
				String[] args= new String[] {fWrapperClass[0].getElementName(), fScopeDescription};	
				return Messages.format(NLSSearchMessages.SearchOperation_singularLabelPostfix, args); 
			}
			String[] args= new String[] {fWrapperClass[0].getElementName(), String.valueOf(nMatches), fScopeDescription};
			return Messages.format(NLSSearchMessages.SearchOperation_pluralLabelPatternPostfix, args); 
		} else {
			if (nMatches == 1) {
				return Messages.format(NLSSearchMessages.NLSSearchQuery_oneProblemInScope_description, fScopeDescription);
			}
			return Messages.format(NLSSearchMessages.NLSSearchQuery_xProblemsInScope_description, new Object[] {String.valueOf(nMatches), fScopeDescription});
		}
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return true;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		if (fResult == null)
			fResult= new NLSSearchResult(this);
		return fResult;
	}
}
