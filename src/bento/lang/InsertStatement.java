/* Bento
 *
 * $Id: InsertStatement.java,v 1.2 2006/09/09 18:12:30 sthippo Exp $
 *
 * Copyright (c) 2006 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;


/**
 * InsertStatement representes an insert statement, a directive for inserting classes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.2 $
 */
public class InsertStatement extends BentoStatement {

    public static final int ABOVE = 1;
    public static final int BELOW = 2;
	
	private int insertionPoint = 0;

    public InsertStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }

    protected void setInsertionPoint(int insertionPoint) {
        this.insertionPoint = insertionPoint;
    }

    public int getInsertionPoint() {
        return insertionPoint;
    }

    public Type getInsertedType() {
    	return (Type) getChild(0);
    }
    
    public Type getTargetType() {
    	return (Type) getChild(1);
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append("insert ");
        sb.append(getInsertedType().getName());
        switch (insertionPoint) {
            case ABOVE:
            	sb.append(" above ");
            	break;
            case BELOW:
            	sb.append(" below ");
            	break;
            default:
            	sb.append(" [unspecified insertion point] ");
                break;
        }
        sb.append(getTargetType().getName());
        return sb.toString();
    }
}
