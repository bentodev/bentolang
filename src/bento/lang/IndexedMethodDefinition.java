/* Bento
 *
 * $Id: IndexedMethodDefinition.java,v 1.3 2013/10/01 13:08:55 sthippo Exp $
 *
 * Copyright (c) 2012-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.*;

import java.lang.reflect.*;
import java.util.*;

/**
* External definition referencing a method returning a collection.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.3 $
*/

public class IndexedMethodDefinition extends MethodDefinition {
    private List<Index> indexes;

    public IndexedMethodDefinition(ExternalDefinition owner, Method method, ArgumentList args, List<Index> indexes ) {
        super(owner, method, args);
        this.indexes = indexes;
    }

    /** Return the class of the object in the map or array pointed to by this definition */
    public Class<?> getInstanceClass(Context context) {
        try {
            ExternalConstruction construction = createConstruction().initExternalObject(context, null);
            Object data = construction.generateData(context, this);
            if (data != null && !data.equals(NullValue.NULL_VALUE)) {
                return data.getClass();
            }
        } catch (Redirection r) {
            ;
        }
        return null;
    }


    protected ExternalConstruction createConstruction() {
        return new IndexedMethodConstruction(this);
    }

    List<Index> getIndexes() {
        return indexes;
    }
}

class IndexedMethodConstruction extends MethodConstruction {
    public IndexedMethodConstruction(IndexedMethodDefinition def) {
        super(def);
    }
   
    protected CacheabilityInfo getCacheability(Context context, Definition def) {
        return NOT_CACHEABLE_INFO;
    }
    
    public Object getCollectionObject(Context context) throws Redirection {
        try {
            Object data = super.generateData(context, getExternalDefinition());
            if (data instanceof Holder) {
                data = ((Holder) data).data;
            }
            return data;
        } catch (Exception e) {
            log("Exception retrieving collection object for indexed value: " + e);
            return null;
        }
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        try {
            Object data = super.generateData(context, def);
            List<Index> indexes = ((IndexedMethodDefinition) getMethodDefinition()).getIndexes();
            Iterator<Index> it = indexes.iterator();
            while (it.hasNext()) {
                Object collection = data;
                Index index = it.next();
                Value value = index.getIndexValue(context);
                if (collection instanceof Map<?,?>) {
                    data = ((Map<?,?>) collection).get(value.getString());
                } else {
                    // must be an array
                    data = java.lang.reflect.Array.get(collection, value.getInt());
                }
            }
            if (data instanceof Holder) {
                data = ((Holder) data).data;
            }
            return data;
        } catch (Exception e) {
            log("Exception retrieving indexed value: " + e);
            return null;
        }
    }
}


