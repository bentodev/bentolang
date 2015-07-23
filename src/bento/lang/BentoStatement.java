/* Bento
 *
 * $Id: BentoStatement.java,v 1.6 2005/06/30 04:20:52 sthippo Exp $
 *
 * Copyright (c) 2002-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * BentoStatement is the base class of all bento statements.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

abstract public class BentoStatement extends AbstractNode {

    private AbstractNode contents;

    public BentoStatement() {
        super();
    }

    public BentoStatement(AbstractNode node) {
        super(node);
        if (node instanceof BentoStatement) {
            contents = ((BentoStatement) node).contents;
        }
    }

    protected void setContents(AbstractNode contents) {
        this.contents = contents;
    }

    public AbstractNode getContents() {
        return contents;
    }

    /** Some bento statements generate data, some do not.  Statement types must implement
     *  this method and return the appropriate value.
     */
    abstract public boolean isDynamic();

    /** Bento statements may or may not be primitives.  Primitives cannot have children, i.e.
     *  getContents won't return a block, even an empty one.
     */
    public boolean isPrimitive() {
        return (!(contents instanceof Block) && !(contents instanceof Expression));
    }


    /** Subclasses which represent constructions should override this method to
     *  return true.  A construction is a bento statement which is executed
     *  during the instantiation of the containing object.
     */
    public boolean isConstruction() {
        return false;
    }

    /** Subclasses which represent definitions should override this method to
     *  return true.
     */
    public boolean isDefinition() {
        return false;
    }

    /** Bento statements are not static data. */
    public boolean isStatic() {
        return false;
    }

    public String getName() {
        return toString().trim();
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        if (contents != null) {
            if (contents.isPrimitive()) {
                sb.append(" = ");
            } else {
                sb.append(' ');
            }
            sb.append(contents.toString(prefix));
            sb.append('\n');
        }
        return sb.toString();
    }
}

