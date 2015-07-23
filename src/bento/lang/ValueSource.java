/* Bento
 *
 * $Id: ValueSource.java,v 1.7 2015/03/19 16:50:59 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;


/**
 * Common base interface for the Value and ValueGenerator interfaces.  This
 * interface specifies one methods, getValue(Context context).  A Value should
 * implement this by returning itself.
 * 
 * The ValueSource interfage also specifies type order.  Most operators with 
 * multiple inputs of different types will return a value with of the same type as 
 * the input with the highest type order.  So, for example, an operation between an
 * <code>int</code> and a <code>byte</code> yields an <code>int</code>, between an 
 * <code>int</code> and a <code>char</code> yields a <code>char</code>, and between
 * anything and a <code>string</code> yields a <code>string</code>.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public interface ValueSource {

    // type order values

    public final static int VOID = 0;
    public final static int BOOLEAN = 1;
    public final static int BYTE = 2;
    public final static int INT = 3;
    public final static int LONG = 4;
    public final static int DOUBLE = 5;
    public final static int CHAR = 6;
    public final static int STRING = 7;

    public Value getValue(Context context) throws Redirection;

}
