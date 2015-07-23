/* Bento
 *
 * $Id: StaticBlock.java,v 1.4 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 * A StaticBlock is a container whose children are static by default.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class StaticBlock extends Block {

    public StaticBlock() {
        super();
    }

    public StaticBlock(AbstractNode[] children) {
        super(children);
    }

    public boolean isStatic() {
        return true;
    }

    public boolean isDynamic() {
        return false;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    public String toString(String prefix) {
        String str = "[|\n" + super.toString(prefix) + prefix + "|]\n";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}