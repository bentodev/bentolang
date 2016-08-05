/* Bento
 *
 * $Id: ChoiceExpression.java,v 1.13 2015/06/18 13:18:09 sthippo Exp $
 *
 * Copyright (c) 2002-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A ChoiceExpression is a construction based on the question mark operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.13 $
 */
public class ChoiceExpression extends Expression {

    public ChoiceExpression() {
        super();
    }

    private ChoiceExpression(ChoiceExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        Value test = getChildValue(context, 0);
        if (test.getBoolean()) {
            return getChildValue(context, 1);
        } else {
            return getChildValue(context, 2);
        }
    }

    /** Return the construction that this choice resolves to.
     */
    public Construction getUltimateConstruction(Context context) {
        try {
            Value test = getChildValue(context, 0);
            Object resolvedObj;
            if (test.getBoolean()) {
                resolvedObj = getChild(1);
            } else {
                resolvedObj = getChild(2);
            }
            if (resolvedObj instanceof Construction) {
                return (Construction) resolvedObj;
            }
        } catch (Redirection r) {
            log("Redirection getting ultimate construction in ChoiceExpression: " + r);
        }
        return this;
    }

    public Type getType(Context context, boolean generate) {
        try {
            Value test = getChildValue(context, 0);
            if (test.getBoolean()) {
                return getChildType(context, generate, 1);
            } else {
                return getChildType(context, generate, 2);
            }
        } catch (Redirection r) {
            return DefaultType.TYPE;
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append('(');
        sb.append(getChild(0).toString());
        sb.append(" ? ");
        sb.append(getChild(1).toString());
        sb.append(" : ");
        sb.append(getChild(2).toString());
        sb.append(')');
        return sb.toString();
    }

    public Expression resolveExpression(Context context) {
        ChoiceExpression resolvedExpression = new ChoiceExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}