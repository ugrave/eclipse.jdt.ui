package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

class RenameAnalyzeUtil {
	
	private RenameAnalyzeUtil(){}
	
	static RefactoringStatus analyzeRenameChanges(TextChangeManager manager,  SearchResultGroup[] oldOccurrences, SearchResultGroup[] newOccurrences) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < oldOccurrences.length; i++) {
			SearchResultGroup searchResultGroup= oldOccurrences[i];
			SearchResult[] searchResults= searchResultGroup.getSearchResults();
			ICompilationUnit cunit= searchResultGroup.getCompilationUnit();
			if (cunit == null)
				continue;
			for (int j= 0; j < searchResults.length; j++) {
				SearchResult searchResult= searchResults[j];
				if (! RenameAnalyzeUtil.existsInNewOccurrences(searchResult, newOccurrences, manager)){
					ISourceRange range= new SourceRange(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart());
					Context context= JavaSourceContext.create(cunit, range); //XXX
					String message= RefactoringCoreMessages.getFormattedString("RenameAnalyzeUtil.shadows", cunit.getElementName());	//$NON-NLS-1$
					result.addError(message , context);
				}	
			}
		}
		return result;
	}

	static ICompilationUnit findWorkingCopyForCu(ICompilationUnit[] newWorkingCopies, ICompilationUnit cu){
		ICompilationUnit originalDeclaringCu= WorkingCopyUtil.getOriginal(cu);
		for (int i= 0; i < newWorkingCopies.length; i++) {
			if (WorkingCopyUtil.getOriginal(newWorkingCopies[i]).equals(originalDeclaringCu))
				return newWorkingCopies[i];
		}
		return null;
	}

	static ICompilationUnit[] getNewWorkingCopies(ICompilationUnit[] compilationUnitsToModify, TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		ICompilationUnit[] newWorkingCopies= new ICompilationUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			ICompilationUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= WorkingCopyUtil.getNewWorkingCopy(cu);
			newWorkingCopies[i].getBuffer().setContents(manager.get(cu).getPreviewTextBuffer().getContent());
			newWorkingCopies[i].makeConsistent(new SubProgressMonitor(pm, 1));
		}
		return newWorkingCopies;
	}
	
	private static boolean existsInNewOccurrences(SearchResult searchResult, SearchResultGroup[] newOccurrences, TextChangeManager manager) throws CoreException{
		SearchResultGroup newGroup= findOccurrenceGroup(searchResult.getResource(), newOccurrences);
		if (newGroup == null)
			return false;
		
		TextRange oldEditRange= getCorrespondingEditChangeRange(searchResult, manager);
		if (oldEditRange == null)
			return false;
		
		SearchResult[] newSearchResults= newGroup.getSearchResults();
		int oldRangeOffset = oldEditRange.getOffset();
		for (int i= 0; i < newSearchResults.length; i++) {
			if (newSearchResults[i].getStart() == oldRangeOffset)
				return true;
		}
		return false;
	}
	
	private static TextRange getCorrespondingEditChangeRange(SearchResult searchResult, TextChangeManager manager) throws CoreException{
		TextChange change= getTextChange(searchResult, manager);
		if (change == null)
			return null;
		
		TextRange oldMatchRange= createTextRange(searchResult);
		EditChange[] editChanges= change.getTextEditChanges();	
		for (int i= 0; i < editChanges.length; i++) {
			if (oldMatchRange.equals(editChanges[i].getTextRange()))
				return change.getNewTextRange(editChanges[i]);
		}
		return null;
	}
	
	private static TextChange getTextChange(SearchResult searchResult, TextChangeManager manager) throws CoreException{
		ICompilationUnit cu= searchResult.getCompilationUnit();
		if (cu == null)
			return null;
		ICompilationUnit oldWorkingCopy= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (oldWorkingCopy == null)
			return null;
		return manager.get(oldWorkingCopy);
	}
	
	private static TextRange createTextRange(SearchResult searchResult) {
		return TextRange.createFromStartAndExclusiveEnd(searchResult.getStart(), searchResult.getEnd());
	}
	private static SearchResultGroup findOccurrenceGroup(IResource resource, SearchResultGroup[] newOccurrences){
		for (int i= 0; i < newOccurrences.length; i++) {
			if (newOccurrences[i].getResource().equals(resource))
				return newOccurrences[i];
		}
		return null;
	}
}
