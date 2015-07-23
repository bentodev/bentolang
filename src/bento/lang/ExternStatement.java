/* Bento
 *
 * $Id: ExternStatement.java,v 1.4 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * ExternStatement represents an extern statement, which declares a name to refer
 * to an external object of a particular binding.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class ExternStatement extends AdoptStatement {

    private String binding;

    public ExternStatement() {
        super();
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getBinding() {
        return binding;
    }
}

