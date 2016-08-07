/* Bento
 *
 * $Id: UnaryExpression.java,v 1.9 2015/06/18 13:18:09 sthippo Exp $
 *
 * Copyright (c) 2002-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A unary expression is a construction based on a unary operator and a single
 * operand.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class UnaryExpression extends Expression {

    public UnaryExpression() {
        super();
    }

    public UnaryExpression(UnaryExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        UnaryOperator op = (UnaryOperator) getChild(0);
        Value operand = getChildValue(context, 1);
        return op.operate(operand);
    }

    public Type getType(Context context, boolean generate) {
        UnaryOperator op = (UnaryOperator) getChild(0);
        return op.getResultType(getChildType(context, generate, 1));
    }

    public Expression resolveExpression(Context context) {
        UnaryExpression resolvedExpression = new UnaryExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}