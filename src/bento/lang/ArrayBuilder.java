/* Bento
 *
 * $Id: ArrayBuilder.java,v 1.7 2015/06/18 20:13:37 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;
import bento.runtime.Logger;

import java.util.*;


/**
 * ArrayBuilder constructs arrays and instantiates their contents.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public class ArrayBuilder extends CollectionBuilder {

    static public Object instantiateElements(Object arrayObject, Context context) throws Redirection {
        Object arrayInstance = arrayObject;
        if (arrayObject instanceof Object[]) {
            int size = ((Object[]) arrayObject).length;
            for (int i = 0; i < size; i++) {
                Object data = ((Object[]) arrayObject)[i];

                if (data instanceof CollectionInstance) {
                     data = ((CollectionInstance) data).getCollectionObject();
                }

                if (data instanceof ElementDefinition) {
                    data = ((ElementDefinition) data).getElement(context);
                }

                if (data instanceof Value) {
                    data = ((Value) data).getValue();

                } else if (data instanceof ValueGenerator) {
                    data = ((ValueGenerator) data).getData(context);
                }

                if (data != ((Object[]) arrayInstance)[i]) {
                    if (arrayInstance == arrayObject) {
                        arrayInstance = new Object[size];
                        System.arraycopy(arrayObject, 0, arrayInstance, 0, size);
                    }
                    ((Object[]) arrayInstance)[i] = data;
                }
            }

        } else if (arrayObject instanceof List<?>) {
            List<Object> list = new ArrayList<Object>(((List<?>) arrayObject).size());
            Iterator<?> it = ((List<?>) arrayObject).iterator();
            while (it.hasNext()) {
                Object data = it.next();

                if (data instanceof CollectionInstance) {
                     data = ((CollectionInstance) data).getCollectionObject();
                }

                if (data instanceof ElementDefinition) {
                    data = ((ElementDefinition) data).getElement(context);
                }

                if (data instanceof Value) {
                    data = ((Value) data).getValue();

                } else if (data instanceof ValueGenerator) {
                    data = ((ValueGenerator) data).getData(context);
                }

                list.add(data);
            }
            arrayInstance = list;
        }
        return arrayInstance;
    }

    protected CollectionDefinition arrayDef = null;

    public ArrayBuilder(CollectionDefinition arrayDef) {
        this.arrayDef = arrayDef;
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        // this could be cached
        return new ResolvedArray(arrayDef, context, args, indexes);
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        // this could be cached
        return new ResolvedArray(arrayDef, context, args, indexes, collectionData);
    }

    public BentoArray getArray(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
    	ResolvedArray array = (ResolvedArray) arrayDef.getCollectionInstance(context, args, indexes);
        return array.getArray();
    }

    /** Generates a list of constructions for a context. */
    public List<Construction> generateConstructions(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        CollectionInstance collection = arrayDef.getCollectionInstance(context, args, indexes);
        Iterator<?> it = collection.iterator();
        int n = collection.getSize();
        List<Construction> constructions = Context.newArrayList(n, Construction.class);
        while (it.hasNext()) {
            Object object = it.next();
            Object element = (object instanceof ElementDefinition ? ((ElementDefinition) object).getElement(context) : object);
            if (element instanceof ConstructionGenerator) {
                constructions.addAll(((ConstructionGenerator) element).generateConstructions(context));
            } else {
                constructions.add((Construction) element);
            }
        }
        return constructions;
    }


}


class ResolvedArray extends ResolvedCollection {

    private CollectionDefinition def = null;
    protected BentoArray array = null;

    public ResolvedArray(CollectionDefinition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, context, args, indexes);

