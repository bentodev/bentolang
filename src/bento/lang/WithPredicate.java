/* Bento
 *
 * $Id: WithPredicate.java,v 1.12 2014/11/01 19:49:43 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.*;

/**
 * The predicate of a <code>with</code> expression in a conditional
 * statement.  Tests for the presence of a parameter or concreteness
 * of a definition
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.12 $
 */

public class WithPredicate extends Instantiation implements ValueGenerator {

    boolean with = true;
    boolean elementPresenceCheck = false;

    public WithPredicate(boolean with) {
        super();
        this.with = with;
    }

    protected void setWithout() {
        with = false;
    }
    
    protected void setElementPresenceCheck(boolean check) {
        elementPresenceCheck = check;
    }

    public boolean isPrimitive() {
        return false;
    }

    public String getString(Context context) {
        return getValue(context).getString();
    }

    public boolean getBoolean(Context context) {
        return getValue(context).getBoolean();
    }

    public byte getByte(Context context) {
        return getValue(context).getByte();
    }

    public char getChar(Context context) {
        return getValue(context).getChar();
    }

    public int getInt(Context context) {
        return getValue(context).getInt();
    }

    public long getLong(Context context) {
        return getValue(context).getLong();
    }

    public double getDouble(Context context) {
        return getValue(context).getDouble();
    }

    public Object getData(Context context) {
        return getValue(context).getValue();
    }

    public Value getValue(Context context) {
        boolean isPresent = false;
        
        if (elementPresenceCheck) {
            ElementReference element = (ElementReference) getDefinition(context);
            if (element != null) {
                try {
                    Definition elementDef = element.getElementDefinition(context);
                    if (elementDef != null) {
                        isPresent = true;
                    }
                
                } catch (Redirection r) {
                    // treat a redirection as a confirmation of nonexistence
                    ;
                }
            }
        
        
        } else {
            isPresent = context.paramIsPresent(getReferenceName());
            if (!isPresent) {
                Definition def = getDefinition(context);
                isPresent = (def != null && !def.isAbstract(context));
            }
        }
        return new PrimitiveValue(with ? isPresent : !isPresent);
    }

    /** for source dumping */
    public String toString(String prefix) {
        String str = prefix + "(" + super.toString("") + ")";
        return str;
    }
}