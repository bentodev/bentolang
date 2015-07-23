/* Bento
 *
 * $Id: Name.java,v 1.15 2013/04/09 13:18:30 sthippo Exp $
 *
 * Copyright (c) 2002-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * Interface for named objects.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.15 $
 */

public interface Name {

    // Special names.

    /** The name of unnamed objects other than a site. */
    public final static String ANONYMOUS = "";

    /** The name of an unnamed site. */
    public final static String DEFAULT = "default";

    /** The definition of the current instantiation. */
    public final static String DEF = "def";

    /** A special name referring to the table where the current instantiation is cached. */
    public final static String CACHE = "cache";

    /** A special name referring to the container of the current instantiation. */
    public final static String CONTAINER = "container";

    /** A special name referring to the size of a collection. */
    public final static String COUNT = "count";

    /** A special name referring to the core. */
    public final static String CORE = "core";

    /** A special name referring to the current context. */
    public final static String HERE = "here";

    /** A special name referring to the keys in a table. */
    public final static String KEYS = "keys";

    /** A special name referring to a local type that is a subclass of a
     *  more global type with the same name, e.g.
     *  
     *      foo [=
     *          my page [/]
     *      =]
     *
     *  defines a type called page local to foo which is a subclass
     *  of the page type defined in the container hierarchy above foo 
     */
    public final static String MY = "my";

    /** A special name referring to the object which this object extends
     *  laterally, i.e. which this object is inserted in front of in the sub
     *  or super inheritance chain. 
     */
    public final static String NEXT = "next";

    /** A special name referring to the object which overrides this object,
     *  i.e., an object of the same name as this object which is a child of 
     *  the immediate subclass of the owner of this object.
     */
    public final static String OVER = "over";

    /** A special name referring to the owner of an object, or, if no object
     *  is specified, the owner of the definition currently being constructed.
     */
    public final static String OWNER = "owner";

    /** A special name referring to the current site. */
    public final static String SITE = "site";

    /** A special name referring to the keys in a table, sorted in their natural order. */
    public final static String SORTED_KEYS = "sorted_keys";

    /** A special name referring to the source code of an object. */
    public final static String SOURCE = "source";

    /** A special name referring to the subdefinition of an object, or, if no object
     *  is specified, the subdefinition of the definition currently being constructed.
     */
    public final static String SUB = "sub";

    /** A special name referring to the superdefinition of an object, or, if no object
     *  is specified, the superdefinition of the definition currently being constructed.
     */
    public final static String SUPER = "super";

    /** A special name referring to the current definition. */
    public final static String THIS = "this";

    /** A special name referring to the current context. */
    public final static String THIS_CONTEXT = "this_context";

    /** A special name referring to the type of a definition. */
    public final static String TYPE = "type";

    /** A special name referring to the object being overridden, i.e., the 
     *  object of the same name as this object which is a child of the immediate
     *  superclass of the owner of this object.
     */
    public final static String UNDER = "under";

    public String getName();
}
