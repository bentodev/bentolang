/* Bento
 *
 * $Id: DuplicateDefinitionException.java,v 1.4 2011/09/01 13:17:32 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 *  Exception thrown when an attempt is made to add a definition with the same
 *  full name and signature as a definition previously added.
 */
public class DuplicateDefinitionException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public DuplicateDefinitionException() {
        super();
    }
    public DuplicateDefinitionException(String str) {
        super(str);
    }
}
