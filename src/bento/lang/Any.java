/* Bento
 *
 * $Id: Any.java,v 1.8 2015/04/01 13:11:27 sthippo Exp $
 *
 * Copyright (c) 2002-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * The regular expression which matches anything not containing a
 * dot ("*").
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */
public class Any extends RegExp implements Construction {
    public Any() {
        super("*");
    }

    public boolean matches(String str) {
        if (str != null && str.indexOf('.') < 0) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "*";
    }

    public String getDefinitionName() {
        return null;
    }

    public Type getType(Context context, boolean generate) {
        return null;
    }

    public boolean getBoolean(Context context) throws Redirection {
        return false;
    }

    public Object getData(Context context) throws Redirection {
        return null;
    }

    public Object getData(Context context, Definition def) throws Redirection {
        return null;
    }

    public String getText(Context context) throws Redirection {
        return null;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

	public String getString(Context context) throws Redirection {
		return null;
	}

	public byte getByte(Context context) throws Redirection {
		return 0;
	}

	public char getChar(Context context) throws Redirection {
		return 0;
	}

	public int getInt(Context context) throws Redirection {
		return 0;
	}

	public long getLong(Context context) throws Redirection {
		return 0;
	}

	public double getDouble(Context context) throws Redirection {
		return 0;
	}

	public Value getValue(Context context) throws Redirection {
		return NullValue.NULL_VALUE;
	}
}
