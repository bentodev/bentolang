/* Bento
 *
 * $Id: ContinueStatement.java,v 1.4 2014/11/05 14:17:56 sthippo Exp $
 *
 * Copyright (c) 2003-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * A directive to continue with a construction after another
 * construction completes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class ContinueStatement extends BentoStatement {

    public ContinueStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }
}
