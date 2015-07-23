/* Bento
 *
 * $Id: bento_context.java,v 1.10 2013/07/08 16:25:24 sthippo Exp $
 *
 * Copyright (c) 2002-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.List;



/**
 * This interface corresponds to the bento_context object, defined in core and representing
 * a Bento construction context, which can be used to construct objects.
 */
public interface bento_context {

    /** Returns the name of the site at the top of this context. **/
    public String site_name();

    /** Returns the cached value, if any, for a particular name in this context. **/
    public Object get(String name) throws Redirection;

    /** Sets the cached value, if any, for a particular name in this context. **/
    public void put(String name, Object data) throws Redirection;

    /** Constructs a Bento object of a particular name.  **/
    public Object construct(String name) throws Redirection;

    /** Constructs a Bento object of a particular name, passing in particular arguments.  **/
    public Object construct(String name, List<Construction> args) throws Redirection;

    /** Returns the context associated with the container of the current context. **/
    public bento_context container_context() throws Redirection;
    
    /** Returns the internal context object corresponding to this context. **/
    public Context getContext();
}


