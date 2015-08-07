/* Bento
 *
 * $Id: ArgumentList.java,v 1.24 2015/04/01 13:11:27 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.*;

import bento.runtime.Context;

/**
 * An ArgumentList is a list of arguments.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.24 $
 */
public class ArgumentList extends ListNode<Construction> {

    /** Object inserted into argument lists on the stack to represent missing arguments. */
    public final static Construction MISSING_ARG = new Construction() {

        public boolean getBoolean(Context context)                   { return false; }
        public String getText(Context context)                       { return null; }
        public Object getData(Context context)                       { return null; }
        public Object getData(Context context, Definition def)       { return null; }
        public boolean isAbstract(Context context)                   { return false; }
        public Type getType(Context context, Definition resolver)    { return null; }
        public String getDefinitionName()                            { return null; }
        public Construction getUltimateConstruction(Context context) { return this; }
        public String toString()                                     { return "(missing arg)"; }
        public String getString(Context context) throws Redirection  { return null; }
        public byte getByte(Context context) throws Redirection      { return 0; }
        public char getChar(Context context) throws Redirection      { return 0; }
        public int getInt(Context context) throws Redirection        { return 0; }
        public long getLong(Context context) throws Redirection      { return 0; }
		public double getDouble(Context context) throws Redirection  { return 0; }
		public Value getValue(Context context) throws Redirection    { return NullValue.NULL_VALUE; }
    };

    private boolean dynamic = false;
    private boolean concurrent = false;

    public ArgumentList() {
        super();
    }

    public ArgumentList(boolean dynamic) {
        super(1);
        this.dynamic = dynamic;
    }

    public ArgumentList(int capacity) {
        super(capacity);
    }

    public ArgumentList(ArgumentList args) {
        super(Context.newArrayList(args));
        setDynamic(args.isDynamic());
        setConcurrent(args.isConcurrent());
    }
    
    public ArgumentList(List<Construction> list) {
        super(list);
    }
    
    public ArgumentList(BentoNode[] nodes) {
        super();
        init(nodes);
    }    

    protected void init(BentoNode[] nodes) {
        
        int len = (nodes == null ? 0 : nodes.length);       
        
        List<Construction> list = new ArrayList<Construction>(len);
        
        for (int i = 0; i < len; i++) {
            BentoNode node = nodes[i];
            
            if (node instanceof Construction) {
                list.add((Construction) node);
            } else if (node instanceof Definition) {
                Instantiation instance = new Instantiation(node);
                list.add(instance);
            } else {
                list.add(new PrimitiveValue(node));
            }
        }
        
        setList(list);
    }

    public boolean equals(Object obj) {
        if (obj instanceof List<?> && ((List<?>) obj).size() == size()) {
            Iterator<?> thisIt = iterator();
            Iterator<?> otherIt = ((List<?>) obj).iterator();
            while (thisIt.hasNext()) {
                if (!thisIt.next().equals(otherIt.next())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if these are dynamic arguments, i.e. enclosed in (: :) rather
     *  than ( )
     **/
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /** Returns true if this a concurrent argument list, i.e. enclosed in (+ +)
     *  rather than ( )
     **/
    public boolean isConcurrent() {
        return concurrent;
    }

    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
    }

    public Object clone() {
        return new ArgumentList(this);
    }

}