        this.def = def;
        if (def.hasStaticData()) {
            array = (BentoArray) def.getStaticData();
        } else {
            array = createArray(def, context, args);
            // has no effect if def is not static
            def.setStaticData(array);
        }
    }

    public ResolvedArray(CollectionDefinition def, Context context, ArgumentList args, List<Index> indexes, Object arrayData) throws Redirection {
        super(def, context, args, indexes);
        
        this.def = def;

        if (arrayData instanceof Instantiation) {
            arrayData = ((Instantiation) arrayData).getData(context);
            if (arrayData instanceof Value) {
                arrayData = ((Value) arrayData).getValue();
            }
        }
        
    	if (arrayData instanceof BentoArray) {
            array = (BentoArray) arrayData;
        } else if (arrayData instanceof CollectionDefinition) {
            // this doesn't support args in anonymous arrays 
            CollectionDefinition arrayDef = (CollectionDefinition) arrayData;
            if (arrayDef.equals(def)) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Array " + def.getName() + " is circularly defined.");
            }
            array = arrayDef.getArray(context, null, null);
        } else if (arrayData instanceof Object[]) {
            array = new FixedArray((Object[]) arrayData);
        } else if (arrayData instanceof List<?>) {
            array = new GrowableArray((List<?>) arrayData);
        } else if (arrayData instanceof ResolvedArray) {
            array = ((ResolvedArray) arrayData).getArray();
        } else if (arrayData != null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unable to initialize array " + def.getName() + "; data in context of wrong type: " + arrayData.getClass().getName());
        }
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        return array;
    }

    /** Creates an array based on the definition.  If the contents of the definition are
     *  of an unexpected type, a ClassCastException is thrown.
     */
    private static BentoArray createArray(CollectionDefinition def, Context context, ArgumentList args) throws Redirection {
        ParameterList params = def.getParamsForArgs(args, context);
        context.push(def, params, args, false);
        try {
            List<Dim> dims = def.getDims();
            Dim majorDim = (Dim) dims.get(0);
            Dim.TYPE dimType = majorDim.getType();
            boolean fixed = (dimType == Dim.TYPE.DEFINITE);
            Object contents = def.getContents();

            BentoArray array = null;

            // empty array
            if (contents == null) {
                array = allocate(fixed, 0);

            // array defined with an ArrayInitExpression
            } else if (contents instanceof ArgumentList) {
                ArgumentList elements = (ArgumentList) contents;
                int size = 0;
                if (dimType == Dim.TYPE.DEFINITE) {
                    size = majorDim.getSize();
                } else if (elements != null) {
                    size = elements.size();
                }

                // for now just handle one dimension
                array = allocate(fixed, size);

                if (elements != null) {
                    if (fixed) {
                        for (int i = 0; i < elements.size(); i++) {
                            Construction element = elements.get(i); 
                            element = resolveElement(def, element, context);
                            array.set(i, element);
                        }

                    } else {
                        Iterator<Construction> it = elements.iterator();
                        while (it.hasNext()) {
                            Construction element = it.next();
                            element = resolveElement(def, element, context);
                            array.add(element);
                        }
                    }
                }
                if (def.isDynamic()) {
                    array = new DynamicArray(array, context);
                }

            } else if (contents instanceof Value) {
                array = new ArrayInstance(((Value) contents).getValue());

            } else if (contents instanceof ValueGenerator) {
                array = new ArrayInstance((ValueGenerator) contents, context);
            }

            return array;
        } finally {
            context.pop();
        }
    }

    private static Construction resolveElement(CollectionDefinition collectionDef, Construction element, Context context) throws Redirection {
        if (element instanceof Instantiation) {
            return resolveInstance((Instantiation) element, context);

        } else if (element instanceof Expression) {
            return ((Expression) element).resolveExpression(context);
            
        } else if (element instanceof Value) {
            Value value = (Value) element;
            Type ownerType = collectionDef.getElementType();
            if (ownerType != null) {
                Class<?> collectionClass = ownerType.getTypeClass(null);
                Class<?> elementClass = value.getValueClass();
                if (!collectionClass.isAssignableFrom(elementClass)) {
                    return new PrimitiveValue(element, collectionClass);
                }
            }
            return element;
            
        } else {
            return element;
        }
    }
   
    private static BentoArray allocate(boolean fixed, int size) {
        if (fixed) {
            return new FixedArray(size);
        } else {
            return new GrowableArray(size);
        }
    }

    /** Returns the ArrayDefinition defining this collection. */
    public CollectionDefinition getCollectionDefinition() {
        return def;
    }


    /** Creates an iterator for the array.
     */
    public Iterator<Definition> iterator() {
        return new ArrayIterator();
    }

    public Iterator<Construction> constructionIterator() {
        return new ArrayConstructionIterator();
    }

    public Iterator<Index> indexIterator() {
        return new ArrayIndexIterator();
    }

    public class ArrayIterator implements Iterator<Definition> {
        int ix = 0;
        Iterator<Object> it;

        public ArrayIterator() {
        	it = array.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();  //ix < array.getSize();
        }

        public Definition next() {
            Object element = it.next();  // array.get(ix++);
            return getElementDefinition(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in array iterators");
        }
    }

    public class ArrayConstructionIterator implements Iterator<Construction> {
        int ix = 0;
        Iterator<Object> it;

        public ArrayConstructionIterator() {
            it = array.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();  //ix < array.getSize();
        }

        public Construction next() {
            Object element = it.next();  // array.get(ix++);
            return getConstructionForElement(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in array iterators");
        }
    }


    public class ArrayIndexIterator implements Iterator<Index> {
        int ix = 0;

        public ArrayIndexIterator() {}

        public boolean hasNext() {
            return ix < array.getSize();
        }

        public Index next() {
            return new ArrayIndex(new PrimitiveValue(ix++));
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }

    public Object getCollectionObject() throws Redirection {
        Object collectionObj = array.getArrayObject();
//        if (collectionObj instanceof List || collectionObj.getClass().isArray()) {
//            collectionObj = ArrayDefinition.instantiateElements(collectionObj, getResolutionContext());
//        }
        return collectionObj;
    }

    public BentoArray getArray() {
        return array;
    }

    protected void setArray(BentoArray array) {
        this.array = array;
    }

    public Object get(int n) {
        return array.get(n);
    }

    public Definition getElement(Index index, Context context) {
        if (context == null) {
        	context = getResolutionContext();
        }

//        ArgumentList args = getArguments();
//        ParameterList params = def.getParamsForArgs(args, context);
//        context.push(def, params, args);
//        try {

            // If the index is a regular array index, get its value as
            // an int and return a definition for the element at that
            // position in the array.  If the index is a table index,
            // do a search for an element containing the table index's
            // key value.  If found, return the index of the string in
            // the array as an integer value; if not found, return -1 as
            // an integer value.

            if (index instanceof ArrayIndex) {
                int ix = index.getIndexValue(context).getInt();
                Object element = (ix >= 0 && ix < array.getSize() ? array.get(ix) : null);
                return getElementDefinition(element);

            } else if (index instanceof TableIndex) {
                // retrieve the element which matches the index key value.  There
                // are two ways an element can match the key:
                //
                // -- if the element is a definition which owns a child named "key"
                //    compare its instantiated string value to the index key
                //
                // -- if the element doesn't have such a "key" field, compare the
                //    string value of the element itself to the index key.

                String key = index.getIndexValue(context).getString();

                if (key == null) {
                    return null;
                }
                NameNode keyName = new NameNode("key");
                int size = array.getSize();
                for (int i = 0; i < size; i++) {
                    Object element = array.get(i);
                    Object object = null;

                    try {
                        if (element instanceof Definition) {
                            Definition keyDef = ((Definition) element).getChildDefinition(keyName, context);
                            if (keyDef != null) {
                                object = context.construct(keyDef, null);
                                if (object == null) {
                                    // null key, can't match
                                    continue;
                                }
                            }
                        }

                        if (object == null) {
                            object = def.getObjectForElement(element);
                            if (object == null) {
                                continue;
                            }
                        }

                        String elementKey;
                        if (object instanceof String) {
                            elementKey = (String) object;
                        } else if (object instanceof Value) {
                            elementKey = ((Value) object).getString();
                        } else if (object instanceof Chunk) {
                            elementKey = ((Chunk) object).getText(context);
                        } else if (object instanceof ValueGenerator) {
                            elementKey = ((ValueGenerator) object).getString(context);
                        } else {
                            elementKey = object.toString();
                        }

                        if (key.equals(elementKey)) {
                            return getElementDefinition(new Integer(i));
                        }

                    } catch (Redirection r) {
                        // don't redirect, we're only checking
                        continue;
                    }
                }
                return getElementDefinition(new Integer(-1));

            } else {
                throw new IllegalArgumentException("Unknown index type: " + index.getClass().getName());
            }
//        } finally {
//            context.unpush();
//        }
    }

    public int getSize() {
        if (array != null) {
            return array.getSize();
        } else {
            return 0;
        }
    }

    public boolean isGrowable() {
        return array.isGrowable();
    }

    public void add(Object element) {
        array.add(getElementDefinition(element));
    }

    public void set(int n, Object element) {
        array.set(n, getElementDefinition(element));
    }

    public String getText(Context context) throws Redirection {
        StringBuffer sb = new StringBuffer();
        
        sb.append("[ ");

        int len = array.getSize();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Construction construction = getConstructionForElement(array.get(i));
                sb.append('"');
                sb.append(construction.getText(context));
                sb.append('"');
            }
        }
        
        sb.append(" ]");
        
        return sb.toString();
    }

}

