/* Bento
 *
 * $Id: ResolvedInstance.java,v 1.39 2015/06/19 13:20:04 sthippo Exp $
 *
 * Copyright (c) 2003-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.*;

import java.util.*;


/**
 * An ResolvedInstance is an Instantiation with a fixed resolution context.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.39 $
 */
public class ResolvedInstance extends Instantiation { //implements Value {

    private Context resolutionContext;
    private Object data;
    private Definition def;

    
    public static ArgumentList resolveArguments(ArgumentList args, Context context) throws Redirection {
        if (args == null || args.size() == 0) {
            return args;
        }
        ArgumentList resolvedArgs = args;
        Context sharedContext = context;
        for (int i = 0; i < args.size(); i++) {
            Construction arg = args.get(i);
            if (arg instanceof Instantiation && !(arg instanceof ResolvedInstance)) {
                Instantiation argInstance = (Instantiation) arg;
                if (resolvedArgs == args) {
                    resolvedArgs = new ArgumentList(args);
                    sharedContext = context.clone(false);
                }
                ResolvedInstance ri = new ResolvedInstance(argInstance, sharedContext);
                resolvedArgs.set(i, ri);
            }
        }
        
        return resolvedArgs;
    }
    
    public static ArgumentList instantiateArguments(ArgumentList args, Context context) throws Redirection {
        if (args == null || args.size() == 0) {
            return args;
        }
        ArgumentList resolvedArgs = args;
        for (int i = 0; i < args.size(); i++) {
            Construction arg = args.get(i);
            if (arg instanceof Instantiation) {
                Instantiation argInstance = (Instantiation) arg;
                if (resolvedArgs == args) {
                    resolvedArgs = new ArgumentList(args);
                }
                Object data = argInstance.generateData(context, null);
                resolvedArgs.set(i, new PrimitiveValue(data));
            }
        }
        
        return resolvedArgs;
    }
    
    
    public ResolvedInstance(Definition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, args, indexes);
        if (args != null && args.isDynamic()) {
            args = instantiateArguments(args, context);
            setArguments(args);
        }
        ParameterList params = def.getParamsForArgs(args, context);
        if (def instanceof BoundDefinition) {
        	resolutionContext = ((BoundDefinition) def).getBoundContext();
        } else {
            try {
                context.push(def, params, args, false);
                resolutionContext = context.clone(false);
            } finally {
                context.pop();
            }
        }
        data = null;
        this.def = def;
        Definition owner = context.getDefiningDef();
        if (owner.equals(def)){
            owner = owner.getOwner();
        }
        setOwner(owner);
        
    }


    public ResolvedInstance(ResolvedInstance instance) {
        super();
        resolutionContext = instance.getResolutionContext();
        data = instance.getValue();
        def = instance.def;
        owner = instance.owner;
        parent = instance.parent;
        children = instance.children;
        firstToken = instance.firstToken;
        lastToken = instance.lastToken;
        is_dynamic = instance.is_dynamic;
        is_static = instance.is_static;
        localDef = instance.localDef;
        explicitDef = instance.explicitDef;
        classDef = instance.classDef;
        externalDef = instance.externalDef;
        isParam = instance.isParam;
        isParamChild = instance.isParamChild;
        kind = instance.kind;
        reference = instance.reference;
        trailingDelimiter = instance.trailingDelimiter;
        args = instance.args;
        indexes = instance.indexes;
    }


    public ResolvedInstance(Instantiation instance, Context context) {
        this(instance, context, false);
    }

    protected ResolvedInstance(Instantiation instance, Context context, boolean shared) {
        super();

        // make a writeable clone unless shared flag is true
        resolutionContext = shared ? context : context.clone(false);
        data = null;
        def = null;

        owner = instance.owner;
        parent = instance.parent;
        children = instance.children;
        firstToken = instance.firstToken;
        lastToken = instance.lastToken;
        is_dynamic = instance.is_dynamic;
        is_static = instance.is_static;
        localDef = instance.localDef;
        explicitDef = instance.explicitDef;
        classDef = instance.classDef;
        externalDef = instance.externalDef;
        isParam = instance.isParam;
        isParamChild = instance.isParamChild;
        kind = instance.kind;
        reference = instance.reference;
        trailingDelimiter = instance.trailingDelimiter;

        Context sharedContext = null;
        args = instance.args;
//        if (args != null && args.size() > 0) {
//            int numArgs = args.size();
//            ArgumentList resolvedArgs = new ArgumentList(Context.newArrayList(numArgs, Construction.class));
//            try {
//                for (int i = 0; i < numArgs; i++) {
//                    Construction arg = args.get(i);
//
//                    // see if we need to resolve the argument
//                    if (arg instanceof Instantiation) {
//                        Instantiation argInstance = (Instantiation) arg;
//                        if (!argInstance.isParameterKind()) {
//                            if (sharedContext == null) {
//                                sharedContext = context.clone(false);
//                            }
//                            arg = new ResolvedInstance(argInstance, sharedContext, false);
//                        }
//                    
//                    } else if (arg instanceof ValueGenerator) {
//                        arg = (Construction) ((ValueGenerator) arg).getValue(context);
//                    }
//                    resolvedArgs.add(arg);
//                }
//                args = resolvedArgs;
//
//            } catch (Throwable t) {
//                String message = "Problem resolving arguments: " + t.toString();
//                log(message);
//            }
//        }

        indexes = instance.indexes;
        if (indexes != null && indexes.size() > 0) {
            int numIndexes = indexes.size();
            List<Index> resolvedIndexes = Context.newArrayList(numIndexes, Index.class);
            try {
                for (int i = 0; i < numIndexes; i++) {
                    Index index = indexes.get(i);

                    BentoNode child = index.getChild(0);
                    // see if we need to resolve the argument
                    if (child instanceof ResolvedInstance) {
                        ; // nothing to do; already resolved
                    } else if (child instanceof Instantiation) {
                        if (sharedContext == null) {
                            sharedContext = context.clone(false);
                        }
                        child = new ResolvedInstance((Instantiation) child, sharedContext, true);
                    } else if (child instanceof Expression) {
                        if (sharedContext == null) {
                            sharedContext = context.clone(false);
                        }
                        child = ((Expression) child).resolveExpression(sharedContext);
                    } else if (child instanceof ValueGenerator) {
                        child = (BentoNode) ((ValueGenerator) child).getValue(context);
                    }
                    index = (Index) index.clone();
                    index.setChild(0, (AbstractNode) child);
                    resolvedIndexes.add(index);
                }
                indexes = resolvedIndexes;

            } catch (Throwable t) {
                String message = "Problem resolving indexes: " + t.toString();
                log(message);
            }
        }
    }

    public Context getResolutionContext() {
        return resolutionContext;
    }

    public Object getData(Context context) throws Redirection {
        return super.getData(resolutionContext, getDefinition(context));
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        if (def == null) {
            def = getDefinition();
        }
        return instantiate(context, def);
//        return super.generateData(resolutionContext, def);
    }

    public List<Construction> generateConstructions(Context context) throws Redirection {
        return super.generateConstructions(resolutionContext);
    }

    public Definition getDefinition(Context context) {
        if (def == null) {
            return super.getDefinition(resolutionContext);
        } else {
            return def;
        }
    }

    public Type getType(Context context, Definition resolver) {
        return super.getType(resolutionContext, resolver);
    }

    public Object instantiate(Context context, Definition definition, ArgumentList args, List<Index> indexes) throws Redirection {
        Object data = null;
        int numPushes = 0;
        try {
            numPushes = resolutionContext.pushParts(this);
            data = super.instantiate(resolutionContext, definition, args, indexes);
        } finally {
            while (numPushes-- > 0) {
                resolutionContext.pop();
            }
        }
        return data;
    }

    //
    // Following are overloaded versions of the above with no context parameter
    //
    public Object generateData() throws Redirection {
        data = super.generateData(resolutionContext, null);
        return data;
    }

    public List<Construction> generateConstructions() throws Redirection {
        return super.generateConstructions(resolutionContext);
    }

    public Definition getDefinition() {
        if (def == null) {
            def = super.getDefinition(resolutionContext);
        }
        return def;
    }

    public Type getType(Definition resolver) {
        return super.getType(resolutionContext, resolver);
    }

    // Get data without a context
    public String getString() {
        try {
			return getString(resolutionContext);
		} catch (Redirection r) {
			// TODO Auto-generated catch block
			r.printStackTrace();
			return "";
		}
        
    }

    public boolean getBoolean() {
        return getBoolean(resolutionContext);
    }

    public byte getByte() {
        return getByte(resolutionContext);
    }

    public char getChar() {
        return getChar(resolutionContext);
    }

    public int getInt() {
        return getInt(resolutionContext);
    }

    public long getLong() {
        return getLong(resolutionContext);
    }

    public double getDouble() {
        return getDouble(resolutionContext);
    }

    public Object getValue() {
        if (data == null) {
            try {
                data = getValue(resolutionContext);
            } catch (Redirection r) {
                data = new NullValue();
            }
        }
        return data;
    }

    public Class<?> getValueClass() {
        
        return getValue().getClass();
    }
    
    public boolean equals(Object obj) {
    	if (obj != null && obj instanceof ResolvedInstance) {
    		ResolvedInstance ri = (ResolvedInstance) obj;
    		return (def.equals(ri.def) && resolutionContext.equals(ri.resolutionContext));
    	}
    	return false;
    }
    
    public String toString(String prefix) {
        return super.toString(prefix) + " (resolved)";
    }

    public String toString() {
        return super.toString() + " (resolved)";
    }

}
