/* Bento
 *
 * $Id: SubtractOperator.java,v 1.6 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * Subtraction operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public class SubtractOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        return !op2;
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 - op2);
    }

    public char operate(char op1, char op2) {
        return (char)( op1 - op2);
    }

    public int operate(int op1, int op2) {
        return op1 - op2;
    }

    public long operate(long op1, long op2) {
        return op1 - op2;
    }

    public double operate(double op1, double op2) {
        return op1 - op2;
    }

    /** String subtraction: remove all occurrences of op2 in op1 */
    public String operate(String op1, String op2) {
        String str = op1;
        int len = op2.length();
        while (true) {
            int ix = str.indexOf(op2);
            if (ix < 0) {
                break;
            } else if (ix == 0) {
                str = str.substring(len);
            } else {
                str = str.substring(0, ix) + str.substring(ix + len);
            }
        }
        return str;
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'-' operator not defined for arrays");
    }

    public String toString() {
        return " - ";
    }
}
