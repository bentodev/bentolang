/* Bento
 *
 * $Id: ExternalArrayBuilder.java,v 1.3 2015/04/03 13:13:28 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.lang.reflect.*;
import java.util.*;

/**
 * Facade class to make a Java object available as a Bento definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */
public class ExternalArrayBuilder extends ArrayBuilder {
    public ExternalArrayBuilder(ExternalCollectionDefinition collectionDef, ExternalDefinition externalDef) {
        super(collectionDef);
    }
}

abstract class BentoArrayAdapter implements BentoArray {

    private BentoArray array;

    public BentoArrayAdapter() {}

    protected void setArray(BentoArray array) {
        this.array = array;
    }

    public BentoArray getArray() {
        return array;
    }

    public Object getArrayObject() {
        ensureArray();
        return array.getArrayObject();
    }

    public Object instantiateArray(Context context) throws Redirection {
        ensureArray();
        return array.instantiateArray(context);
    }

    public Object get(int n) {
        ensureArray();
        return array.get(n);
    }

    public int getSize() {
        ensureArray();
        return array.getSize();
    }

    public boolean isGrowable() {
        ensureArray();
        return array.isGrowable();
    }

    public boolean add(Object element) {
        ensureArray();
        array.add(element);
        return true;
    }

    public Object set(int n, Object element) {
        ensureArray();
        Object oldElement = array.get(n);
        array.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        ensureArray();
        return array.iterator();
    }

    abstract protected void ensureArray();
}


class MethodArrayDefinition extends BentoArrayAdapter {
    private Object instance;
    private Method method;
    private ArgumentList args;

    public MethodArrayDefinition(Object instance, Method method, ArgumentList args) {
        this.instance = instance;
        this.method = method;
        this.args = args;
    }

    @SuppressWarnings("unchecked")
    protected void ensureArray() {
        BentoArray array = getArray();
        if (array == null) {
            int numArgs = (args != null ? args.size() : 0);
            Object[] argObjects = new Object[numArgs];
            for (int i = 0; i < numArgs; i++) {
                Object arg = args.get(i);
                if (arg instanceof Value) {
                    argObjects[i] = ((Value) arg).getValue();
                } else if (arg instanceof ValueGenerator) {
                    try {
                        argObjects[i] = ((ValueGenerator) arg).getData(null);
                    } catch (Redirection r) {
                        argObjects[i] = new PrimitiveValue();
                    }

                }
            }
            try {
                Object object = method.invoke(instance, argObjects);
                if (object != null) {
                    if (object instanceof List) {
                        setArray(new ExternalGrowableArray((List<Object>) object));
                    } else if (object instanceof BentoArray) {
                    	setArray((BentoArray) object);
                    } else {
                        setArray(new ExternalFixedArray(object));
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception generating data via external method: " + e);
            }
        }
    }
}

class FieldArrayDefinition extends BentoArrayAdapter {
    private Object instance;
    private Field field;

    public FieldArrayDefinition(Object instance, Field field) {
        this.instance = instance;
        this.field = field;
    }

    @SuppressWarnings("unchecked")
    protected void ensureArray() {
        BentoArray array = getArray();
        if (array == null) {
            try {
                Object object = field.get(instance);
                if (object != null) {
                    if (object instanceof List) {
                        setArray(new ExternalGrowableArray((List<Object>) object));
                    } else {
                        setArray(new ExternalFixedArray(object));
                    }
                }

            } catch (Exception e) {
                System.out.println("Exception generating data via external field: " + e);
            }
        }
    }
}

class ExternalFixedArray implements BentoArray {
    private Object array = null;

    public ExternalFixedArray(Object array) {
        this.array = array;
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("object passed to ExternalFixedArray constructor is not an array");
        }
    }

    public Object getArrayObject() {
        return array;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(array, context);
    }

    public Object get(int n) {
        if (array == null) {
            return null;
        } else {
            return Array.get(array, n);
        }
    }

    public int getSize() {
        return Array.getLength(array);
    }

    public boolean isGrowable() {
        return false;
    }

    public boolean add(Object element) {
        return false;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            return null;
        } else {
            Object oldElement = Array.get(array, n);
            Array.set(array, n, element);
            return oldElement;
        }
    }

    public Iterator<Object> iterator() {
        // need to fix this for non-Object[] arrays
        try {
            return Arrays.asList((Object[]) array).iterator();
        } catch (ClassCastException cce) {
            System.out.println("iterator on " + array.getClass().getName() + " array type not yet implemented");
            return (new EmptyList<Object>()).iterator();
        }
    }
}

class ExternalGrowableArray implements BentoArray {
    private List<Object> list;

    public ExternalGrowableArray(List<Object> list) {
        this.list = list;
    }

    public Object getArrayObject() {
        return list.toArray();
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(list.toArray(), context);
    }

    public Object get(int n) {
        if (list == null) {
            return null;
        } else {
            return list.get(n);
        }
    }

    public int getSize() {
        if (list == null) {
            return 0;
        } else {
            return list.size();
        }
    }

    public boolean isGrowable() {
        return true;
    }

    public boolean add(Object element) {
        if (list == null) {
            throw new IllegalStateException("this ExternalGrowableArray points to a null array");
        }
        list.add(element);
        return true;
    }

    public Object set(int n, Object element) {
        if (list == null) {
            throw new IllegalStateException("this ExternalGrowableArray points to a null array");
        }
        Object oldElement = list.get(n);
        list.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        return list.iterator();
    }
}

