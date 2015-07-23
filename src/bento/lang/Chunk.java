/* Bento
 *
 * $Id: Chunk.java,v 1.9 2015/04/01 13:11:27 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A Chunk is a bento object with a visible manifestation.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public interface Chunk extends ValueGenerator {

    /** Gets the contents of the chunk as a boolean value, given a particular 
     *  context.
     */
    public boolean getBoolean(Context context) throws Redirection;

    /** Gets the contents of the chunk as text, given a particular context.  This
     *  is a short cut for calling toString on the object returned by getData,
     *  for convenience and to eliminate unnecessary conversions.
     */
    public String getText(Context context) throws Redirection;

    /** Gets the data for this chunk.  This data may be either generated or
     *  retrieved from a cache, depending on the context and the durability
     *  property (static, dynamic, contextual) of this chunk.  The data is
     *  returned as an instance of the native class of the chunk (i.e., the
     *  class corresponding to the primitive type of the data).
     */
    public Object getData(Context context) throws Redirection;

    /** Returns true if this chunk is abstract, i.e., if it cannot be 
     *  instantiated because to do so would require instantiating an abstract
     *  definition.
     */
    public boolean isAbstract(Context context);
}
