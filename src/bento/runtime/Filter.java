/* Bento
 *
 * $Id: Filter.java,v 1.3 2005/06/30 04:20:56 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;


/**
 * A Filter distinguishes among paths.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public interface Filter {

    /** Returns true if the path is distinguished by this filter. */
    public boolean filter(String path);
}
