/* Bento
 *
 * $Id: Continuation.java,v 1.4 2011/09/12 13:20:58 sthippo Exp $
 *
 * Copyright (c) 2003-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 *  A Continuation is thrown by a Bento <code>continue</code> statement.  This transfers
 *  the current construction of the page to a different definition.
 */
public class Continuation extends Throwable {

    private static final long serialVersionUID = 1L;
    
    String location;

    public Continuation(String location) {
        super();
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
