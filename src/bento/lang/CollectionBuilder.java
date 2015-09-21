/* Bento
 *
 * $Id: CollectionBuilder.java,v 1.1 2013/05/02 16:47:35 sthippo Exp $
 *
 * Copyright (c) 2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.*;

/**
 * CollectionBuilder is the common base for classes that construct arrays and tables
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */

abstract public class CollectionBuilder {

    public CollectionBuilder() {
        super();
    }

    
    public static CollectionInstance createCollectionInstanceForDef(Definition def, Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        if (def.is_array()) {
            return new ResolvedArray(def, context, args, indexes, collectionData);
        } else if (def.is_table()) {
            return new ResolvedTable(def, context, args, indexes, collectionData);
        } else {
            return null;
        }
    }

    
    /** Creates a resolved instance of this collection in the specified context with the specified
     *  arguments.
     */
    abstract public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection;

    /** Wraps the passed data in a collection instance in the specified context with the specified
     *  arguments.
     */
    abstract public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection;

}

