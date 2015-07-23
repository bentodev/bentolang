/* Bento
 *
 * $Id: BooleanOperator.java,v 1.6 2008/08/15 21:28:05 sthippo Exp $
 *
 * Copyright (c) 2002-2008 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * Base class for boolean operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

abstract public class BooleanOperator extends BinaryOperator {

    public BooleanOperator() {}

    abstract public boolean operate(boolean bool1, Value val2);

    public Value operate(Value firstVal, Value secondVal) {
        boolean bool = operate(firstVal.getBoolean(), secondVal);
        return new PrimitiveValue(bool);
    }

    /** Always returns boolean type */
    public Type getResultType(Type firstType, Type secondType, Context context) {
        return PrimitiveType.BOOLEAN;
    }
}
