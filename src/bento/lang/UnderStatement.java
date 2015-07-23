/* Bento
 *
 * $Id: UnderStatement.java,v 1.2 2012/02/06 05:00:32 sthippo Exp $
 *
 * Copyright (c) 2008-2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * An under statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.2 $
 */

public class UnderStatement extends AbstractConstruction {

    public UnderStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "under;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
