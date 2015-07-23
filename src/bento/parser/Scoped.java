/* Bento
 *
 * $Id: Scoped.java,v 1.6 2014/03/24 02:16:02 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.parser;

import bento.lang.*;

/**
 * Interface for parsed nodes which can have a scoper modifier (public, private
 * or protected).
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public interface Scoped extends Initializable {
    public void setModifiers(int access, int dur);
    public NameNode getDefName();
}
