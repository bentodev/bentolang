/* Bento
 *
 * $Id: Ellipsis.java,v 1.3 2005/06/30 04:20:52 sthippo Exp $
 *
 * Copyright (c) 2003-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * An ellipsis in an array definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class Ellipsis extends SubStatement {

    public Ellipsis() {
        super();
    }

    public String toString(String prefix) {
        return "...";
    }
}
