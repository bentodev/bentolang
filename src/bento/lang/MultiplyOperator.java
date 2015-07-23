/* Bento
 *
 * $Id: MultiplyOperator.java,v 1.7 2006/02/09 17:33:56 sthippo Exp $
 *
 * Copyright (c) 2002-2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * Addition operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public class MultiplyOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        return op1 && op2;   // interpret '*' as logical and
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 * op2);
    }

    public char operate(char op1, char op2) {
        return (char)(op1 * op2);
    }

    public int operate(int op1, int op2) {
        return op1 * op2;
    }

    public long operate(long op1, long op2) {
        return op1 * op2;
    }

    public double operate(double op1, double op2) {
        return op1 * op2;
    }

    /** The * operator on Strings in bento concatenates like the + operator, except
     *  that it adds a space if the first string ends with a non-whitespace character
     *  and the second string starts with a non-whitespace character.
     */
    public String operate(String op1, String op2) {
        int len1 = op1.length();

        if (op1.length() > 0 && op2.length() > 0) {
            if (!Character.isWhitespace(op1.charAt(len1 - 1)) && !Character.isWhitespace(op2.charAt(0))) {
                return op1 + ' ' + op2;
            }
        }
        return op1 + op2;
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'*' operator not defined for arrays");
    }

    public String toString() {
        return " * ";
    }
}
