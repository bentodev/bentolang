/* Bento
 *
 * $Id: SingleItemList.java,v 1.4 2011/08/12 21:17:59 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.*;


/**
 * A SingleItemList is a List which only contains one entry.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class SingleItemList<E> extends AbstractList<E> {

    private E item;

    public SingleItemList(E item) {
        this.item = item;
    }

    public int size() {
        return 1;
    }

    public E get(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("only valid index for this list is zero");
        }
        return item;
    }

    public E set(int index, E element) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("only valid index for this list is zero");
        }
        E oldElement = item;
        item = element;
        return oldElement;
    }
}
