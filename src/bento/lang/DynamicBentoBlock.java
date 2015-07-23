/* Bento
 *
 * $Id: DynamicBentoBlock.java,v 1.14 2015/02/09 14:33:44 sthippo Exp $
 *
 * Copyright (c) 2003-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;
import bento.parser.BentoParser;
import bento.parser.ParseException;

import java.util.*;
import java.io.StringReader;

/**
 * A DynamicBentoBlock is a BentoBlock which constructs Bento code.  DynamicBentoBlocks
 * are used by code comprehensions and are delimited by <code>[[</code> and <code>]]</code>
 * brackets.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.14 $
 */

public class DynamicBentoBlock extends BentoBlock implements CodeGenerator, DynamicObject {

    protected AbstractNode generatedNode = null;
    protected List<String> generatedProblems = null;
    private boolean initialized = false;

    public DynamicBentoBlock() {
        super();
    }

    protected DynamicBentoBlock(DynamicBentoBlock proto, Context context) {
        super();
        this.owner = proto.getOwner();
        this.children = proto.children;
        generatedNode = generateContents(context);
        initialized = true;
    }

    public AbstractNode[] generateChildren(Context context) {
        if (generatedNode == null) {
            generatedNode = generateContents(context);
        }
        return generatedNode.children;
    }
    
    private AbstractNode generateContents(Context context) {
        AbstractNode node = null;
        try {
            String code = generateCode(context);
            BentoParser parser = new BentoParser(new StringReader(code));
            Definition owner = getOwner();
            
            node = generateNode(parser);
            node.owner = owner;
            
            Site site = owner.getSite();
            Core core = site.getCore();
            node.jjtAccept(new Initializer(core, site, true), owner);
            node.jjtAccept(new Resolver(), null);
            Validater validater = new Validater();
            node.jjtAccept(validater, null);
            generatedProblems = validater.getProblems();
            
            // should there be a Linker?

        } catch (Redirection r) {
            System.out.println("Redirection generating dynamic code: " + r);
        } catch (Exception e) {
            System.out.println("Exception generating dynamic code: " + e);
        }
        return node;
    }
    
    protected AbstractNode generateNode(BentoParser parser) throws ParseException {
        return parser.generateBentoBlock();
    }
    
    public List<String> getProblems() {
        return generatedProblems;
    }

    public String generateCode(Context context) throws Redirection {
        BentoBlock delegatedBlock = new BentoBlock(children);
        return delegatedBlock.getText(context);
    }

    public List<Construction> getConstructions(Context context) {
        Block block = (Block) initForContext(context, null, null);
        return block.getConstructions();
    }

    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) {
        if (initialized) {
            return this;
        } else {
            return new DynamicBentoBlock(this, context);
        }
    }

    /** Returns true if this object is already initialized for the specified context,
     *  i.e., if <code>initForContext(context, args) == this</code> is true.
     */
    public boolean isInitialized(Context context) {
        return initialized;
    }

    
    public BentoNode getChild(int n) {
        return (generatedNode == null ? null : generatedNode.getChild(n));
    }

    public Iterator<BentoNode> getChildren() {
        return (generatedNode == null ? null : generatedNode.getChildren());
    }

    public int getNumChildren() {
        return (generatedNode == null ? 0 : generatedNode.getNumChildren());
    }

    public String getTokenString(String prefix) {
        String str = prefix + "[[\n" + getChildrenTokenString(prefix + "    ") + prefix + "]]\n";
        return str;
    }

    public String toString(String prefix) {
        String str = super.toString(prefix);
        str = str.substring(2, str.length()).substring(2);
        str = "[%" + str + "%]";
        return str;
    }

}

