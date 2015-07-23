/* Bento
 *
 * $Id: RedirectStatement.java,v 1.9 2015/04/01 13:11:27 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;


/**
 * A directive to redirect the client to a different page.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class RedirectStatement extends BentoStatement implements Construction {

    private boolean dynamic = false;
    
    public RedirectStatement() {
        super();
    }

    public boolean isDynamic() {
        return dynamic;
    }
    
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Redirection getRedirection(Context context) {
        BentoNode child = getChild(0);
        if (isDynamic()) {
            DynamicBentoBlock block = (DynamicBentoBlock) child;
            child = block.children[0];
            if (child instanceof Instantiation) {
                return new Redirection((Instantiation) child, context);
            } else {
                throw new IllegalArgumentException("Dynamic redirection requires an instantiation");
            }
        } else {
            if (child instanceof Name) {
                return new Redirection(child.getName());
            } else {
                throw new IllegalArgumentException("Non-dynamic redirection requires a name");
            }
        }
        
    }
    
    
    public boolean getBoolean(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public String getText(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public Object getData(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public Object getData(Context context, Definition def) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public boolean isAbstract(Context context) {
        throw new UnsupportedOperationException();
    }
    
    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, Definition resolver) {
        throw new UnsupportedOperationException();
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        throw new UnsupportedOperationException();
    }
    
    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

	public String getString(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public byte getByte(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public char getChar(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public int getInt(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public long getLong(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public double getDouble(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

	public Value getValue(Context context) throws Redirection {
        throw new UnsupportedOperationException();
	}

}
