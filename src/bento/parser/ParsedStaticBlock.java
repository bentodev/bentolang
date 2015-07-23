/* Bento
 *
 * $Id: ParsedStaticBlock.java,v 1.4 2006/08/22 22:43:27 sthippo Exp $
 *
 * Copyright (c) 2002-2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.parser;

import bento.lang.*;

/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class ParsedStaticBlock extends StaticBlock implements Initializable {
    public ParsedStaticBlock(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        int n = children.length;
        if (n > 0) {
            if (children[0] instanceof StaticText) {
                ((StaticText) children[0]).setTrimLeadingWhitespace(true);
            }
            if (children[n - 1] instanceof StaticText) {
                ((StaticText) children[n - 1]).setTrimTrailingWhitespace(true);

            // if this block has a catch block, then the second-to-last child is really the last item in the block
            } else if (getCatchBlock() != null && n > 1 && children[n - 2] instanceof StaticText) {
                ((StaticText) children[n - 2]).setTrimTrailingWhitespace(true);
            }
        }
    }

    void setCatch(Block block) {
        if (block != null) {
            setCatchBlock(block);
        }
    }
}
