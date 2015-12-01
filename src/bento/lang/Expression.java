/* Bento
 *
 * $Id: Expression.java,v 1.22 2015/06/18 13:18:09 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.*;

import java.util.*;

/**
 * An Expression is a construction based on operators and objects.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.22 $
 */

abstract public class Expression extends AbstractConstruction implements ValueGenerator, ConstructionContainer {

    public Expression() {
        super();
    }
    
    protected Expression(Expression expression) {
        super(expression);
    }

//    public Expression(Definition owner) {
//        super(owner);
//    }

    public boolean isPrimitive() {
        return false;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        boolean parens = (!(getParent() instanceof ArgumentList) && !(getParent() instanceof Index));
        if (parens) {
            sb.append('(');
        }
        int numChildren = getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(getChild(i).toString(""));
        }
        if (parens) {
            sb.append(')');
        }
        return sb.toString();
    }

//    protected int getCacheability(Context context) {
//        int numChildren = getNumChildren();
//        for (int i = 0; i < numChildren; i++) {
//            BentoNode node = getChild(i);
//            if (node instanceof AbstractConstruction) {
//                int childCacheability = ((AbstractConstruction) node).getCacheability(context);
//                if (childCacheability != FULLY_CACHEABLE) {
//                   return CACHE_STORABLE;    
//                }
//                
//            } else if (!node.isStatic()) {
//                return CACHE_STORABLE;    
//            }
//        }<Construction>
//         return FULLY_CACHEABLE;
//    }

    public List<Construction> getConstructions(Context context) {
        List<Construction> constructions = Context.newArrayList(1, Construction.class);
        int numChildren = getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            BentoNode node = getChild(i);
            if (node instanceof ConstructionContainer) {
                constructions.addAll(((ConstructionContainer) node).getConstructions(context));
            } else if (node instanceof Construction) {
                constructions.add((Construction) node);
            }
        }
        return constructions;
    }
    
    /** Returns true if this expression contains a sub statement. **/
    public boolean hasSub() {
//        int numChildren = getNumChildren();
//        for (int i = 0; i < numChildren; i++) {
//            BentoNode node = getChild(i);
//            if (node instanceof AbstractConstruction) {
//                return ((AbstractConstruction) node).hasSub();
//            }
//        }
        return false;
    }

    abstract public Type getType(Context context, Definition resolver);
    /*  {
        try {
            Value value = getValue(context);
            if (value instanceof PrimitiveValue) {
                return new PrimitiveType(value.getValueClass());
            }
        } catch (Redirection r) {
            // ignore redirection, we're just examining the type
            ;
        }
        return DefaultType.TYPE;
    } */
    
    abstract public Expression resolveExpression(Context context);
    
    protected void resolveChildrenInPlace(Context context) {
        int numChildren = getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            BentoNode node = getChild(i);
            if (node instanceof Instantiation && !(node instanceof ResolvedInstance)) {
                setChild(i, new ResolvedInstance((Instantiation) node, context, true));
            } else if (node instanceof Expression) {
                setChild(i, ((Expression) node).resolveExpression(context));
            }
        }
    }

    protected Value getChildValue(Context context, int n) throws Redirection {
        Object child = getChild(n);
        if (child instanceof Value) {
            return (Value) child;
        } else {
            return ((ValueGenerator) child).getValue(context);
        }
    }

    protected Type getChildType(Context context, Definition resolver, int n) {
        Object child = getChild(n);
        if (child instanceof Construction) {
            return ((Construction) child).getType(context, resolver);
        } else {
            try {
                Value value = getChildValue(context, n);
                if (value instanceof PrimitiveValue) {
                    return ((PrimitiveValue)value).getType();
                }
            } catch (Redirection r) {
                ;
            }
        }
        return DefaultType.TYPE;
    }


}
