/* Bento
 *
 * $Id: BreakStatement.java,v 1.3 2005/06/30 04:20:54 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * A <code>break</code> statement
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class BreakStatement extends BentoStatement {

    public BreakStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }
}
