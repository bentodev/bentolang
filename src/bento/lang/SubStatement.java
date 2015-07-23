/* Bento
 *
 * $Id: SubStatement.java,v 1.6 2012/02/06 05:00:32 sthippo Exp $
 *
 * Copyright (c) 2002-2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A sub statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public class SubStatement extends AbstractConstruction {

    public SubStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    /** Returns true. **/
    public boolean hasSub() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "sub;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
