/* Bento
 *
 * $Id: bento_domain.java,v 1.14 2014/12/31 14:03:16 sthippo Exp $
 *
 * Copyright (c) 2002-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.Map;

/**
 * This interface corresponds to the bento_domain object, defined in core and representing
 * a Bento domain, which consists of Bento code loaded under a particular set of restrictions.
 * Multiple bento_domains may be combined together into a single Bento application, but only by
 * dividing the application into multiple sites, since a site can only have one immediate
 * parent domain.
 */
public interface bento_domain {

    /** Sites in this domain (keyed on site name). **/
    public Map<String, Site> sites();

    /** The name of the main site.  The main site is the first site to be queried when a name
     *  does not explicitly specify a site (followed by the default site and core).
     **/
    public String main_site();
    
    public String name();

    /** Returns the definition table associated with this domain. **/
    public Map<String, Definition> defs();

    /** Creates a bento_context object which can be used to construct Bento objects.  The
     *  bento_context will be able to construct objects whose definitions are in any of the
     *  sites in this domain.
     */
    public bento_context context();

    
    public Object get(String expr) throws Redirection;
    public Definition get_definition(String expr) throws Redirection;
    public Object get_instance(String expr) throws Redirection;
    public Object[] get_array(String expr) throws Redirection;
    public Map<String, Object> get_table(String expr) throws Redirection;

    /** Returns the existing child domain with a given name, or null if it does not exist. **/
    public bento_domain child_domain(String name);
    
    /** Creates a new domain which is a child of this domain. **/
    public bento_domain child_domain(String name, String type, String src, boolean isUrl);
    public bento_domain child_domain(String name, String type, String path, String filter, boolean recursive);
}


