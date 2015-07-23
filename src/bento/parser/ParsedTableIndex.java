/* Bento
 *
 * $Id: ParsedTableIndex.java,v 1.4 2007/10/08 15:36:58 sthippo Exp $
 *
 * Copyright (c) 2002-2007 by bentodev.org
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
public class ParsedTableIndex extends TableIndex {
    public ParsedTableIndex(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    public void setDynamic() {
        setDynamic(true);
    }
}
