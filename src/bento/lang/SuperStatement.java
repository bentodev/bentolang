/* Bento
 *
 * $Id: SuperStatement.java,v 1.4 2012/02/06 05:00:32 sthippo Exp $
 *
 * Copyright (c) 2004-2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A super statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class SuperStatement extends AbstractConstruction {

    public SuperStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "super;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
