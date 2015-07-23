/* Bento
 *
 * $Id: ComplexName.java,v 1.14 2014/05/19 13:15:20 sthippo Exp $
 *
 * Copyright (c) 2002-2013 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.parser.BentoParser;

import java.util.*;
import java.io.StringReader;

/**
 * Base class for compound names.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.14 $
 */

public class ComplexName extends NameNode implements Name {

    public ComplexName() {
        super();
    }

    public ComplexName(AbstractNode node, int start, int end) {
        super();
        int len = end - start;
        children = new AbstractNode[len];
        for (int i = 0; i < len; i++) {
            children[i] = node.children[start + i];
        }
    }

    /** Combines two names into a new single name */
    public ComplexName(NameNode prefix, NameNode suffix) {
        super();
        int plen = (prefix instanceof ComplexName ? prefix.children.length : 1);
        int slen = (suffix instanceof ComplexName ? suffix.children.length : 1);

        children = new AbstractNode[plen + slen];

        if (prefix instanceof ComplexName) {
            for (int i = 0; i < plen; i++) {
                children[i] = prefix.children[i];
            }
        } else {
            children[0] = prefix;
        }

        if (suffix instanceof ComplexName) {
            for (int i = 0; i < slen; i++) {
                children[plen + i] = suffix.children[i];
            }
        } else {
            children[plen] = suffix;
        }
    }

    public ComplexName(NameNode node) {
        super();
        if (node instanceof ComplexName) {
            children = node.children;
        } else {
            setChild(0, node);
        }
    }


    public ComplexName(String name) {
        super(name);
        parseName();
    }

    public void setName(String name) {
        super.setName(name);
        parseName();
    }

