/* Bento
 *
 * $Id: ValueMap.java,v 1.3 2011/09/06 13:51:41 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.Map;


/**
 * Interface for objects which hold named values, such as a record in a
 * database.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public interface ValueMap extends Map<String, Object>, ValueSource {

    public String getString(String key);

    public boolean getBoolean(String key);

    public byte getByte(String key);

    public char getChar(String key);

    public int getInt(String key);

    public long getLong(String key);

    public double getDouble(String key);

    public Object getValue(String key);

    public Class<?> getValueClass(String key);
}
