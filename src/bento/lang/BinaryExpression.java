/* Bento
 *
 * $Id: BinaryExpression.java,v 1.12 2015/06/18 13:18:09 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A BinaryExpression is a construction based on a binary operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.12 $
 */

public class BinaryExpression extends Expression {

    public BinaryExpression() {
        super();
    }

    private BinaryExpression(BinaryExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        // a binary expression can have multiple instances of a binary
        // operator, e.g. <code>a + b + c</code> is parsed into a single
        // BinaryExpression which owns three values and two instances of
        // AddOperator.
        int len = getNumChildren();
        ValueSource val = (ValueSource) getChild(0);
        for (int i = 1; i < len - 1; i += 2) {
            BinaryOperator op = (BinaryOperator) getChild(i);
            ValueSource nextVal = (ValueSource) getChild(i + 1);
            val = op.operate(val, nextVal, context, getOwner());
        }
        return val;

//        Value val = getChildValue(context, 0);
//        for (int i = 1; i < len - 1; i += 2) {
//            BinaryOperator op = (BinaryOperator) getChild(i);
//            Value nextVal = new DeferredValue((ValueSource) getChild(i + 1), context);
//            val = op.operate(val, nextVal);
//        }
//        return val;
    }

    public Type getType(Context context, Definition resolver) {
        int len = getNumChildren();
        Type type = getChildType(context, resolver, 0);
        for (int i = 1; i < len - 1; i += 2) {
            BinaryOperator op = (BinaryOperator) getChild(i);
            type = op.getResultType(type, getChildType(context, resolver, i + 1), context);
        }
        return type;
    }

    public Expression resolveExpression(Context context) {
        BinaryExpression resolvedExpression = new BinaryExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}