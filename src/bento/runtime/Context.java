/* Bento
 *
 * $Id: Context.java,v 1.421 2015/07/13 20:04:24 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A Context contains a stack of definitions representing the state of a construction
 * operation.  When a page is instantiated, its definition is pushed onto the stack.  As
 * each construction called for by the page definition is executed, the definition of the
 * object being constructed is pushed onto the stack; if that definition istself contains
 * constructions the process continues recursively.  When the construction of an object is
 * complete, its definition is popped from the stack.  Thus, at any point in the process,
 * the stack contains the nested definitions of the objects-within-objects being
 * constructed at that point.
 *
 * Definition parameters and instantiation arguments, if present, are pushed on the
 * stack along with the definition.
 *
 * A context is not just a stack, however; it is also a tree.  At any point, a context
 * may be cloned, and the clone may subsequently follow a separate history.  When two
 * different definitions are pushed on to a context and its clone, the context branches.
 * Indeed, the tree is created on the first push, at which point the two copies of the
 * context are no longer identical.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.421 $
 */

public class Context {

    /** Value to pass to setErrorThreshhold to redirect everything. */
    public final static int EVERYTHING = 0;

    /** Value to pass to setErrorThreshhold to redirect all warnings and errors. */
    public final static int WARNINGS = 1;

    /**
     *  Value to pass to setErrorThreshhold to ignore warnings and redirect all
     *  errors, including ignorable ones such as undefined instances.
     */
    public final static int IGNORABLE_ERRORS = 2;

    /**
     *  Value to pass to setErrorThreshhold to ignore warnings and ignorable errors
     *  and redirect all functional and fatal errors.
     */
    public final static int FUNCTIONAL_ERRORS = 3;

    /** Value to pass to setErrorThreshhold to ignore everything except for fatal errors. */
    public final static int FATAL_ERRORS = 4;

    private final static int MAX_POINTER_CHAIN_LENGTH = 10;
    
    private final static int DEFAULT_MAX_CONTEXT_SIZE = 250;

    private final static void vlog(String str) {
        SiteBuilder.vlog(str);
    }

    /** Anything bigger than this will be treated as runaway recursion.  The default value
     *  is DEFAULT_MAX_CONTEXT_SIZE.
     **/
    private static int maxSize = DEFAULT_MAX_CONTEXT_SIZE;

    /** Set the maximum size for any context.  The maximum number of levels of nesting
     *  may be slightly less than this number, since some constructions require temporary
     *  pushing of additional definitions onto the stack during instantiation.
     */
    public static void setGlobalMaxSize(int max) {
        maxSize = max;
    }

    /** A static class instantiated once per context tree capable of generating id
     *  values which are globally unique within a context tree (a root context and
     *  all contexts copied and cloned from it).
     *
     *  State id's are nonnegative integers (along with the special value -1 for
     *  an empty context), meaning there are about 2 billion unique states.  The
     *  id generator will roll over safely when it hits the limit and continue
     *  to generate nonnegative id's, but those id's are no longer absolutely
     *  guaranteed to be unique.
     *
     *  To minimize the possibility of nonunique states, the rollover logic skips
     *  low id values, so that id values up to 1000 <i>are</i> guaranteed to remain
     *  unique.
     */
    private static class StateFactory {
        /** The value to which the state wraps around to.  Global and persistent state
         *  values will most likely have low values, so the wrap around value should
         *  be well above zero.
         */
        private final static int WRAP_AROUND_STATE = 1001;
        private int state = -1;
        public StateFactory() {}

        public int nextState() {
            ++state;
            if (state < 0) {
                // wrap arround
                state = WRAP_AROUND_STATE;
            }
            return state;
        }
        public int lastState() { return state; }
    }


    /** Dereferences the passed definition if necessary to return the proper
     *  definition to push on the context.
     * @throws Redirection 
     */
    private Definition getContextDefinition(Definition definition) {
        Definition contextDef = null;

        // first resolve element references to element definitions,
        // which are handled below
        if (definition instanceof ElementReference) {
            try {
                definition = ((ElementReference) definition).getElementDefinition(this);
            } catch (Redirection r) {
                throw new IllegalStateException("Redirection on attempt to get element definition: " + r);
            }
        }
        
        
        if (definition instanceof DefinitionFlavor) {
            contextDef = ((DefinitionFlavor) definition).def;
        } else if (definition instanceof TypeDefinition) {
            contextDef = ((TypeDefinition) definition).def;
        //} else if (definition instanceof ElementReference) {
        //    // array and table references defer to their collections for context.
        //    contextDef = ((ElementReference) definition).getCollectionDefinition();
        } else if (definition instanceof ElementDefinition) {
            Object element = ((ElementDefinition) definition).getElement(this);
            if (element instanceof Definition) {
                contextDef = getContextDefinition((Definition) element);
            } else if (element instanceof Instantiation) {
                Instantiation instance = (Instantiation) element;
                contextDef = getContextDefinition(instance.getDefinition(this));
            }
        }
        
        if (contextDef != null) {
            return contextDef;

        } else {
        // anything else has to be a named definition
           return definition;
        }
    }

    public static String makeGlobalKey(String fullName) {
        return fullName;
//        if (fullName == null || fullName.indexOf('.') < 0) {
//            return fullName;
//        } else {
//        
//            // strip off the site name
//            return fullName.substring(fullName.indexOf('.') + 1);
//        }
    }

    /** Takes a cache key and strips off modifiers indicating arguments or loop index. */
    public static String baseKey(String key) {
        // argument modifier
        int ix = key.indexOf('(');
        if (ix > 0) {
            key = key.substring(0, ix);
        } else {
            // loop modifier
            ix = key.indexOf('#');
            if (ix > 0) {
                key = key.substring(0, ix);
            }
        }
        return key;
    }


    
    private static int instanceCount = 0;
    public static int getNumContextsCreated() {
        return instanceCount;
    }
    private static int numClonedContexts = 0;
    public static int getNumClonedContexts() {
        return numClonedContexts;
    }

    // -------------------------------------------
    // Context properties
    // -------------------------------------------

    private Context rootContext;
    private StateFactory stateFactory;

    private Entry rootEntry = null;
    protected Entry topEntry = null;
    private Entry popLimit = null;
    private Definition definingDef = null;
    private NamedDefinition instantiatedDef = null;

    private Map<String, Object> cache = null;
    private Map<String, Pointer> keepMap = null;
    private Map<String, Map<String,Object>> siteCaches = null;
    private Map<String, Object> globalCache = null;
    private Session session = null;
    
    private int stateCount;

    private int size = 0;

    private int errorThreshhold = EVERYTHING;

    private Entry unpushedEntries = null;

    // only root entries have nonull values for abandonedEntries
    private Entry abandonedEntries = null;
    
    // debugging interface
    private BentoDebugger debugger = null;

    /** Constructs a context beginning with the specified definition */
    public Context(Definition def) throws Redirection {
        this(def, null, null);
    }

    /** Constructs a context beginning with the specified definition, parameters
     *  and arguments.
     */
    private Context(Definition def, ParameterList params, ArgumentList args) throws Redirection {
        instanceCount++;
        rootContext = this;
        stateFactory = new StateFactory();
        stateCount = stateFactory.lastState();
        cache = newHashMap(Object.class);
        keepMap = newHashMap(Pointer.class);
        siteCaches = newHashMapOfMaps(Object.class);
        globalCache = newHashMap(Object.class);
        
        if (def != null) {
            try {
                push(def, params, args, true);
                
            } catch (Redirection r) {
                vlog("Error creating context: " + r.getMessage());
                throw r;
            }
        }
        
        popLimit = topEntry;
    }

    /** Constructs a new context with the specified root entry.
     * @throws Redirection 
     */
    public Context(Entry rootEntry) throws Redirection {
        // initialize according to root entry
        instanceCount++;
        rootContext = this;
        stateFactory = new StateFactory();
        stateCount = stateFactory.lastState();
        cache = newHashMap(Object.class);
        keepMap = newHashMap(Pointer.class);
        siteCaches = newHashMapOfMaps(Object.class);
        globalCache = newHashMap(Object.class);
        push(newEntry(rootEntry, true));
        popLimit = topEntry;
    }

    /** Constructs a context which is a copy of the passed context.
     */
    public Context(Context context, boolean clearCache) {
        instanceCount++;
        copy(context, clearCache);
    }

    public Context.Entry getRootEntry() {
        return rootEntry;
    }
    
    public int getErrorThreshhold() {
        return errorThreshhold;
    }

    public void setErrorThreshhold(int threshhold) {
        errorThreshhold = threshhold;
    }
    
    public BentoDebugger getDebugger() {
    	return debugger;
    }
    
    public void setDebugger(BentoDebugger debugger) {
    	this.debugger = debugger;
    }
    
    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
    
    public Map<String, Object> getCache() {
        return cache;
    }
    
    public Map<String, Pointer> getKeepMap() {
        return keepMap;
    }

    public Map<String, Object> getSiteCache(String name) {
    	return siteCaches.get(name);
    }
    
    public Map<String, Object> getGlobalCache(String name) {
        return globalCache;
    }
    
