/* Bento
 *
 * $Id: BentoContext.java,v 1.19 2014/12/31 14:03:17 sthippo Exp $
 *
 * Copyright (c) 2003-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import java.util.List;
import java.util.Map;

import bento.lang.ArgumentList;
import bento.lang.ComplexName;
import bento.lang.Construction;
import bento.lang.Definition;
import bento.lang.Instantiation;
import bento.lang.NameNode;
import bento.lang.Redirection;
import bento.lang.Site;
import bento.lang.bento_context;

/**
 * This object wraps a Context and implements a Bento bento_context object, defined in core and representing
 * a Bento construction context, which can be used to construct objects.
 */
public class BentoContext implements bento_context {

    /** Returns the internal context object associated with this context. **/
    private Context context;
    private Site site;
    private boolean initialized;
    private boolean inUse = false;

    public BentoContext(BentoDomain bentoSite) {
        site = (Site) bentoSite.getMainOwner();
        context = bentoSite.getNewContext();
        initialized = true;
    }
 
    public BentoContext(Site site, Context context) {
        this.site = site;
        this.context = context;
        initialized = false;
    }
    
    public BentoContext(BentoContext bentoContext) {
        site = bentoContext.site;
        context = new Context(bentoContext.context, false);
        while (!context.peek().equals(context.getRootEntry())) {
            context.pop();
        }
        initialized = true;
    }
    
    private void init() {
        initialized = true;
        context = new Context(context, false);
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
    /** Returns the name of the site at the top of this context. **/
    public String site_name() {
        return site.getName();
    }
    
    /** Returns the cached value, if any, for a particular name in this context. **/
    public Object get(String name) throws Redirection {
        if (!initialized) {
            init();
        }
        return context.getData(null, name, null, null);
    }

    /** Sets the cached value, if any, for a particular name in this context. **/
    public void put(String name, Object data) throws Redirection {
        if (!initialized) {
            init();
        }
        // find the definition if any corresponding to this name
        Instantiation instance = new Instantiation(new NameNode(name), context.topEntry.def);
        Definition def = instance.getDefinition(context);
        
        context.putData(def, null, null, name, data);
    }

    /** Constructs a Bento object of a particular name.  **/
    public Object construct(String name) throws Redirection {
        return construct(name, null);
    }

    /** Constructs a Bento object of a particular name, passing in particular arguments.  **/
    public Object construct(String name, List<Construction> args) throws Redirection {
        if (!initialized) {
            init();
        }
        NameNode nameNode = (name.indexOf('.') > 0 ? new ComplexName(name) : new NameNode(name));
        Instantiation instance;
        if (args != null) {
            ArgumentList argList = (args instanceof ArgumentList ? (ArgumentList) args : new ArgumentList(args));
            instance = new Instantiation(nameNode, argList, null, context.topEntry.def);
        } else {
            instance = new Instantiation(nameNode, context.topEntry.def);
        }
        return instance.getData(context);
    }
    
    public BentoContext container_context() {
        if (context.size() <= 1) {
            return null;
        }
        context.unpush();            
        try {
            BentoContext containerContext = new BentoContext(site, context);
            // force the copy before unpopping
            containerContext.init();
            return containerContext;
        } finally {
            context.repush();
        }        
    }

    public Object get(Definition def) throws Redirection {
        return def.get(getContext());
    }

    public Object get(Definition def, ArgumentList args) throws Redirection {
        return def.get(getContext(), args);
    }

    public List<Object> get_array(Definition def) throws Redirection {
        return def.get_array(getContext());
    }
    
    public Map<String, Object> get_table(Definition def) throws Redirection {
        return def.get_table(getContext());
    }

    
    
    /** Returns the internal context object associated with this context. **/
    public Context getContext() {
        // don't return the uninitialized context, to prevent it being altered
        if (!initialized) {
            init();
        }
        return context;
    }
    
    public String toString() {
        return context.toHTML();
    }
}