class FixedArray implements BentoArray {
    private Object[] array = null;
//    private Object[] arrayObj = null;
    private int size;

    public FixedArray(int size) {
        this.size = size;
    }

    public FixedArray(Object[] array) {
        this.array = array;
        size = array.length;
    }

    public Object getArrayObject() {
        return array;        
        
//        if (arrayObj == null) {
//            arrayObj = new Object[size];
//            for (int i = 0; i < size; i++) {
//                if (array[i] instanceof CollectionInstance) {
//                    arrayObj[i] = ((CollectionInstance) array[i]).getCollectionObject();
//                } else {
//                    arrayObj[i] = array[i];
//                }
//            }
//        }
//        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(array, context);
    }

    public Object get(int n) {
        if (array == null) {
            return null;
        } else {
            return array[n];
        }
    }

    public int getSize() {
        return size;
    }

    public boolean isGrowable() {
        return false;
    }

    public boolean add(Object element) {
        return false;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            array = new Object[size];
        }
        Object oldElement = array[n];
        array[n] = element;
        return oldElement;
    }

    public Iterator<Object> iterator() {
        return Arrays.asList(array).iterator();
    }
}

class GrowableArray implements BentoArray {
    private List<Object> array;
//    private List<Object> arrayObj = null;
    private int initialSize;
    private final static Object[] EMPTY_ARRAY = new Object[0];

