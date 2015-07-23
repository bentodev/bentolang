/* Bento
 *
 * $Id: RightShiftOperator.java,v 1.5 2006/02/09 17:33:56 sthippo Exp $
 *
 * Copyright (c) 2002-2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * Shift bits right operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class RightShiftOperator extends ShiftOperator {

    public byte operate(byte op1, int op2) {
        return (byte) (op1 >> op2);
    }

    public char operate(char op1, int op2) {
        return (char) (op1 >> op2);
    }

    public int operate(int op1, int op2) {
        return op1 >> op2;
    }

    public long operate(long op1, int op2) {
        return op1 >> op2;
    }

    public String operate(String op1, int op2) {
        // interpret right shift on a string as a truncation operation
        return op1.substring(0, op1.length() - op2);
    }

    public String toString() {
        return " >> ";
    }
}
