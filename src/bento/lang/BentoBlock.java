/* Bento
 *
 * $Id: BentoBlock.java,v 1.6 2005/06/30 04:20:53 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;


/**
 * A BentoBlock is a container whose children are Bento statements.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public class BentoBlock extends Block {

    public BentoBlock() {
        super();
    }

    public BentoBlock(AbstractNode[] children) {
        super(children);
    }

    public boolean isDynamic() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    //    public String toString() {
//        String str = "[=\n";
//        Iterator it = getChildren();
//        while (it.hasNext()) {
//            BentoNode node = (BentoNode) it.next();
//            str = str + "\n    " + node.toString();
//        }
//        str = str + "\n=]\n";
//        return str;
//    }

    public String getTokenString(String prefix) {
        String str = prefix + "[=\n" + getChildrenTokenString(prefix + "    ") + prefix + "=]\n";
        return str;
    }

    public String toString(String prefix) {
        String str = "[=\n" + super.toString(prefix) + prefix + "=]";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}

