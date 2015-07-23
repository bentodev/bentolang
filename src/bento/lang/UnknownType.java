/* Bento
 *
 * $Id: UnknownType.java,v 1.7 2011/08/12 21:17:59 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.List;

/**
 * The unknown type.  There is a single global instance of this class, named
 * <code>UnknownType.TYPE</code>.  The constructor is private to enforce the
 * singleton pattern.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public class UnknownType extends AbstractType {

    public final static Type TYPE = new UnknownType();

    private UnknownType() {
        super();
    }

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns an empty string. */
    public String getName() {
        return "";
    }

    public List<Dim> getDims() {
        return new EmptyList<Dim>();
    }

    public ArgumentList getArguments(Context context) {
        return null;
    }

    public Class<?> getTypeClass(Context context) {
        return Object.class;
    }


    /** Returns the passed value unchanged. */
    public Value convert(Value val) {
        return val;
    }
}
