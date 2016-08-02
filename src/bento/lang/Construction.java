/* Bento
 *
 * $Id: Construction.java,v 1.9 2013/07/26 13:16:39 sthippo Exp $
 *
 * Copyright (c) 2002-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A construction is a Bento statement which generates data.  Exactly
 * when the data is generated varies, depending on whether a construction
 * is dynamic, static, or contextual.  Dynamic constructions are
 * evaluated every time they are executed.  Static constructions are only 
 * evaluated once.  A contextual construction is re-evaluated only if the
 * context has changed.
 *
 * Regardless of the variety, data is generated lazily, i.e. at runtime
 * rather than compile time.  
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public interface Construction extends Chunk {

    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, Definition resolver);

    /** Returns the name of the definition being constructed */
    public String getDefinitionName();

    /** Gets the data for this construction, using the passed definition if
     *  provided.  This data may be either generated or retrieved from a cache, 
     *  depending on the context and the durability property (static, dynamic,
     *  contextual) of this construction.
     *  
     *  The definition parameter may be null, in which case the construction 
     *  object determines the definition based on the object's name and type
     *  as well as the context.
     */
    public Object getData(Context context, Definition def) throws Redirection;
    
    public Construction getUltimateConstruction(Context context);

    public boolean isPrimitive();

}
