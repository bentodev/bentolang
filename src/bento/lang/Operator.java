/* Bento
 *
 * $Id: Operator.java,v 1.5 2011/08/12 21:17:59 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.List;

/**
 * Operator interface.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public interface Operator {

    /** Operates on one or more operands and return the result.
     */
    public Value operate(List<Value> operands);

}
