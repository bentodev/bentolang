/* Bento
 *
 * $Id: ExternalTableBuilder.java,v 1.1 2013/05/02 16:47:35 sthippo Exp $
 *
 * Copyright (c) 2002-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Facade class to make a Java object available as a Bento definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */
public class ExternalTableBuilder extends TableBuilder {

    private ExternalDefinition externalDef = null;

    public ExternalTableBuilder(ExternalCollectionDefinition collectionDef, ExternalDefinition externalDef) {
        super(collectionDef);
        this.externalDef = externalDef; 
    }

}


