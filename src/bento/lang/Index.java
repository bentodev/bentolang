/* Bento
 *
 * $Id: Index.java,v 1.10 2014/05/21 13:21:00 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * Base class for ArrayIndex and TableIndex classes; refers to a
 * location (offset or key) in a collection.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */
abstract public class Index extends AbstractNode {

    private boolean dynamic = false;

    public Index() {
        super();
    }

    public Index(Value value) {
        super();
        setChild(0, (AbstractNode) value);
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return dynamic;
    }

    protected void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    public Value getIndexValue(Context context) {
        BentoNode node = getChild(0);
        if (node instanceof Value) {
            return (Value) node;
        } else {
            try {
                return ((ValueGenerator) node).getValue(context);
            } catch (Redirection r) {
                return new PrimitiveValue();
            }
        }
    }
    
    public Index instantiateIndex(Context context) {
        Index instantiatedIndex = createIndex();
        instantiatedIndex.setChild(0, (AbstractNode) getIndexValue(context));
        return instantiatedIndex;
    }
    
    public Index resolveIndex(Context context) {
        Index resolvedIndex = (Index) clone();
        AbstractNode node = (AbstractNode) resolvedIndex.getChild(0);
        if (node instanceof Instantiation) {
            try {
                node = AbstractConstruction.resolveInstance((Instantiation) node, context);
            } catch (Redirection r) {
                ;
            }
        }
        resolvedIndex.setChild(0, node);
        return resolvedIndex;
    }
    
    public String getModifierString(Context context) {
        String str = toString();
        
        // find loop parameters
        AbstractNode node = (AbstractNode) getChild(0);
        if (node instanceof Instantiation) {
            int kind = ((Instantiation) node).getKind();
            if (kind == Instantiation.FOR_PARAMETER || kind == Instantiation.FOR_PARAMETER_CHILD) {
                Context resolutionContext = ((node instanceof ResolvedInstance) ? ((ResolvedInstance)node).getResolutionContext() : context);
                str = str + "#" + resolutionContext.getLoopIndex();                  
            }
        }
        
        return str;
    }
    
    abstract protected Index createIndex(); 
    
    abstract public String toString();
}