    public GrowableArray(int size) {
        initialSize = size;
    }

    @SuppressWarnings("unchecked")
    public GrowableArray(List<?> array) {
        this.array = (List<Object>) array;
        initialSize = array.size();
    }

    private void init() {
        array = Context.newArrayList(initialSize, Object.class);
    }

    public Object getArrayObject() {
        if (array == null) {
            return EMPTY_ARRAY;
        } else {
            return array;
        }
//        if (arrayObj == null && array != null) {
//            arrayObj = new ArrayList(array);
//            int len = arrayObj.size();
//            for (int i = 0; i < len; i++) {
//                Object item = arrayObj.get(i);
//                if (item instanceof CollectionInstance) {
//                    arrayObj.set(i, ((CollectionInstance) item).getCollectionObject());
//                }
//            }
//        }
//        Object[] arrayObj = (array == null ? EMPTY_ARRAY : array.toArray());
//        for (int i = 0; i < arrayObj.length; i++) {
//            if (arrayObj[i] instanceof CollectionInstance) {
//                arrayObj[i] = ((CollectionInstance) arrayObj[i]).getCollectionObject();
//            }
//        }
//        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(array, context);
    }

    public Object get(int n) {
        if (array == null) {
            return null;
        } else {
            return array.get(n);
        }
    }

    public int getSize() {
        if (array == null) {
            return initialSize;
        } else {
            return array.size();
        }
    }

    public boolean isGrowable() {
        return true;
    }

    public boolean add(Object element) {
        if (array == null) {
            init();
        }
        array.add(element);
        return true;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            init();
        }
        Object oldElement = array.get(n);
        array.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
    	if (array == null) {
    		init();
    	}
        return array.iterator();
    }
}

class ArrayInstance implements BentoArray, DynamicObject {
    private ValueGenerator valueGen;
    private BentoArray array = null;
    private Object data = null;
    private Context initContext = null;

    public ArrayInstance(Object data) {
        this.data = data;
    }

