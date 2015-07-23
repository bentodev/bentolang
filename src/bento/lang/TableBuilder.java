/* Bento
 *
 * $Id: TableBuilder.java,v 1.5 2015/04/06 13:52:55 sthippo Exp $
 *
 * Copyright (c) 2013-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.*;

/**
 * TableBuilder constructs tables and instantiates their contents.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class TableBuilder extends CollectionBuilder {

    private CollectionDefinition tableDef = null;

    public TableBuilder(CollectionDefinition tableDef) {
        this.tableDef = tableDef;
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        return new ResolvedTable(tableDef, context, args, indexes);
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        return new ResolvedTable(tableDef, context, args, indexes, collectionData);
    }

    public static String getTextForMap(CollectionDefinition collectionDef, Map map, Context context) throws Redirection {
        StringBuffer sb = new StringBuffer();
        
        sb.append("{ ");

        Object[] keys = map.keySet().toArray();
        if (keys.length > 0) {
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String key = keys[i].toString();
                Object element = map.get(keys[i]);
                sb.append(key);
                sb.append(": ");
                Definition def = (collectionDef == null ? null : ResolvedCollection.getElementDefinitionForCollection(element, collectionDef, context));
                Construction construction = AbstractConstruction.getConstructionForElement(element, context);
                if (def != null && def.hasChildDefinition("decorate_element")) {
                    List<Construction> list = new SingleItemList<Construction>(construction);
                    ArgumentList args = new ArgumentList(list);
                    Object data = def.getChild(new NameNode("decorate_element"), args, null, null, context, true, true, null);
                    sb.append(data.toString());
               
                } else {
                    sb.append('"');
                    sb.append(construction.getText(context));
                    sb.append('"');
                }
            }
        }
        
        sb.append(" }");
        
        return sb.toString();
    }

}

class ResolvedTable extends ResolvedCollection {

    protected Map<String, Object> table = null;
    private CollectionDefinition def = null;

    public ResolvedTable(CollectionDefinition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, context, args, indexes);

        this.def = def;

//        String name = def.getName();
//        String modifier = Instantiation.getNameModifier(args, null);
//        Definition defInCache = null;

//        if (def.getDurability() != Definition.DYNAMIC && (args == null || !args.isDynamic())) {
//            cachevlog("  = = =]  table: retrieving " + name + " from cache [- - - ");
//            table = (Map) context.getData(def.getFullName(), name, modifier, args, null);
//            defInCache = context.getCachedDefinition(def, modifier, args);
//            cachevlog("  = = =]  " + name + " table data: " + (table == null ? "null" : table.toString()));
//            cachevlog("  = = = =]  context: " + context.toString());
//        }
//        if (table == null || !def.equals(defInCache)) {
            if (def.hasStaticData()) {
                table = (Map<String, Object>) def.getStaticData();
            } else {
                table = createTable(def, context, args);
                // has no effect if def is not static
                def.setStaticData(table);
            }
//            cachevlog("  = = =]  table: storing data for " + name + " in cache [- - - ");
//            context.putData(def, args, null, name, modifier, table);
//            cachevlog("  = = =]  " + name + " table data: " + (table == null ? "null" : table.toString()));
//        }
    }

    public ResolvedTable(CollectionDefinition def, Context context, ArgumentList args, List<Index> indexes, Object tableData) throws Redirection {
        super(def, context, args, indexes);

        this.def = def;

        if (tableData instanceof Map<?,?>) {
        	table = (Map<String, Object>) tableData;

        } else if (tableData instanceof CollectionDefinition) {
            // this doesn't support args in anonymous arrays 
            CollectionDefinition tableDef = (CollectionDefinition) tableData;
            if (tableDef.equals(def)) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Table " + def.getName() + " is circularly defined.");
            }
            table = tableDef.getTable(context, null, null);

        } else if (tableData != null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unable to initialize table " + def.getName() + "; data in context of wrong type: " + tableData.getClass().getName());
        }
        
    }

    public Map<String, Object> getTable() {
        return table;
    }
    
    public Object generateData(Context context, Definition def) throws Redirection {
        return table;
    }
    
    /** Creates a table based on the definition.  If the contents of the definition are
     *  of an unexpected type, a ClassCastException is thrown.
     */
    private Map<String, Object> createTable(CollectionDefinition def, Context context, ArgumentList args) throws Redirection {
        
        ParameterList params = def.getParamsForArgs(args, context);
        context.push(def, params, args, false);
        try {

            Map<String, Object> table = null;
            Object contents = def.getContents();
            if (contents != null) {

                // table defined with an TableInitExpression
                if (contents instanceof ArgumentList) {
                    List<Dim> dims = def.getDims();
                    Dim majorDim = dims.get(0);
                    Dim.TYPE dimType = majorDim.getType();

                    ArgumentList elements = (ArgumentList) contents;
                    int size = 0;
                    if (dimType == Dim.TYPE.DEFINITE) {
                        size = majorDim.getSize();
                    } else if (elements != null) {
                        size = elements.size();
                    }

                    // for now just handle one dimension
                    table = new HashMap<String, Object>(size);
                    addElements(context, elements, table);

                // table is aliased or externally defined
                } else if (contents instanceof ValueGenerator) {
                    table = new TableInstance((ValueGenerator) contents, context);

                } else {
                    Object obj = contents;
                    if (obj instanceof Construction) {
                    	obj = ((Construction) obj).getData(context);
                    }
                    if (obj instanceof Value) {
                        obj = ((Value) obj).getValue();
                    }
                    Map<String, Object> map;
                    if (obj == null || obj instanceof Map) {
                        map = (Map<String, Object>) obj;
                    } else if (obj instanceof List || obj.getClass().isArray()) {
                        map = new MappedArray(obj, context);
                    } else {
                        throw new ClassCastException("Table value must be a collection type (Map, List or array)");
                    }
                    table = new TableInstance(map);

                }

        //            if (table instanceof HashMap && def.getDurability() != STATIC) {
        //                table = new HashMap(table);
        //            }

                if (table instanceof DynamicObject) {
                    table = (Map<String, Object>) ((DynamicObject) table).initForContext(context, args, indexes);
                }

            } else {
                table = new HashMap<String, Object>();
            }
            return table;
        } finally {
            context.pop();
        }
    }


    private void addElements(Context context, List<Construction> elements, Map<String, Object> table) throws Redirection {
        if (elements != null) {
            Iterator<Construction> it = elements.iterator();
            while (it.hasNext()) {
                Construction item = it.next();
                if (item instanceof TableElement) {
                    TableElement element = (TableElement) item;
                    String key = (element.isDynamic() ? element.getDynamicKey(context).getString() : element.getKey().getString());
                    Definition elementDef = getElementDefinition(element);
                    table.put(key, elementDef);
                } else if (item instanceof ConstructionGenerator) {
                    List<Construction> constructions = ((ConstructionGenerator) item).generateConstructions(context);
                    addElements(context, constructions, table);
                } else {
                    throw new Redirection(Redirection.STANDARD_ERROR, def.getFullName() + " contains invalid element type: " + item.getClass().getName());
                }
            }
        }
    }
    /** Returns the TableDefinition defining this collection. */
    public CollectionDefinition getCollectionDefinition() {
        return def;
    }

    public Object get(Object key) {
        return table.get(key);
    }

    public Definition getElement(Index index, Context context) {
        Object element = null;
        if (context == null) {
        	context = getResolutionContext();
        }
        if (index instanceof ArrayIndex) {
            int i = index.getIndexValue(context).getInt();
            Object[] keys = table.keySet().toArray();
            Arrays.sort(keys);
            element = getElement(keys[i]);
        } else {
            String key = index.getIndexValue(context).getString();
            element = getElement(key);
        }
        return getElementDefinition(element);
    }
    
    private Object getElement(Object key) {
        if (table instanceof InstantiatedMap) {
            return ((InstantiatedMap) table).getElement(key);
        } else {
            return table.get(key);
        }
        
    }

    public int getSize() {
        return (table != null ? table.size() : 0);
    }

    public Object getCollectionObject() {
        return table;
    }


    /** Returns an iterator for the elements in the table.
     */
    public Iterator<Definition> iterator() {
        return new TableElementIterator();
    }
    
    public class TableElementIterator implements Iterator<Definition> {
        Iterator<String> keys = table.keySet().iterator();
        
        public TableElementIterator() {}

        public boolean hasNext() {
            return keys.hasNext();
        }

        public Definition next() {
            String key = (String) keys.next();
            return getElementDefinition(table.get(key));
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }


    public Iterator<Construction> constructionIterator() {
        return new TableConstructionIterator();
    }
            
    public class TableConstructionIterator implements Iterator<Construction> {
        Iterator<String> keys = table.keySet().iterator();
        
        public TableConstructionIterator() {}

        public boolean hasNext() {
            return keys.hasNext();
        }

        public Construction next() {
            String key = (String) keys.next();
            Object element = table.get(key);
            return getConstructionForElement(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }

    public Iterator<Index> indexIterator() {
        return new TableIndexIterator();
    }

    public class TableIndexIterator implements Iterator<Index> {
        Iterator<String> keys = table.keySet().iterator();
        public TableIndexIterator() {}

        public boolean hasNext() {
            return keys.hasNext();
        }

        public Index next() {
            String key = (String) keys.next();
            return new TableIndex(new PrimitiveValue(key));
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }

    public void put(Object key, Object element) {
        Definition elementDef = null;
        elementDef = getElementDefinition(element);
        String k = null;
        if (key instanceof Value) {
            k = ((Value) key).getString();
        } else if (key != null) {
            k = key.toString();
        }
        table.put(k, elementDef);
    }

    public void putElement(TableElement element) {
        put(element.getKey().getString(), element);
    }

    public void add(Object element) {
        if (element instanceof TableElement) {
            putElement((TableElement) element);
        } else {
            throw new IllegalArgumentException("Only TableElements may be added to a table.");
        }
    }
    
    public String getText(Context context) throws Redirection {
        return TableBuilder.getTextForMap(def, table, context);
    }
}

class TableInstance implements Map<String, Object> {
    private ValueGenerator valueGen;
    private Context initContext = null;
    private Map<String, Object> map = null;

    public TableInstance(Map<String, Object> map) {
        this.map = map;
    }

    public TableInstance(ValueGenerator valueGen, Context context) throws Redirection {
        this.valueGen = valueGen;
        initContext = (Context) context.clone();
        Object obj = null;
        boolean pushed = false;
        ArgumentList args = null;
        List<Index> indexes = null;
        try {
            if (valueGen instanceof Instantiation) {
                Instantiation instance = (Instantiation) valueGen;
                Instantiation ultimateInstance = instance.getUltimateInstance(context);
                indexes = (instance == ultimateInstance ? null : instance.getIndexes());
                Definition def = ultimateInstance.getDefinition(context);
                if (def != null) {
                    args = ultimateInstance.getArguments();
                    obj = ultimateInstance.instantiate(context, def, args, indexes);
                    ParameterList params = def.getParamsForArgs(args, context);
                    context.push(def, params, args, false);
                    pushed = true;
                }
            } else if (valueGen instanceof IndexedMethodConstruction) {
                obj = ((IndexedMethodConstruction) valueGen).getCollectionObject(context);
                
            } else {
                obj = valueGen.getData(context);
                if (obj instanceof Value) {
                    obj = ((Value) obj).getValue();
                }
            }
            if (obj instanceof DynamicObject) {
                obj = ((DynamicObject) obj).initForContext(context, args, indexes);
            }
            
            if (indexes != null) {
                obj = context.dereference(obj, indexes);
            }
            
            if (obj instanceof Map<?,?>) {
                map = (Map<String, Object>) obj;
            } else if (obj instanceof List<?> || (obj != null && obj.getClass().isArray())) {
                map = new MappedArray(obj, context);
            } else if (obj instanceof CollectionInstance) {
                obj = ((CollectionInstance) obj).getCollectionObject();
                if (obj instanceof Map<?,?>) {
                    map = (Map<String, Object>) obj;
                } else {
                    map = new MappedArray(obj, context);
                }
            } else if (obj instanceof CollectionDefinition) {
                CollectionInstance collectionInstance = ((CollectionDefinition) obj).getCollectionInstance(context, args, indexes);
                obj = ((CollectionInstance) obj).getCollectionObject();
                if (obj instanceof Map<?,?>) {
                    map = (Map<String, Object>) obj;
                } else {
                    map = new MappedArray(obj, context);
                }

            } else if (obj == null) {
                map = null;
                
            } else {
                throw new Redirection(Redirection.STANDARD_ERROR, "Can't instantiate table in TableInstance; unsupported class " + obj.getClass().getName());
            }
    
        } finally {
            if (pushed) {
                context.pop();
            }
        }
    }


    public int size() {
        if (map == null) {
            return 0;
        } else {
            return map.size();
        }
    }

    public boolean isEmpty() {
        if (map == null) {
            return true;
        } else {
            return map.isEmpty();
        }
    }

    public boolean containsKey(Object key) {
        if (map == null) {
            return false;
        } else {
            return map.containsKey(key);
        }
    }

    public boolean containsValue(Object value) {
        if (map == null) {
            return false;
        } else {
            return map.containsValue(value);
        }
    }

    public Object get(Object key) {
        if (map == null) {
            return null;
        } else {
            return map.get(key);
        }
    }

    public Object put(String key, Object value) {
        if (map == null) {
            throw new NullPointerException("Cannot put value in table; map is null");
        }
        return map.put(key, value);
    }

    public Object remove(Object key) {
        if (map == null) {
            throw new NullPointerException("Cannot remove value from table; map is null");
        }
        return map.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        if (map == null) {
            throw new NullPointerException("Cannot put values into table; map is null");
        }
        map.putAll(t);
    }

    public void clear() {
        if (map != null) {
            map.clear();
        }
    }

    public Set<String> keySet() {
        if (map == null) {
            return null;
        } else {
            return map.keySet();
        }
    }

    public Collection<Object> values() {
        if (map == null) {
            return null;
        } else {
            return map.values();
        }
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        if (map == null) {
            return null;
        } else {
            return map.entrySet();
        }
    }

    public boolean equals(Object o) {
        if (map == null) {
            return (o == null || (o instanceof TableInstance && ((TableInstance) o).map == null));
        }
        return map.equals(o);
    }

    public int hashCode() {
        if (map == null) {
            return 0;
        } else {
            return map.hashCode();
        }
    }

}

