/* Bento
 *
 * $Id: InOperator.java,v 1.1 2009/04/06 13:16:46 sthippo Exp $
 *
 * Copyright (c) 2009 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import bento.runtime.Context;

/**
 * In operator.  Returns true if the first operand is a member of the
 * second operand.  If the second operand is not a collection, returns
 * false.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */
public class InOperator extends BinaryOperator {

    public Value operate(Value firstVal, Value secondVal) {
        
        Object memberObj = firstVal.getValue();
        Object collectionObj = secondVal.getValue();
        boolean isIn = false;
        
        Class clazz = collectionObj.getClass();
        if (clazz.isArray()) {
            List list = Arrays.asList(collectionObj);
            isIn = list.contains(memberObj);

        } else if (collectionObj instanceof List) {
            isIn = ((List) collectionObj).contains(memberObj);
                
        } else if (collectionObj instanceof Map) {
            isIn = ((Map) collectionObj).containsValue(memberObj);
        }        

        return new PrimitiveValue(isIn);
    }

    /** Always returns boolean type */
    public Type getResultType(Type firstType, Type secondType, Context context) {
        return PrimitiveType.BOOLEAN;
    }

    public String toString() {
        return " in ";
    }
}
