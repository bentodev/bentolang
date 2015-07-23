/* Bento
 *
 * $Id: ConstructionGenerator.java,v 1.6 2011/08/13 19:22:14 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.List;

/**
 * A ConstructionGenerator is an object which generates a list of constructions
 * for a given context.  It is used in dynamic arrays; if an element of such an
 * array is a ConstructionGenerator, the elements retrieved from the array are
 * the generated constructions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public interface ConstructionGenerator {

    /** Generates a list of constructions for a context. */
    public List<Construction> generateConstructions(Context context) throws Redirection;
}
