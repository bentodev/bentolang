package bento.compiler.visitor;

import bento.lang.AbstractNode;

public class BentoCompilerVisitDestination {
    
    public void visit( BentoCompilerVisitor visitor ) {
        visitor.visit( (AbstractNode)this );
    }

}