    public ArrayInstance(ValueGenerator valueGen, Context context) throws Redirection {
        this.array = null;
        this.valueGen = valueGen;
        List<Index> indexes = null;
        initContext = (Context) context.clone();
        if (valueGen instanceof Instantiation) {
            Instantiation instance = (Instantiation) valueGen;
            Instantiation ultimateInstance = instance.getUltimateInstance(context);
            indexes = (instance == ultimateInstance ? null : instance.getIndexes());
            Definition def = ultimateInstance.getDefinition(context);
            if (def == null) {
                data = null;
            } else {
                // commented out to make cached_array_test work
                //ArgumentList args = ultimateInstance.getArguments();
                //ParameterList params = def.getParamsForArgs(args, context);
                //context.push(def, params, args, false);
                try {
                    data = ultimateInstance.generateData(context, def);
                } finally {
                //    context.pop();
                }
            }
        } else if (valueGen instanceof IndexedMethodConstruction) {
            data = ((IndexedMethodConstruction) valueGen).getCollectionObject(context);
            
        } else {
            data = valueGen.getData(context);
        }
        if (data != null) {
            if (data instanceof Value) {
                data = ((Value) data).getValue();
            }
            if (data instanceof DynamicObject) {
                data = ((DynamicObject) data).initForContext(context, null, null);
            }
            if (indexes != null) {
                data = context.dereference(data, indexes);
            }
        }
    }


    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (initContext == null && data == null) {
            return new ArrayInstance(valueGen, context);
        } else {
            return this;
        }
    }

    public boolean isInitialized(Context context) {
        return (initContext != null && initContext.equals(context));
    }

    private void init_array() {
        if (data == null) {
            Logger.vlog("data for array instance is null; initializing to empty array");
            array = new FixedArray(new Object[0]);
        } else if (data instanceof BentoArray) {
            array = (BentoArray) data;
        } else if (data instanceof Object[]) {
            array = new FixedArray((Object[]) data);
        } else if (data instanceof List<?>) {
            array = new GrowableArray((List<?>) data);
        } else if (data instanceof ResolvedArray) {
            array = ((ResolvedArray) data).getArray();
        } else {
            throw new UninitializedObjectException("Unable to initialize array, data type not supported: " + data.getClass().getName());
        }
    }

    public Object getArrayObject() {
        if (array == null) {
            init_array();
        }
        return array.getArrayObject();
    }

    public Object instantiateArray(Context context) throws Redirection {
        return array.instantiateArray(context);
    }

    public Object get(int n) {
        if (array == null) {
            init_array();
        }
        return array.get(n);
    }

    public int getSize() {
        if (array == null) {
            init_array();
        }
        return array.getSize();
    }

    public boolean isGrowable() {
        if (array == null) {
            init_array();
        }
        return array.isGrowable();
    }

    public boolean add(Object element) {
        if (array == null) {
            init_array();
        }
        array.add(element);

        return true;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            init_array();
        }
        Object oldElement = array.get(n);
        array.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        if (array == null) {
            init_array();
        }
        return array.iterator();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        Iterator<Object> it = iterator();
        while (it.hasNext()) {
            Object item = it.next();
            if (item != null) {
                sb.append(item.toString());
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
}

class DynamicArray implements BentoArray, DynamicObject {
    private BentoArray statements;
    private Context initContext;
    private transient List<Object> generatedArray = null;


    public DynamicArray(BentoArray statements) {
        this.statements = statements;
    }

    public DynamicArray(BentoArray statements, Context context) throws Redirection {
        this.statements = statements;
        initContext = (Context) context.clone();
        generateForContext(context);
    }

    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (initContext == null) {
            return new DynamicArray(statements, context);
        } else {
            return this;
        }
    }

    public boolean isInitialized(Context context) {
        return (initContext != null && initContext.equals(context));
    }

    private void generateForContext(Context context) throws Redirection {
        boolean fixedSize = !statements.isGrowable();
        int n = fixedSize ? statements.getSize() : statements.getSize() * 2;
        generatedArray = Context.newArrayList(n, Object.class);

        Iterator<Object> it = statements.iterator();
        while (it.hasNext()) {
            addAllConstructions(context, it.next(), generatedArray);
            if (fixedSize && generatedArray.size() >= n) {
                break;
            }
        }
    }
    
    private static void addAllConstructions(Context context, Object object, List<Object> array) throws Redirection {
        Object element = (object instanceof ElementDefinition ? ((ElementDefinition) object).getElement(context) : object);
        List<Construction> constructions = null;
        if (element instanceof ConstructionGenerator) {
            constructions = ((ConstructionGenerator) element).generateConstructions(context);
            array.addAll(constructions);
        } else if (element instanceof ConstructionContainer) {
            constructions = ((ConstructionContainer) element).getConstructions(context);
            if (constructions != null) {
                Iterator<Construction> it = constructions.iterator();
                while (it.hasNext()) {
                    addAllConstructions(context, it.next(), array);
                }
            }
        } else {
            array.add(element);
        }
    }

    public Object getArrayObject() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        Object[] arrayObj = generatedArray.toArray();
        for (int i = 0; i < arrayObj.length; i++) {
            if (arrayObj[i] instanceof CollectionInstance) {
                try {
                    arrayObj[i] = ((CollectionInstance) arrayObj[i]).getCollectionObject();
                } catch (Redirection r) {
                    System.err.println("Error getting collection instance: " + r);
                }
            }
        }
        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(getArrayObject(), context);
    }

    public Object get(int n) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.get(n);
    }

    public int getSize() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.size();
    }

    public boolean isGrowable() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return statements.isGrowable();
    }

    public boolean add(Object element) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        statements.add(element);
        return true;
    }

    public Object set(int n, Object element) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        Object oldElement = generatedArray.get(n);
        statements.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.iterator();
    }
}

