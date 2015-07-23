/* Bento
 *
 * $Id: ArrayIndex.java,v 1.5 2012/04/26 13:41:17 sthippo Exp $
 *
 * Copyright (c) 2002-2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * An ArrayIndex represents an index to an array.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class ArrayIndex extends Index {

    public ArrayIndex() {
        super();
    }

    public ArrayIndex(Value value) {
        super(value);
    }

    public int getIndex(Context context) {
        return getIndexValue(context).getInt();
    }

    public String toString() {
        return "[" + getChild(0).toString() + "]";
    }

    protected Index createIndex() {
        return new ArrayIndex();
    }
}
