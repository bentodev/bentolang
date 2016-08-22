/* Bento
 *
 * $Id: BentoObjectWrapper.java,v 1.11 2015/07/10 12:51:11 sthippo Exp $
 *
 * Copyright (c) 2005-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import java.util.List;

import bento.lang.*;

/**
 * @author Michael St. Hippolyte
 * @version $Revision: 1.11 $
 */

public class BentoObjectWrapper {

    Construction construction;
    Context context;
    Type type;
    Definition def;

    /** Constructs a new BentoWrapperObject, given an arbitrary Bento construction
     *  and a BentoSite.
     */
    public BentoObjectWrapper(Construction construction, BentoDomain site) {
        this.construction = construction;
        if (construction instanceof ResolvedInstance) {
            ResolvedInstance ri = (ResolvedInstance) construction;
            context = ri.getResolutionContext();
            def = ri.getDefinition();
            type = ri.getType();
                    
        } else {
            context = site.getNewContext();
            type = construction.getType(context, false);
            def = type.getDefinition();
        }
    }

    /** Constructs a new BentoWrapperObject, given a definition, data
     *  and context.
     */
    public BentoObjectWrapper(Definition def, ArgumentList args, List<Index> indexes, Context context) throws Redirection {
        def = def.getSubdefInContext(context);
        ResolvedInstance ri = new ResolvedInstance(def, context, args, indexes);
        construction = ri;
        
        Context resolutionContext = ri.getResolutionContext();
        this.context = (resolutionContext == context ? context.clone(false) : resolutionContext);
        
        this.def = def;
        type = def.getType();
    }

    public Definition getDefinition() {
    	return def;
    }
    
    public Type getType() {
    	return type;
    }
    
    public Context getResolutionContext() {
        return context;
    }
    
    public Construction getConstruction() {
        return construction;
    }
    
    public ArgumentList getArguments() {
        if (construction instanceof Instantiation) {
            return ((Instantiation) construction).getArguments();
        } else {
            return null;
        }
    }
    
    public Object getData() throws Redirection {
        return construction.getData(context);
    }

    public String getText() throws Redirection {
        Object data = construction.getData(context);
        if (data instanceof BentoObjectWrapper) {
            return null;
        } else {
            return AbstractConstruction.getStringForData(data);
        }
    }

    public Object getChildData(String name) {
        return getChildData(name, null, null);
    }

    public Object getChildData(String name, Type type, ArgumentList args) {
        try {
            return def.getChildData(new NameNode(name), type, context, args);
        } catch (Redirection r) {
            return null;
        }
    }

    public Object getChildData(NameNode name) {
        try {
            return def.getChildData(name, null, context, name.getArguments());
        } catch (Redirection r) {
            return null;
        }
    }

    public boolean getChildBoolean(String name) {
        try {
            return PrimitiveValue.getBooleanFor(def.getChildData(new NameNode(name), null, context, null));
        } catch (Redirection r) {
            return false;
        }
    }

    public String getChildText(String name) {
        try {
            return PrimitiveValue.getStringFor(def.getChildData(new NameNode(name), null, context, null));
        } catch (Redirection r) {
            return null;
        }
    }

    public boolean isChildDefined(String name) {
        return def.hasChildDefinition(name);
    }
    
    public String toString() {
        return "(" + construction.toString() + ")";
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, boolean generate, boolean trySuper, Object parentObject, Definition resolver) throws Redirection {
        return def.getChild(node, args, indexes, parentArgs, context, generate, trySuper, null, resolver);
    }
}

