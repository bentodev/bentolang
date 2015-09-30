/* Bento
 *
 * $Id: CollectionInstance.java,v 1.9 2012/06/01 12:56:15 sthippo Exp $
 *
 * Copyright (c) 2004-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.Iterator;

/**
 * Interface for resolved collection instances.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public interface CollectionInstance {

    /** Returns the object containing the collection.  The collection is shallowly
     *  instantiated; i.e., the container (table or array) is instantiated but the
     *  individual items in the container are not necessarily instantiated.
     * @throws Redirection 
     */
    public Object getCollectionObject() throws Redirection;

    public int getSize();
    
    public String getName();

    public Iterator<Definition> iterator();
    
    public Iterator<Construction> constructionIterator();

    public Iterator<Index> indexIterator();

    public Definition getDefinition();

    public Definition getElement(Index index, Context context);

    /** For a growable collection, adds an element to the collection.  Throws
     *  an UnsupportedOperationException on a fixed collection.
     */
    //public void add(Object element);

}
