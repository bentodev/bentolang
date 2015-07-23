/* Bento
 *
 * $Id: ParsedFloatingPointLiteral.java,v 1.4 2005/06/30 04:20:55 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.parser;

import bento.lang.*;

/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class ParsedFloatingPointLiteral extends PrimitiveValue {
    public ParsedFloatingPointLiteral(int id) {
        super(null, Double.class);
    }

    public void setValue(String val) {
        char lastChar = val.charAt(val.length() - 1);
        if (lastChar == 'f' || lastChar == 'F') {
            try {
                setValueAndClass(Float.valueOf(val), Float.TYPE);
            } catch (NumberFormatException nfe) {
                setValueAndClass(new Float(Float.NaN), Float.TYPE);
            }
        } else {
            try {
                setValueAndClass(Double.valueOf(val), Double.TYPE);
            } catch (NumberFormatException nfe) {
                setValueAndClass(new Double(Double.NaN), Double.TYPE);
            }
        }
    }


    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
