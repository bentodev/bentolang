/* Bento
 *
 * $Id: ParsedDefTypeName.java,v 1.4 2014/11/01 19:49:41 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.parser;

import bento.lang.*;

import java.util.*;

/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class ParsedDefTypeName extends NameWithParams implements Initializable {
    public ParsedDefTypeName(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        if (children != null && children.length > 0) {
            ArrayList<ParameterList> list = new ArrayList<ParameterList>(children.length);
            for (int i = 0; i < children.length; i++) {
                list.add((ParameterList) children[i]);
            }
            setParamLists(list);
        }
    }
}
