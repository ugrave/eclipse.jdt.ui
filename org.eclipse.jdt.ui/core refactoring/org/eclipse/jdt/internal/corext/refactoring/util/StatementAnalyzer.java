/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.CompilationUnitBuffer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * Analyzer to check if a selection covers a valid set of statements of an abstract syntax
 * tree. The selection is valid iff
 * <ul>
 * 	<li>it does not start or end in the middle of a comment.</li>
 * 	<li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 */
public class StatementAnalyzer extends SelectionAnalyzer {

	protected ICompilationUnit fCUnit;
	private CompilationUnitBuffer fBuffer;
	private RefactoringStatus fStatus;

	public StatementAnalyzer(ICompilationUnit cunit, Selection selection, boolean traverseSelectedNode) throws JavaModelException {
		super(selection, traverseSelectedNode);
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		fStatus= new RefactoringStatus();
		fBuffer= new CompilationUnitBuffer(fCUnit);
	}
	
	protected void checkSelectedNodes() {
		ASTNode[] nodes= getSelectedNodes();
		int selectionOffset= getSelection().getOffset();
		int pos= fBuffer.indexOfNextToken(selectionOffset, false);
		ASTNode node= nodes[0];
		if (node.getStartPosition() != pos) {
			ISourceRange range= new SourceRange(selectionOffset, node.getStartPosition() - selectionOffset + 1);
			invalidSelection(
				RefactoringCoreMessages.getString("StatementAnalyzer.beginning_of_selection"),  //$NON-NLS-1$
				JavaSourceContext.create(fCUnit, range));
			return;
		}	
		
		node= nodes[nodes.length - 1];
		pos= fBuffer.indexOfNextToken(node.getStartPosition() + node.getLength(), false);
		if (pos != -1 && pos <= getSelection().getInclusiveEnd()) {
			ISourceRange range= new SourceRange(ASTNodes.getExclusiveEnd(node), pos - ASTNodes.getExclusiveEnd(node));
			invalidSelection(
				RefactoringCoreMessages.getString("StatementAnalyzer.end_of_selection"), 	//$NON-NLS-1$
				JavaSourceContext.create(fCUnit, range)); 
		}
	}
	
	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	protected CompilationUnitBuffer getBuffer() {
		return fBuffer;
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(CompilationUnit node) {
		if (!hasSelectedNodes()) {
			super.endVisit(node);
			return;
		}
		ASTNode selectedNode= getFirstSelectedNode();
		Selection selection= getSelection();
		if (node != selectedNode) {
			ASTNode parent= selectedNode.getParent();
			fStatus.merge(CommentAnalyzer.perform(selection, fBuffer, parent.getStartPosition(), parent.getLength()));
		}
		if (!fStatus.hasFatalError())
			checkSelectedNodes();
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(DoStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getBody()) && contains(selectedNodes, node.getExpression())) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.do_body_expression")); //$NON-NLS-1$
			}
		}
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(ForStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			boolean containsExpression= contains(selectedNodes, node.getExpression());
			boolean containsUpdaters= contains(selectedNodes, node.updaters());
			if (contains(selectedNodes, node.initializers()) && containsExpression) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.for_initializer_expression")); //$NON-NLS-1$
			} else if (containsExpression && containsUpdaters) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.for_expression_updater")); //$NON-NLS-1$
			} else if (containsUpdaters && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.for_updater_body")); //$NON-NLS-1$
			}
		}
		super.endVisit(node);
	}

	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(SwitchStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			List cases= getSwitchCases(node);
			for (int i= 0; i < selectedNodes.length; i++) {
				ASTNode topNode= selectedNodes[i];
				if (cases.contains(topNode)) {
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.switch_statement")); //$NON-NLS-1$
					break;
				}
			}
		}
		super.endVisit(node);
	}

	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(SynchronizedStatement node) {
		ASTNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			if (firstSelectedNode == node.getBody()) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.synchronized_statement")); //$NON-NLS-1$
			}
		}
		super.endVisit(node);
	}

	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(TryStatement node) {
		ASTNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (firstSelectedNode == node.getBody() || firstSelectedNode == node.getFinally()) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement")); //$NON-NLS-1$
			} else {
				List catchClauses= node.catchClauses();
				for (Iterator iterator= catchClauses.iterator(); iterator.hasNext();) {
					CatchClause element= (CatchClause)iterator.next();
					if (element == firstSelectedNode || element.getBody() == firstSelectedNode) {
						invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement")); //$NON-NLS-1$
					} else if (element.getException() == firstSelectedNode) {
						invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.catch_argument")); //$NON-NLS-1$
					}
				}
			}
		}
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(WhileStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getExpression()) && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.while_expression_body")); //$NON-NLS-1$
			}
		}
		super.endVisit(node);
	}	
	
	private boolean doAfterValidation(ASTNode node, ASTNode[] selectedNodes) {
		return selectedNodes.length > 0 && node == selectedNodes[0].getParent() && getSelection().getEndVisitSelectionMode(node) == Selection.AFTER;
	}
	
	protected void invalidSelection(String message) {
		fStatus.addFatalError(message);
		reset();
	}
	
	protected void invalidSelection(String message, Context context) {
		fStatus.addFatalError(message, context);
		reset();
	}
	
	private static List getSwitchCases(SwitchStatement node) {
		List result= new ArrayList();
		for (Iterator iter= node.statements().iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof SwitchCase)
				result.add(element);
		}
		return result;
	}
	
	protected static boolean contains(ASTNode[] nodes, ASTNode node) {
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] == node)
				return true;
		}
		return false;
	}	
	
	protected static boolean contains(ASTNode[] nodes, List list) {
		for (int i = 0; i < nodes.length; i++) {
			if (list.contains(nodes[i]))
				return true;
		}
		return false;
	}	
}