    private void parseName() {
        try {
            BentoParser parser = new BentoParser(new StringReader(super.getName()));
            ComplexName generatedName = parser.parseComplexName();
            children = (AbstractNode[]) generatedName.children.clone();
        } catch (Exception e) {
            System.out.println("Exception parsing name in ComplexName: " + e);
            children = new AbstractNode[1];
            String name = '(' + super.getName() + ')';
            children[0] = new NameNode(name);
        }
    }

    
    public String getName() {
        if (cachedName != null) {
            return cachedName;
        }
        boolean isCacheable = true; 
        
        String name = null;
        Iterator<BentoNode> it = getChildren();
        BentoNode node = null;
        while (it.hasNext()) {
            node = it.next();
            if (node instanceof Name) {
                String n = ((Name) node).getName();
                if (name == null) {
                    name = n;
                } else {
                    name = name + '.' + n;
                }
                if (node instanceof NameNode) {
                    if (!((NameNode) node).nameCacheable) {
                        isCacheable = false;
                    }
                } else {
                    isCacheable = false;
                }
            } else if (node instanceof Dim) {
                name = name + "[]";
            }
        }
        if (isCacheable) {
            cachedName = name;
            nameCacheable = true;
        }
        return name;
    }

    
    public String getName(int start, int end) {
        int len = end - start;
        String name = null;
        Iterator<BentoNode> it = getChildren();
        BentoNode node = null;
        for (int i = 0; i < start + len; i++) {
            if (!it.hasNext()) {
                break;
            }
            node = it.next();
            if (i >= start) {
                if (node instanceof Name) {
                    String n = ((Name) node).getName();
                    if (name == null) {
                        name = n;
                    } else {
                        name = name + '.' + n;
                    }
                } else if (node instanceof Dim) {
                    name = name + "[]";
                }
            }
        }
        return name;
    }

    
    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>true</code> if the name has dynamic arguments in any
     *  part of the name, else <code>false</code>.
     */
    public boolean isDynamic() {
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof NameNode) {
                if (((NameNode) children[i]).isDynamic()) {
                    return true;
                }
            } else if (children[i] instanceof ArgumentList) {
                if (((ArgumentList) children[i]).isDynamic()) {
                    return true;
                }
            }
        }
        return false;
    }

    
    /** Returns <code>true</code> indicating that this is a complex name, i.e., its
     *  children are names.
     */
    public boolean isComplex() {
        return true;
    }

    public int numParts() {
        int n = getNumChildren();
        if (n == 1 && ((NameNode) getChild(0)).isComplex()) {
            n = ((NameNode) getChild(0)).numParts();
        }
        return n;        
    }

    /** Returns the first part of the name. */
    public NameNode getFirstPart() {
        NameNode firstNode = (NameNode) getChild(0);
        if (firstNode.isComplex()) {
            return firstNode.getFirstPart();
        } else {
            return firstNode;
        }
    }

    /** Returns the last part of the name. */
    public NameNode getLastPart() {
        NameNode lastNode = (NameNode) getChild(getNumChildren() - 1);
        if (lastNode.isComplex()) {
            return lastNode.getLastPart();
        } else {
            return lastNode;
        }
    }

    /** Returns the nth part of the name.  Tthrows an IndexOutOfBounds exception if
     *   there are fewer than n + 1 parts.
     */
    public NameNode getPart(int n) {
        if (n < getNumChildren()) {
            return (NameNode) getChild(n);
        } else {
            NameNode firstNode = (NameNode) getChild(0);
            if (firstNode.isComplex()) {
                return firstNode.getPart(n);
            } else {
                throw new IndexOutOfBoundsException("part number " + n + " is too big for name");
            }
        }
    }
    
        /** Returns true if this name has one or more arguments, else false. */
    public boolean hasArguments() {
        return (getArguments() != null);
    }


    /** Returns the list of arguments associated with this name. */
    public ArgumentList getArguments() {
        // this name has arguments if the last child name has arguments, or
        // is followed by arguments.
        int n = children.length;
        for (int i = n - 1; i >= 0; i--) {
            if (children[i] instanceof NameNode) {
                return ((NameNode) children[i]).getArguments();
            } else if (children[i] instanceof ArgumentList) {
                return (ArgumentList) children[i];
            }
        }
        return null;
    }


    /** Returns true if this name has one or more indexes, else false. */
    public boolean hasIndexes() {
        // this name has indexes if any child has indexes, or is an index.
        int n = children.length;
        for (int i = 0; i < n; i++) {
            if (children[i] instanceof NameNode && ((NameNode) children[i]).hasIndexes()) {
                return true;
            } else if (children[i] instanceof Index) {
                return true;
            }
        }
        return false;
    }

    /** Returns a list of indexes associated with this name, or null if none. */
    public List<Index> getIndexes() {
        // this name has indexes if the last child name has indexes, or
        // is followed by indexes.
        int n = children.length;
        for (int i = n - 1; i >= 0; i--) {
            if (children[i] instanceof NameNode) {
                return ((NameNode) children[i]).getIndexes();
            } else if (children[i] instanceof Index) {
                return new SingleItemList<Index>((Index) children[i]);
            }
        }
        return null;
    }

    public boolean matches(String name) {
        Iterator<BentoNode> it = getChildren();
        StringTokenizer tok = new StringTokenizer(name, ".");

        BentoNode node = null;
        String namePart = null;
        while (it.hasNext() && tok.hasMoreTokens()) {
            node = (BentoNode) it.next();
            namePart = tok.nextToken();
            if (node instanceof Name) {
                Name nm = (Name) node;
                if (nm instanceof RegExp) {

                    RegExp regexp = (RegExp) nm;

                    if (!regexp.matches(namePart)) {
                        return false;
                    }
                    if (nm instanceof AnyAny) {
                        if (it.hasNext()) {
                            node = (BentoNode) it.next();
                            if (!(node instanceof Name)) {
                                throw new RuntimeException("only a Name may follow an AnyAny");
                            }
                            String thisNamePart = ((Name) node).getName();
                            while (tok.hasMoreTokens() && !thisNamePart.equals(namePart)) {
                                namePart = tok.nextToken();
                            }
                        }
                    }

                } else {
                    if (!nm.getName().equals(namePart)) {
                        return false;
                    }
                }
            }
        }
        if (it.hasNext()) {
            return false;
        } else if (tok.hasMoreTokens()) {
            return (node instanceof AnyAny);
        } else {
            return true;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof ComplexName || obj instanceof ComplexType) {
            Iterator<BentoNode> it = getChildren();
            Iterator<BentoNode> otherIt = ((BentoNode) obj).getChildren();

            BentoNode node = null;
            BentoNode otherNode = null;
            while (it.hasNext() && otherIt.hasNext()) {
                node = it.next();
                otherNode = otherIt.next();
                if (node instanceof Name && otherNode instanceof Name) {
                    Name name = (Name) node;
                    Name otherName = (Name) otherNode;
                    if (name instanceof RegExp) {

                        if (otherName instanceof RegExp) {
                            throw new IllegalArgumentException("Cannot compare two names with regexps");
                        }

                        if (!((RegExp) name).matches(otherName.getName())) {
                            return false;
                        }
                        if (name instanceof AnyAny) {
                            if (it.hasNext()) {
                                node = it.next();
                                while (!node.equals(otherNode) && otherIt.hasNext()) {
                                    otherNode = otherIt.next();
                                }
                            }
                        }

                    } else if (otherName instanceof RegExp) {
                        if (!((RegExp) otherName).matches(name.getName())) {
                            return false;
                        }
                        if (otherName instanceof AnyAny) {
                            if (otherIt.hasNext()) {
                                otherNode = otherIt.next();
                                while (!otherNode.equals(node) && it.hasNext()) {
                                    node = it.next();
                                }
                            }
                        }
                    } else {
                        if (!name.getName().equals(otherName.getName())) {
                            return false;
                        }
                    }
                }
            }
            if (it.hasNext()) {
                return (otherNode instanceof AnyAny);
            } else if (otherIt.hasNext()) {
                return (node instanceof AnyAny);
            } else {
                return true;
            }

        } else if (obj instanceof Name) {
            Name otherName = (Name) obj;
            String name = getName();
            if (name == null) {
                return (otherName.getName() == null);
            } else {
                return name.equals(otherName.getName());
            }
        }
        return false;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        
        Iterator<BentoNode> it = getChildren();
        AbstractNode node = null;
        while (it.hasNext()) {
            node = (AbstractNode) it.next();
            sb.append(node.toString(""));
            if (it.hasNext()) {
                sb.append('.');
            }
        }
        return sb.toString();
    }


}
