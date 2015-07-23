/* Bento
 *
 * $Id: NameNode.java,v 1.21 2014/05/19 13:15:20 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.List;

/**
 * NameNode is the base class of Nodes which represent names (including type
 * names).  It holds a single identifier token; subclasses represent more
 * complex names.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.21 $
 */
public class NameNode extends AbstractNode implements Name {

    public static NameNode ANY = new NameNode("*");

    private String name;

    /** For optimization, the base class has a place for caching the name; subclasses can put a value here to
     *  avoid recalculating the name on each call.
     */ 
    protected String cachedName = null;
    protected boolean nameCacheable = false;

    public NameNode() {
        super();
    }

    public NameNode(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        if (cachedName != null) {
            return cachedName;
        }
        return stripDelims(name);
    }

    public void setName(String name) {
        this.name = name;
        cachedName = stripDelims(name);
    }
    
    public void appendName(String name) {
        this.name = this.name + name;
    }
    
    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>true</code> if the name has dynamic arguments in any
     *  part of the name, else <code>false</code>.
     */
    public boolean isDynamic() {
        return (hasArguments() && getArguments().isDynamic());
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Returns <code>true</code> if this is a complex name, i.e., if its children
     *  are names.
     */
    public boolean isComplex() {
        return false;
    }

    /** Returns the number of parts in this name.  The base class always returns 1. */
    public int numParts() {
        return 1;
    }
    
    /** Returns the first part of the name.  The base class simply returns the name. */
    public NameNode getFirstPart() {
        return this;
    }
    
    /** Returns the last part of the name.  The base class simply returns the name. */
    public NameNode getLastPart() {
        return this;
    }

    /** Returns the nth part of the name.  The base class returns the name if n is zero,
     *  else throws an IndexOutOfBounds exception.
     */
    public NameNode getPart(int n) {
        if (n == 0) {
            return this;
        } else {
            throw new IndexOutOfBoundsException("name only has 1 part");
        }
    }

    /** Returns <code>true</code> if this is a special name.
     */
    public boolean isSpecial() {
        return isSpecialName(getName());
    }        
    
    public static boolean isSpecialName(String name) {
        if (name == DEFAULT || name == CONTAINER || name == COUNT || name == CORE
        		|| name == KEYS || name == HERE || name == OVER || name == OWNER
        		|| name == SITE || name == SOURCE || name == SUB || name == SUPER
        		|| name == THIS || name == TYPE || name == UNDER) {
            return true;
        } else {
            return false;
        }
    }


    /** Returns <code>true</code> if this name has one or more constraints, else false.  The base
     *  class returns false. */
    public boolean hasConstraints() {
        return false;
    }
    
    /** Returns the list of constraints associated with this name.  The base class
     *  always returns null.
     */
    public List<BentoNode> getConstraints() {
        return null;
    }

    
    /** Returns <code>true</code> if this name has one or more arguments, else false. */
    public boolean hasArguments() {
        return false;
    }

    /** Returns the list of arguments associated with this name.  The base
     *  class always returns null.
     */
    public ArgumentList getArguments() {
        return null;
    }

    /** Returns true if this name has one or more indexes, else false. */
    public boolean hasIndexes() {
        return false;
    }

    /** Returns the list of indexes associated with this name.  The base class
     *  always returns null.
     */
    public List<Index> getIndexes() {
        return null;
    }

    /** Returns the list of dimensions associated with this name.  The base class
     *  always returns null.
     */
    public List<Dim> getDims() {
        return null;
    }

    public String toString(String prefix) {
        return getName();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Name) {
            String thisName = getName();
            String otherName = ((Name) obj).getName();
            if (thisName == null) {
                return (otherName == null);
            } else {
                return thisName.equals(otherName);
            }
        }
        return false;
    }
    
    private static String stripDelims(String str) {
        if (str != null) {
            int len = str.length();
            if (len > 1) {
                char a = str.charAt(0);
                char b = str.charAt(len - 1);
                if (a == b && (a == '"' || a == '\'' || a == '`')) {
                    str = str.substring(1, len - 1);
                }
            }
        }
        return str;
    }
}
