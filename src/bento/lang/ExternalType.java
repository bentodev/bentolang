/* Bento
 *
 * $Id: ExternalType.java,v 1.11 2014/12/30 13:54:36 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A Type corresponding to an externally defined class.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.11 $
 */
public class ExternalType extends ComplexType {

    public ExternalType(ExternalDefinition def) {
        super(def, def.getExternalTypeName(), def.getDims(), def.getArguments());
    }

    public Class<?> getTypeClass(Context context) {
        return ((ExternalDefinition) getDefinition()).getExternalClass(context);
    }
}


