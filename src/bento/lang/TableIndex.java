/* Bento
 *
 * $Id: TableIndex.java,v 1.5 2012/04/26 13:41:17 sthippo Exp $
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
public class TableIndex extends Index {

    public TableIndex() {
        super();
    }

    public TableIndex(Value value) {
        super(value);
    }

    public String getKey(Context context) {
        return getIndexValue(context).getString();
    }

    public String toString() {
        String str = getChild(0).toString().trim();
        if (str.endsWith(";")) {
            str = str.substring(0, str.length() - 1);
        }

        return "{" + str + "}";
    }

    protected Index createIndex() {
        return new TableIndex();
    }
}
