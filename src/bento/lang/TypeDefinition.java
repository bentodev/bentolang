/* Bento
 *
 * $Id: TypeDefinition.java,v 1.10 2015/01/05 03:57:05 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * TypeDefinition is a definition corresponding to the Bento <code>type</code> keyword.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class TypeDefinition extends NamedDefinition {

    public Definition def;
    public String contextName;

    public TypeDefinition(Definition def) {
        super();
        this.def = def;
        setContents(new PrimitiveValue(def.getName()));
    }

    /** Returns <code>false</code>.
     */
    public boolean isAbstract(Context context) {
        return false;
    }

    /** Returns <code>PUBLIC_ACCESS</code>. */
    public int getAccess() {
        return PUBLIC_ACCESS;
    }

    /** Returns <code>DYNAMIC</code>. */
    public int getDurability() {
        return DYNAMIC;
    }

    /** Returns the default type. */
    public Type getType() {
        return DefaultType.TYPE;
    }

    /** Returns null. */
    public Type getSuper() {
        return null;
    }

    /** Returns false. */
    public boolean isSuperType(String name) {
        return false;
    }

    /** Returns false. */
    public boolean isAnonymous() {
        return false;
    }
    
    /** Returns the encapsulated definition's full name.
     */
    public String getFullName() {
        return def.getFullName();
    }

    /** Returns the encapsulated definition's full name in context.
     */
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context);
    }

    public String getName() {
        if (def instanceof DefParameter) {
            String name = ((DefParameter) def).getReferenceName();
            if ("sub".equals(name)) {
                return contextName;
            } else {
                return name;
            }
        } else {
            return def.getName();
        }
    }

    public NameNode getNameNode() {
    	return def.getNameNode();
    }
    
    /** Returns the encapsulated definition. */
    public Definition getOwner() {
        return def;
    }
}
