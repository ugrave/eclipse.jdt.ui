/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.packages;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePackageRefactoring extends Refactoring implements IRenameRefactoring{
	
	private IPackageFragment fPackage;
	private String fNewName;
	
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;

	public RenamePackageRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragment pack, String newName){
		super();
		Assert.isNotNull(pack, "package");
		Assert.isNotNull(changeCreator, "change creator");
		Assert.isNotNull(newName, "new name");
		fTextBufferChangeCreator= changeCreator;		
		fPackage= pack;
		fNewName= newName;
	}
	
	public RenamePackageRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragment pack){
		super();
		Assert.isNotNull(pack, "package");
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;		
		fPackage= pack;
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return "RenamePackageRefactoring:" + fPackage.getElementName() + " to " + fNewName;
	}

	public RenamePackageRefactoring(IJavaSearchScope scope, IPackageFragment pack, String newName){
		super(scope);
		fPackage= pack;
		Assert.isNotNull(fPackage);
		fNewName= newName;
		Assert.isNotNull(fNewName);
	}
	
	public final void setJavaElement(IJavaElement javaElement){
		Assert.isNotNull(javaElement);
		Assert.isTrue(javaElement.exists(), "package must exist");	
		fPackage= (IPackageFragment)javaElement;
	}
	
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	public final String getNewName(){
		return fNewName;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("Checking preconditions", 1);
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fPackage));
		
		if (fPackage.isDefaultPackage())
			result.addFatalError("Cannot rename the default package");
		pm.done();	
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		/*
		 * not checked preconditions:
		 *  a. native methods in locally defined types in this package (too expensive - requires AST analysis)
		 */
		pm.beginTask("", 13);
		pm.subTask("Checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkNewName());
		pm.worked(1);
		pm.subTask("Analyzing");
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 6, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK))));
		pm.worked(1);
		result.merge(checkForNativeMethods());
		pm.worked(1);
		result.merge(checkForMainMethods());
		pm.worked(1);
		result.merge(analyzeAffectedCompilationUnits());
		pm.worked(2);
		result.merge(checkPackageName());
		pm.worked(1);
		pm.done();
		return result;
	}
	
	private List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			fOccurrences= RefactoringSearchEngine.search(pm, getScope(), createSearchPattern());		}	
		return fOccurrences;
	}
	
	private RefactoringStatus checkForMainMethods() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		if (cus == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForMainMethods(cus[i]));
		return result;
	}
	
	private RefactoringStatus checkForNativeMethods() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		if (cus == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForNativeMethods(cus[i]));
		return result;
	}
	
	public RefactoringStatus checkNewName(){
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkPackageName(fNewName));
		if (Checks.isAlreadyNamed(fPackage, fNewName))
			result.addFatalError("Choose another name.");
		result.merge(checkPackageInCurrentRoot());
		return result;
	}
	
	
	
	private RefactoringStatus checkPackageInCurrentRoot(){
		if  (((IPackageFragmentRoot)fPackage.getParent()).getPackageFragment(fNewName).exists()){
			RefactoringStatus result= new RefactoringStatus();
			result.addFatalError("Package already exists.");
			return result;
		}
		else 
			return null;		
	}

	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fPackage, IJavaSearchConstants.REFERENCES);
	}
	
	private RefactoringStatus checkPackageName() throws JavaModelException{
		RefactoringStatus r= checkPackageInCurrentRoot();
		if (r != null && (!r.isOK())) 
			return r;
		
		RefactoringStatus result= new RefactoringStatus();
		
		IPackageFragmentRoot[] roots= fPackage.getJavaProject().getPackageFragmentRoots();
		
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].getPackageFragment(fNewName).exists()){
				result.addFatalError("Package " + fNewName + " already exists in this project");
				break;
			}
		}
		return result;	
	}
		
	//-------------- AST visitor-based analysis
	
	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		Iterator iter= getOccurrences(null).iterator();
		RenamePackageASTAnalyzer analyzer= new RenamePackageASTAnalyzer(fNewName);
		while (iter.hasNext()){
			analyzeCompilationUnit(analyzer, (List)iter.next(), result);
		}
		return result;
	}
	
	private void analyzeCompilationUnit(RenamePackageASTAnalyzer analyzer, List searchResults, RefactoringStatus result)  throws JavaModelException {
		SearchResult searchResult= (SearchResult)searchResults.get(0);
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResult.getResource()));
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzer.analyze(searchResults, cu));
	}
	
	// ----------- Changes ---------------
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("creating rename package change", 1 + getOccurrences(null).size());
		CompositeChange builder= new CompositeChange();
		addOccurrences(pm, builder);
		builder.addChange(new RenamePackageChange(fPackage, fNewName));
		pm.worked(1);
		pm.done();
		HackFinder.fixMeSoon("maybe add dispose() method?");
		fOccurrences= null; //to prevent memory leak
		return builder;
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange("Package Reference Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName);
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		for (Iterator iter= getOccurrences(null).iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= fTextBufferChangeCreator.create("Rename Package", (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource()));
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
	}
	
}