    void addKeeps(Definition def) throws Redirection {
        if (def != null && def instanceof NamedDefinition) {
            List<KeepStatement> keeps = ((NamedDefinition) def).getKeeps();
            if (keeps != null) {
                Iterator<KeepStatement> it = keeps.iterator();
                while (it.hasNext()) {
                    KeepStatement k = it.next();
                    try {
                        keep(k);
                    } catch (Redirection r) {
                        vlog("Error in keep statement: " + r.getMessage());
                        throw r;
                    }
                }
            }
            List<InsertStatement> inserts = ((NamedDefinition) def).getInserts();
            if (inserts != null) {
                Iterator<InsertStatement> it = inserts.iterator();
                while (it.hasNext()) {
                    InsertStatement i = it.next();
                    try {
                        insert(i);
                    } catch (Redirection r) {
                        vlog("Error in insert statement: " + r.getMessage());
                        throw r;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void keep(KeepStatement k) throws Redirection {
        Map<String, Object> table = null;
        Instantiation instance = k.getTableInstance();

        // the current container entry; this is the entry corresponding to the code
        // block containing the instantiation being constructed.
        Entry containerEntry = null;
        Map<String, Object> containerTable = null;
        String containerKey = null;
        boolean inContainer = k.isInContainer(this);
        if (inContainer) {
            // back up to the new frame entry
            for (Entry entry = topEntry; entry.getPrevious() != null; entry = entry.getPrevious()) {
                if (entry.superdef == null) {
                    containerEntry = entry.getPrevious();
                    containerTable = containerEntry.getKeepCache();
                    break;
                }
            }
            table = containerTable;

        } else if (instance == null) {
            table = topEntry.getKeepCache();

        } else {
            NamedDefinition def = (NamedDefinition) instance.getDefinition(this);
            if (def instanceof CollectionDefinition) {
                
                CollectionDefinition collectionDef = (CollectionDefinition) def; // ((TableDefinition) def).initCollection(this);
                CollectionInstance collection = collectionDef.getCollectionInstance(this, instance.getArguments(), instance.getIndexes());
                if (collectionDef.isTable()) {
                    table = (Map<String, Object>) collection.getCollectionObject();
                } else {    
                    table = new MappedArray(collection.getCollectionObject(), this);
                }
                
            } else {
                Object tableObject = instance.getData(this);
                if (tableObject == null || tableObject.equals(NullValue.NULL_VALUE)) {
                    throw new Redirection(Redirection.STANDARD_ERROR, "error in keep: table " + instance.getName() + " not found");
                }
                
                if (tableObject instanceof AbstractNode) {
                    def.initNode((AbstractNode) tableObject);
                }
                if (tableObject instanceof Map<?,?>) {
                    table = (Map<String, Object>) tableObject;
                } else if (tableObject instanceof List || tableObject.getClass().isArray()) {
                    table = new MappedArray(tableObject, this);
                } else if (tableObject instanceof CollectionInstance) {
                    Object obj = ((CollectionInstance) tableObject).getCollectionObject();
                    if (obj instanceof Map<?,?>) {
                        table = (Map<String, Object>) obj;
                    } else {
                        table = new MappedArray(obj, this);
                    }
                } else if (tableObject instanceof CollectionDefinition) {
                    CollectionInstance collection = ((CollectionDefinition) tableObject).getCollectionInstance(this, instance.getArguments(), instance.getIndexes());
                    if (((CollectionDefinition) tableObject).isTable()) {
                        table = (Map<String, Object>) collection.getCollectionObject();
                    } else {    
                        table = new MappedArray(collection.getCollectionObject(), this);
                    }
                }
            }
        }
        if (table != null) {
            ResolvedInstance[] resolvedInstances = k.getResolvedInstances(this);
            Name asName = k.getAsName(this);
            Name byName = k.getByName();
            String key = null;
            boolean asthis = false;
            if (asName != null) {
                key = asName.getName();
                if (key == Name.THIS) {
                    key = topEntry.def.getName();
                    asthis = true;
                }
            } else if (byName != null) {
                NameNode keepName = k.getDefName();
                Definition owner = getDefiningDef();
                KeepHolder keepHolder = new KeepHolder(keepName, owner, resolvedInstances, (NameNode) byName, table, k.isPersist(), inContainer, asthis);
                topEntry.addDynamicKeep(keepHolder);
                return;
            }

            containerKey = (asthis ? key : topEntry.def.getName() + (key == null ? "." : "." + key));
            topEntry.addKeep(resolvedInstances, key, table, containerKey, containerTable, k.isPersist(), keepMap, cache);
        }
    }

    private void setKeepsFromEntry(Entry entry) {
        topEntry.copyKeeps(entry);
    }

    private void addKeepsFromEntry(Entry entry) {
        topEntry.addKeeps(entry);
    }

    private void insert(InsertStatement i) throws Redirection {
        topEntry.addInsert(i.getInsertedType(), i.getTargetType(), i.getInsertionPoint());
    }
        
    private void setInsertsFromEntry(Entry entry) {
        topEntry.copyInserts(entry);
    }
    
    public Type getInsertedAbove(Type t) {
        Entry entry = topEntry;
        
        while (entry != null && t.equals(entry.def.getType())) {
            entry = entry.link;

        }
        if (entry != null && entry.insertAboveMap != null) {
            return (Type) entry.insertAboveMap.get(t.getName()); 
        }
        return null;
    }

    public Type getInsertedBelow(Type st, Type t) {
        Entry entry = topEntry;
        Definition topDef = topEntry.def;

        while (entry != null && topDef.equals(entry.def)) {
            entry = entry.link;
        }
        if (entry != null && entry.insertBelowMap != null) {
            return (Type) entry.insertBelowMap.get(st.getName()); 
        }
        return null;
    }

    /** Looks through the context for the immediate subdefinition of the superdefinition at the
     *  top of the stack.
     */
    private synchronized NamedDefinition getSubdefinition() {
        
        // if there is no superdef in the top context entry, then there is
        // no subdefinition
        if (topEntry.superdef == null) {
            return null;
        }
        
        int numUnpushes = 0;
        Definition superdef = topEntry.superdef;
        try {
            while (topEntry != null) { 
                Definition subdef = (topEntry.superdef != null ? topEntry.superdef : topEntry.def);
                NamedDefinition sd = subdef.getSuperDefinition(this);
                if (sd != null && sd.includes(superdef)) {
                    return (NamedDefinition) subdef;
                }
                if (topEntry.link == null) {
                    break;
                }
                unpush();
                numUnpushes++;
            }
        } finally {
            while (numUnpushes > 0) {
                repush();
                numUnpushes--;
            }
        }
        return null;
    }

    private Object constructSuper(NamedDefinition def, ArgumentList args, NamedDefinition instantiatedDef) throws Redirection {
    	return constructSuper(def, args, instantiatedDef, null);
    }

    
    private Object constructSuper(NamedDefinition def, ArgumentList args, NamedDefinition instantiatedDef, LinkedList<Definition> nextList) throws Redirection {
    	Object data = null;
        boolean pushed = false;
        boolean hasMore = (nextList != null && nextList.size() > 0);
        
        if (!hasMore) {
            ParameterList params = def.getParamsForArgs(args, this, false);
            push(instantiatedDef, def, params, args);
            pushed = true;
        }
        
        try {
            List<Construction> constructions = def.getConstructions(this);
            int numConstructions = (constructions == null ? 0 : constructions.size());
            NamedDefinition superDef = def.getSuperDefinition(this);

            if (!hasMore && superDef != null && (superDef.hasSub(this) || numConstructions == 0)) {
            	Type st = def.getSuper(this);
                ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                data = constructSuper(superFlavor, superArgs, instantiatedDef);

            } else {
            	
                if (hasMore) {
                	Definition nextDef = nextList.removeFirst();
                	constructions = nextDef.getConstructions(this);
                	numConstructions = (constructions == null ? 0 : constructions.size());
                }
            	
                if (numConstructions == 1) {
                    Construction object = constructions.get(0);
                    if (object instanceof SubStatement) {
                        NamedDefinition sub = (NamedDefinition) peek().def;
                        data = constructSub(sub, instantiatedDef);

                    } else if (object instanceof SuperStatement) {
                        Type st = def.getSuper(this);
                        ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                        NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                        data = constructSuper(superFlavor, superArgs, instantiatedDef);

                    } else if (object instanceof NextStatement) {
                        data = constructSuper(def, args, instantiatedDef, nextList);
                    	
                    } else if (object instanceof RedirectStatement) {
                        RedirectStatement redir = (RedirectStatement) object;
                        throw redir.getRedirection(this);

                    } else if (object instanceof Value) {
                        data = object;

                    } else if (object instanceof ValueGenerator) {
                        data = ((ValueGenerator) object).getData(this);

                    } else {
                        data = object.getData(this);
                    }
                    
                    if (data instanceof Value) {
                        data = ((Value) data).getValue();
                    } else if (data instanceof AbstractNode) {
                        instantiatedDef.initNode((AbstractNode) data);
                    }

                } else if (numConstructions > 1) {
                    Iterator<Construction> it = constructions.iterator();
                    while (it.hasNext()) {
                        Construction chunk = it.next();
                        if (chunk == null) {
                            continue;

                        } else if (chunk instanceof RedirectStatement) {
                            RedirectStatement redir = (RedirectStatement) chunk;
                            throw redir.getRedirection(this);

                        } else {
                            Object chunkData = null;

                            if (chunk instanceof SubStatement) {
                                NamedDefinition sub = getSubdefinition();
                                Object subData = (sub == null ? null : constructSub(sub, instantiatedDef));
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getValue();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this);
                                    } else {
                                        chunkData = subData;
                                    }
                                }

                            } else if (chunk instanceof SuperStatement) {
                                Type st = def.getSuper(this);
                                ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                Object superData = constructSuper(superFlavor, superArgs, instantiatedDef);
                                if (superData != null) {
                                    if (superData instanceof Value) {
                                        chunkData = ((Value) superData).getValue();
                                    } else if (superData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) superData).getValue(this);
                                    } else {
                                        chunkData = superData;
                                    }
                                }

                            } else if (chunk instanceof NextStatement) {
                                chunkData = constructSuper(def, args, instantiatedDef, nextList);
                            	
                            } else {
                                chunkData = chunk.getData(this);
                                if (chunkData != null && chunkData instanceof Value) {
                                    chunkData = ((Value) chunkData).getValue();
                                }
                            }
                            if (chunkData != null) {
                                if (data == null) {
                                    data = chunkData;
                                } else {
                                    data = PrimitiveValue.getStringFor(data) + chunkData;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
        	if (pushed) {
                pop();
        	}
        }
        return data;
    }

    public Object constructSub(NamedDefinition def, NamedDefinition instantiatedDef) throws Redirection {
        Object data = getLocalData("sub", null, null); // getData(null, "sub", null, null, null);
        if (data != null) {
            return data;
        }

  	    LinkedList<Definition> nextList = null;
        
        // call the form of getSuperDefinition that does not take
        // a context parameter, because we want the multidefinition
        // if there is one.
        Definition superDef = def.getSuperDefinition();

        if (superDef != null && superDef.hasNext(this)) {
      	    nextList = superDef.getNextList(this);
        }
        
        if (nextList != null && nextList.size() > 0) {
            Definition nextDef = nextList.removeFirst();
            data = constructSub(nextDef, instantiatedDef, nextList);
        
        } else {
        	try {
        		unpush();
                data = constructSub(def, instantiatedDef, null);
        	} finally {
        		repush();
        	}
        }
    
        if (data == null) {
            data = NullValue.NULL_VALUE;
        }
        putData(def, null, null, "sub", data);
        return data;
    }
    

    private Object constructSub(Definition def, NamedDefinition instantiatedDef, LinkedList<Definition> nextList) throws Redirection {
        Object data = null;    
        List<Construction> constructions = def.getConstructions(this);
        boolean hasMoreNext = (nextList != null && nextList.size() > 0);
        	
        if (constructions == null || constructions.size() == 0) {
            NamedDefinition sub = getSubdefinition();
            data = (sub == null ? null : constructSub(sub, instantiatedDef));
        } else {
            if (def.equals(instantiatedDef)) {
                data = construct(constructions);
            } else {
                int n = constructions.size();
                if (n == 1) {
                    Construction object = constructions.get(0);
                    if (object instanceof NextStatement) {
                        if (hasMoreNext) {
                            Definition nextDef = nextList.removeFirst();
                            data = constructSub(nextDef, instantiatedDef, nextList);
                        } else {
                            NamedDefinition sub = getSubdefinition();
                            if (sub == null) {
                            	data = null;
                            } else {
	                            try {
	                            	unpush();
		                            data = constructSub(sub, instantiatedDef, null);
	                            } finally {
	                            	repush();
	                            }
                            }
                        }
                        
                    } else if (object instanceof SubStatement) {
                        NamedDefinition sub = getSubdefinition();
                        data = (sub == null ? null : constructSub(sub, instantiatedDef));

                    } else if (object instanceof SuperStatement) {
                        Definition superDef = def.getSuperDefinition(this);
                        if (superDef == null) {
                            if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                            } else {
                                data = null;
                            }
                        } else {
                            Type st = def.getSuper(this);
                            ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                            NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                            data = constructSuper(superFlavor, superArgs, instantiatedDef);
                        }
                        
                    } else if (object instanceof RedirectStatement) {
                        RedirectStatement redir = (RedirectStatement) object;
                        throw redir.getRedirection(this);

                    } else if (object instanceof Value) {
                        data =  object;
                    } else if (object instanceof ValueGenerator) {
                        data = ((ValueGenerator) object).getData(this);
                    } else {
                        data = object.getData(this);
                    }
                    if (data instanceof Value) {
                        data = ((Value) data).getValue();
                    } else if (data instanceof AbstractNode) {
                        instantiatedDef.initNode((AbstractNode) data);
                    }


                } else if (n > 1) {
                    Iterator<Construction> it = constructions.iterator();
                    while (it.hasNext()) {
                        Construction chunk = it.next();
                        if (chunk == null) {
                            continue;

                        } else if (chunk instanceof RedirectStatement) {
                            RedirectStatement redir = (RedirectStatement) chunk;
                            throw redir.getRedirection(this);

                        } else {
                            Object chunkData = null;
                            if  (chunk instanceof NextStatement) {
                            	Object subData = null;
                            	if (hasMoreNext) {
                                    Definition nextDef = nextList.removeFirst();
                                    subData = constructSub(nextDef, instantiatedDef, nextList);
                                } else { 
                                    NamedDefinition sub = getSubdefinition();
                                    if (sub == null) {
                                    	subData = null;
                                    } else {
                                        try {
                                        	unpush();
    	                                    subData = constructSub(sub, instantiatedDef, null);
                                        } finally {
                                        	repush();
                                        }
                                    }
                                }
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getValue();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this);
                                    } else {
                                        chunkData = subData;
                                    }
                                }
                                
                            } else if (chunk instanceof SubStatement) {
                                NamedDefinition sub = getSubdefinition();
                                Object subData = (sub == null ? null : constructSub(sub, instantiatedDef));
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getValue();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this);
                                    } else {
                                        chunkData = subData;
                                    }
                                }

                            } else if (chunk instanceof SuperStatement) {
                                Definition superDef = def.getSuperDefinition(this);
                                if (superDef == null) {
                                    if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                        throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                    } else {
                                        chunkData = null;
                                    }
                                } else {
                                    Type st = def.getSuper(this);
                                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                    Object superData = constructSuper(superFlavor, superArgs, instantiatedDef);
                                    if (superData != null) {
                                        if (superData instanceof Value) {
                                            chunkData = ((Value) superData).getValue();
                                        } else if (superData instanceof ValueGenerator) {
                                            chunkData = ((ValueGenerator) superData).getValue(this);
                                        } else {
                                            chunkData = superData;
                                        }
                                    }
                                }
                            } else {
                                chunkData = chunk.getData(this);
                                if (chunkData != null && chunkData instanceof Value) {
                                    chunkData = ((Value) chunkData).getValue();
                                }
                            }
                            if (chunkData != null) {
                                if (data == null) {
                                    data = chunkData;
                                } else {
                                    data = PrimitiveValue.getStringFor(data) + chunkData;
                                }
                            }
                        }
                    }
                }
            }
        }
        return data;
    }


    public Object construct(Definition definition, ArgumentList args) throws Redirection {
        Object data = null;
        
        boolean pushedSuperDef = false;
        boolean pushedParamDef = false;
        boolean pushedContext = false;
        int pushedParams = 0;

        Block catchBlock = (definition instanceof AnonymousDefinition ? ((AnonymousDefinition) definition).getCatchBlock() : null);

        NamedDefinition oldInstantiatedDef = instantiatedDef;
        
        if (definition instanceof NamedDefinition) {
            instantiatedDef = (NamedDefinition) definition;
        }

        ParameterList params = null;

        // determine if this defines a namespace and therefore a new context level.
        // No need to push external definitions, because external names are
        // resolved externally
        if (!definition.isAnonymous() && !definition.isExternal()) {
            
            // get the arguments and parameters, if any, to push on the
            // context stack with the definition

            params = definition.getParamsForArgs(args, this);

            // Local definitions share the current level, rather than
            // start a new level.
//            if (definition.getAccess() == Definition.LOCAL_ACCESS) {
//                pushedParams = (params == null ? 0 : params.size());
//                for (int i = 0; i < pushedParams; i++) {
//                    pushParam((DefParameter) params.get(i), args.get(i));
//                }
//
//            } else {

                push(definition, params, args, true);
                pushedContext = true;
//            }
        }

        try {
            List<Construction> constructions = definition.getConstructions(this);
            boolean constructed = false;
            NamedDefinition superDef = definition.isAnonymous() ? null : definition.getSuperDefinition(this);

            Type st = definition.getSuper(this);
            
            if (!constructed && superDef != null && definition.getName() != Name.SUB) {
                
                // check to see if this is an alias, and the alias definition extends or equals the
                // superdefinition, in which case we shouldn't bother constructing the superdef here,
                // it will get constructed when the alias is constructed
                Definition aliasDef = null;
                if (definition.isAliasInContext(this)) {
                    Instantiation aliasInstance = definition.getAliasInstanceInContext(this);
                    if (definition.isParamAlias()) {
                        aliasInstance = aliasInstance.getUltimateInstance(this);
                    }
                    aliasDef = aliasInstance.getDefinition(this, definition);
                }
                AbstractNode contents = definition.getContents();
                Definition constructedDef = null;
                if (contents instanceof Construction) {
                    Type constructedType = ((Construction) contents).getType(this, definition);
                    if (constructedType != null) {
                        constructedDef = constructedType.getDefinition();
                    }
                }
                if ((aliasDef == null || !aliasDef.equalsOrExtends(superDef))
                        && (constructedDef == null || !constructedDef.equalsOrExtends(superDef))) {
                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                    if (superFlavor != null && (superFlavor.hasSub(this) || (constructions == null || constructions.size() == 0))) {
                        NamedDefinition ndef = (NamedDefinition) peek().def;
                        data = constructSuper(superFlavor, superArgs, ndef);
                        constructed = true;
                    }
                }
            }

            // not handled by one of the above cases
            if (!constructed) {
                if (definition.isAlias()) {
                    Construction construction = constructions.get(0);
                    if (construction instanceof Value) {
                        data = construction;
                    } else if (construction instanceof ValueGenerator) {

                        // no this isn't right
//                        if (construction instanceof Instantiation) {
//                            Instantiation instance = (Instantiation) construction;
//                            ArgumentList instanceArgs = instance.getArguments();
//                            if (instanceArgs != null) {
//                                if (args == null || instanceArgs.size() != args.size()) {
//                                    construction = new Instantiation(instance, args, instance.getIndexes());
//                                }
//                            }
//                        }
                        
                        data = ((ValueGenerator) construction).getData(this);
                    } else {
                        data = construction.getData(this);
                    }
                } else {
                    data = construct(constructions);
                }
            }
            
            if (data instanceof Value) {
                data = ((Value) data).getValue();

            } else if (data instanceof AbstractNode) {
                instantiatedDef.initNode((AbstractNode) data);
            }
            
            return data;

        } catch (Redirection r) {

            if (catchBlock != null) {
                String location = r.getLocation();
                String catchIdentifier = catchBlock.getCatchIdentifier();
                while (catchIdentifier != null && catchIdentifier.length() > 0) {
                    if (catchIdentifier.equals(location)) {
                        return catchBlock.getData(this);
                    } else {
                        catchBlock = catchBlock.getCatchBlock();
                        if (catchBlock == null) {
                            throw r;
                        }
                        catchIdentifier = catchBlock.getCatchIdentifier();
                    }
                }
                return catchBlock.getData(this);

            } else {
                throw r;
            }
        } catch (Throwable t) {
            if (catchBlock != null && catchBlock.getCatchIdentifier() == null) {
                return catchBlock.getData(this);
            } else {
                String className = t.getClass().getName();
                String message = t.getMessage();
                if (message == null) {
                    message = className;
                } else {
                    message = className + ": " + message;
                }
                t.printStackTrace();
                throw new Redirection(Redirection.STANDARD_ERROR, message);
            }

        } finally {
            if (pushedParams > 0) {
                for (int i = 0; i < pushedParams; i++) {
                    popParam();
                }

            } else if (pushedContext) {
                pop();
            }

            if (pushedSuperDef) {
                pop();
            }

            if (pushedParamDef) {
                pop();
            }
            instantiatedDef = oldInstantiatedDef;
        }
    }


    public Object construct(List<Construction> constructions) throws Redirection {
        Object data = null;
        if (constructions != null) {
            StringBuffer sb = null;
            int n = constructions.size();
            for (int i = 0; i < n; i++) {
                Construction object = constructions.get(i);
                            
                if (object instanceof RedirectStatement) {
                    RedirectStatement redir = (RedirectStatement) object;
                    throw redir.getRedirection(this);

                } else if (data == null) {
                    if (object instanceof SubStatement) {
                        NamedDefinition sub = getSubdefinition();
                        data = (sub == null ? null : constructSub(sub, instantiatedDef));

                    } else if (object instanceof SuperStatement) {
                        Definition def = peek().def;
                        NamedDefinition superDef = def.getSuperDefinition();
                        if (superDef == null) {
                            if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                            } else {
                                data = null;
                            }
                        } else {
                        	LinkedList<Definition> nextList = null;
                            if (superDef.hasNext(this)) {
                          	    nextList = superDef.getNextList(this);
                            }
                        	
                        	// get the specific definition for this context
                        	superDef = def.getSuperDefinition(this);
                            if (superDef == null) {
                                if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                } else {
                                    data = null;
                                }
                            } else {
                                Type st = def.getSuper(this);
                                ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                data = constructSuper(superFlavor, superArgs, instantiatedDef, nextList);
                            }
                        }

                    } else if (object instanceof Value) {
                        data = object;
                    } else if (object instanceof Chunk) {
                        data = object.getData(this);
                    } else if (object instanceof ValueGenerator) {
                        data = ((ValueGenerator) object).getData(this);
                    } else {
                        data = object;
                    }
                    
                    if (data instanceof Value) {
                        data = ((Value) data).getValue();
                    } else if (data instanceof AbstractNode) {
                        if (instantiatedDef != null) {
                            instantiatedDef.initNode((AbstractNode) data);
                        } else {
                            vlog("Null instantiatedDef in constructions for " + peek().def.getFullName());
                        }
                    }

                } else {
                    String str = null;
                    if (object instanceof SubStatement) {
                        NamedDefinition sub = getSubdefinition();
                        if (sub != null) {
                            Object obj = constructSub(sub, instantiatedDef);
                            if (obj != null && !obj.equals(NullValue.NULL_VALUE)) {
                                str = obj.toString();
                            }
                        }
                            
                    } else if (object instanceof SuperStatement) {
                        Definition def = peek().def;
                        NamedDefinition superDef = def.getSuperDefinition();
                        if (superDef == null) {
                            if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                            } else {
                                str = null;
                            }
                        } else {
                        	LinkedList<Definition> nextList = null;
                            if (superDef.hasNext(this)) {
                          	    nextList = superDef.getNextList(this);
                            }
                        	
                        	// get the specific definition for this context
                        	superDef = def.getSuperDefinition(this);
                            if (superDef == null) {
                                if (errorThreshhold <= Context.IGNORABLE_ERRORS) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                } else {
                                    str = null;
                                }
                            } else {
	                            Type st = def.getSuper(this);
	                            ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
	                            NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
	                            Object obj = constructSuper(superFlavor, superArgs, instantiatedDef, nextList);
	                            if (obj != null && !obj.equals(NullValue.NULL_VALUE)) {
	                                str = obj.toString();
	                            }
                            }
                        }
                    } else if (object instanceof Value) {
                        if (!object.equals(NullValue.NULL_VALUE)) {
                            str = ((Value) object).getString();
                        }
                    } else if (object instanceof Chunk) {
                        str = ((Chunk) object).getText(this);
                    } else if (object instanceof ValueGenerator) {
                        str = ((ValueGenerator) object).getValue(this).getString();
                    } else if (object != null) {
                        str = object.toString();
                    }
                    if (str != null && str.length() > 0) {
                        if (sb == null) {
                            sb = new StringBuffer(PrimitiveValue.getStringFor(data));
                            data = sb;
                        }
                        sb.append(str);
                    }
                }
            }
            if (sb != null && data == sb) {
                data = sb.toString();
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private List<String>[] addDynamicKeeps(String name, ArgumentList args) throws Redirection {
        List<String> allAddedKeeps[] = (List<String>[]) new List[size];
        int numUnpushes = 0;
        int i = 0;
        try {
            while (topEntry != null) {
                List<String> addedKeeps = null;
                if (topEntry.dynamicKeeps != null) {
                    Iterator<KeepHolder> it = topEntry.dynamicKeeps.iterator();
                    
                    while (it.hasNext()) {
                        KeepHolder kh = it.next();
                        NameNode keepName = kh.keepName;
                        Definition keyOwner = kh.owner;
                        if (keepName.getName().equals(name)) {
                            if (addedKeeps == null) {
                                addedKeeps = new ArrayList<String>(4);
                            }
                            addedKeeps.add(name);
                        
                            Definition keepDef = keyOwner.getChildDefinition(kh.keepName, this);
                            Object keyObj = null;
                            if (keepDef != null && keepDef.hasChildDefinition(kh.byName.getName())) {
                                // temporarily restore the stack in case the definition has to access
                                // parameters that have been unpushed
                                for (int j = 0; j < numUnpushes; j++) {
                                    repush();
                                }
                                ParameterList params = keepDef.getParamsForArgs(args, this);
                                push(keepDef, params, args, false);
                                keyObj = keepDef.getChildData(kh.byName, null, this, args);
                                pop();
                                for (int j = 0; j < numUnpushes; j++) {
                                    unpush();
                                }
                            } else {
                                keyObj = getData(null, kh.byName.getName(), args, null);
                                if (keyObj == null) {
                                    keyObj = keyOwner.getChildData(kh.byName, null, this, args);
                                }
                            }
                            if (keyObj == null || keyObj.equals(NullValue.NULL_VALUE)) {
                                Instantiation keyInstance = new Instantiation(kh.byName, topEntry.def);
                                keyObj = keyInstance.getData(this);
                                if (keyObj == null || keyObj.equals(NullValue.NULL_VALUE)) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "error in keep by directive: key is null");
                                }
                            }
                            String key = (keyObj instanceof Value ? ((Value) keyObj).getString() : keyObj.toString());
                            
                            Entry containerEntry = null;
                            Map<String, Object> containerTable = null;
                            String containerKey = null;
                            if (kh.inContainer) {
                                // back up to the new frame entry
                                for (Entry e = topEntry; e.getPrevious() != null; e = e.getPrevious()) {
                                    if (e.superdef == null) {
                                        containerEntry = e.getPrevious();
                                        containerTable = containerEntry.getCache();
                                        break;
                                    }
                                }
                                if (containerEntry != null) {
                                    containerKey = (kh.asThis ? key : topEntry.def.getName() + (key == null ? "." : "." + key));
                                    containerEntry.addKeep(kh.resolvedInstances, containerKey, containerTable, null, null, kh.persist, keepMap, cache);
                                }
                            } else {
                                topEntry.addKeep(kh.resolvedInstances, keyObj, kh.table, containerKey, containerTable, kh.persist, keepMap, cache);
                            }
                        }
                    }
                }
                allAddedKeeps[i++] = addedKeeps;
                if (topEntry.link == null) {
                    break;
                }
                numUnpushes++;
                unpush();
            }
        } finally {
            while (numUnpushes > 0) {
                repush();
                numUnpushes--;
            }
        }
        return allAddedKeeps;
    }
    
    private void removeDynamicKeeps(List<String>[] allAddedKeeps) {
        Iterator<Entry> entryIt = iterator();
        int i = 0;
        while (entryIt.hasNext()) {
            Entry entry = entryIt.next();
            try {
                List<String> addedKeeps = allAddedKeeps[i];
                if (addedKeeps != null) {
                    Iterator<String> it = addedKeeps.iterator();
                    while (it.hasNext()) {
                        String name = it.next();
                        entry.removeKeep(name);
                    }
                }
            } finally {
                i++;
            }
        }
    }
            
    
    /** Returns any cached data for a definition with the specified name
     *  in the current frame of the current context, or null if there is none.
     */
    public Object getLocalData(String name, ArgumentList args, List<Index> indexes) throws Redirection {
        return getData(null, name, args, indexes, true);
    }    

    /** Returns any cached data for a definition with the specified name
     *  in the current context, or null if there is none.
     */
    public Object getData(Definition def, String name, ArgumentList args, List<Index> indexes) throws Redirection {
        return getData(def, name, args, indexes, false);
    }    

    synchronized private Object getData(Definition def, String name, ArgumentList args, List<Index> indexes, boolean local) throws Redirection {
        if (name == null || name.length() == 0) {
            return null;
        }
        String fullName = (def == null ? name : def.getFullNameInContext(this));

        Object data = null;
        
        if (topEntry != null) {
            List<String>[] keeps = addDynamicKeeps(name, args);
            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(name, indexes);
            data = topEntry.get(key, fullName, args, local);
            removeDynamicKeeps(keeps);
        }

        if (data == null) {

            // TODO: modify fullName to match name if name is multipart
            //
            
            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(fullName, indexes);
            data = getContextData(key);
        }
        return data;
    }
    
    /** Modify the name used to cache a value with indexes, to discriminate
     *  cached collections from cached elements.
     */
    private String addIndexesToKey(String key, List<Index> indexes) {
        if (indexes != null && indexes.size() > 0) {
            Iterator<Index> it = indexes.iterator();
            while (it.hasNext()) {
                key = key + it.next().getModifierString(this);
            }
        }
        return key;
    }
    
    /** Returns data cached in the context via a keep statement.  
     */
    public Object getContextData(String name) throws Redirection {
        return getContextData(name, false);
    }
    
    public Holder getContextHolder(String name) throws Redirection {
        return (Holder) getContextData(name, true);
    }

    private Object getContextData(String name, boolean getHolder) throws Redirection {
        if (name == null || cache == null || keepMap == null) {
            return null;
        }
        Object data = null;
        Holder holder = null;
        String key = name;
        if (keepMap != null && keepMap.get(key) != null) {
            Pointer p = keepMap.get(key);
            if (!p.persist) {
                return null;
            }

            // Problem: the cache map stored in the pointer might no longer be
            // valid, depending on its scope and what has happened since the pointer
            // was created.
            //
            // So, this is what we have to do: Instead of storing the map
            // directly, we store the def name of the entry where it's locally
            // cached and the key it's cached under.  
            
            Map<String, Object> keepTable = p.cache;
            data = keepTable.get(p.getKey());

            if (data instanceof Pointer) {
                int i = 0;
                do {
                    p = (Pointer) data;
                    data = p.cache.get(p.getKey());
                    if (data instanceof Holder) {
                        holder = (Holder) data;
                        data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                    } else {
                        holder = null;
                    }
                    i++;
                    if (i >= MAX_POINTER_CHAIN_LENGTH) {
                        throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                    }
                } while (data instanceof Pointer);
            } else if (data instanceof Holder) {
                holder = (Holder) data;
                data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
            }
        }
    
        return getHolder ? holder : data;
    }
    
    /** Returns the definition associated with cached data which is the same or the 
     *  equivalent of the specified definition in the current context, or null if there is none.
     */
    
    public Definition getCachedDefinition(Definition def, ArgumentList args) throws Redirection {
        String name = def.getName();
        String fullName = def.getFullNameInContext(this);
        Definition defInCache = getDefinition(name, fullName, args);
        if (defInCache == null) {
            Definition defOwner = def.getOwner();
            int numUnpushes = 0;
            try {
                for (Definition topDef = topEntry.def; topDef != defOwner && size() > 1; topDef = topEntry.def) {
                    unpush();
                    numUnpushes++;
                }
                if (numUnpushes > 0) {
                    defInCache = getDefinition(name, fullName, args);
                }
            } catch (Throwable t) {
                String message = "Unable to find definition in cache for array " + name + ": " + t.toString();
                vlog(message);
               
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
            }
        }
        return defInCache;
    }
    
    
    /** Returns the cached definition holder associated with the specified definition in the current context,
     *  or null if there is none.
     */
    
    public Holder getCachedHolderForDef(Definition def, ArgumentList args, List<Index> indexes) throws Redirection {
        String name = def.getName();
        String fullName = def.getFullNameInContext(this);
        Holder holder = getDefHolder(name, fullName, args, indexes, false);
        if (holder == null) {
            Definition defOwner = def.getOwner();
            int numUnpushes = 0;
            try {
                for (Definition topDef = topEntry.def; topDef != defOwner && size() > 1; topDef = topEntry.def) {
                    unpush();
                    numUnpushes++;
                }
                if (numUnpushes > 0) {
                    holder = getDefHolder(name, fullName, args, indexes, false);
                }
            } catch (Throwable t) {
                String message = "Unable to find holder in cache for array " + name + ": " + t.toString();
                vlog(message);
               
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
            }
        }
        return holder;
    }

    /** Returns the definition associated with cached data for a specified name
     *  in the current context, or null if there is none.
     */
    public Definition getDefinition(String name, String fullName, ArgumentList args) throws Redirection {
        if (topEntry == null || name == null || name.length() == 0) {
            return null;
        }
        //List keeps = addDynamicKeeps(name, args);
        Definition def = topEntry.getDefinition(name, makeGlobalKey(fullName), args);
        //removeDynamicKeeps(keeps);
        
        return def;
    }
    
    
    /** Returns a Holder containing the definition and arguments associated with cached data for a 
     *  specified name in the current context, or null if there is none.
     */
    synchronized public Holder getDefHolder(String name, String fullName, ArgumentList args, List<Index> indexes, boolean local) throws Redirection {
        if (topEntry == null || name == null || name.length() == 0) {
            return null;
        }
       
        // use indexes as part of the key otherwise a cached element may be confused with a cached array 
        String key = addIndexesToKey(name, indexes);
        List<String>[] keeps = addDynamicKeeps(key, args);
        Holder holder = topEntry.getDefHolder(key, makeGlobalKey(fullName), args, local);
        removeDynamicKeeps(keeps);
        
        if (holder == null) {
            holder = getContextHolder(name);
        }
        
        // if this is an identity, then use the definition of the passed argument, if available,
        // else the superdefinition instead so children etc. resolve to it
        if (holder != null && holder.nominalDef != null && holder.nominalDef.isIdentity()) {
            Construction arg = (args != null && args.size() > 0 ? args.get(0) : null);
            if (arg != null && arg instanceof Instantiation) {
                Instantiation argInstance = ((Instantiation) arg).getUltimateInstance(this);
                Definition argDef = argInstance.getDefinition(this);
                if (argDef != null && !argDef.getType().isPrimitive()) {
                    holder.def = argDef;
                    holder.args = argInstance.getArguments();
                }
            }
        }
        return holder;
    }

    public void putDefinition(Definition def, String name, ArgumentList args, List<Index> indexes) throws Redirection {
        putData(def, args, indexes, name, null); //AbstractNode.UNINSTANTIATED);
    }
    
    /** Caches data associated with the specified name
     *  in the current context.
     */
    public void putData(Definition def, ArgumentList args, List<Index> indexes, String name, Object data) throws Redirection {
        putData(def, args, def, args, indexes, name, data, null);
    }    
    /** Caches data associated with the specified name
     *  in the current context.
     */
    synchronized public void putData(Definition nominalDef, ArgumentList nominalArgs, Definition def, ArgumentList args, List<Index> indexes, String name, Object data, ResolvedInstance resolvedInstance) throws Redirection {
    	if (topEntry != null && name != null && name.length() > 0) {
    	    int maxCacheLevels = getMaxCacheLevels(nominalDef);
            List<String>[] keeps = addDynamicKeeps(name, args);

            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(name, indexes);
            topEntry.put(key, nominalDef, nominalArgs, def, args, this, data, resolvedInstance, maxCacheLevels);
            
//            // if an identity is being instantitated, cache this definition and data for the identity
//            if (def != null && instantiatedDef != null && instantiatedDef.isIdentity()) {
//                // but don't overwrite the argument definition with the parameter definition
//                if (!(def instanceof DefParameter)) {
//                    topEntry.put(instantiatedDef.getName(), null, def, args, this, data, maxCacheLevels);
//                }
//            }
            removeDynamicKeeps(keeps);
        }
    }

    /** Determine how far down the context stack to go looking to see if a value should
     *  be cached, whether by a keep statement or because the definition resides at
     *  that level.
     */
    private int getMaxCacheLevels(Definition def) {
        int levels = 0;
        if (def == null) {
            return -1;
        }
        ComplexDefinition scopeOwner = getComplexOwner(def);
        Context.Entry entry = topEntry;
        Definition entryDef = entry.def;
        boolean reachedScope = (scopeOwner == null || scopeOwner.equals(entryDef) || scopeOwner.isSubDefinition(entryDef));
        while (true) {
            levels++;
            entry = entry.link;
            if (entry == null) {
            	// may have been obtained by reflection or some other out-of-scope mechanism; don't try to
            	// cache beyond local level
            	if (!reachedScope) {
            		levels = 0;
            	}
                break;
            }
            if (reachedScope) {
                if (!entry.def.equals(entryDef)) {
                    break;
                }
            } else {
                entryDef = (NamedDefinition) entry.def;
                reachedScope = (scopeOwner.equals(entryDef) || 
                		        scopeOwner.isSubDefinition(entryDef) ||
                                (scopeOwner instanceof Site && 
                                        (entry.hasSiteCacheEntryFor(def) || entry.isAdopted(scopeOwner.getNameNode()))));
            }
        }
        
        return levels;
    }
    
    private static ComplexDefinition getComplexOwner(Definition def) {
        Definition owner = def.getOwner();
        while (owner != null) {
            if (owner instanceof ComplexDefinition) {
                return (ComplexDefinition) owner;
            }
            owner = owner.getOwner();
        }
        return null;
    }

    /** Checks to see if a name corresponds to a parameter, and if so returns
     *  the parameter type, otherwise null.
     */
    public Type getParameterType(NameNode node, boolean inContainer) {
        if (topEntry == null) {
            return null;
        }
        Type paramType = null;
        if (inContainer) {
            synchronized (this) {
                int i = 0;
                try {
                    while (topEntry != null) {
                        paramType = getParameterType(node, false);
                        if (paramType != null || topEntry.getPrevious() == null) {
                            break;
                        }
                        unpush();
                        i++;
                    }
                } finally {
                    while (i > 0) {
                        repush();
                        i--;
                    }
                }
            }
        } else if (node.numParts() > 1) {
        	try {
                Definition paramDef = getParameterDefinition(node, false);
                if (paramDef != null) {
                    paramType = paramDef.getType();
                }
        	} catch (Redirection r) {
        		;
            }
        	
        } else {
            String name = node.getName();
            int numParams = topEntry.params.size();
            for (int i = numParams - 1; i >= 0; i--) {
                DefParameter param = topEntry.params.get(i);
                String paramName = param.getName();
                if (name.equals(paramName)) {
                    paramType = param.getType();
                }
            }
        }
        // Warning: this might not work on multidimensional arrays that have fewer
        // indexes than dimenstions
        if (paramType != null && paramType.isCollection() && node.hasIndexes()) {
            Definition def = paramType.getDefinition();
            if (def instanceof CollectionDefinition) {
            	paramType = ((CollectionDefinition) def).getElementType();
            }
        }
        return paramType;
    }

    /** Returns true if a parameter of the specified name is present at the top of the
     *  context stack.
     */
    public boolean paramIsPresent(NameNode nameNode) {
        if (topEntry != null) {
            return topEntry.paramIsPresent(nameNode, true);
        } else {
            return false;
        }
    }


    /** Returns true if this is the instantiation of a child of a parameter at the
     *  top of the context stack.
     */
    public boolean isParameterChildDefinition(NameNode node) {
        if (node == null) {
            return false;
        }
        String name  = node.getName();
        if (topEntry == null) {
            return false;
        }
        int numParams = topEntry.params.size();
        for (int i = numParams - 1; i >= 0; i--) {
            DefParameter param = topEntry.params.get(i);
            String paramName = param.getName();
            if (name.startsWith(paramName + '.')) {
                return true;
            }
        }
        return false;
    }

    public Object getArgumentForParameter(NameNode name, boolean checkForChild, boolean inContainer) throws Redirection {
        if (topEntry == null || topEntry == rootEntry) {
            return null;
        }
        
        Entry entry = topEntry;
        
        if (inContainer) {
            while (!entry.paramIsPresent(name, true)) {
                if (entry.link == null || entry.link == rootEntry) {
                    break;
                }
                entry = entry.link;
            }
        }
        
        String checkName  = name.getName();
        ArgumentList args = entry.args;
        int numArgs = args.size();
        ParameterList params = entry.params;
        int numParams = params.size();

        DefParameter param = null;
        Construction arg = null;
        int i;
        int n = (numParams > numArgs ? numArgs : numParams);
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }
        
        if (arg != null && entry.args.isDynamic()) {
            ArgumentList argHolder = new ArgumentList(true);
            argHolder.add(arg);
            return argHolder;
        } else {
            return arg;
        }
    }

    
    public Object getParameterInstance(NameNode name, boolean checkForChild, boolean inContainer, Definition argOwner) throws Redirection {
        Entry entry = topEntry;
        int numUnpushes = 0;
        while (!entry.def.equalsOrExtends(argOwner)) {
            if (entry.link == null || entry.link == rootEntry) {
                numUnpushes = 0;
                break;
            }
            numUnpushes++;
            entry = entry.link;
        }
        
        try {
            for (int i = 0; i < numUnpushes; i++) {
                unpush();
            }
            return getParameterInstance(name, checkForChild, inContainer);            
        } finally {
            while (numUnpushes-- > 0) {
                repush();
            }
        }
    }
    
    
    /** Checks to see if a name corresponds to a parameter, and if so returns the instance
     *  or, if the instantiate flag is true, instantiates it and returns the generated data.
     */
    public Object getParameterInstance(NameNode name, boolean checkForChild, boolean inContainer) throws Redirection {
//        if (checkForChild) {
//            return getParameter(name, inContainer, Object.class);
//        }            
        
        if (topEntry == null || topEntry == rootEntry) {
            return null;
        }
        
        Entry entry = topEntry;
        int numUnpushes = 0;

        if (inContainer) {
            while (!entry.paramIsPresent(name, false)) {
                if (entry.link == null || entry.link == rootEntry) {
                    numUnpushes = 0;
                    break;
                }
                numUnpushes++;
                entry = entry.link;
            }
        }
        
        String checkName  = name.getName();
        ArgumentList args = entry.args;
        int numArgs = args.size();
        ParameterList params = entry.params;
        int numParams = params.size();

        DefParameter param = null;
        Object arg = null;
        int i;
        int n = (numParams > numArgs ? numArgs : numParams);
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }

        if (arg == null || arg == ArgumentList.MISSING_ARG) {
            return null;

        } else {
            try {
            	Object data = null;

            	for (i = 0; i < numUnpushes; i++) {
                    unpush();
                }

                Context resolutionContext = (arg instanceof ResolvedInstance ? ((ResolvedInstance) arg).getResolutionContext() : this);
                
                if (checkForChild) {
                    // the child consists of everything past the first dot, which is the
                    // same as a complex name consisting of every node in the name
                    // except for the first
                    List<Index> indexes = ((NameNode) name.getChild(0)).getIndexes();
                    ComplexName childName = new ComplexName(name, 1, name.getNumChildren());
                    data = instantiateParameterChild(childName, param, arg, indexes);
        
                } else {
                    data = instantiateParameter(param, arg, name);
                }
                return data;
            
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
            }
        }
    }


    private Object instantiateParameter(DefParameter param, Object arg, NameNode argName) throws Redirection {
        Object data = null;
        ArgumentList argArgs = argName.getArguments();
        List<Index> indexes = argName.getIndexes();
        int numUnpushes = 0;
        boolean pushedOwner = false;
        if (arg instanceof AbstractNode) {
            Definition argOwner = ((AbstractNode) arg).getOwner();
            Entry entry = topEntry;
            while (!entry.def.equalsOrExtends(argOwner)) {
                if (entry.link == null || entry.link == rootEntry) {
                    numUnpushes = (size > 1 && !param.isInFor() ? 1 : 0);
                    break;
                }
                numUnpushes++;
                entry = entry.link;
            }
        }
        numUnpushes = Math.max((size > 1 && !param.isInFor() ? 1 : 0), numUnpushes);

        if (arg instanceof Instantiation) {
            Instantiation argInstance = (Instantiation) arg;
            boolean inContainer = argInstance.isContainerParameter(this);      
            boolean isParam = argInstance.isParameterKind();
            BentoNode argRef = argInstance.getReference();
            Definition argDef = null;

            // handle parameters which reference parameters in their containers
            if (argRef instanceof NameNode && isParam) {
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
                Context resolutionContext = (arg instanceof ResolvedInstance ? ((ResolvedInstance) arg).getResolutionContext() : this);
                try {
                    argDef = argInstance.getDefinition(this);
                    data = resolutionContext.getParameterInstance((NameNode) argRef, argInstance.isParamChild, inContainer);
                    if (argDef != null) {
                        String key = argInstance.getName();
                        // too expensive for a large loop
                        //if (argInstance.isForParameter()) {
                        //    key = key + addLoopModifier();
                        //}
                        putData(argDef, argArgs, argDef, argArgs, indexes, key, data, null);
                    }
                    
                } finally {
                    for (int i = 0; i < numUnpushes; i++) {
                        repush();
                    }
                }
                if (data != null) {
                    if (indexes != null) {
                        data = dereference(data, indexes);
                    }
                    return data;
                } else {
                    return NullValue.NULL_VALUE;
                }
            }
        }

        Definition argDef = null;
        boolean unpoppedArgDef = false;
        try {
            if (arg instanceof Definition) {
                if (arg instanceof ElementDefinition) {
                    Object element = ((ElementDefinition) arg).getElement();
                    if (element instanceof Definition) {
                        argDef = (Definition) element;
    
                    } else if (element instanceof Instantiation) {
                        Instantiation instance = (Instantiation) element;
                        arg = instance;
                        argDef = instance.getDefinition(this);
                        if (argDef != null && argArgs == null) {
                            argArgs = instance.getArguments();
                            if (instance.isSuper() && argArgs == null) {
                                argArgs = topEntry.args;
                            }
                        }
    
                    } else {
                        if (element instanceof Value) {
                            data = ((Value) element).getValue();
                        } else if (element instanceof Chunk) {
                            data = ((Chunk) element).getData(this);
                        } else if (element instanceof ValueGenerator) {
                            data = ((ValueGenerator) element).getData(this);
                        }  else {
                            data = element;
                            //throw new Redirection(Redirection.STANDARD_ERROR, "unrecognized element class: " + element.getClass().getName());
                        }
    
                        arg = element;
                    }
                } else {  // if (arg instanceof CollectionDefinition) {
                    argDef = (Definition) arg;
                }
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
    
            } else {
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
                try {
                    if (data == null) {
                        if (arg instanceof Instantiation) {
                            if (argArgs != null || indexes != null) {
                                for (int i = 0; i < numUnpushes; i++) {
                                    repush();
                                }
                                if (indexes != null) {
                                    indexes = instantiateIndexes(indexes);
                                }
                                if (argArgs != null) {
                                    argArgs = resolveArguments(argArgs);
                                }
                                for (int i = 0; i < numUnpushes; i++) {
                                    unpush();
                                }
                            }
                            if (arg instanceof ResolvedInstance) {
                            	ResolvedInstance ri = (ResolvedInstance) arg;
                            	if (argArgs != null && argArgs.size() > 0) {
                                    ri.setArguments(argArgs);
                                }
                                data = ri.getData(ri.getResolutionContext());
                            } else {
                                Instantiation instance = (Instantiation) arg;
                                if (instance.isSuper() && argArgs == null) {
                                    argArgs = topEntry.args;
                                }
                                if (argArgs != null || indexes != null) {
                                    instance = new Instantiation(instance, argArgs, indexes);
                                    indexes = null;
                                }
                                
                                data = instance.getData(this);
                            }
                    		
                    	} else if (arg instanceof PrimitiveValue) {
                            data = arg; //((PrimitiveValue) arg).getValue();
                        	
                    	} else if (arg instanceof Expression) {
                            data = ((ValueGenerator) arg).getData(this);
                    	
                    	} else if (arg instanceof Chunk) { 
                            argDef = param.getDefinitionFor(this, (Chunk) arg);
                            // there must be a better way to avoid this, but for now...
                            if (argDef == null && numUnpushes > 0) {
                                for (int i = 0; i < numUnpushes; i++) {
                                    repush();
                                }
                                numUnpushes = 0;
                                argDef = param.getDefinitionFor(this, (Chunk) arg);
                            }
                            if (argDef != null && arg instanceof Instantiation) {
                                Instantiation instance = (Instantiation) arg;
                                argArgs = instance.getArguments();
                                if (instance.isSuper() && argArgs == null) {
                                    argArgs = topEntry.args;
                                }
                            }

                        } else if (arg instanceof ValueGenerator) {
                            data = ((ValueGenerator) arg).getData(this);
    
                        } else {
                            data = arg;
                        }
    
                    	if (data != null) {
                            // if the name is indexed, and the argument is raw data, then get
                            // the appropriate item in the collection.  In such a case, the data
                            // must be of the appropriate type for the indexes.
                            if (indexes != null) {
                                data = dereference(data, indexes);
                            }
                        } else {
                            data = NullValue.NULL_VALUE;
                        }
    
                    }
                } finally {
                    ;
                }
            }

            if (argDef != null) {
                // this is to partly handle definitions returned from out of context, e.g. the
                // ones returned by descendants_of_type
                if (arg instanceof Definition) {
                    Definition argOwner = ((AbstractNode) arg).getOwner();
                    if (!topEntry.def.equalsOrExtends(argOwner)) {
                        push(argOwner, null, null, true);
                        pushedOwner = true;
                    }
                }

                if (!(argDef.isFormalParam())) {
                    unpop(argDef, argDef.getParamsForArgs(argArgs, this, false), argArgs);
                    unpoppedArgDef = true;
                }

                // if the name has one or more indexes, and the argument definition is a
                // collection definition, get the appropriate element in the collection.
                if (argDef instanceof CollectionDefinition && indexes != null) {
                    CollectionDefinition collectionDef = (CollectionDefinition) argDef;
                    argDef = collectionDef.getElementReference(this, argArgs, indexes);
                }
                data = constructDef(argDef, argArgs, indexes);
            }
        } finally {
            if (unpoppedArgDef) {
                repop();
            }

            if (pushedOwner) {
                pop();
            }
            
            for (int i = 0; i < numUnpushes; i++) {
                repush();
            }
        }
        return data;
    }

    private List<Index> instantiateIndexes(List<Index> indexes) throws Redirection {
        if (indexes == null || indexes.size() == 0) {
            return indexes;
        }
        List<Index> instantiatedIndexes = new ArrayList<Index>(indexes.size());
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext()) {
            Index index = it.next();
            Index instantiatedIndex = index.instantiateIndex(this);
            instantiatedIndexes.add(instantiatedIndex);
        }
        
        return instantiatedIndexes;
    }
    
    private List<Index> resolveIndexes(List<Index> indexes) throws Redirection {
        if (indexes == null || indexes.size() == 0) {
            return indexes;
        }
        List<Index> resolvedIndexes = new ArrayList<Index>(indexes.size());
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext()) {
            Index index = it.next();
            Index resolvedIndex = index.resolveIndex(this);
            resolvedIndexes.add(resolvedIndex);
        }
        
        return resolvedIndexes;
    }
    
    private ArgumentList resolveArguments(ArgumentList args) throws Redirection {
        if (args == null || args.size() == 0) {
            return args;
        }
        ArgumentList resolvedArgs = args;
        Context sharedContext = null;
        for (int i = 0; i < args.size(); i++) {
            Construction arg = args.get(i);
            if (arg instanceof Instantiation && !(arg instanceof ResolvedInstance)) {
                Instantiation argInstance = (Instantiation) arg;
                if (resolvedArgs == args) {
                    resolvedArgs = new ArgumentList(args);
                }
                ResolvedInstance ri = new ResolvedInstance(argInstance, this);
                resolvedArgs.set(i, ri);
            }
        }
        
        return resolvedArgs;
    }
    
    private Object instantiateParameterChild(ComplexName childName, DefParameter param, Object arg, List<Index> indexes) throws Redirection {
//if ("status".equals(childName.getName()))
        if (arg instanceof Value) {
        	Object val = ((Value) arg).getValue();
        	if (val instanceof BentoObjectWrapper) {
        		arg = val;
        	}
        }
    	Object data = null;
        Instantiation instance = (arg instanceof Instantiation ? (Instantiation) arg : null);
        int numUnpushes = 0;
        if (arg instanceof AbstractNode) {
            Definition argOwner = ((AbstractNode) arg).getOwner();
            Definition argOwnerOwner = (argOwner != null ? argOwner.getOwner() : null);
            Entry entry = topEntry;
            while (!entry.def.equalsOrExtends(argOwner) && !entry.def.equalsOrExtends(argOwnerOwner)) {
                if (entry.link == null || entry.link == rootEntry) {
                    numUnpushes = 0;
                    break;
                }
                numUnpushes++;
                entry = entry.link;
            }
        }

        Context fallbackContext = this;
        Definition argDef = null;

        numUnpushes = Math.max((size > 1 && !param.isInFor() ? 1 : 0), numUnpushes);

        try {
            for (int i = 0; i < numUnpushes; i++) {
                unpush();
            }
            
            if (arg instanceof Definition) {
                if (arg instanceof ElementDefinition) {
                    Object contents = ((ElementDefinition) arg).getContents();
                    if (contents instanceof Definition) {
                        argDef = (Definition) contents;
    
                    } else if (contents instanceof Instantiation) {
                        instance = (Instantiation) contents;
                        arg = instance;
                        argDef = instance.getDefinition(this);
                        if (instance instanceof ResolvedInstance) {
                            fallbackContext = ((ResolvedInstance) instance).getResolutionContext();
                        }
    
                    } else {
                        argDef = (Definition) arg;
                        arg = contents;
                    }
                } else {
                    argDef = (Definition) arg;
                }
            } else if (arg instanceof BentoObjectWrapper) {
            	BentoObjectWrapper wrapper = (BentoObjectWrapper) arg;
                // TODO: this doesn't handle children of children
            	data = wrapper.getChildData(childName.getName());
                
            } else {
                if (instance != null && instance.isParameterKind()) {
                    Context resolutionContext = this;
                    while (instance.isParameterKind()) {
                        if (instance instanceof ResolvedInstance) {
                            resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                        }
                        String checkName = instance.getName();
                        NameNode instanceName = instance.getReferenceName();
                        Entry entry = resolutionContext.topEntry;
                        if (instance.isContainerParameter(resolutionContext)) {
                            while (entry != null) {
                                if (entry.paramIsPresent(instanceName, true)) {
                                    break;
                                }
                                entry = entry.link;
                            }
                            if (entry == null) {
                                return null;
                            }
                        }
                        
                        ArgumentList args = entry.args;
                        int numArgs = args.size();
                        ParameterList params = entry.params;
                        int numParams = params.size();
      
                        DefParameter p = null;
                        Object a = null;
                        int i;
                        int n = (numParams > numArgs ? numArgs : numParams);
                        for (i = n - 1; i >= 0; i--) {
                            p = params.get(i);
                            String paramName = p.getName();
                            if (checkName.equals(paramName)) {
                                a = args.get(i);
                                break;
                            }
                        }
                        if (a == null) {
                            break;
                        }
                        
                        if (a instanceof Value) {
                            Object o = ((Value) a).getValue();
                            a = o;
                        }
                        
                        if (a instanceof Definition) {
                            if (a instanceof NamedDefinition && p.getType().isTypeOf("definition")) {
                                argDef = new AliasedDefinition((NamedDefinition) a, instance.getReferenceName());
                            } else {
                                argDef = (Definition) a;
                            }
                            break;
                        } else if (!(a instanceof Instantiation)) {
                            break;
                        }

                        instance = (Instantiation) a;
                       
                        arg = a;
                        param = p;
                        if (!p.isInFor()) {
                            unpush();
                            numUnpushes++;
                        }
                    }
                    if (instance.isParameterChild()) {
                        NameNode compName = new ComplexName(instance.getReferenceName(), childName);
                        data = resolutionContext.getParameter(compName, instance.isContainerParameter(resolutionContext), Object.class);
                        // trying to avoid multiple instantiation attempts, so commented this out.
                        //if (data == null || data == NullValue.NULL_VALUE) {
                        //    data = getParameter(compName, instance.isContainerParameter(this), Object.class);
                        //}
                    }
                    
                }
                
                if (data == null && argDef == null) {
                    if (arg instanceof Chunk) {
                        argDef = param.getDefinitionFor(this, (Chunk) arg);
                    
                        if (arg instanceof ResolvedInstance) {
                            fallbackContext = ((ResolvedInstance) arg).getResolutionContext();
                        }
                        
                    } else if (arg instanceof Map<?,?> && arg != null) {
                        String nm = childName.getName();
                        if (nm.equals("keys")) {
                        	Set<?> keySet = ((Map<?,?>) arg).keySet();
                        	List<String> keys = new ArrayList<String>(keySet.size());
                        	Iterator<?> it = keySet.iterator();
                        	while (it.hasNext()) {
                        	    keys.add(it.next().toString());
                        	}
                            data = keys;
                        	
                        } else {
                            data = ((Map<?,?>) arg).get(childName.getName());
                        }
                
                    } else {
                        data = arg;
                    }
                }
            }

            
            if (data != null) {
                // if the name is indexed, and the argument is raw data, then get
                // the appropriate item in the collection.  In such a case, the data
                // must be of the appropriate type for the indexes.
                if (indexes != null) {
                    data = dereference(data, indexes);
                }
                return data;
            } 

        } finally {
            // un-unpush if necessary
            for (int i = 0; i < numUnpushes; i++) {
                repush();
            }
        }

        if (argDef != null) {
            ArgumentList args = (instance != null ? instance.getArguments() : null);
            List<Index> argIndexes = (instance != null ? instance.getIndexes() : null);
            if (argDef.isIdentity() && (instance == null || !(instance instanceof ResolvedInstance))) {
                Holder holder = getDefHolder(argDef.getName(), argDef.getFullNameInContext(this), args, argIndexes, false);
                if (holder != null && holder.def != null && !holder.def.isIdentity()) {
                    argDef = holder.def;
                    args = holder.args;
                }
            }

            
            argDef = initDef(argDef, args, indexes);

//            
//           The following line replaces the commented out section following it.  The commented
//           out section in some cases tries twice to instantiate the child, first with this
//           context and second with the fallbackContext, in cases where the argument resolves  
//           to a ResolvedInstance, in which case the fallbackContext gets the value of  
//           the ResolvedInstance's resolution context.  The problem with this is that often
//           the null return value is intentional, and not caused by being unable to resolve            
//           the child reference.  So, now we are trying something different.  We will use the
//           ResolvedInstance's resolution context first and only, when it exists.  Since
//           fallbackContext is initialized to this, we can achieve this by simply using
//           falllbackContext (no longer aptly named), and not falling back to anything.            
//
            
            data = fallbackContext.instantiateArgChild(childName, param.getType(), argDef, args, null);
            
//          // commented out to fix Array Element Child Array test, because of
//          // a reference to a loop parameter.
//            
//          //  for (int i = 0; i < numUnpushes; i++) {
//          //      unpush();
//          //  }
//
//            data = instantiateArgChild(childName, param.getType(), argDef, args, null);
//          //  for (int i = 0; i < numUnpushes; i++) {
//          //      repush();
//          //  }
//
//            if ((data == null || data == NullValue.NULL_VALUE) && fallbackContext != this) {
//                data = fallbackContext.instantiateArgChild(childName, param.getType(), argDef, args, null);
//            }
        }
        
        return data;
    }


    private Object instantiateArgChild(ComplexName name, Type paramType, Definition def, ArgumentList args, List<Index> indexes) throws Redirection {

        int n = name.numParts();

        NameNode childName = name.getFirstPart();
        int numPushes = 0;

        try {
            // Keep track of intermediate definitions during alias dereferencing
            // by pushing them onto the context stack in case their parameters are
            // referenced in the child being instantiated.  Ensure however that
            // the original definition remains on top
            for (int i = 0; i < n - 1; i++) {
                if (def == null) {
                    break;
                }
                ParameterList params = def.getParamsForArgs(args, this);

                Definition childDef = null;

                if (def.isExternal()) {
                    push(def, params, args, false);
                    numPushes++;
                    childDef = def.getChildDefinition(childName, childName.getArguments(), childName.getIndexes(), null, this);

                } else {
                    push(def, params, args, false);
                    numPushes++;
                    childDef = def.getChildDefinition(childName, childName.getArguments(), childName.getIndexes(), null, this);
                    //pop();
                    //numPushes--;
                }
                numPushes += pushSupersAndAliases(def, args, childDef);
                def = childDef;
                if (def != null && childName != null) {
                    args = childName.getArguments();
                    def = initDef(def, childName.getArguments(), childName.getIndexes());
                }

                childName = (NameNode) name.getChild(i + 1);
            }

            return _instantiateArgChild(childName, paramType, def, args, indexes);

        } finally {
            while (numPushes-- > 0) {
                pop();
            }
        }
    }

    public int pushSupersAndAliases(Definition def, ArgumentList args, Definition childDef) throws Redirection {
        // track back through superdefinitions and aliases to push intermediate definitions
        if (childDef != null  /* && !isSpecialDefinition(childDef) */ ) {

            // find the complex owner of the child
            Definition childOwner = childDef.getOwner();
            while (childOwner != null && !(childOwner instanceof ComplexDefinition)) {
                childOwner = childOwner.getOwner();
            }
            if (childOwner == null) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Improperly initialized definition tree");
            }

            return pushSupersAndAliases((ComplexDefinition) childOwner, def, args);
        } else {
            return 0;
        }
    }

    public int pushSupersAndAliases(ComplexDefinition owner, Definition def, ArgumentList args) throws Redirection {
        Definition instantiatedDef = getContextDefinition(def);
        int numPushes = 0;
        ParameterList params = def.getParamsForArgs(args, this);
        Definition superdef = null;
        while (!def.equals(owner)) {
            push(instantiatedDef, params, args, false);
            numPushes++;

            Type st = def.getSuper(this);
            superdef = def.getSuperDefinition(this);

            // this doesn't completely work, because it misses
            // superclasses of intermediate aliases.  To really
            // handle this right, we need a flag for getChildDefinition
            // which prevents it from restoring the context, so
            // that none of the pushing here would be necessary.
            // Instead we would use a clone of the context, which
            // we could just throw away when we're done.
            if (def.isAliasInContext(this)) {
                Definition aliasDef = def;
                int numAliasPushes = 0;
                ArgumentList aliasArgs = args;
                ParameterList aliasParams = def.getParamsForArgs(args, this);
                while (aliasDef != null && aliasDef.isAliasInContext(this)) {
                    push(instantiatedDef, aliasParams, aliasArgs, false);
                    numAliasPushes++;
                    Instantiation aliasInstance = aliasDef.getAliasInstanceInContext(this);
                    aliasDef = (Definition) aliasInstance.getDefinition(this);  // lookup(this, false);
                    aliasArgs = aliasInstance.getArguments();  // getUltimateInstance(this).getArguments();
                    if (aliasDef != null) {
                        aliasParams = aliasDef.getParamsForArgs(aliasArgs, this);
                    }
                }
                if (aliasDef != null && aliasDef.equals(owner)) {
                    def = aliasDef;
                    args = aliasArgs;
                    params = aliasParams;
                    numPushes += numAliasPushes;
                    continue;
                } else {
                    while (numAliasPushes-- > 0) {
                        pop();
                    }
                }
            }
            if (st == null || superdef == null) {
                break;
            }
            def = superdef;
            args = st.getArguments(this);
            params = def.getParamsForArgs(args, this);
        }
        if (superdef != null) {
            push(instantiatedDef, superdef, params, args);
            numPushes++;
        }
  
        return numPushes;
    }

    synchronized private Object _instantiateArgChild(NameNode childName, Type paramType, Definition argDef, ArgumentList argArgs, List<Index> argIndexes) throws Redirection {
        Object data = null;
        boolean unpopped = false;
        int numPushes = 0;
        int numUnpushes = 0;

        // initialization dynamic objects such as collections initialized with
        // comprehensions or external methods
        if (argDef instanceof DynamicObject) {
            argDef = (Definition) ((DynamicObject) argDef).initForContext(this, argArgs, argIndexes);
        }

        try {

//            Definition argOwner = argDef.getOwner();
//            Entry entry = topEntry;
//            while (!entry.def.equalsOrExtends(argOwner)) {
//                if (entry.link == null || entry.link == rootEntry) {
//                    numUnpushes = 0;
//                    break;
//                }
//                numUnpushes++;
//                entry = entry.link;
//            }
//            for (int i = 0; i < numUnpushes; i++) {
//                unpush();
//            }

            while (argDef.isAliasInContext(this)) {
                ParameterList params = argDef.getParamsForArgs(argArgs, this);
                push(argDef, params, argArgs, false);
                numPushes++;
                Instantiation aliasInstance = argDef.getAliasInstanceInContext(this);  //.getUltimateInstance(this);
                Definition newDef = (Definition) aliasInstance.getDefinition(this);  // lookup(this, false);
                if (newDef == null) {
                    pop();
                    numPushes--;
                    break;
                } else {
                    argDef = newDef;
                }
                argArgs = aliasInstance.getArguments();
            }

            if (argDef != null) {
                
                if (argDef instanceof ElementReference) {
                    unpush();
                    numUnpushes++;
                    argDef = ((ElementReference) argDef).getElementDefinition(this);
                    repush();
                    numUnpushes--;
                    if (argDef == null) {
                        return null;
                    }
                }

                // if it's a NamedDefinition, but not an external definition, push the 
                // definition of the parameter onto the context in order to properly resolve 
                // any of its children which may be instantiated
                if (argDef instanceof NamedDefinition) { // && !argDef.isExternal()) {
    
                    // unpop the stack since the child's arguments have to be
                    // resolved where they are, not up at the level of its parent's
                    // referenced parameter.
                    push(argDef, argDef.getParamsForArgs(argArgs, this, false), argArgs);
                    numPushes++;
                }
                
                data = argDef.getChildData(childName, paramType, this, argArgs);
            }

        } finally {
            while (numPushes-- > 0) {
                pop();
            }
            while (numUnpushes-- > 0) {
            	repush();
            }
        }

        return data;
    }

    public Object getDescendant(Definition parentDef, ArgumentList args, NameNode name, boolean generate, Object parentObj) throws Redirection {
        Definition def = parentDef;

        // if this is a reference to a collection element, forward to its definition
        if (def instanceof ElementReference) {
            Definition elementDef = ((ElementReference) def).getElementDefinition(this);
            if (elementDef instanceof ElementDefinition) {
                // might have to fix the args and parentArgs here
                return ((ElementDefinition) elementDef).getChild(name, args, null, null, this, generate, true, parentObj); 
            }
        }
        
        Definition childDef = null;
        NameNode childName = name.getFirstPart();
        ArgumentList childArgs = childName.getArguments();
        List<Index> childIndexes = childName.getIndexes();
        int numPushes = 0;
        ComplexName restOfName = null;
        int numNameParts = name.numParts();
        if (numNameParts > 1) {
            restOfName = new ComplexName(name, 1, numNameParts);
        }
        
        try {
            // Keep track of intermediate definitions during alias dereferencing
            // by pushing them onto the context stack in case their parameters are
            // referenced in the child being instantiated.  Look for cached
            // definitions and arguments

            if (args == null && !(def instanceof AliasedDefinition)) {
                String nm = def.getName();
                String fullNm = def.getFullNameInContext(this);
                Holder holder = getDefHolder(nm, fullNm, null, null, false);
                if (holder != null && holder.nominalDef != null && holder.nominalDef.getDurability() != Definition.DYNAMIC && !((BentoNode) holder.nominalDef).isDynamic()) {
                    def = holder.nominalDef;
                    args = holder.nominalArgs;
                }
            }
            ParameterList params = def.getParamsForArgs(args, this);
            
            if (!def.isExternal() && (!def.isCollection() || parentObj == null)) {
                push(def, params, args, true);
                numPushes++;

                Definition superDef = def.getSuperDefinition(this);
                while (def.isAliasInContext(this) && !def.isCollection()) {
                    Instantiation aliasInstance = def.getAliasInstanceInContext(this);
                    if (def.isParamAlias()) {
                        aliasInstance = aliasInstance.getUltimateInstance(this);
                    }
                    NameNode aliasName = aliasInstance.getReferenceName();
                    if (aliasName.isComplex()) {
                        numPushes += pushParts(aliasInstance);
                    }
                    
                    ArgumentList aliasArgs = aliasInstance.getArguments();
                    List<Index> aliasIndexes = aliasInstance.getIndexes();
                    Definition aliasDef = aliasInstance.getDefinition(this, def);
                    if (aliasDef == null) {
                        break;
                    }
                    
                    // we are only interested in aliases in the same hierarchy
                    if (superDef != null && !aliasDef.equalsOrExtends(superDef)) {
                        break;
                    }
                    
                    def = aliasDef;
                    args = aliasArgs;
                    if (args == null && aliasIndexes == null) {
                        String nm = aliasInstance.getName();
                        String fullNm = parentDef.getFullNameInContext(this) + "." + nm;
                        Holder holder = getDefHolder(nm, fullNm, null, null, false);
                        if (holder != null && holder.nominalDef != null && holder.nominalDef.getDurability() != Definition.DYNAMIC && !((BentoNode) holder.nominalDef).isDynamic()) {
                            def = holder.nominalDef;
                            args = holder.nominalArgs;
                        }
                    }
                    params = def.getParamsForArgs(args, this);
                    
                    push(def, params, args, false); //true);
                    numPushes++;
                }
            }

            if (childIndexes == null) {
                String nm = childName.getName();
                String fullNm = parentDef.getFullNameInContext(this) + "." + nm;
                Holder holder = getDefHolder(nm, fullNm, childArgs, childIndexes, false);
                if (holder != null) {
                    Definition nominalDef = holder.nominalDef;
                    if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.DYNAMIC) { 
                        if (nominalDef.isIdentity()) {
                            childDef = holder.def;
                            if (childArgs == null) {
                                childArgs = holder.args;
                            }
                        } else {
                            childDef = nominalDef;
                            if (childArgs == null) {
                                childArgs = holder.nominalArgs;
                            }
                        }
                        // this should work, but doesn't seem to for
                        // cached aliased parameters and nested cached identities
                        //if (generate && holder.data != null) {
                        //    return holder.data;
                        //}
                    }
                }
            }
            
            if (childDef == null) {
                return def.getChild(name, name.getArguments(), name.getIndexes(), args, this, generate, true, parentObj);
            }

            DefinitionInstance childDefInstance = null;
            if (childDef != null) {
                if (generate && childName != null) {
                    childDef = initDef(childDef, childArgs, childName.getIndexes());
                }

                if (restOfName != null) {
                    if (generate) {
                        return getDescendant(childDef, childArgs, restOfName, generate, parentObj);
                    } else {
                        childDefInstance = (DefinitionInstance) getDescendant(childDef, childArgs, restOfName, generate, parentObj);
                    }
                }
            }            
            
            if (!generate) {
                //while (childDef.isAlias()) {
                //    Instantiation aliasInstance = childDef.getAliasInstance();
                //    childDef = aliasInstance.getDefinition(this, childDef);
                //}
                
                if (childDefInstance != null) {
                    return childDefInstance;
                } else if (childDef != null) {
                    return childDef.getDefInstance(childArgs, childIndexes);
                } else {
                    return null;
                }
            }
            
            if (childDefInstance != null) {
                childDef = childDefInstance.def;
            }

            if (childDef == null) {
                return AbstractNode.UNDEFINED;
                
            } else {
                return childDef.instantiate(childArgs, childName.getIndexes(), this);
            }

        } finally {
            while (numPushes-- > 0) {
                pop();
            }
        }
    }
            

    public Definition dereference(Definition def, ArgumentList args, List<Index> indexes) throws Redirection {
        if (indexes != null) {
            CollectionDefinition collectionDef = null;

            if (def instanceof CollectionDefinition) {
                collectionDef = (CollectionDefinition) def;

            } 
            
            Definition checkDef = def;
            ArgumentList checkArgs = args;
            while (collectionDef == null && checkDef != null) {
                int numAliasPushes = 0;
                ParameterList aliasParams = checkDef.getParamsForArgs(checkArgs, this);
                try {
                    Instantiation checkInstance = null;
                    while (checkDef != null && checkDef.isAliasInContext(this)) {
                        checkInstance = checkDef.getAliasInstanceInContext(this);
                        checkArgs = checkInstance.getArguments();    // getUltimateInstance(this).getArguments();
                        aliasParams = (checkDef != null ? checkDef.getParamsForArgs(checkArgs, this) : null);
                        push(checkDef, aliasParams, checkArgs, false);
                        numAliasPushes++;
                        checkDef = (Definition) checkInstance.getDefinition(this);  // lookup(context, false);
                    }
                    if (checkDef != null) {
                        if (checkDef instanceof CollectionDefinition) {
                            collectionDef = (CollectionDefinition) checkDef;
                            return collectionDef.getElementReference(this, args, indexes);
                        } else if (!(checkDef instanceof IndexedMethodDefinition)) {
                            ResolvedInstance instance = new ResolvedInstance(checkDef, this, checkArgs, null);
                            return new IndexedInstanceReference(instance, indexes);
                        } else {
                            checkDef = null;
                        }
                    }
                } finally {
                    while (numAliasPushes-- > 0) {
                        pop();
                    }
                }
            }
            
            if (collectionDef != null) {
                def = collectionDef.getElementReference(this, args, indexes);
            }
        }
        return def;
    }
    
    
    public Definition initDef(Definition def, ArgumentList args, List<Index> indexes) throws Redirection {
        if (def instanceof ExternalDefinition) {
            def = ((ExternalDefinition) def).getDefForContext(this, args);
        }

//        return dereference(def, args, indexes);
        
        // if the reference has one or more indexes, and the definition is a
        // collection definition, get the appropriate element in the collection.
        // Note: this fails when there is an index on an aliased collection definition
        if (indexes != null && indexes.size() > 0 && def.isCollection()) {
            CollectionDefinition collectionDef = def.getCollectionDefinition(this, args);

//            // if this is an alias, delegate to the aliased definition
//            if (collectionDef == null && def.isAlias()) {
//                int numPushes = 0;
//                try {
//                    Definition aliasDef = def;
//                    ArgumentList aliasArgs = args;
//                    ParameterList aliasParams = def.getParamsForArgs(args, this);
//                    while (collectionDef == null && aliasDef != null && aliasDef.isAlias()) {
//                        push(instantiatedDef, aliasParams, aliasArgs, false);
//                        numPushes++;
//                        Instantiation aliasInstance = (Instantiation) aliasDef.getContents();
//                        aliasDef = (Definition) aliasInstance.lookup(this, false);
//                        aliasArgs = aliasInstance.getArguments();
//                        if (aliasDef != null) {
//                            aliasParams = aliasDef.getParamsForArgs(aliasArgs, this);
//                            collectionDef = aliasDef.getCollectionDefinition();
//                        }
//                    }
//                } finally {
//                    while (numPushes-- > 0) {
//                        pop();
//                    }
//                }
//            }
            
            if (collectionDef != null) {
                indexes = resolveIndexes(indexes);
            	def = collectionDef.getElementReference(this, args, indexes);
            } else {
                def = null;
            }
        }

        return def;
    }

    public Object dereference(Object data, List<Index> indexes) throws Redirection {
        // dereference collections represented as values
        if (data instanceof Value) {
            data = ((Value) data).getValue();
            if (data == null) {
                return null;
            }
        }

        // dereference collections represented as BentoArray objects
        if (data instanceof BentoArray) {
            data = ((BentoArray) data).getArrayObject();
        }
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext() && data != null) {
            Index index = it.next();
            data = getElement(data, index);
        }
        return data;
    }

    private Object getElement(Object collection, Index index) throws Redirection {
        Object data = null;

        // this occurs with anonymous collections in the input
        if (collection instanceof CollectionDefinition) {
            collection = ((CollectionDefinition) collection).getCollectionInstance(this, null, null);
        }

        if (collection instanceof CollectionInstance) {
            collection = ((CollectionInstance) collection).getCollectionObject();
        }

        if (collection instanceof Value) {
            collection = ((Value) collection).getValue();

        } else if (collection instanceof ValueGenerator) {
            collection = ((ValueGenerator) collection).getData(this);
        }

        boolean isArray = collection.getClass().isArray();
        boolean isList = (collection instanceof List<?>);
        if (index instanceof TableIndex) {
            String key = index.getIndexValue(this).getString();
            if (isArray || isList) {
                // NOTE: the following is the comment accompanying the related logic
                // in the method getElement in ArrayDefinition:
                //
                //     retrieve the element which matches the index key value.  There
                //     are two ways an element can match the key:
                //
                //     -- if the element is a definition which owns a child named "key"
                //        compare its instantiated string value to the index key
                //
                //     -- if the element doesn't have such a "key" field, compare the
                //        string value of the element itself to the index key.
                //
                // The logic below is operating on an instantiated array, so it contains
                // instantiated elements, and as a result the element definitions are
                // not available.  Therefore only the second of the two methods
                // described above can be implemented.
                //
                // This inconsistency between the logic here and in ArrayDefinition
                // is a bug and needs to be corrected, preferably by finding a way to
                // put the logic in one place.

                if (key == null) {
                    return null;
                }
                int size = (isArray ? Array.getLength(collection) : ((List<?>) collection).size());
                int ix = -1;
                for (int i = 0; i < size; i++) {
                    Object element = (isArray ? Array.get(collection, i) : ((List<?>) collection).get(i));
                    try {
                        String elementKey;
                        if (element instanceof String) {
                            elementKey = (String) element;
                        } else if (element instanceof Value) {
                            elementKey = ((Value) element).getString();
                        } else if (element instanceof Chunk) {
                            elementKey = ((Chunk) element).getText(this);
                        } else if (element instanceof ValueGenerator) {
                            elementKey = ((ValueGenerator) element).getString(this);
                        } else {
                            elementKey = element.toString();
                        }

                        if (key.equals(elementKey)) {
                            ix = i;
                            break;
                        }

                    } catch (Redirection r) {
                        // don't redirect, we're only checking
                        continue;
                    }
                }
                data = new PrimitiveValue(ix);

            } else if (collection instanceof Map<?,?>) {
                data = ((Map<?,?>) collection).get(key);
            }
        } else {    // must be an ArrayIndex
            int i = index.getIndexValue(this).getInt();
            if (collection.getClass().isArray()) {
                data = Array.get(collection, i);

            } else if (collection instanceof List<?>) {
                data = ((List<?>) collection).get(i);

            } else if (collection instanceof Map<?,?>) {
                Object[] keys = ((Map<?,?>) collection).keySet().toArray();
                Arrays.sort(keys);
                data = ((Map<?,?>) collection).get(keys[i]);
            }
        }
        while (data instanceof Holder) {
            data = ((Holder) data).data;
        }
        if (data instanceof ElementDefinition) {
            data = ((ElementDefinition) data).getElement();
        }
        return data;
    }

    public Object constructDef(Definition definition, ArgumentList args, List<Index> indexes) throws Redirection {
        // initialization expressions
        if (definition instanceof DynamicObject) {
            definition = (Definition) ((DynamicObject) definition).initForContext(this, args, indexes);
        }

        Logger.logInstantiation(this, definition);

        if (definition instanceof CollectionDefinition) {
            CollectionInstance collection = ((CollectionDefinition) definition).getCollectionInstance(this, args, indexes);
            return collection.getCollectionObject();

        } else {
            return construct(definition, args);
        }
    }

    /** Gets the definition associated with the parameter at the nth position
     *  in this context's parameter list.
     */
    private Definition getParameterDefinitionAt(int n) {
        if (topEntry == null) {
            return null;
        }
        Entry entry = topEntry;
        Definition argDef = null;
        Object arg = entry.args.get(n);
        if (arg instanceof Definition) {
            argDef = (Definition) arg;
        } else if (topEntry.link != null) {
            // temporarily pop the stack to match the context of the
            // argument in lookup operations
            unpush();
            DefParameter param = entry.params.get(n);
            argDef = param.getDefinitionFor(this, arg);
            // unpop the stack to restore the proper context for the parameter
            repush();
        }

        return argDef;
    }

    /** Checks to see if a name corresponds to a parameter, and if so returns
     *  the definition associated with it (i.e., the argument passed as the
     *  parameter's value).
     */
    public Definition getParameterDefinition(NameNode name, boolean inContainer) throws Redirection {
        return (Definition) getParameter(name, inContainer, Definition.class);
    }
            
    public Entry getParameterEntry(NameNode name, boolean inContainer) throws Redirection {
        return (Entry) getParameter(name, inContainer, Entry.class);
    }
    
    public Object getParameter(NameNode name, boolean inContainer, Class returnClass) throws Redirection {        
        if (topEntry == null) {
            return null;
        }
        Entry entry = topEntry;

        if (inContainer) {
            Object paramObj = null;
            synchronized (this) {
                int i = 0;
                while (topEntry != null) {
                    paramObj = getParameter(name, false, returnClass);
                    if (paramObj != null || topEntry.getPrevious() == null) {
                        break;
                    }
                    unpush();
                    i++;
                }
                while (i > 0) {
                    repush();
                    i--;
                }
            }
            return paramObj;

        //} else if (name.getOwner() != null && !topEntry.def.equalsOrExtends(name.getOwner()) && name.hasArguments()) {
        //    vlog("param owner: " + name.getOwner().getFullName() + ", topEntry: " + topEntry.def.getFullName()); 
        }

        boolean checkForChild = (name.numParts() > 1);
        String checkName  = name.getName();
        ArgumentList args = entry.args;
        int numArgs = args.size();
        ParameterList params = entry.params;
        int numParams = params.size();
        
        ArgumentList argArgs = null;
        ParameterList argParams = null;

        Object arg = null;
        int n = (numParams > numArgs ? numArgs : numParams);
        boolean mustUnpush = false;
        DefParameter param = null;
        Type paramType = null;

        int i;
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }
        
        if (arg == null) {
            return null;
        }

        Definition argDef = null;
        
        // for loop arguments are in the same context, not the next higher context
        if (!param.isInFor() && size > 1) {
            mustUnpush = true;
            unpush();
        }

        int numPushes = 0;
        Instantiation argInstance = null;
        try {
            if (arg instanceof Definition) {
                argDef = (Definition) arg;
            } else if (arg != ArgumentList.MISSING_ARG) {
                argDef = param.getDefinitionFor(this, arg);
                if (arg instanceof Instantiation && argDef != null) {
                    argInstance = (Instantiation) arg;
                    argArgs = argInstance.getArguments();
                    if (argInstance.isSuper() && argArgs == null) {
                        argArgs = topEntry.args;
                    }
                    argParams = argDef.getParamsForArgs(argArgs, this);
                    
                    numPushes += pushParts(argInstance);
                }
            }
            
            if (argDef == null) {
                return null;
            }

            push(argDef, argParams, argArgs);
            numPushes++;
            
            paramType = param.getType();

            
//                // if the argument references a parameter in the container, recursively
//                // resolve it
//                if (argDef.isFormalParam()) {
//                    // get the corresponding argument.  Push the original
//                    // context back on the stack because the parameter lookup
//                    // will pop it again.
//                    if (mustUnpush) {
//                        repush();
//                    }
//                    Definition paramDef = getParameterDefinitionAt(i);
//                    argDef = paramDef;
//
//                    if (mustUnpush) {
//                        unpush();
//                    }
//                } else 

            Definition lastDef = null;
            // don't dereference global definitions, or we might miss a globally cached value
            while (!(arg instanceof ResolvedInstance) && argDef != lastDef && !argDef.isGlobal() && argDef.isAliasInContext(this)) {
                lastDef = argDef;
                NameNode alias = argDef.isParamAlias() ? argDef.getParamAlias() : argDef.getAliasInContext(this);
                if (alias == null) {
                    break;
                }
                ArgumentList aliasArgs = alias.getArguments();
                Instantiation aliasInstance = argDef.getAliasInstanceInContext(this);
                numPushes += pushParts(aliasInstance);
                
                Context.Entry aliasEntry = getParameterEntry(alias, aliasInstance.isContainerParameter(this));
                if (aliasEntry == null) {
                    List<Index> aliasIndexes = alias.getIndexes();
                    for (Definition owner = argDef.getOwner(); owner != null; owner = owner.getOwner()) {
                        owner = getSubdefinitionInContext(owner);
                        DefinitionInstance aliasDefInstance = (DefinitionInstance) owner.getChild(alias, aliasArgs, aliasIndexes, args, this, false, true, null);
                        Definition aliasDef = (aliasDefInstance == null ? null : aliasDefInstance.def);
                        if (aliasDef != null) {
                            argDef = aliasDef;
                            argArgs = aliasArgs;
                            argParams = argDef.getParamsForArgs(argArgs, this);
                            push(argDef, argParams, argArgs);
                            numPushes++;
                            break;
                        }
                    }
                } else {
                    argDef = aliasEntry.def;
                    argArgs = aliasEntry.args;
                    argParams = aliasEntry.params;
                    push(aliasEntry);
                    numPushes++;
                }
                
            }

            // dereference the argument definition if the reference includes indexes
            NameNode paramNameNode = (checkForChild ? (NameNode) name.getChild(0) : name);
            ArgumentList paramArgs = paramNameNode.getArguments();
            List<Index> paramIndexes = paramNameNode.getIndexes();
            if ((paramArgs != null && paramArgs.size() > 0) || (paramIndexes != null && paramIndexes.size() > 0)) {
                if (mustUnpush) {
                    repush();
                }
                argDef = initDef(argDef, paramArgs, paramIndexes);
                if (mustUnpush) {
                    unpush();
                }
            }
            
            // if this is a child of a parameter, resolve it.
            if (checkForChild && argDef != null) {

                // the child consists of everything past the first dot, which is the
                // same as a complex name consisting of every node in the name
                // except for the first
                ComplexName childName = new ComplexName(name, 1, name.getNumChildren());

                // see if the argument definition has a child definition by that name
                ArgumentList childArgs = childName.getArguments();
                Definition childDef = argDef.getChildDefinition(childName, childArgs, childName.getIndexes(), args, this);

                // if not, then look for an aliased external definition
                if (childDef == null) {
                    if (argDef.isAlias()) {
                        NameNode aliasName = argDef.getAlias();
                        childName = new ComplexName(aliasName, childName);
                        NamedDefinition ndef = (NamedDefinition) peek().def;
                        childDef = ExternalDefinition.createForName(ndef, childName, param.getType(), argDef.getAccess(), argDef.getDurability(), this);
                    }

                    // if that didn't work, look for a special definition child
                    if (childDef == null) {
                        if (paramType != null && paramType.getName().equals("definition")) {
                            childDef = ((AnonymousDefinition) argDef).getDefinitionChild(childName, this, args);

                     //   } else {
                     //       String cName = childName.getName();
                     //       if (cName.equals("defs") || cName.equals("descendants_of_type") || cName.equals("full_name")) {
                     //           ExternalDefinition externalDef = new ExternalDefinition("bento.lang.AnonymousDefinition", argDef, argDef, paramType, argDef.getAccess(), argDef.getDurability(), this, null);
                     //           childDef = externalDef.getExternalChildDefinition(childName, this);
                     //       }
                        }
                    }
                    
                } else {
                    childDef = childDef.getUltimateDefinition(this);
                }
                argDef = childDef;

                if (arg instanceof Value && ((Value) arg).getValueClass().equals(BentoObjectWrapper.class)) {
                	BentoObjectWrapper wrapper = (BentoObjectWrapper) ((Value) arg).getValue();
                	Context argContext = wrapper.context;
                	argDef = new BoundDefinition(argDef, argContext);
                }
                
                //if (childDef != null) {
                //    argArgs = childName.getArguments();
                //    argParams = childDef.getParamsForArgs(argArgs, this);
                //} else {
                //    argArgs = null;
                //    argParams = null;
                //}
            }

            if (returnClass == Entry.class) {
                if (argDef == null) {
                    return null;
                } else {
                    return newEntry(argDef, argDef, argParams, argArgs);
                }
            } else if (returnClass == Definition.class) {
                //if (argArgs != null && !(argDef instanceof BoundDefinition)) {
                //    argDef = new BoundDefinition(argDef, this);
                //}
                return argDef;
            } else {
                Object data = (argDef == null ? null : construct(argDef, argArgs));
                if (data == null) {
                    return NullValue.NULL_VALUE;
                } else {
                    return data;
                }
            }

        } finally {
            // restore the stack
            while (numPushes-- > 0) {
                pop();
            }
            if (mustUnpush) {
                repush();
            }
        }
    }

    public Definition getSubdefinitionInContext(Definition def) {
        Definition subdef = def;
        for (Entry entry = topEntry; entry != null; entry = entry.link) {
            if (entry.def.equalsOrExtends(def)) {
                subdef = entry.def;
                break;
            }
        }
        
        return subdef;
    }
    
    /** Returns a copy of the passed argument list with any arguments that 
     *  reference parameters in this context replaced by the arguments referenced 
     *  by those parameters.  If no arguments reference parameters, the original
     *  argument list is returned. 
     */
    public ArgumentList getUltimateArgs(ArgumentList args) throws Redirection {
        if (args == null) {
            return null;
        }
        
        ArgumentList newArgs = null;
        Iterator<Construction> it = args.iterator();
        int n = 0;
        while (it.hasNext()) {
            Construction arg = it.next();
            if (arg instanceof Instantiation) {
                Instantiation argInstance = (Instantiation) arg;
                if (argInstance.isParameterKind()) {
                    if (newArgs == null) {
                        newArgs = new ArgumentList(args);
                    }
                    Construction newArg = null;
                    Object obj = getArgumentForParameter(argInstance.getReferenceName(), argInstance.isParameterChild(), argInstance.isContainerParameter(this));
                    if (obj != null) {
                        if (obj instanceof ArgumentList) {
                            ArgumentList argHolder = (ArgumentList) obj;
                            newArg = argHolder.get(0);
                            if (argHolder.isDynamic()) {
                                newArgs.setDynamic(true);
                            }
                        } else {
                            newArg = (Construction) obj;
                        }
                        newArgs.set(n, newArg);
                    }
                }
            }
            n++;
        }
        if (newArgs != null) {
            return newArgs;
        } else {
            return args;
        }
    }
    
    

    public int pushParts(Instantiation instance) throws Redirection {
        if (instance != null) {
            NameNode nameNode = instance.getReferenceName();
            if (nameNode != null) {
                if (nameNode.isComplex()) {
                    Context resolutionContext = this;
                    //if (instance instanceof ResolvedInstance) {
                    //    resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                    //}
                    int num = nameNode.numParts();
                    return resolutionContext.pushParts(nameNode, num - 1, instance.getOwner());
                }
            }
        }
        return 0;
    }
    
    public int pushParts(NameNode nameNode, int numParts, Definition owner) throws Redirection {
        int numPushes = 0;
        try {
            for (int part = 0; part < numParts; part++) {
                NameNode partName = nameNode.getPart(part);
                Definition partDef = null;
                
                List<Index> partIndexes = partName.getIndexes();
                ArgumentList partArgs = partName.getArguments();
                if (partIndexes == null || partIndexes.size() == 0) {
                    String nm = partName.getName();
                    String fullNm = owner.getFullNameInContext(this) + "." + nm;
                    Holder holder = getDefHolder(nm, fullNm, partArgs, partIndexes, false);
                    if (holder != null) {
                        Definition nominalDef = holder.nominalDef;
                        if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.DYNAMIC) { 
                            if (nominalDef.isIdentity()) {
                                partDef = holder.def;
                                if (partArgs == null) {
                                    partArgs = holder.args;
                                }
                            } else {
                                partDef = nominalDef;
                                if (partArgs == null) {
                                    partArgs = holder.nominalArgs;
                                }
                            }
                        }
                    }
                }
                
                Instantiation partInstance = new Instantiation(partName, owner);
                partInstance.setKind(getParameterKind(partName.getName()));
                
                if (partDef == null) {
                    partDef = partInstance.getDefinition(this);
                    if (partDef == null) {
                        break;
                    }
                }
                if (partInstance.isParameterKind()) {
                    Entry partEntry = getParameterEntry(partInstance.getReferenceName(), partInstance.isContainerParameter(this));
                    push(partEntry);
                } else {
                    ParameterList partParams = partDef.getParamsForArgs(partArgs, this, false);
                    push(partDef, partParams, partArgs, false);
                }
                numPushes++;
                while (partDef.isAliasInContext(this) && !partDef.isIdentity()) {
                    partInstance = partDef.getAliasInstanceInContext(this);
                    if (partInstance.isParameterKind()) {
                        Entry partEntry = getParameterEntry(partInstance.getReferenceName(), partInstance.isContainerParameter(this));
                        partDef = partEntry.def;
                        if (partDef == null) {
                            break;
                        }
                        push(partEntry);
                    } else {
                        partDef = partInstance.getDefinition(this);
                        if (partDef == null) {
                            break;
                        }
                        partArgs = partInstance.getArguments();
                        ParameterList partParams = partDef.getParamsForArgs(partArgs, this);
                        push(partDef, partParams, partArgs, false);
                    }
                    numPushes++;
                }
            }
        } finally {
           ;
        }
        return numPushes;
    }

    public int getParameterKind(String name) {
        int kind = Instantiation.UNRESOLVED;
        boolean isChild = (name.indexOf('.') > 0);        
        DefParameter param = topEntry.getParam(name);
        
        if (param != null) {
            if (param.isInFor()) {
                return (isChild ? Instantiation.FOR_PARAMETER_CHILD : Instantiation.FOR_PARAMETER);
            } else {
                return (isChild ? Instantiation.PARAMETER_CHILD : Instantiation.PARAMETER);
            }
        }
        
        for (Entry entry = topEntry.getPrevious(); entry != null; entry = entry.getPrevious()) {
            param = entry.getParam(name);
            if (param != null) {
                return (isChild ? Instantiation.CONTAINER_PARAMETER_CHILD : Instantiation.CONTAINER_PARAMETER);
            }
        }
        return kind;
    }
    
