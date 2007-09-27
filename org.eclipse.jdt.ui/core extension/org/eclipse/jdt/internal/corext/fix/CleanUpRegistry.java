/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;

import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;
import org.eclipse.jdt.internal.ui.fix.CodeFormatCleanUp;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.CommentFormatCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.CopyrightUpdaterCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.ImportsCleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.MapCleanUpOptions;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpMessages;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeFormatingTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeStyleTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CopyrightTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MemberAccessesTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.UnnecessaryCodeTabPage;

/**
 * The clean up registry provides a set of clean ups
 * and there corresponding UI representatives.
 * 
 * @since 3.4
 */
public class CleanUpRegistry {
	
	public abstract class CleanUpTabPageDescriptor {
		
		private String fName;
		private String fId;
		
		/**
		 * @param id a unique id of the page described by this
		 * @param name a human readable name of the page
		 */
		public CleanUpTabPageDescriptor(String id, String name) {
			fId= id;
			fName= name;
		}

		/**
		 * @return unique id of this page
		 */
		public String getId() {
			return fId;
		}
		
		/**
		 * @return the name of the tab
		 */
		public String getName() {
			return fName;
		}
		
		/**
		 * @return new instance of a tab page
		 */
		public abstract ICleanUpTabPage createTabPage();
	}
	
	private ICleanUp[] fCleanUps;
	private CleanUpTabPageDescriptor[] fPageDescriptors;
	
	/**
	 * @return a set of registered clean ups.
	 */
	public synchronized ICleanUp[] getCleanUps() {
		ensureCleanUpRegistered();
		return fCleanUps;
	}

	/**
	 * @return set of clean up tab page descriptors
	 */
	public synchronized CleanUpTabPageDescriptor[] getCleanUpTabPageDescriptors() {
		ensurePagesRegistered();
		return fPageDescriptors;
	}
	
	/**
	 * Returns the default options for the specified clean up kind.
	 * 
	 * @param kind the kind of clean up for which to retrieve the options
	 * @return the default options
	 * 
	 * @see ICleanUp#DEFAULT_CLEAN_UP_OPTIONS
	 * @see ICleanUp#DEFAULT_SAVE_ACTION_OPTIONS
	 */
	public MapCleanUpOptions getDefaultOptions(int kind) {
		MapCleanUpOptions result= new MapCleanUpOptions();
		
		ICleanUp[] cleanUps= getCleanUps();
		
		for (int i= 0; i < cleanUps.length; i++) {
			CleanUpOptions options= cleanUps[i].getDefaultOptions(kind);
			result.addAll(options);
		}
		
		return result;
	}
	
	private synchronized void ensureCleanUpRegistered() {
		if (fCleanUps != null)
			return;
		
		ArrayList result= new ArrayList();
		
		result.add(new CodeStyleCleanUp());
		result.add(new ControlStatementsCleanUp()); 
		result.add(new ConvertLoopCleanUp());
		result.add(new VariableDeclarationCleanUp());
		result.add(new ExpressionsCleanUp());
		result.add(new UnusedCodeCleanUp());
		result.add(new Java50CleanUp());
		result.add(new PotentialProgrammingProblemsCleanUp()); 
		result.add(new UnnecessaryCodeCleanUp()); 
		result.add(new StringCleanUp());
		result.add(new UnimplementedCodeCleanUp());
		result.add(new SortMembersCleanUp());
		result.add(new ImportsCleanUp());
		result.add(new CommentFormatCleanUp());
		result.add(new CodeFormatCleanUp());
		
		if (isUpdateCopyrightEnabled()) {
			result.add(new CopyrightUpdaterCleanUp());
		}
		
		fCleanUps= (ICleanUp[]) result.toArray(new ICleanUp[result.size()]);	
	}
	
	private synchronized void ensurePagesRegistered() {
		if (fPageDescriptors != null)
			return;
		
		ArrayList result= new ArrayList();
		
		result.add(new CleanUpTabPageDescriptor(CodeStyleTabPage.ID, CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeStyle) {
			public ICleanUpTabPage createTabPage() {
				return new CodeStyleTabPage(); 
			}
		});
		
		result.add(new CleanUpTabPageDescriptor(MemberAccessesTabPage.ID, CleanUpMessages.CleanUpModifyDialog_TabPageName_MemberAccesses) {
			public ICleanUpTabPage createTabPage() {
				return new MemberAccessesTabPage(); 
			}
		});
		
		result.add(new CleanUpTabPageDescriptor(UnnecessaryCodeTabPage.ID, CleanUpMessages.CleanUpModifyDialog_TabPageName_UnnecessaryCode) {
			public ICleanUpTabPage createTabPage() {
				return new UnnecessaryCodeTabPage(); 
			}
		});
		
		result.add(new CleanUpTabPageDescriptor(MissingCodeTabPage.ID, CleanUpMessages.CleanUpModifyDialog_TabPageName_MissingCode) {
			public ICleanUpTabPage createTabPage() {
				return new MissingCodeTabPage(); 
			}
		});
		
		result.add(new CleanUpTabPageDescriptor(CodeFormatingTabPage.ID, CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeFormating) {
			public ICleanUpTabPage createTabPage() {
				return new CodeFormatingTabPage(); 
			}
		});
		
	
		if (isUpdateCopyrightEnabled()) {
			result.add(new CleanUpTabPageDescriptor(CopyrightTabPage.ID, "Copyright") { //$NON-NLS-1$
				public ICleanUpTabPage createTabPage() {
					return new CopyrightTabPage(); 
				}
			});
		}
		
		fPageDescriptors= (CleanUpTabPageDescriptor[]) result.toArray(new CleanUpTabPageDescriptor[result.size()]);		
	}
	
	private boolean isUpdateCopyrightEnabled() {
		return "true".equals(System.getProperty("org.eclipse.jdt.ui/UpdateCopyrightOnSave")); //$NON-NLS-1$ //$NON-NLS-2$
	}

}