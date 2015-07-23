/* Bento
 *
 * $Id: SingleItemIterator.java,v 1.2 2011/08/29 19:29:58 sthippo Exp $
 *
 * Copyright (c) 2007-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.*;


/**
 * A SingleItemIterator is an iterator which only returns one item.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.2 $
 */

public class SingleItemIterator<E> implements Iterator<E> {
    boolean done = false;
    E item;
        
    public SingleItemIterator(E item) {
        this.item = item;
    }
   
    public boolean hasNext() {
        return !done;
    }
    
    public E next() {
        done = true;
        return item;
    }
   
    public void remove() {
        throw new UnsupportedOperationException("SingleItemIterator does not support the remove method.");
    }
}