//    private boolean isSuperDef(NamedDefinition def, NamedDefinition subDef) {
//        for (NamedDefinition superDef = subDef.getSuperDefinition(); superDef != null; superDef = superDef.getSuperDefinition()) {
//            if (superDef.equals(def)) {
//                return true;
//            }
//        }
//        return false;
//    }


    public int size() {
        return size;
    }

    public void push(Definition def, ParameterList params, ArgumentList args) throws Redirection {
        Definition contextDef = getContextDefinition(def);
        Entry entry = newEntry(contextDef, contextDef, params, args);
        push(entry);
    }

    public void push(Definition def, ParameterList params, ArgumentList args, boolean newFrame) throws Redirection {
        Definition contextDef = getContextDefinition(def);
        Definition superdef = (newFrame ? null : contextDef);
        Entry entry = newEntry(contextDef, superdef, params, args);
        push(entry);
    }

    public void push(Definition instantiatedDef, Definition superdef, ParameterList params, ArgumentList args) throws Redirection {
//        Entry entry = newEntry(getContextDefinition(instantiatedDef), superdef, params, args);
        Entry entry = newEntry(getContextDefinition(instantiatedDef), getContextDefinition(superdef), params, args);
        push(entry);
    }

    private void push(Entry entry) throws Redirection {
        boolean newFrame = (entry.superdef == null);
        boolean newScope = (entry.def != entry.superdef);

        if (entry.def instanceof Site) {
            // if we are pushing a site, share the cache from the
            // root entry
            if (rootEntry != null && !entry.equals(rootEntry)) {
                entry.cache = rootEntry.cache;
            }
            
        } else {
            Site entrySite = entry.def.getSite();
            if (entrySite != null && !(entrySite instanceof Core) && topEntry != null) {
                Site currentSite = topEntry.def.getSite();
                if (!entrySite.equals(currentSite)) {
                    Map<String, Object> siteCache = siteCaches.get(entrySite.getName());
                    if (siteCache == null) {
                    	siteCache = newHashMap(Object.class);
                    	siteCaches.put(entrySite.getName(), siteCache);
                    }
                    entry.setSiteCache(siteCache);
                }
            }
        }
        
//        ArgumentList args = entry.args;
        stateCount = stateFactory.nextState();
        entry.setState(stateCount);
        _push(entry);
        if (entry.def instanceof NamedDefinition) {
            definingDef = entry.def;
            
            // add keep and cache directives to this entry's list.
            NamedDefinition scopedef = (NamedDefinition) ((entry.superdef == null && entry.def instanceof NamedDefinition) ? entry.def : entry.superdef);
            Entry prev = entry.link;
            if (!newFrame && prev != null) {
                if (sharesKeeps(entry, prev, true)) {
                    setKeepsFromEntry(prev);
                    setInsertsFromEntry(prev);
                } else {
                    List<KeepStatement> keeps = scopedef.getKeeps();
                    if (keeps != null) {
                        Iterator<KeepStatement> it = keeps.iterator();
                        while (it.hasNext()) {
                            KeepStatement k = it.next();
                            try {
                                keep(k);
                            } catch (Redirection r) {
                                vlog("Error in keep statement: " + r.getMessage());
                                throw r;
                            }
                        }
                        
                        String keepCacheKey = scopedef.getName() + ".keep";
                        String globalKeepCacheKey = makeGlobalKey(scopedef.getFullNameInContext(this)) + ".keep";
                        while (prev != null) {
                            Object keepCache = prev.get(keepCacheKey, globalKeepCacheKey, null, true);
                            if (keepCache != null) {
                                topEntry.addKeepCache((Map<String, Object>) keepCache);
                                break;
                            }
                            prev = prev.link;
                        }
                    }
                }

            } else if (newScope) {
                List<KeepStatement> keeps = scopedef.getKeeps();
                if (keeps != null) {
                    Iterator<KeepStatement> it = keeps.iterator();
                    while (it.hasNext()) {
                        KeepStatement k = it.next();
                        try {
                            keep(k);
                        } catch (Redirection r) {
                            vlog("Error in keep statement: " + r.getMessage());
                            throw r;
                        }
                    }
                }
                String keepCacheKey = scopedef.getName() + ".keep";
                String globalKeepCacheKey = makeGlobalKey(scopedef.getFullNameInContext(this)) + ".keep";
                while (prev != null) {
                    Object keepCache = prev.get(keepCacheKey, globalKeepCacheKey, null, true);
                    if (keepCache != null) {
                        topEntry.addKeepCache((Map<String, Object>) keepCache);
                        break;
                    }
                    prev = prev.link;
                }

                List<InsertStatement> inserts = scopedef.getInserts();
                if (inserts != null) {
                    Iterator<InsertStatement> it = inserts.iterator();
                    while (it.hasNext()) {
                        InsertStatement i = it.next();
                        try {
                            insert(i);
                        } catch (Redirection r) {
                            vlog("Error in insert statement: " + r.getMessage());
                            throw r;
                        }
                    }
                }
            }
        }
    }

    private NamedDefinition dealias(NamedDefinition def) {
        while (def != null && (def.isIdentity() || def.isAlias())) {
            // if this is an identity, look for the definition of the passed instance.
            // Look directly in the top entry rather than calling getDefinition, which in
            // some cases would cause infinite regression
            if (def.isIdentity()) {
                Holder holder = peek().getDefHolder(def.getName(), makeGlobalKey(def.getFullNameInContext(this)), null, false);
                if (holder != null && !def.equals(holder.def)) {
                    def = (NamedDefinition) holder.def;
                } else {
                    break;
                }

            } else if (def.isAlias()) {
                Instantiation aliasInstance = def.getAliasInstance();
                Definition aliasDef = aliasInstance.getDefinition(this, def);
                if (aliasDef == null) {
                    break;
                }
                def = (NamedDefinition) aliasDef;
            }
        }                 
        return def;
    }
    
    private boolean sharesKeeps(Entry entry, Entry prev, boolean forward) {
        boolean shares = false;
        
        if (forward && prev.def.equals(entry.def) && (prev.superdef == null || entry.superdef == null || prev.superdef.equalsOrExtends(entry.superdef) || entry.superdef.equalsOrExtends(prev.superdef))) {
            shares = true;
        } else if (!forward && prev.def.isAlias()) {
            Definition prevSuperdef = prev.def.getSuperDefinition();
            Definition thisSuperdef = entry.def.getSuperDefinition();
            if (prevSuperdef != null && thisSuperdef != null && (prevSuperdef.equalsOrExtends(thisSuperdef) || thisSuperdef.equalsOrExtends(prevSuperdef))) {
                shares = true;
            }
        }
        return shares;
    }
    
    
    private synchronized void _push(Entry entry) {
        if (entry.def == null) {
            throw new NullPointerException("attempt to push null definition on context");
        }
        
        if (size >= maxSize) {
            throw new RuntimeException("blown context");
        } else if (size == 200) {
            System.err.println("**** context exceeding 200 ****");
        } else if (size == 100) {
            System.err.println("**** context exceeding 100 ****");
        } else if (size == 50) {
            System.err.println("**** context exceeding 50 ****");
        }
        size++;

        if (rootEntry == null) {
            if (entry.getPrevious() != null) {
                entry = newEntry(entry, true);
            }
            setRootEntry(entry);
        } else {
            if (entry.getPrevious() != topEntry) {
                if (entry.getPrevious() != null) {
                    entry = newEntry(entry, true);
                }
                entry.setPrevious(topEntry);
            }
        }
        setTop(entry);
    }

    private void setRootEntry(Entry entry) {
    	rootEntry = entry;

    	Site site = entry.def.getSite();
    	List<Name> adoptedSites = site.getAdoptedSiteList();
    	if (adoptedSites != null) {
            Iterator<Name> it = adoptedSites.iterator();
            while (it.hasNext()) {
                Name adoptedSite = it.next();
                Map<String, Object> adoptedSiteCache = siteCaches.get(adoptedSite.getName());
                if (adoptedSiteCache == null) {
                    adoptedSiteCache = newHashMap(Object.class);
                    siteCaches.put(adoptedSite.getName(), adoptedSiteCache);
                }
            }
    		rootEntry.setSiteCacheMap(siteCaches, adoptedSites);
    	}
    	
    }
    
