/* Bento
 *
 * $Id: ParsedStaticText.java,v 1.4 2012/05/14 13:05:26 sthippo Exp $
 *
 * Copyright (c) 2002-2012 by bentodev.org
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
public class ParsedStaticText extends StaticText {

    private boolean preserveLeading = false;
    private boolean preserveTrailing = false;

    public ParsedStaticText(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(BentoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    void setPreserveLeading(boolean preserve) {
        preserveLeading = preserve;
    }

    void setPreserveTrailing(boolean preserve) {
        preserveTrailing = preserve;
    }

    public void setTrimLeadingWhitespace(boolean trim) {
        if (preserveLeading) {
            super.setTrimLeadingWhitespace(false);
        } else {
            super.setTrimLeadingWhitespace(trim);
        }
    }

    public void setTrimTrailingWhitespace(boolean trim) {
        if (preserveTrailing) {
            super.setTrimTrailingWhitespace(false);
        } else {
            super.setTrimTrailingWhitespace(trim);
        }
    }
}
