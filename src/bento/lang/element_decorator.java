/* Bento
 *
 * $Id: element_decorator.java,v 1.2 2012/04/18 18:26:46 sthippo Exp $
 *
 * Copyright (c) 2012 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * When collections are instantiated, an site may want control over how
 * the individual elements are presented -- for instance, whether they 
 * are quoted or not.  This interface corresponds to the Bento definition
 * that specifies such a decorator.
 */
public interface element_decorator {

    /** Decorate a collection element. **/
    public String decorate_element(Object data);
}


