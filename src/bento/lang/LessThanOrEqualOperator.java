/* Bento
 *
 * $Id: LessThanOrEqualOperator.java,v 1.7 2015/06/01 12:58:56 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * Less than or equal to operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */
public class LessThanOrEqualOperator extends RelationalOperator {

    public Value collectionOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'<=' operator not defined for collections");
    }

    public boolean operate(boolean op1, boolean op2) {
        return op2;
    }
    public boolean operate(byte op1, byte op2) {
        return op1 <= op2;
    }
    public boolean operate(char op1, char op2) {
        return op1 <= op2;
    }
    public boolean operate(int op1, int op2) {
        return op1 <= op2;
    }
    public boolean operate(long op1, long op2) {
        return op1 <= op2;
    }
    public boolean operate(double op1, double op2) {
        return op1 <= op2;
    }
    public boolean operate(String op1, String op2) {
        if (ignoreCase) {
            return op1.compareToIgnoreCase(op2) <= 0;
        } else {
            return op1.compareTo(op2) <= 0;
        }
    }

    public String toString() {
        if (ignoreCase) {
            return " ~<= ";
        } else {
            return " <= ";
        }
    }
}
