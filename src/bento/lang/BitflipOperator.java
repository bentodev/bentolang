/* Bento
 *
 * $Id: BitflipOperator.java,v 1.6 2015/05/29 12:56:52 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Bitwise Not operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public class BitflipOperator extends UnaryOperator {

    @SuppressWarnings("unchecked")
    public Value operate(Value val) {
        Value result = null;
        Class<?> valueClass = val.getValueClass();
        if (valueClass.isArray() || List.class.isAssignableFrom(valueClass) || BentoArray.class.isAssignableFrom(valueClass)) {
            Object obj = val.getValue();
            if (obj instanceof BentoArray) {
                obj = ((BentoArray) obj).getArrayObject();
            }
            Iterator<Object> it;
            int size;
            if (obj instanceof List) {
                size = ((List<Object>) obj).size();
                it = ((List<Object>) obj).iterator();
            } else {
                size = ((Object[]) obj).length;
                it = Arrays.asList((Object[]) obj).iterator();
            }
            List<Object> resultList = new ArrayList<Object>(size);
            while (it.hasNext()) {
                Object element = it.next();
                Value elementVal = (element instanceof Value ? (Value) element : new PrimitiveValue(element));
                resultList.add(operate(elementVal));
            }
            result = new PrimitiveValue(resultList);
            
        } else {
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
                    result = new PrimitiveValue(~val.getInt());
                    break;
                case Value.LONG:
                    result = new PrimitiveValue(~val.getLong());
                    break;
                default:
                    throw new UnsupportedOperationException("bitflip operator only works on booleans and integral values");
            }
        }
        return result;
    }

    public String toString() {
        return "~";
    }
}
