/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.core.refactoring.Assert;

public class Selection {
	
	public int start;		// inclusive
	public int end;		// inclusive
	
	public Selection(int s, int e) {
		start= s;
		end= e;
		Assert.isTrue(start <= end);
	}
	
	// enclosed* methods do a open interval check.
	
	public boolean enclosedBy(AstNode node) {
		return node.sourceStart < start && end < node.sourceEnd;		
	}
	
	public boolean enclosedBy(TypeDeclaration node) {
		return node.declarationSourceStart < start && end < node.declarationSourceEnd;
	}
	
	public boolean enclosedBy(AbstractMethodDeclaration node) {
		return node.bodyStart <= start && end <= node.bodyEnd;
	}
	
	// cover* methods do a closed interval check.
	
	public boolean covers(AstNode node) {
		return start <= node.sourceStart && node.sourceEnd <= end;
	}
	
	public boolean covers(int position) {
		return start <= position && position <= end;
	}
	
	public boolean coveredBy(AstNode node) {
		return coveredBy(node.sourceStart, node.sourceEnd);
	}
	
	public boolean coveredBy(int sourceStart, int sourceEnd) {
		return sourceStart <= start && end <= sourceEnd;
	}
	
	public boolean endsIn(AstNode node) {
		return node.sourceStart <= end && end < node.sourceEnd;
	}
	
	public boolean intersectsBlock(Block block) {
		return block.sourceStart < start && start <= block.sourceEnd && block.sourceEnd <= end ||
		       start <= block.sourceStart && block.sourceStart <= end && end < block.sourceEnd;
	}
}