/* Bento
 *
 * $Id: ParsedAnonymousTable.java,v 1.8 2014/05/29 13:03:25 sthippo Exp $
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
 * @version $Revision: 1.8 $
 */
public class ParsedAnonymousTable extends CollectionDefinition implements Initializable {

    public ParsedAnonymousTable(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        int len = (children == null ? 0 : children.length);
        // TableDefinition expects the contents to be in an ArgumentList; this 
        // particular ArgumentList constructor accepts any kind of node but
        // if it isn't a Construction, it gets turned into one.
        ArgumentList contents = (len > 0 ? new ArgumentList(children) : null);
        NameNode name = new NameNode(Name.ANONYMOUS);
        Dim dim = new Dim(Dim.TYPE.DEFINITE, len);
        dim.setTable(true);
        List<Dim> dims = new SingleItemList<Dim>(dim);
        setDims(dims);
        init(null, name, contents);
    }
}