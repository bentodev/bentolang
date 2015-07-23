/* Bento
 *
 * $Id: ConcurrentBentoBlock.java,v 1.5 2014/10/24 20:01:36 sthippo Exp $
 *
 * Copyright (c) 2003-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * A ConcurrentBentoBlock is a BentoBlock in which each construction is executed
 * concurrently.  ConcurrentBentoBlocks are delimited by <code>[+</code> and <code>+]</code>
 * brackets.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class ConcurrentBentoBlock extends BentoBlock {

    public ConcurrentBentoBlock() {
        super();
    }


    public String getTokenString(String prefix) {
        String str = prefix + "[++\n" + getChildrenTokenString(prefix + "    ") + prefix + "++]\n";
        return str;
    }

    public String toString(String prefix) {
        String str = super.toString(prefix);
        str = str.substring(2, str.length()).substring(2);
        str = "[++" + str + "++]";
        return str;
    }

}

