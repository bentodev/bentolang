/* Bento
 *
 * $Id: BentoTable.java,v 1.3 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * Interface for bento tables.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public interface BentoTable {

    /** Returns an element in the table.  The return value may be a Chunk,
     *  a Value, another array or null.  Throws a NoSuchElementException if the
     *  table does not contain an entry for the specified key, nor a default
     *  entry.
     */
    public Object get(Object key);

    public int getSize();

    public boolean isGrowable();

    public void put(Object key, Object element);
}
