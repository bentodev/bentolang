/* Bento
 *
 * $Id: RegExp.java,v 1.3 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * Base class for regexp nodes.  A regexp node holds a pattern matching string.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */
abstract public class RegExp extends NameNode {

    public RegExp(String regexp) {
        super(regexp);
    }

    abstract public boolean matches(String str);
}