//    private void checkEntry(Entry entry) {
//        // check to see if the parameters have been properly cleaned up
//        if (entry != null) {
//            if (entry.params != null && entry.origParamsSize != entry.params.size()) {
//                int n = entry.params.size() - entry.origParamsSize;
//                vlog(" !!! " + n + " parameters have not been popped");
//            }
//        }
//    }

    public synchronized void pop() {
        Entry entry = _pop();

        if (topEntry != null) {
            stateCount = topEntry.getState();
            definingDef = topEntry.def;
            if (sharesKeeps(entry, topEntry, false)) {
                addKeepsFromEntry(entry);
            }
        } else {
            stateCount = -1;
            definingDef = null;
        }
        oldEntry(entry);
    }

    private Entry _pop() {
        if (size > 0) {
            if (topEntry == popLimit) {
                vlog("popping context beyond popLimit");
//                throw new IndexOutOfBoundsException("Illegal pop attempt; can only pop entries pushed after this copy was made.");
            }
            Entry entry = topEntry;
            setTop(entry.getPrevious());
            size--;
            //entry.setPrevious(null);
            return entry;
        } else {
            return null;
        }
    }

    public synchronized Entry unpush() {
        if (size <= 1) {
            throw new IndexOutOfBoundsException("attempt to unpush root entry in context");
        }
        Entry entry = _pop();
        entry.setPrevious(unpushedEntries);
        setUnpushed(entry);
        return entry;
    }

    public synchronized void repush() {
        Entry newUnpushedEntries = unpushedEntries.getPrevious();
        Entry entry = unpushedEntries;
        // by pre-setting the link to topEntry, we avoid the logic in _push that clones
        // the entry being pushed
        entry.setPrevious(topEntry);
        setUnpushed(newUnpushedEntries);
        _push(entry);
    }


    public synchronized void unpop(Definition def, ParameterList params, ArgumentList args) {
        Definition contextDef = getContextDefinition(def);
        _push(newEntry(contextDef, contextDef, params, args));
    }

    public synchronized void unpop(Entry entry) {
        _push(entry);
    }

    public synchronized Entry repop() {
        Entry entry = _pop();
        return entry;
    }

    public Entry peek() {
        return topEntry;
    }

    public Definition getDefiningDef() {
        return definingDef;
    }

    public void pushParam(DefParameter param, Construction arg) {
        if (size >= maxSize) {
            throw new RuntimeException("blown context");
        } else if (size < 1) {
            throw new NoSuchElementException("Cannot push a parameter onto an empty context");
        }

        Entry entry = topEntry;
        if (entry.params == null || entry.params.size() == 0) {
            entry.params = new ParameterList(newArrayList(1, DefParameter.class));
        } else if (entry.params.size() == entry.origParamsSize) {
            entry.params = new ParameterList(newArrayList(entry.params));
        }
        if (entry.args == null || entry.args.size() == 0) {
            entry.args = new ArgumentList(newArrayList(1, Construction.class));
        } else if (entry.args.size() == entry.origArgsSize) {
            entry.args = new ArgumentList(newArrayList(entry.args));
        }

        entry.params.add(param);
        entry.args.add(arg);
    }

    public void popParam() {
        Entry entry = topEntry;
        int n = entry.params.size();
        if (n > 0) {
            entry.params.remove(n - 1);
            // this entry may have started with fewer args than params
            entry.args.remove(entry.args.size() - 1);
        }
    }

    /** Advances the current loop index to a new unique integer value, i.e. a value
     *  not used by any other pass in this loop or any other loop in the same context,
     *  and returns this value.
     *
     *  @throws NullPointerException if the context is uninitialized.
     */
    public int nextLoopIndex() {
        topEntry.advanceLoopIndex();
        return topEntry.getLoopIndex();
    }

    public void resetLoopIndex() {
        topEntry.resetLoopIndex();
    }

    public int getLoopIndex() {
        if (topEntry != null) {
            return topEntry.getLoopIndex();
        }
        return -1;
    }


    public ArgumentList getArguments() {
        if (topEntry != null) {
            return topEntry.args;
        }
        return null;
    }

    public ParameterList getParameters() {
        if (topEntry != null) {
            return topEntry.params;
        }
        return null;
    }

    public Iterator<Entry> iterator() {
        return new ContextIterator();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Context) {
            Context other = (Context) obj;
            return (rootContext == other.rootContext && stateCount == other.stateCount && getLoopIndex() == other.getLoopIndex());

        } else if (obj instanceof ContextMarker) {
            ContextMarker marker = (ContextMarker) obj;
            return (rootContext == marker.rootContext && stateCount == marker.stateCount && getLoopIndex() == marker.loopIndex);

        } else {
            return false;
        }
    }

    public boolean equalsOrPrecedes(Object obj) {
        if (obj instanceof Context) {
            Context context = (Context) obj;
            if (rootContext == context.rootContext) {
	            if (stateCount == context.stateCount && getLoopIndex() <= context.getLoopIndex()) {
	                return true;
	            } else if (stateCount < context.stateCount) {
	                Iterator<Entry> it = context.iterator();
	                while (it.hasNext()) {
	                    Entry entry = (Entry) it.next();
	                    if (entry.equals(topEntry)) {
	                        return true;
	                    }
	                }
	            }
            }
        } else if (obj instanceof ContextMarker) {
            ContextMarker marker = (ContextMarker) obj;
            if (rootContext == marker.rootContext) {
	            if (stateCount == marker.stateCount && getLoopIndex() <= marker.loopIndex) {
	                return true;
	            } else if (stateCount < marker.stateCount) {
	            	return true;
	            }
            }
        }
        return false;
    }

    public boolean isCompatible(Context context) {
        if (context != null && rootContext == context.rootContext) {
            if (stateCount == context.stateCount) {
                return true;
            } else if (stateCount < context.stateCount) {
            	if (topEntry != null) {
	                Iterator<Entry> it = context.iterator();
	                while (it.hasNext()) {
	                    Entry entry = it.next();
	                    if (entry.equals(topEntry)) {
	                        return true;
	                    }
	                }
            	}
            } else if (context.topEntry != null) {
                Iterator<Entry> it = iterator();
                while (it.hasNext()) {
                    Entry entry = it.next();
                    if (entry.equals(context.topEntry)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {

//        Entry entries = unpushedEntries;
        setUnpushed(null);
//        clearEntries(entries);

//        entries = topEntry;
        setTop(null);
//        clearEntries(entries);

        definingDef = null;
        instantiatedDef = null;
        popLimit = null;
        cache = null;
        size = 0;
        errorThreshhold = EVERYTHING;

        // careful -- these are dangerous to leave null for long
        rootContext = null;
        rootEntry = null;
    }

    private void setUnpushed(Entry entry) {
        if (unpushedEntries != null) {
            unpushedEntries.decRefCount();
        }
        unpushedEntries = entry;
        if (unpushedEntries != null) {
            unpushedEntries.incRefCount();
        }
    }

    private void setTop(Entry entry) {
        if (topEntry != null) {
            topEntry.decRefCount();
        }
        topEntry = entry;
        if (topEntry != null) {
            topEntry.incRefCount();
        }
    }

    // remove an entry; if the ref count of the entry it links to is zero, remove
    // it, and son on.  Return null if the whole chain is cleared, else the leading
    // entry of the remaining chain.
//    private Entry clearEntries(Entry entry) {
//        while (entry != null && entry.refCount == 0) {
//            Entry link = entry.getPrevious();
//            entry.setPrevious(null);
//            oldEntry(entry);
//            entry = link;
//        }
//        return entry;
//    }

    /** Makes this context a copy of the passed context. */
    synchronized public void copy(Context context, boolean clearCache) {
        clear();

        // copy the root
        rootContext = context.rootContext;
        definingDef = context.definingDef;
        instantiatedDef = context.instantiatedDef;

        // this is a copy, so don't pop past the current top
        popLimit = topEntry;

        // copy the global state variables
        stateCount = context.stateCount;
        stateFactory = context.stateFactory;
        size = context.size;
        errorThreshhold = context.errorThreshhold;

        // copy the session
        session = context.session;
        
        // deep copy the unpushed entries, so the context and its copy
        // can unpush separately
        Entry toEntry = null;
        for (Entry fromEntry = context.unpushedEntries; fromEntry != null; fromEntry = fromEntry.link) {
            if (toEntry == null) {
                toEntry = newEntry(fromEntry, true);
                unpushedEntries = toEntry;
            } else {
                toEntry.link = newEntry(fromEntry, true);
                toEntry = toEntry.link;
            }
        }

        keepMap = context.keepMap;
        
        // just one global cache
        globalCache = context.globalCache;

        if (!clearCache) {
            rootEntry = context.rootEntry;
            // share the cache
            cache = context.cache;
            siteCaches = context.siteCaches;
        
            if (context.topEntry != null) {
                if (context.topEntry == context.rootEntry) {
                    setRootEntry(newEntry(context.rootEntry, true));
                    setTop(rootEntry);

                } else {
                    // clone the whole list, from top to root.  The newEntry function does a shallow
                    // copy of the entry, meaning that caches are shared.  This means that access to 
                    // entry caches always needs to be synchronized.

                    setTop(newEntry(context.topEntry, true));
                    // copy the links also (newEntry doesn't do it, to avoid unnecessary references)
                    topEntry.setPrevious(context.topEntry.getPrevious());
                }
            }
        
        } else {
        	cache = newHashMap(Object.class);
        	siteCaches = newHashMapOfMaps(Object.class);
            setRootEntry(newEntry(context.rootEntry, false));
            setTop(rootEntry);
        }
    }

    public int hashCode() {
        int n = (rootEntry.hashCode() << 16) + stateCount;
        return n;
    }
    
    public String toString() {
        return dump("\n");
    }
    
    public String toHTML() {
        return "<pre>" + Utils.htmlEncode(toString()) + "</pre>";
    }
        
    private String dump(String newline) { 
        String str = "context #" + hashCode() + " $" + stateCount + newline;
        str = str + "    top> " + topEntry.toString() + newline;
        for (Context.Entry entry = topEntry.link; entry != null; entry = entry.link) {
            str = str + "      -> " + entry.toString() + newline;
        }
        return str;
    }

    public Object getMarker(Object obj) {
        if (obj == null) {
            return new ContextMarker(this);
        } else {
            mark(obj);
            return obj;
        }
    }

    public Object getMarker() {
        return new ContextMarker(this);
    }

    public void mark(Object obj) {
        if (obj instanceof ContextMarker) {
            synchronized (obj) {
                ContextMarker marker = (ContextMarker) obj;
                marker.rootContext = rootContext;
                marker.stateCount = stateCount;
                marker.loopIndex = getLoopIndex();
            }
        } else {
            throw new IllegalArgumentException("attempt to mark a strange object");
        }
    }

    public Object clone() {
        Context context = new Context(this, false);
        numClonedContexts++;
        return context;
    }
    
    public Context clone(boolean clearCache) {
        Context context = new Context(this, clearCache);
        numClonedContexts++;
        return context;
    }
  
    private static Object getCachedData(Map<String, Object> cache, String key) {
        Object data = null;

        data = cache.get(key);
        
        if (data == null) {
            int ix = key.indexOf('.');
            if (ix > 0) {
                String firstKey = key.substring(0, ix);
                String restOfKey = key.substring(ix + 1);
                Object obj = cache.get(firstKey);
                if (obj != null && obj instanceof Holder) {
                    Holder holder = (Holder) obj;
                    if (holder.data != null && holder.data instanceof Map<?,?>) {
                        data = getCachedData((Map<String, Object>) holder.data, restOfKey); 
                    }
                }
                if (data == null) {
                    String keepCacheKey = firstKey + ".keep";
                    Map<String, Object> keepCache = (Map<String, Object>) cache.get(keepCacheKey);
                    if (keepCache != null) {
                        data = getCachedData(keepCache, restOfKey);
                    }
                }
            }
        }
        return data;
    }
    
/***************

    private static Object getCachedData(Map<String, Object> cache, String key) {
        Object data = null;

        int ix = key.indexOf('.');
        if (ix > 0) {
            String firstKey = key.substring(0, ix);
            String restOfKey = key.substring(ix + 1);
            Object obj = cache.get(firstKey);
            if (obj != null && obj instanceof Holder) {
                Holder holder = (Holder) obj;
                if (holder.data != null && holder.data instanceof Map<?,?>) {
                    data = getCachedData((Map<String, Object>) holder.data, restOfKey); 
                }
            }
            if (data == null) {
                String keepCacheKey = firstKey + ".keep";
                Map<String, Object> keepCache = (Map<String, Object>) cache.get(keepCacheKey);
                if (keepCache != null) {
                    data = getCachedData(keepCache, restOfKey);
                }
                if (data == null) {
                    data = cache.get(key);
                }
            }
        } else {
            data = cache.get(key);
        }
        return data;
    }
*****/    
    static class KeepHolder {
        public NameNode keepName;
        public Definition owner;
        public ResolvedInstance[] resolvedInstances;
        public NameNode byName;
        public Map<String, Object> table;
        public boolean persist;
        public boolean inContainer;
        public boolean asThis;
        
        KeepHolder(NameNode keepName, Definition owner, ResolvedInstance[] resolvedInstances, NameNode byName, Map<String, Object> table, boolean persist, boolean inContainer, boolean asThis) {
            this.keepName = keepName;
            this.owner = owner;
            this.resolvedInstances = resolvedInstances;
            this.byName = byName;
            this.table = table;
            this.persist = persist;
            this.inContainer = inContainer;
            this.asThis = asThis;
        }
        
        Definition[] getDefinitions() {
            int numDefs = resolvedInstances.length;
            Definition[] defs = new Definition[numDefs];
            for (int i = 0; i < numDefs; i++) {
                defs[i] = resolvedInstances[i].getDefinition();
            }
            return defs;
        }
    }
    
    
    static class Pointer {
        ResolvedInstance ri;
        ResolvedInstance riAs;
        Object key;
        Map<String, Object> cache;
        boolean persist;
        public Pointer(ResolvedInstance ri, Object key, Map<String, Object> cache, boolean persist) {
            this(ri, ri, key, cache, persist);
        }
        public Pointer(ResolvedInstance ri, ResolvedInstance riAs, Object key, Map<String, Object> cache, boolean persist) {
            this.ri = ri;
            this.riAs = riAs;
            this.key = key;
            this.cache = cache;
            this.persist = persist;
        }
        
        public String getKey() {
        	if (key instanceof Value) {
        		return ((Value) key).getString();
        	} else {
        		return key.toString();
        	}
        }
        
        public String toString() {
            return toString("");            
        }

        public String toString(String prefix) {
            StringBuffer sb = new StringBuffer(prefix);
            prefix += "    ";
            sb.append("===> ");
            sb.append(key == null ? "(null)" : key);
            sb.append(": ");
            if (cache != null) {
                Object obj = cache.get(key);
                if (obj != null) {
                    if (obj instanceof AbstractNode) {
                        sb.append("\n");
                        sb.append(((AbstractNode) obj).toString(prefix));
                    } else if (obj instanceof Pointer) {
                        sb.append("\n");
                        sb.append(((Pointer) obj).toString(prefix));
                    } else {
                        sb.append(obj.toString());
                    }
                } else {
                    sb.append("(null)");
                }
            }
            return sb.toString();
        }
    }

    private static int hashMapsCreated = 0;
    public static int getNumHashMapsCreated() {
        return hashMapsCreated;
    }

    public static <E> HashMap<String, E> newHashMap(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, E>();
    }

    public static <E> HashMap<String, E> newHashMap(Map<String, E> map) {
        hashMapsCreated++;
        return new HashMap<String, E>(map);
    }

    public static <E> HashMap<String, Map<String,E>> newHashMapOfMaps(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, Map<String, E>>();
    }

    private static int arrayListsCreated = 0;
    private static long totalListSize = 0L;
    public static int getNumArrayListsCreated() {
        return arrayListsCreated;
    }
    public static long getTotalListSize() {
        return totalListSize;
    }

    public static <E> ArrayList<E> newArrayList(int size, Class<E> c) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(int size, List<E> list) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(List<E> list) {
        arrayListsCreated++;
        totalListSize += list.size();
        return new ArrayList<E>(list);
    }

    protected static int entriesCreated = 0;
    public static int getNumEntriesCreated() {
        return entriesCreated;
    }
    protected static int entriesCloned = 0;
    public static int getNumEntriesCloned() {
        return entriesCloned;
    }


    public Entry newEntry(Definition def, Definition superdef, ParameterList params, ArgumentList args) {
        
        Map<String, Object> entryCache = null;
        if (def instanceof Site) {
            entryCache = siteCaches.get(def.getName());
            if (entryCache == null) {
                entryCache = newHashMap(Object.class);
                siteCaches.put(def.getName(), entryCache);
            }
        }
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedEntry will always return null
        Entry entry = getAbandonedEntry();
        if (entry != null) {
            entry.init(def, superdef, params, args, entryCache, globalCache);
        } else {
            entry = new Entry(def, superdef, params, args, entryCache, globalCache);
        }
        return entry;
    }

    public Entry newEntry(Entry copyEntry, boolean copyCache) {
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedEntry will always return null
        Entry entry = getAbandonedEntry();
        if (entry != null) {
            entry.copy(copyEntry, copyCache);
        } else {
            entry = new Entry(copyEntry, copyCache);
        }
        return entry;
    }

    private void oldEntry(Entry entry) {
        // recycle if the refCount has dropped to zero, unless it's the top of the
        // unpushedEntries stack, which may have a refCount of 0 but should
        // definitely not be abandoned.

        synchronized (entry) {
            if (entry.refCount == 0 && entry != unpushedEntries) {
                entry.clear();
                addAbandonedEntry(entry);
            } else {
                //vlog(" !!! popped an entry with ref count of " + entry.refCount);
            }
        }

    }
    
    // DISABLE ENTRY REUSE FOR NOW
    // see addAbandonedEntry
    private Entry getAbandonedEntry() {
        Entry entry = rootContext.abandonedEntries;
        if (entry != null) {
            synchronized (rootContext.abandonedEntries) {
                Entry next = entry.getPrevious();
                entry.setPrevious(null);
                rootContext.abandonedEntries = next;
            }
        }
        return entry;
    }

    // DISABLE ENTRY REUSE FOR NOW
    // by disabling this function
    private void addAbandonedEntry(Entry entry) {
        //entry.setPrevious(rootContext.abandonedEntries);
        //rootContext.abandonedEntries = entry;
    }

    public static class Entry {
        public Definition def;
        public Definition superdef;
        public ParameterList params;
        public ArgumentList args;

        public Map<String, Pointer> keepMap = null;
        public List<KeepHolder> dynamicKeeps = null;
        public Map<String, Type> insertAboveMap = null;
        public Map<String, Type> insertBelowMap = null;
        protected Entry link = null;
        int refCount = 0;   // number of links by other entries to this one
        private int contextState = -1;
        private int loopIx = -1;
        protected int origParamsSize;
        protected int origArgsSize;
        private StateFactory loopIndexFactory;

        // Only one copy of a context entry gets to write to the cache; other copies have
        // read-only access.
        private Map<String, Object> cache = null;
        private Map<String, Object> readOnlyCache = null;
        private Map<String, Object> globalCache = null;

        // Objects that are persisted through keep directives are cached here
        private Map<String, Object> keepCache = null;
        
        private Map<String, Object> siteCache = null;
        private Map<String, Map<String, Object>> siteCacheMap = null;
        private List<Name> adoptedSites = null;

        protected Entry(Definition def, Definition superdef, ParameterList params, ArgumentList args, Map<String, Object> cache, Map<String, Object> globalCache) {
            entriesCreated++;

            this.def = def;
            this.superdef = superdef;
            this.params = (params != null ? (ParameterList) params.clone() : new ParameterList(newArrayList(0, DefParameter.class)));
            this.args = (args != null ? (ArgumentList) args.clone() : new ArgumentList(newArrayList(0, Construction.class)));
            origParamsSize = this.params.size();
            origArgsSize = this.args.size();

            // fill out the argument list with nulls if it's shorter than the parameter list
            while (origArgsSize < origParamsSize) {
                this.args.add(ArgumentList.MISSING_ARG);
                origArgsSize++;
            }
            loopIndexFactory = new StateFactory();
            
            this.cache = cache;
            this.siteCache = cache;
            this.globalCache = globalCache;
        }

        protected Entry(Entry entry, boolean copyCache) {
            entriesCreated++;
            entriesCloned++;

            def = entry.def;
            superdef = entry.superdef;
            params = (entry.params != null ? (ParameterList) entry.params.clone() : new ParameterList(newArrayList(0, DefParameter.class)));
            args = (entry.args != null ? (ArgumentList) entry.args.clone() : new ArgumentList(newArrayList(0, Construction.class)));

            // don't clone the link to avoid duplicating references.  If the
            // clone needs to point somewhere, it has to be done explicitly.

            contextState = entry.contextState;
            loopIx = entry.loopIx;
            loopIndexFactory = entry.loopIndexFactory;
            origParamsSize = entry.origParamsSize;
            origArgsSize = entry.origArgsSize;
            
            // the keep map and global cache are always shared
            keepMap = entry.keepMap;
            globalCache = entry.globalCache;
            
            // make shallow copy of cache if copyCache flag is true, otherwise they
            // will be null;
            if (copyCache) {
                // for now, no read only cache, let everybody write (yikes!)
                //entry.readOnlyCache = (cache != null ? cache : readOnlyCache);
                cache = entry.getCache();
                keepCache = entry.getKeepCache();
                siteCache = entry.siteCache;
                siteCacheMap = entry.siteCacheMap;
                adoptedSites = entry.adoptedSites;
                dynamicKeeps = entry.dynamicKeeps;
            }
        }

        void init(Definition def, Definition superdef, ParameterList params, ArgumentList args, Map<String, Object> cache, Map<String, Object> globalCache) {
            this.def = def;
            this.superdef = superdef;
            this.params = (params != null ? (ParameterList) params.clone() : new ParameterList(newArrayList(0, DefParameter.class)));
            this.args = (args != null ? (ArgumentList) args.clone() : new ArgumentList(newArrayList(0, Construction.class)));

            origParamsSize = this.params.size();
            origArgsSize = this.args.size();

            // fill out the argument list with nulls if it's shorter than the parameter list
            while (origArgsSize < origParamsSize) {
                this.args.add(ArgumentList.MISSING_ARG);
                origArgsSize++;
            }
            
            this.cache = cache;
            this.globalCache = globalCache;
            this.keepCache = null;
            this.siteCache = cache;
            this.siteCacheMap = null;
        }

        void copy(Entry entry, boolean copyCache) {
            if (refCount > 0) {
                throw new RuntimeException("Attempt to copy over entry with non-zero refCount");
            }

            def = entry.def;
            superdef = entry.superdef;
            if (params != null) {
                params.clear();
                if (entry.params != null) {
                    params.addAll(entry.params);
                }
            } else {
                params = (entry.params != null ? (ParameterList) entry.params.clone() : new ParameterList(newArrayList(0, DefParameter.class)));
            }

            if (args != null) {
                args.clear();
                if (entry.args != null) {
                    args.addAll(entry.args);
                }
            } else {
                args = (entry.args != null ? (ArgumentList) entry.args.clone() : new ArgumentList(newArrayList(0, Construction.class)));
            }

            contextState = entry.contextState;
            loopIx = entry.loopIx;
            loopIndexFactory = entry.loopIndexFactory;
            origParamsSize = entry.origParamsSize;
            origArgsSize = entry.origArgsSize;
            // for now, let everybody write (yikes!)
            //entry.readOnlyCache = (cache != null ? cache : readOnlyCache);
            if (copyCache) {
                copyKeeps(entry);
            } else {
                cache = null;
                keepCache = null;
                siteCache = null;

                // keepMap and globalCache are shared everywhere
                keepMap = entry.keepMap;
                globalCache = entry.globalCache;
            }
            
        }

        void copyKeeps(Entry entry) {
            // Calling getCache allocates the cache if it doesn't exist; this is
            // wasteful if the cache never gets used, but it guarantees that if the
            // cache is used, it's the same cache for every entry that wants to use
            // the same cache.
            //
            // To eliminate the waste, we could wait till the cache is allocated,
            // then backfill previous entries as appropriate.  For now, we do the
            // allocation on the first copy, trading greater waste for less risk.
            cache = entry.getCache();
            keepCache = entry.keepCache;
            siteCache = entry.siteCache;
            siteCacheMap = entry.siteCacheMap;
            adoptedSites = entry.adoptedSites;
            keepMap = entry.keepMap;
            globalCache = entry.globalCache;
            dynamicKeeps = entry.dynamicKeeps;
        }

        void addKeeps(Entry entry) {
            if (cache != null) {
                if (entry.cache != null && entry.cache != cache) {
                    synchronized (cache) {
                        cache.putAll(entry.cache);
                    }
                }
            } else {
                cache = entry.cache;
            }

            if (keepCache != null) {
                if (entry.keepCache != null && entry.keepCache != keepCache) {
                    synchronized (keepCache) {
                        keepCache.putAll(entry.keepCache);
                    }
                }
            } else {
                keepCache = entry.keepCache;
            }
            
            if (siteCache != null) {
                if (entry.siteCache != null && entry.siteCache != siteCache) {
                    synchronized (siteCache) {
                        siteCache.putAll(entry.siteCache);
                    }
                }
            } else {
                siteCache = entry.siteCache;
            }

            if (siteCacheMap != null) {
                
                if (entry.siteCacheMap != null && entry.siteCacheMap != siteCacheMap) {
                    synchronized (siteCacheMap) {
                        siteCacheMap.putAll(entry.siteCacheMap);
                    }
                }
            } else {
                siteCacheMap = entry.siteCacheMap;
            }

            if (adoptedSites != null) {
                if (entry.adoptedSites != null && entry.adoptedSites != adoptedSites) {
                    synchronized (adoptedSites) {
                        adoptedSites.addAll(entry.adoptedSites);
                    }
                }
            } else {
                adoptedSites = entry.adoptedSites;
            }

            if (keepMap != null) {
                if (entry.keepMap != null && entry.keepMap != keepMap) {
                    synchronized (keepMap) {
                        keepMap.putAll(entry.keepMap);
                    }
                }
            } else {
                keepMap = entry.keepMap;
            }

            if (globalCache != null) {
                if (entry.globalCache != null && entry.globalCache != globalCache) {
                    synchronized (globalCache) {
                        globalCache.putAll(entry.globalCache);
                    }
                }
            } else {
                globalCache = entry.globalCache;
            }
        }

        
        void copyInserts(Entry entry) {
            insertAboveMap = entry.insertAboveMap;
        }

        public void clear() {
            if (refCount > 0) {
                throw new RuntimeException("Attempt to clear entry with non-zero refCount");
            }

            def = null;
            superdef = null;
            if (params != null) {
                params.clear();
            }
            if (args != null) {
                args.clear();
            }
            setPrevious(null);
            origParamsSize = 0;
            origArgsSize = 0;

            keepMap = null;
            insertAboveMap = null;
            cache = null;
            readOnlyCache = null;
            globalCache = null;
            keepCache = null;
            siteCache = null;
            siteCacheMap = null;
            adoptedSites = null;
        }

        public void addInsert(Type insertedType, Type targetType, int insertionPoint) {
            switch (insertionPoint) {
                case InsertStatement.ABOVE:
                    if (insertAboveMap == null) {
                        insertAboveMap = new HashMap<String, Type>();
                    }
                    insertAboveMap.put(targetType.getName(), insertedType);
                    break;
                case InsertStatement.BELOW:
                    if (insertBelowMap == null) {
                        insertBelowMap = new HashMap<String, Type>();
                    }
                    insertBelowMap.put(targetType.getName(), insertedType);
                    break;
            }
        }
            
        public void addDynamicKeep(KeepHolder keepHolder) {
            if (dynamicKeeps == null) {
                dynamicKeeps = new ArrayList<KeepHolder>(4);
            }
            if (!dynamicKeeps.contains(keepHolder)) {
                dynamicKeeps.add(keepHolder);
            }
        }
   
        public void addKeep(ResolvedInstance[] resolvedInstances, Object keyObj, Map<String, Object> table, String containerKey, Map<String, Object> containerTable, boolean persist, Map<String, Pointer> contextKeepMap, Map<String, Object> contextCache) {
            boolean inContainer = (containerTable != null);
            
            if (def.isGlobal()) {
                contextCache = globalCache;
            }
            
            if (keepMap == null) {
                keepMap = newHashMap(Pointer.class);
            }
            
            synchronized (keepMap) {
                int numInstances = resolvedInstances.length;
                String key = (keyObj == null ? null : (keyObj instanceof Value ? ((Value) keyObj).getString() : keyObj.toString())); 
                if (key != null) {
                    if (!key.endsWith(".")) {
                        
                        ResolvedInstance riAs = resolvedInstances[numInstances - 1];
                        for (int i = 0; i < numInstances; i++) {
                            if (resolvedInstances[i] != null) {
                                Pointer p = new Pointer(resolvedInstances[i], riAs, keyObj, table, persist);
                                String keepKey = (inContainer ? key : resolvedInstances[i].getName());
                                keepMap.put(keepKey, p);
                                
                                String contextKey = def.getFullName() + '.' + keepKey;
                                contextKey = contextKey.substring(contextKey.indexOf('.') + 1);
                                Pointer contextp = new Pointer(resolvedInstances[i], riAs, contextKey, contextCache, persist);
                                contextKeepMap.put(contextKey, contextp);
                            }
                        }
                    } else {
                        for (int i = 0; i < numInstances; i++) {
                            if (resolvedInstances[i] != null) {
                                String name = resolvedInstances[i].getName();
                                String keepKey = key + name;
                                Pointer p = new Pointer(resolvedInstances[i], keepKey, table, persist);
                                keepMap.put(keepKey, p);

                                if (inContainer) {
                                    p = new Pointer(resolvedInstances[i], keepKey, containerTable, persist);
                                    keepMap.put(containerKey + name, p);
                                }
                                
                                String contextKey = def.getFullName() + '.' + keepKey;
                                contextKey = contextKey.substring(contextKey.indexOf('.') + 1);
                                Pointer contextp = new Pointer(resolvedInstances[i], contextKey, contextCache, persist);
                                contextKeepMap.put(contextKey, contextp);
                            }
                        }
                    }

                } else {
                    for (int i = 0; i < numInstances; i++) {
                        if (resolvedInstances[i] != null) {
                            String name = resolvedInstances[i].getName();
                            Pointer p = new Pointer(resolvedInstances[i], name, table, persist);
                            keepMap.put(name, p);
                            
                            if (inContainer) {
                                p = new Pointer(resolvedInstances[i], containerKey + name, containerTable, persist);
                                keepMap.put("+" + name, p);
                            }

                            String contextKey = def.getFullName() + '.' + name;
                            contextKey = contextKey.substring(contextKey.indexOf('.') + 1);
                            Pointer contextp = new Pointer(resolvedInstances[i], contextKey, contextCache, persist);
                            contextKeepMap.put(contextKey, contextp);
                        }
                    }
                }
            }
        }
        
        public void removeKeep(String name) {
            synchronized (keepMap) {
                keepMap.remove(name);
            }
        }

        /** Returns true if a parameter of the specified name is present in this entry. */
        public boolean paramIsPresent(NameNode nameNode, boolean checkForArg) {
            boolean isPresent = false;
            String name = nameNode.getName();
            int n = name.indexOf('.');
            if (n > 0) {
                name = name.substring(0, n);
            }
            int numParams = params.size();
            for (int i = 0; i < numParams; i++) {
                DefParameter param = params.get(i);
                if (name.equals(param.getName()) && (!checkForArg || args.get(i) != ArgumentList.MISSING_ARG)) {
                    isPresent = true;
                    break;
                }
            }
            return isPresent;
        }

        /** Return the parameter by a given name, or null if there isn't one. */
        public DefParameter getParam(String name) {
            int n = name.indexOf('.');
            if (n > 0) {
                name = name.substring(0, n);
            }
            int numParams = params.size();
            for (int i = 0; i < numParams; i++) {
                DefParameter param = params.get(i);
                if (name.equals(param.getName()) && args.get(i) != ArgumentList.MISSING_ARG) {
                    return param;
                }
            }
            return null;
        }
        
        
        public Object get(String key, String globalKey, ArgumentList args, boolean local) {
            return get(key, globalKey, args, false, local);
        }

        public Definition getDefinition(String key, String globalKey, ArgumentList args) {
            Holder holder = (Holder) get(key, globalKey, args, true, false);
            if (holder != null) {
                return holder.def;
            } else {
            	return null;
            }
        }
        
        public Holder getDefHolder(String key, String globalKey, ArgumentList args, boolean local) {
            return (Holder) get(key, globalKey, args, true, local);
        }

        private Object get(String key, String globalKey, ArgumentList args, boolean getDefHolder, boolean local) {
            Holder holder = null;
            Map<String, Object> c = (cache != null ? cache : readOnlyCache);

            if (globalCache != null && globalKey != null && globalCache.get(globalKey) != null) {
                c = globalCache;
            }
            
            if (c == null && siteCache == null && siteCacheMap == null && (keepMap == null || keepMap.get(key) == null)) {
                if (this.def == null) {
                    return null;
                }
                if (!local && link != null && !this.def.hasChildDefinition(key) && !NameNode.isSpecialName(key)) {
                    return link.get(key, globalKey, args, getDefHolder, false);
                } else {
                    return null;
                }
            }

            Object data = null;
            Definition def = null;
            ResolvedInstance ri = null;

            // If there is a keep entry here but no value was retrieved from the cache above
            // then it means the value has not been instantiated yet in this context.  If
            // this is not a keep statement, or there is no value for this key in the
            // out-of-context cache, or if the key is accompanied by a non-null modifier,
            // return null rather than continue the search up the context chain in order to
            // force instantiation and avoid bypassing the designated cache.
            if (data == null && keepMap != null && keepMap.get(key) != null) {
                Pointer p = keepMap.get(key);
                if (!p.persist) {
                    return null;
                }
                ri = p.ri;

                Map<String, Object> keepTable = p.cache;
                data = keepTable.get(p.getKey());

                if (data instanceof Pointer) {
                    int i = 0;
                    do {
                        p = (Pointer) data;
                        data = p.cache.get(p.getKey());
                        if (data instanceof Holder) {
                            holder = (Holder) data;
                            data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                            def = holder.def;
                            args = holder.args;
                            ri = holder.resolvedInstance;
                        }
                        i++;
                        if (i >= MAX_POINTER_CHAIN_LENGTH) {
                            throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                        }
                    } while (data instanceof Pointer);
                } else if (data instanceof Holder) {
                    holder = (Holder) data;
                    data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                    def = p.riAs.getDefinition();
                    args = holder.args;
                    ri = holder.resolvedInstance;
                } else if (data instanceof ElementDefinition) {
                    def = (Definition) data;
                    data = ((ElementDefinition) data).getElement();
                    data = AbstractNode.getObjectValue(null, data);
                }
            }
                
            if (data == null && !local && (siteCache != null || siteCacheMap != null)) {
                if (siteCache != null) {
                    data = siteCache.get(key);
                } else {
                    Iterator<Name> it = adoptedSites.iterator();
                    while (it.hasNext()) {
                        NameNode adoptedSiteName = (NameNode) it.next();
                        Map<String, Object> adoptedSiteCache = (Map<String, Object>) siteCacheMap.get(adoptedSiteName.getName());
                        data = adoptedSiteCache.get(key);
                        if (data != null) {
                            break;
                        }
                    }
                }
           
                if (data != null) {
                        
                    if (data instanceof Holder) {
                        holder = (Holder) data;
                        data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                        def = holder.def;
                        args = holder.args;
                        ri = holder.resolvedInstance;
                    } else if (data instanceof Pointer) {
                        def = ((Pointer) data).riAs.getDefinition();
    
                        // strip off modifier if present
                        key = baseKey(((Pointer) data).getKey());
                    }

                    String loopModifier = getLoopModifier();
                    if (loopModifier != null) {
                        if (data instanceof Pointer && ((Pointer) data).cache == siteCache) {
                            data = siteCache.get(key + loopModifier);
                        }
                    }

                    if (data != null) {
                        // to prevent an infinite loop arising from a circular list
                        // of pointers, abort after reaching a maximum
                        int i = 0;
                        if (data instanceof Pointer) {
                            do {
                                Pointer p = (Pointer) data;
                                data = p.cache.get(p.getKey());
                                if (data instanceof Holder) {
                                    holder = (Holder) data;
                                    data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                                    def = holder.def;
                                    args = holder.args;
                                    ri = holder.resolvedInstance;
                                }
                                i++;
                                if (i >= MAX_POINTER_CHAIN_LENGTH) {
                                    throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                                }
                            } while (data instanceof Pointer);
                        } else if (data instanceof Holder) {
                            holder = (Holder) data;
                            data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                            def = holder.def;
                            args = holder.args;
                            ri = holder.resolvedInstance;
                        }
                    } 
                }
            }
            
            if (data == null && c != null) {
                String ckey = (c == globalCache ? globalKey : key);
                data = getCachedData(c, ckey);
                if (data != null) {

	                if (data instanceof ElementDefinition) {
	                    def = (Definition) data;
	                    data = ((ElementDefinition) def).getElement();
	                } else if (data instanceof Holder) {
	                    holder = (Holder) data;
	                    data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
	                    def = holder.def;
	                    args = holder.args;
	                    ri = holder.resolvedInstance;
	                } else if (data instanceof Pointer) {
	                    def = ((Pointer) data).riAs.getDefinition();
	                    ckey = baseKey(((Pointer) data).getKey());
	                }
	
	                String loopModifier = getLoopModifier();
	                if (loopModifier != null) {
	                    if (data instanceof Pointer && ((Pointer) data).cache == c) {
	                        data = c.get(ckey + loopModifier);
	                    }
	                }
                }

                if (data != null) {
                    // to prevent an infinite loop arising from a circular list
                    // of pointers, abort after reaching a maximum
                    int i = 0;
                    if (data instanceof Pointer) {
                        do {
                            Pointer p = (Pointer) data;
                            data = p.cache.get(p.getKey());
                            if (data instanceof Holder) {
                                holder = (Holder) data;
                                data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                                def = holder.def;
                                args = holder.args;
                                ri = holder.resolvedInstance;
                            }
                            i++;
                            if (i >= MAX_POINTER_CHAIN_LENGTH) {
                                throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                            }
                        } while (data instanceof Pointer);
                    } else if (data instanceof Holder) {
                        holder = (Holder) data;
                        data = (holder.data == AbstractNode.UNINSTANTIATED ? null : holder.data);
                        def = holder.def;
                        args = holder.args;
                        ri = holder.resolvedInstance;
                    }
                }
            }

            // continue up the context chain
            
            if (data == null && (def == null || !getDefHolder) && !local && link != null && !this.def.hasChildDefinition(key) && !NameNode.isSpecialName(key)) {
                return link.get(key, globalKey, args, getDefHolder, false);
            }

            // return either the definition or the data, depending on the passed flag
            if (getDefHolder) {
            	if (holder == null) {
            	    
            	    if (def == null && ri != null) {
            	        def = ri.getDefinition();
            	    }
            	    
            	    // if def is null, this might be an entry resulting from an "as" clause
	                // in a keep statement, so look in the keep table for an entry.
	                if (def == null && keepMap != null && keepMap.get(key) != null) {
	                    Pointer p = keepMap.get(key);
	                    def = p.riAs.getDefinition();
	                }
	                if (def != null) {
                        Definition nominalDef = def;
                        ArgumentList nominalArgs = args;
                        holder = new Holder(nominalDef, nominalArgs, def, args, null, data, ri);
	                } else if (data != null) {
                        holder = new Holder(null, null, null, null, null, data, null);
	                }
            	}
                return holder;
            } else {
                return data;
            }
        }


        /** If the current cache contains a Pointer under the specified key, stores the
         *  data in the cache pointed to by the Pointer; otherwise stores the data in the
         *  current cache.  Then traverses back up the context tree, storing the data in
         *  any other context level where data for that key is explicitly stored (i.e. the
         *  cache contains a Pointer for that key, or the entry definition is the owner or
         *  a subdefinition of the owner of a child whose name is the key).
         *
         *  If the definition parameter is non-null, the data and the definition are
         *  wrapped in a Holder, which is cached.
         */
        public void put(String key, Definition nominalDef, ArgumentList nominalArgs, Definition def, ArgumentList args, Context context, Object data, ResolvedInstance resolvedInstance, int maxLevels) {
            Holder holder = new Holder(nominalDef, nominalArgs, def, args, context, data, resolvedInstance);
            put(key, holder, context, maxLevels);
        }
        
        private void put(String key, Holder holder, Context context, int maxLevels) {
            boolean kept = false;
            Definition def = holder.def;
            Definition nominalDef = holder.nominalDef;
            Map<String, Object> localCache = getCache();
            synchronized (localCache) {
                if (localPut(localCache, key, holder)) {
                    kept = true;
                }
            }

            if (nominalDef != null) {
                if (nominalDef.isGlobal() && nominalDef.getName().equals(key)) {
                    if (globalCache != null) {
                        synchronized (globalCache) {
                            String globalKey = makeGlobalKey(nominalDef.getFullNameInContext(context));
                            localPut(globalCache, globalKey, holder);
                        }
                    }
                }
                if (nominalDef.getOwner() instanceof Site) {
                    Map<String, Object> siteCache = getSiteCache(nominalDef);
                    if (siteCache != null) {
                        synchronized (siteCache) {
                            localPut(siteCache, key, holder);
                        }
                    }
                }
            }
            
            //if (contextPut(key, holder, context)) {
            //    kept = true;
            //}
            
            // if this is an identity, copy the keep cache (if any) from the real
            // def to the nominal def
// this seems to catch too wide a net, and doesn't seem to be needed anyway
//            if (def != null && !key.equals(def.getName())) {
//                String keepCacheKey = def.getName() + ".keep";
//                Object keepCache = localCache.get(keepCacheKey);
//                if (keepCache != null) {
//                    localCache.put(key + ".keep", keepCache);
//                }
//            }

            int access = (nominalDef != null ? nominalDef.getAccess() : Definition.LOCAL_ACCESS);
            if (link != null && access != Definition.LOCAL_ACCESS) {
                String ownerName = this.def.getName();
                if (ownerName != null && nominalDef != null && !nominalDef.isFormalParam()) { 
                    Definition defOwner = nominalDef.getOwner();
                    for (Entry nextEntry = link; nextEntry != null; nextEntry = nextEntry.link) {
                        if (nextEntry.def.equalsOrExtends(defOwner)) {
                            // get the subbest subclass
                            do {
                                defOwner = nextEntry.def;
                                nextEntry = nextEntry.link;
                            } while (nextEntry != null && nextEntry.def.equalsOrExtends(defOwner));

                            break;
                        }
                    }
                    for (String k = key; defOwner != null && k.indexOf('.') > 0; k = k.substring(k.indexOf('.') + 1)) {
                        defOwner = defOwner.getOwner();
                    }

                    if (defOwner != null && this.def.equalsOrExtends(defOwner)) {
                        Definition defOwnerOwner = defOwner.getOwner();
                        Entry entry = link;
                        while (entry != null) {
                            if (entry.def.equalsOrExtends(defOwnerOwner)) {
                                break;
                            }
                            entry = entry.link;
                        }
                        if (entry != null) {
                            Map<String, Object> ownerCache = entry.getCache();
                            synchronized (ownerCache) {
                                entry.localPut(ownerCache, ownerName + "." + key, holder);
                            }
                        }
                    }
                }
                
                // keep directives intercept cache updates
                if (maxLevels > 0) maxLevels--;
                if (!kept && maxLevels != 0) {
                    if (this.def.hasChildDefinition(key)) {
                        KeepStatement keep = this.def.getKeep(key);
                        if (keep == null || !(keep.isInContainer() || keep.getTableInstance() != null /* || this.def.equals(link.def) */ )) {
                            return;
                        }
                    }
                    link.checkForPut(key, holder, context, maxLevels);
                }
            }
        }

        private Entry getContainerEntry(Definition def) {
            if (def != null) {
                Entry entry = this;
                while (entry != null) {
                    if (!entry.def.equalsOrExtends(def)) {
                        break;
                    }
                    entry = entry.link;
                }
                return entry;
            }
            return null;
        }
        
        private Entry getOwnerContainerEntry(Definition def) {
            if (def != null) {
                Definition ownerDef = def.getOwner();
                if (ownerDef != null) {
                    Entry entry = this;
                    while (entry != null) {
                        if (entry.def.equalsOrExtends(ownerDef)) {
                            break;
                        }
                        entry = entry.link;
                    }
                    return entry;
                }
            }
            return null;
        }

        
        public boolean localPut(Map<String, Object> cache, String key, Holder holder) {
            boolean kept = false;
            boolean persist = false;
            Pointer p = null;
            Object oldData = cache.get(key);

            // if this is the first entry, set up any required pointers for keep tables
            // and modifiers, save the data and return
            if (oldData == null || (oldData instanceof Holder && (((Holder) oldData).data == AbstractNode.UNINSTANTIATED || ((Holder) oldData).data == null))) {
                Object newData;
                
                newData = holder;
                //if (def != null) {
                //    newData = new Holder(def, args, context, data);
                //} else {
                //    newData = data;
                //}
                if (keepMap != null) {
                    p = keepMap.get(key);
                    if (p != null) {
                        // first check for a pointer to the container.  If there is one it will have
                        // the same key but prepended with a "+"
                        Pointer pContainer = keepMap.get("+" + key);
                        if (pContainer != null) {
                            Map<String, Object> containerTable = pContainer.cache;
                            containerTable.put(pContainer.getKey(), newData);
                        }
                        persist = p.persist;
                        Map<String, Object> keepTable = p.cache;
                        if (keepTable != cache || !key.equals(p.getKey())) {
                            synchronized (keepTable) {
                                p = new Pointer(p.ri, p.riAs, p.getKey(), keepTable, persist);

                                // two scenarios: keep as and cached identity.  With keep as, we want the
                                // pointer def; with cached identity, we want the holder def.  We can tell
                                // cached identities because the def and nominalDef in the holder are different.
                                Definition newDef;
                                if (holder.def != null && !holder.def.equals(holder.nominalDef)) {
                                    newDef = holder.def; 
                                } else {
                                    newDef = p.riAs.getDefinition();
                                }
                                ArgumentList newArgs = (newDef == holder.def ? holder.args : null);
                                Holder newHolder = new Holder(holder.nominalDef, holder.nominalArgs, newDef, newArgs, null, holder.data, holder.resolvedInstance);
                                keepTable.put(p.getKey(), newHolder);
                                newData = p;
                            }
                        }

                        kept = true;
                    }
                }
                cache.put(key, newData);

            } else {

                // There is an existing entry.  See if it's a modifier
                if (oldData instanceof Pointer) {
                    p = (Pointer) oldData;
                    persist = p.persist;
                    Map<String, Object> keepTable = p.cache;
                    if (keepTable != cache || !key.equals(p.getKey())) {
                        p = new Pointer(p.ri, p.riAs, p.getKey(), keepTable, persist);

                        // two scenarios: keep as and cached identity.  With keep as, we want the
                        // pointer def; with cached identity, we want the holder def.  We can tell
                        // cached identities either from the definition's identity flag or because 
                        // the def and nominalDef in the holder are different.
                        Definition newDef = (!holder.def.equals(holder.nominalDef) || holder.def.isIdentity() ? holder.def : p.riAs.getDefinition()); 
                        ArgumentList newArgs = (newDef == holder.def ? holder.args : null);
                        holder = new Holder(holder.nominalDef, holder.nominalArgs, newDef, newArgs, null, holder.data, holder.resolvedInstance);
                        kept = true;
                    }
                }

                // Follow the pointer chain to update the data
                Map<String, Object> nextCache = cache;
                String nextKey = key;
                Object nextData = oldData;
                while (nextData != null && nextData instanceof Pointer) {
                    p = (Pointer) nextData;
                    nextCache = p.cache;
                    nextKey = p.getKey();
                    nextData = nextCache.get(nextKey);
                }
                synchronized (nextCache) {
                    //if (def != null) {
                    //    data = new Holder(def, args, context, data);
                    //}
                    nextCache.put(nextKey, holder);
                }
                
                // Finally look for a container cache pointer.  nextKey at this point
                // should have the unmodified, unaliased version of the key
                p = (Pointer) cache.get("+" + nextKey);
                if (p != null) {
                    nextCache = p.cache;
                    nextKey = p.getKey();
                    synchronized (nextCache) {
                        nextCache.put(nextKey, holder);
                    }
                }
                
            }

            return kept;
        }

        public boolean contextPut(String key, Holder holder, Context context) {
            
            boolean kept = false;
            boolean persist = false;

            // repeat the above logic for the context cache, if necessary
            Map<String, Pointer> contextKeepMap = context.getKeepMap();
            String contextKey = key;
            if (holder.nominalDef != null) {
                String fullName = holder.nominalDef.getFullName();
                if (fullName != null) {
                    // this sort of solves the problem of multipart keys
                    if (fullName.endsWith(key)) {
                        contextKey = fullName;
                    } else {
                        int ix = fullName.lastIndexOf('.');
                        if (ix > 0) {
                            contextKey = fullName.substring(0, ix + 1) + key;
                        }
                    }
                }
            }

            // context caching logic goes as follows:
            //
            // -- unlike local caching, context caching only happens when explicitly
            //    specified by a keep statement.
            //
            // -- so look for a keep statement for the context key, determined above.
            //    A keep statement implies that a keep map exists, and a context exists, 
            //    and a Pointer object is stored in the keep map under the key 
            //
            if (contextKeepMap != null && contextKey != null) {
                Pointer contextp = contextKeepMap.get(contextKey);
                if (contextp != null) {
                    Map<String, Object> contextCache = context.getCache();
                    persist = contextp.persist;
                    Object oldContextData = contextp.cache.get(contextKey);
                    if (oldContextData == null) {
                        Object newContextData;
                        
                        newContextData = holder;
                        //if (def != null) {
                        //    newContextData = new Holder(def, args, context, data);
                        //} else {
                        //    newContextData = data;
                        //}
                        
                        if (contextp.cache != contextCache || !contextp.getKey().equals(contextKey)) {
                            contextp = new Pointer(contextp.ri, contextp.riAs, contextp.getKey(), contextCache, persist);
                            contextp.cache.put(contextp.getKey(), newContextData);
                            newContextData = contextp;
                        }
                        contextCache.put(contextKey, newContextData);
    
                    } else {
    
                        // There is an existing entry.  See if it's a modifier
                        if (oldContextData instanceof Pointer) {
                            contextp = (Pointer) oldContextData;
                            persist = contextp.persist;
                            if (contextp.cache == contextCache) { // it's a modifier or cache alias
                                String pkey = contextp.getKey();
                                if (contextKey.equals(baseKey(pkey)) && !pkey.equals(contextKey)) {   // it's a modifier but not the same modifier
                                    // move the chain data to the new spot
                                    synchronized (contextCache) {
                                        oldContextData = contextCache.get(pkey);
                                        contextCache.put(pkey, null);
                                        contextp = new Pointer(contextp.ri, contextp.riAs, contextKey + addLoopModifier(), contextCache, persist);
                                        contextCache.put(contextp.getKey(), oldContextData);
                                        contextCache.put(contextKey, contextp);
                                        oldContextData = contextp;
                                    }
                                }
                            } else {    // it's a table in a keep directive
                                kept = true;
                            }
                        }
    
    
                        // Follow the pointer chain to update the data
                        Map<String, Object> nextCache = contextCache;
                        String nextKey = contextKey;
                        Object nextData = oldContextData;
                        while (nextData != null && nextData instanceof Pointer) {
                            Pointer nextp = (Pointer) nextData;
                            nextCache = nextp.cache;
                            nextKey = nextp.getKey();
                            nextData = nextCache.get(nextKey);
                        }
                        synchronized (nextCache) {
                            nextCache.put(nextKey, holder);
                        }
                    }
                }   
            }
            
            return kept;
        }

        private void checkForPut(String key, Holder holder, Context context, int maxLevels) {
       	
        	if ((cache != null && cache.get(key) != null) ||
                    (keepMap != null && keepMap.get(key) != null) ||
                    hasSiteCache(def) ||
                    this.def.hasChildDefinition(key)) {

            	put(key, holder, context, maxLevels);
            } else {
                if (maxLevels > 0) maxLevels--;
                if (link != null && maxLevels != 0) {
                    link.checkForPut(key, holder, context, maxLevels);
                }
            }
        }
        
        boolean hasSiteCacheEntryFor(Definition def) {
        	if (def != null) {
        	    String key = def.getName();
        	    if (siteCache != null) {
        	        if (siteCache.get(key) != null) {
        	            return true;
        	        }
        	    }
        	    if (siteCacheMap != null) {
        	        Definition defOwner = def.getOwner();
        	        if (defOwner instanceof Site) {
        	            String defSiteName = defOwner.getName();
        	            Map<String, Object> cache = (Map<String, Object>) siteCacheMap.get(defSiteName);
        	            if (cache != null) {
        	                if (cache.get(key) != null) {
        	                    return true;
        	                }
        	            }
            		}
        		}
        	}
        	return false;
        }

        private boolean hasSiteCache(Definition def) {
        	if (siteCache != null) {
        	    return true;
        	}
        	if (siteCacheMap != null && def != null) {
        		Definition defOwner = def.getOwner();
        		if (defOwner instanceof Site) {
        			String defSiteName = defOwner.getName();
            		Iterator<Name> it = adoptedSites.iterator();
            		while (it.hasNext()) {
            	        NameNode siteName = (NameNode) it.next();
            		    if (siteName.getName().equals(defSiteName)) {
            	            return true;
            			}
            		}
        		}
        	}
        	return false;
        }

        boolean isAdopted(Name name) {
            if (adoptedSites != null) {
                return adoptedSites.contains(name);
            } else {
                return false;
            }
        }

        private String getLoopModifier() {
            int loopIx = getLoopIndex();
            if (loopIx >= 0) {
                return "#" + String.valueOf(loopIx);
            } else {
                return null;
            }
        }

        private String addLoopModifier() {
            int loopIx = getLoopIndex();
            if (loopIx >= 0) {
                return '#' + String.valueOf(loopIx);
            } else {
                return "";
            }
        }

        Map<String, Object> getCache() {
            if (cache == null) {
                if (readOnlyCache != null) {
                    //throw new UnsupportedOperationException("this context entry does not have write access to the cache");
                }
                
                cache = newHashMap(Object.class);
            }
            return cache;
        }

        Map<String, Object> getKeepCache() {
            if (keepMap == null) {
                keepMap = newHashMap(Pointer.class);
            }
            
            if (keepCache == null) {
                // cache the keep cache in the owner entry in the context
                Entry containerEntry = getOwnerContainerEntry(def);
                if (containerEntry != null) {
                    String key = def.getName() + ".keep";
                    
                    Map<String, Object> containerCache = containerEntry.getCache();
                    synchronized (containerCache) {
                        keepCache = (Map<String, Object>) containerCache.get(key);
                        if (keepCache == null) {
                            keepCache = newHashMap(Object.class);
                            containerCache.put(key, keepCache);
                            keepCache.put("from", keepMap);
                        } else {
                            vlog(" ---)> retrieving keep cache for " + def.getName() + " from " + containerEntry.def.getName());
                        }
                    }
                } else {
                    keepCache = newHashMap(Object.class);
                }                    
            } else {
                // make sure the keep cache is cached in the owner entry 
                // in the context
                Entry containerEntry = getOwnerContainerEntry(def);
                if (containerEntry != null) {
                    String key = def.getName() + ".keep";
                    Map<String, Object> containerCache = containerEntry.getCache();
                    Map<String, Object> containerKeepCache = (Map<String, Object>) containerCache.get(key);
                    if (containerKeepCache == null) {
                        keepCache.put("from", keepMap);
                        containerCache.put(key, keepCache);
                    }                
                }
            }
            return keepCache;
        }
        
        void addKeepCache(Map<String, Object> cache) {
            if (keepCache == null) {
                keepCache = newHashMap(Object.class);
            }
            Set<Map.Entry<String, Object>> entrySet = cache.entrySet();
            Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                String key = entry.getKey();
                if (!key.equals("from")) {
                    keepCache.put(entry.getKey(), entry.getValue());
                }
            }
            if (keepMap == null) {
                keepMap = newHashMap(Pointer.class);
                keepCache.put("from", keepMap);
            }
            Map<String, Pointer> map = (Map<String, Pointer>) cache.get("from");
            if (map != null) {
                keepMap.putAll(map);
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof Entry) {
                Entry entry = (Entry) obj;
                if (def.equals(entry.def) && args.equals(entry.args)) {
                	if (superdef == null) {
                		return (entry.superdef == null);
                	} else {
                		return superdef.equals(entry.superdef); 
                	}
                }
            }
            return false;
        }
        
        public boolean covers(Definition def) {
            if (def.equals(this.def) || def.equals(superdef)) {
                return true;
            } else {
                return def.equalsOrExtends(this.def.getSuperDefinition());
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(def.getName());
            
            sb.append(" <");
            if (superdef != null) {
                sb.append(superdef.getName());
            }

            sb.append("> A:");
            if (args != null && args.size() > 0) {
                sb.append(args.toString());
            } else {
                sb.append("()");
            }

            sb.append(" P:");
            if (params != null && params.size() > 0) {
                sb.append(params.toString());
            } else {
                sb.append("()");
            }
            
            if (loopIx > -1) {
                sb.append("@" + loopIx);
            }
            return sb.toString();
        }

        int getState() {
            return contextState;
        }

        void setState(int state) {
            contextState = state;
        }

        int getLoopIndex() {
            return loopIx;
        }

        void advanceLoopIndex() {
            loopIx = loopIndexFactory.nextState();
        }

        void resetLoopIndex() {
            loopIx = -1;
        }
        
        public boolean isInLoop() {
            return loopIx != -1;
        }

        public Entry getPrevious() {
            return link;
        }

        void setPrevious(Entry entry) {
            // decrement the ref count in the old link
            if (link != null) {
                link.refCount--;
            }
            link = entry;
            if (link != null) {
                link.refCount++;
            }
        }
        void incRefCount() {
            refCount++;
        }

        void decRefCount() {
            refCount--;
        }
        
        Map<String, Object> getSiteCache(Definition def) {
        	if (siteCache != null) {
        		return siteCache;
        	} else if (siteCacheMap != null) {
                Site site = def.getSite();
                if (site != null) {
        		    return siteCacheMap.get(site.getName());
                }
        	}
            return null;
        }
        
        void setSiteCache(Map<String, Object> siteCache) {
            this.siteCache = siteCache;
        }

        void setSiteCacheMap(Map<String, Map<String, Object>> siteCacheMap, List<Name> adoptedSites) {
            this.siteCacheMap = siteCacheMap;
            this.adoptedSites = adoptedSites;
        }
    }

    class ContextIterator implements Iterator<Entry> {
        private Entry nextEntry;

        public ContextIterator() {
            nextEntry = topEntry;
        }

        public boolean hasNext() {
            return (nextEntry != null);
        }

        public Entry next() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }
            Entry entry = nextEntry;
            nextEntry = nextEntry.getPrevious();
            return entry;
        }

        public void remove() {
            throw new UnsupportedOperationException("ReverseIterator does not support remove");
        }
    }
}

class ContextMarker {
    Context rootContext = null;
    int stateCount = -1;
    int loopIndex = -1;

    public ContextMarker() {}

    public ContextMarker(Context context) {
        context.mark(this);
    }

    public boolean equals(Object object) {
        if (object instanceof ContextMarker) {
            ContextMarker marker = (ContextMarker) object;
            if (loopIndex >= 0) {
            	SiteBuilder.vlog("comparing context marker loop indices: " + loopIndex + " to " + marker.loopIndex);
            }
            return (marker.rootContext == rootContext && marker.stateCount == stateCount && marker.loopIndex == loopIndex);
        } else {
            return object.equals(this);
        }
    }
    public int hashCode() {
        int n = (rootContext.getRootEntry().hashCode() << 16) + stateCount;
        return n;
    }
}