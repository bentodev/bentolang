/* Bento
 *
 * $Id: ArrayReferenceException.java,v 1.4 2011/08/29 19:29:58 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 *  Exception thrown on illegal array references (for example, a reference to
 *  a non-array object with an array index)
 */
public class ArrayReferenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ArrayReferenceException() {
        super();
    }
    public ArrayReferenceException(String str) {
        super(str);
    }
}
