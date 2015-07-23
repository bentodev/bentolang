/* Bento
 *
 * $Id: LogicalNotOperator.java,v 1.5 2006/02/09 17:33:56 sthippo Exp $
 *
 * Copyright (c) 2002-2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * Logical Not operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class LogicalNotOperator extends UnaryOperator {

    public Value operate(Value val) {
        boolean result = !val.getBoolean();
        return new PrimitiveValue(result);
    }

    /** Always returns boolean type */
    public Type getResultType(Type type) {
        return PrimitiveType.BOOLEAN;
    }

    public String toString() {
        return "!";
    }
}
