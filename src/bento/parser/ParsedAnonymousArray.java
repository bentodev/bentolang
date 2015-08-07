/* Bento
 *
 * $Id: ParsedAnonymousArray.java,v 1.7 2014/05/29 13:03:25 sthippo Exp $
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
 * @version $Revision: 1.7 $
 */
public class ParsedAnonymousArray extends CollectionDefinition implements Initializable {

    public ParsedAnonymousArray(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        int len = (children != null ? children.length : 0);
        ArgumentList contents = null;

        // ArrayDefinition expects the contents to be in an ArgumentList
        if (len > 0) {
            contents = new ArgumentList(children);
        }
        NameNode name = new NameNode(Name.ANONYMOUS);
        Dim dim = new Dim(Dim.TYPE.DEFINITE, len);
        List<Dim> dims = new SingleItemList<Dim>(dim);
        setDims(dims);
        init(null, name, contents);
    }
}