package bento.compiler.visitor;

import java.util.Iterator;

import bento.lang.AbstractNode;

abstract public class BentoCompilerVisitor {
    abstract public void visit( AbstractNode target );
    public boolean done() {
        return false;
    }
    
    public void traverse( AbstractNode n ) {
        traverse( n.getChildren() );
    }
    
    void traverse( Iterator i ) {
        while ( i.hasNext() ) {
            if ( done() ) {
                return;
            }
            AbstractNode n = (AbstractNode)i.next();
            visit( n );
            traverse( n.getChildren() );
        }
    }
}
