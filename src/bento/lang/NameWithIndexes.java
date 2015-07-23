/* Bento
 *
 * $Id: NameWithIndexes.java,v 1.6 2014/11/01 19:49:43 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.Iterator;
import java.util.List;

/**
 * A NameWithIndexes is an identifier, possibly with arguments, and a list 
 * of Index objects
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public class NameWithIndexes extends NameWithArgs {

    private List<Index> indexes;

    public NameWithIndexes() {
        super();
    }

    public NameWithIndexes(String name, List<Index> indexes) {
        super(name, null);
        this.indexes = indexes;
    }

    public NameWithIndexes(String name, ArgumentList args, List<Index> indexes) {
        super(name, args);
        this.indexes = indexes;
    }

    public boolean isPrimitive() {
        return false;
    }

    /** Always returns true. */
    public boolean hasIndexes() {
        return true;
    }

    /** Returns the list of indexes associated with this name. */
    public List<Index> getIndexes() {
        return indexes;
    }

    protected void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }
    
    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(super.toString(prefix));
        Iterator<Index> it = getIndexes().iterator();
        while (it.hasNext()) {
            AbstractNode node = (AbstractNode) it.next();
            sb.append(node.toString());
        }
        return sb.toString();
    }
    
}
