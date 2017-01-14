/* Bento
 *
 * $Id: InOperator.java,v 1.1 2009/04/06 13:16:46 sthippo Exp $
 *
 * Copyright (c) 2009-2017 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.Iterator;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Value operate(Value firstVal, Value secondVal) {
        
        Object memberObj = firstVal.getValue();
        Object collectionObj = secondVal.getValue();
        boolean isIn = false;

        
        if (collectionObj instanceof Map) {
            isIn = ((Map) collectionObj).containsValue(memberObj);
        } else {
            List<?> list = null;
            Class clazz = collectionObj.getClass();
            if (clazz.isArray()) {
                list = Arrays.asList(collectionObj);
            } else if (collectionObj instanceof List) {
                list = ((List<?>) collectionObj);
            }
            
            if (list != null) {
                Iterator<Object> it = (Iterator<Object>) list.iterator();
                while (it.hasNext() && !isIn) {
                    Object element = it.next();
                    if (element.equals(memberObj)) {
                        isIn = true;
                    } else if (element instanceof Value && memberObj.equals(((Value) element).getValue())) {
                        isIn = true;    
                    }
                }
            }
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
