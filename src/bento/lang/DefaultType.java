/* Bento
 *
 * $Id: DefaultType.java,v 1.10 2014/03/24 14:53:13 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.List;

/**
 * The default object type.  There is a single global instance of this class, named
 * <code>DefaultType.TYPE</code>.  The constructor is private to enforce the singleton pattern.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class DefaultType extends AbstractType {

    public final static Type TYPE = new DefaultType();

    private DefaultType() {
        super();
        setName("");
    }

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    private static List<Dim> empty_dims = new EmptyList<Dim>();
    public List<Dim> getDims() {
        return empty_dims;
    }

    public ArgumentList getArguments(Context context) {
        return null;
    }

    /** Returns the passed value unchanged. */
    public Value convert(Value val) {
        return val;
    }

    /** Returns <code>Object.class</code>.
     */
    public Class<?> getTypeClass(Context context) {
        return Object.class;
    }
}
