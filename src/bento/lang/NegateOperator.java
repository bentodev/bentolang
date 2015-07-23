/* Bento
 *
 * $Id: NegateOperator.java,v 1.5 2006/02/09 17:33:56 sthippo Exp $
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
public class NegateOperator extends UnaryOperator {

    public Value operate(Value val) {
        Value result = null;
        int order = getTypeOrder(val.getValueClass());
        switch (order) {
            case Value.BOOLEAN:
                result = new PrimitiveValue(!val.getBoolean());
                break;
            case Value.BYTE:
                byte b = (byte) ~val.getByte();
                result = new PrimitiveValue(b);
                break;
            case Value.CHAR:
                char c = (char) ~val.getChar();
                result = new PrimitiveValue(c);
                break;
            case Value.INT:
                result = new PrimitiveValue(-val.getInt());
                break;
            case Value.LONG:
                result = new PrimitiveValue(-val.getLong());
                break;
            case Value.DOUBLE:
                result = new PrimitiveValue(-val.getDouble());
                break;
            default:
                throw new UnsupportedOperationException("negate operator only works on numbers");
        }
        return result;
    }

    public String toString() {
        return "-";
    }
}
