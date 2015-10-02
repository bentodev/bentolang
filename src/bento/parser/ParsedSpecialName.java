/* Bento
 *
 * $Id: ParsedSpecialName.java,v 1.5 2007/09/25 22:36:56 sthippo Exp $
 *
 * Copyright (c) 2003-2015 by bentodev.org
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
 * @version $Revision: 1.5 $
 */

public class ParsedSpecialName extends NameNode {
    public ParsedSpecialName(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    public boolean isSpecial() {
    	return true;
    }
}
