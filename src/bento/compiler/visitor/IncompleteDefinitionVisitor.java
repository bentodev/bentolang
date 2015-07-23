package bento.compiler.visitor;

import bento.lang.AbstractNode;
import bento.lang.NullValue;

public class IncompleteDefinitionVisitor extends BentoCompilerVisitor {

	boolean foundIncompleteDefinition = false;

	public void visit( AbstractNode target ) {
		if ( target instanceof NullValue ) {
			NullValue nvTarget = (NullValue)target;
			foundIncompleteDefinition = nvTarget.isAbstract(null);
		}
	}
	
	public boolean isIncomplete() {
		return foundIncompleteDefinition;
	}

}
