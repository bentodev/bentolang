/* Bento
 *
 * $Id: ValueGenerator.java,v 1.8 2015/04/01 13:11:27 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * Interface for objects which can generate Values.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */

public interface ValueGenerator extends ValueSource {

    public String getString(Context context) throws Redirection;

    public boolean getBoolean(Context context) throws Redirection;

    public byte getByte(Context context) throws Redirection;

    public char getChar(Context context) throws Redirection;

    public int getInt(Context context) throws Redirection;

    public long getLong(Context context) throws Redirection;

    public double getDouble(Context context) throws Redirection;

    /** Get the data directly.  This is equivalent to <code>getValue(context).getValue()</code>,
     *  and is included in the ValueGenerator interface in order to avoid wrapping the data in
     *  a Value object when not necessary.
     */
    public Object getData(Context context) throws Redirection;
}
