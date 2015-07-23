/* Bento
 *
 * $Id: DynamicObject.java,v 1.10 2012/04/25 13:10:07 sthippo Exp $
 *
 * Copyright (c) 2002-2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.List;

import bento.runtime.Context;

/**
 * Interface for wrapped objects (e.g. arrays) whose value must be recomputed
 * when the context changes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public interface DynamicObject {

    /** Returns a copy of the object initialized for the specified context, given the
     *  specified arguments (if the object is something that takes arguments, such as a
     *  definition instance).   If the object is already initialized or needs no
     *  initialization, this method returns the original object.
     */
    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection;

    /** Returns true if this object is already initialized for the specified context,
     *  i.e., if <code>initForContext(context, args) == this</code> is true.
     */
    public boolean isInitialized(Context context);
}
