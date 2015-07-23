/* Bento
 *
 * $Id: DivideByOperator.java,v 1.7 2006/02/09 17:33:56 sthippo Exp $
 *
 * Copyright (c) 2002-2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * Division operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */
public class DivideByOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        throw new UnsupportedOperationException("'/' operator not defined for boolean values");
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 / op2);
    }

    public char operate(char op1, char op2) {
        return (char) (op1 / op2);
    }

    public int operate(int op1, int op2) {
        return op1 / op2;
    }

    public long operate(long op1, long op2) {
        return op1 / op2;
    }

    public double operate(double op1, double op2) {
        return op1 / op2;
    }

    public String operate(String op1, String op2) {
        throw new UnsupportedOperationException("'/' operator not defined for strings");
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'/' operator not defined for arrays");
    }

    public String toString() {
        return " / ";
    }
}
