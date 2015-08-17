/* Bento
 *
 * $Id: ComplexDefinition.java,v 1.108 2015/07/01 13:09:09 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.BentoObjectWrapper;
import bento.runtime.Context;
import bento.runtime.Holder;

import java.util.*;

/**
* ComplexDefinition is a named definition which may contain other named definitions.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.108 $
*/

public class ComplexDefinition extends NamedDefinition {

    public static ComplexDefinition getComplexOwner(Definition owner) {
        while (owner != null) {
            if (owner instanceof ComplexDefinition) {
                return (ComplexDefinition) owner;
            }
            owner = owner.getOwner();
        }
        return null;
    }

    private DefinitionTable definitions;

    public ComplexDefinition() {
        super();
    }

    public ComplexDefinition(Definition def, Context context) throws Redirection {
        super(def, context);
        if (def instanceof ComplexDefinition) {
            definitions = ((ComplexDefinition)def).definitions;
        }
    }

    public List<Dim> getDims() {
        int numChildren = getNumChildren();
        int ix = numChildren;
        while (ix > 0) {
            if (getChild(ix - 1) instanceof Dim) {
                ix--;
                continue;
            } else {
                break;
            }
        }
        int n = numChildren - ix;
        if (n == 0) {
            return new EmptyList<Dim>();
        } else if (n == 1) {
            return new SingleItemList<Dim>((Dim) getChild(ix));
        } else {
            List<Dim> dims = Context.newArrayList(n, Dim.class);
            for (int i = ix; i < ix + n; i++) {
                dims.add((Dim) getChild(i));
            }
            return dims;
        }
    }
   
    /** Returns true if this definition can have child definitions. */
    public boolean canHaveChildDefinitions() {
        return true;
    }

    void setDefinitionTable(DefinitionTable table) {
        definitions = table;
    } 

    DefinitionTable getDefinitionTable() {
        if (definitions == null) {
            return super.getDefinitionTable();
        } else {
            return definitions;
        }
    }

    public void addDefinition(Definition def, boolean replace) throws DuplicateDefinitionException {
        // don't add definitions without a proper name
        NameNode name = def.getNameNode();
        if (name == null) {
            return;
        }

        Definition currentDef = definitions.getDefinition((NamedDefinition) def.getOwner(), name);

        // Parameters can appear in multiple parameter lists, as long
        // as the type is the same.
        if (def.isFormalParam()) {
            if (currentDef != null && currentDef.getSite() == def.getSite()) {
                if (!(currentDef.isFormalParam())) {
                    throw new DuplicateDefinitionException(name.getName() + " already defined");
                } else if (!currentDef.getType().equals(def.getType())) {
                    throw new DuplicateDefinitionException("parameter " + name.getName() + " declared with more than one type");

                } else {
                    return;
                }
            }

        } else {
            setIsOwner(true);
        }

        synchronized (definitions) {
            definitions.addDefinition(def, replace);
        }
    }

    Definition getClassDefinition(NameNode type) {
        Definition def = null;
        for (NamedDefinition nd = getSuperDefinition(); nd != null; nd = nd.getSuperDefinition()) {
            def = nd.getExplicitChildDefinition(type);
            if (def != null) {
                break;
            }
        }
        return def;
    }

//    public Definition getChildDefinition(NameNode node, Context context) {
//        // this is not exactly right -- it will break if "owner" is
//        // overridden.  But checking the superclass first won't work
//        // because it interprets owner differently
//        if (node.getName() == Name.OWNER) {
//            return getOwnerInContext(context);
//        } else if (node.getName() == Name.SITE) {
//            return getSite();
//        } else {
//            return super.getChildDefinition(node, context);
//        }
//    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj) throws Redirection {
        if (context == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "getChild requires a context; none provided.");
        } else if (context.peek() == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "getChild requires a non-empty context; passed context is empty.");
        }
        NamedDefinition instantiatedDef = (NamedDefinition) context.peek().def;

        if (node.getName() == Name.OWNER) {
            Definition owner = getOwnerInContext(context);
            if (owner == null) {
                return null;
            }
            if (generate) {
                return owner.instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition((NamedDefinition) owner, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName().equals(Name.DEF)) {
            if (generate) {
                return instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(this, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName().equals(Name.THIS)) {
            if (generate) {
                // this does not deal with arguments or indexes
                return new BentoObjectWrapper(this, args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(this, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName() == Name.SITE) {
            if (generate) {
                return getSite().instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(getSite(), node);
                return aliasedDef.getDefInstance(args, indexes);
            }
            
        // strip off the owner prefix, if present
        } else if (node.isComplex() && ((Name) node.getChild(0)).getName() == Name.OWNER) {
            int n = node.getNumChildren();
            node = new ComplexName(node, 1, n);
            // if there's another owner prefix, forward to the owner
            if (node.getName() == Name.OWNER || (node.isComplex() && ((Name) node.getChild(0)).getName() == Name.OWNER)) {
                return getOwner().getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
            }
        }

        // if this is an alias, call the superclass to look up the definition
        // in the alias
//       if (isAlias()) {
//           return super.getChild(node, args, context, generate);
//       }

        // not an alias.  Check to see if this is a built-in field such as <code>type</code>
        // or <code>count</code>
        NameNode lastNode = node;
        int n = (node.isComplex() ? node.getNumChildren() : 0);
        if (n > 0) {
            lastNode = (NameNode) node.getChild(n - 1);
        }

        if (/* lastNode */ node.getName() == Name.TYPE) {
//            if (n <= 1) {
            	Definition ultimateDef = getUltimateDefinition(context);
                if (generate) {
                    return new PrimitiveValue(ultimateDef.getType());
                } else {
                    Definition typeDef = new TypeDefinition(ultimateDef);
                    return typeDef.getDefInstance(null, null);
                }

//            } else {
//                NameNode typeNode = new ComplexName(node, 0, n - 1);
//                if (generate) {
//                    return new PrimitiveValue(typeNode.getName());
//                } else {
//                    Definition typeDef = ((DefinitionInstance) getChild(typeNode, args, indexes, context, false, trySuper, parentObj)).def;
//                    if (typeDef == null) {
//                        return null;
//                    }
//                    typeDef = new TypeDefinition(typeDef);
//                    return typeDef.getDefInstance(null, null);
//                }
//            }
        } else if (lastNode.getName().equals(Name.KEYS)) {
            Definition keysDef = new KeysDefinition(this, context);
            if (generate) {
                return keysDef.instantiate(args, indexes, context);
            } else {
                return keysDef.getDefInstance(null, indexes);
            }
            
        } else if (lastNode.getName() == Name.COUNT) {
            if (n <= 1) {
                CollectionDefinition collectionDef = getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                }
                if (generate) {
                    return new PrimitiveValue(1);
                } else {
                    Definition countDef = new CountDefinition(this, context, args, indexes);
                    return countDef.getDefInstance(null, null);
                }
            } else {
                NameNode countNode = new ComplexName(node, 0, n - 1);
                DefinitionInstance defInstance = (DefinitionInstance) getChild(countNode, args, indexes, parentArgs, context, false, trySuper, parentObj);
                Definition countDef = defInstance.def;
                if (countDef == null) {
                    return (generate ? UNDEFINED : null);
                } else if (countDef instanceof DynamicObject) {
                    countDef = (Definition) ((DynamicObject) countDef).initForContext(context, args, indexes);
                }

                CollectionDefinition collectionDef = countDef.getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(lastNode, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                }
                
                if (generate) {
                    if (countDef instanceof CollectionDefinition) {
                        return new PrimitiveValue(((CollectionDefinition) countDef).getSize(context, args, indexes));
                    } else {
                        return new PrimitiveValue(1);
                    } 
                } else {
                    countDef = new CountDefinition(countDef, context, args, indexes);
                    return countDef.getDefInstance(null, null);
                }
            }
        }

        // next check to see if this is an external definition
        Definition def = getExternalDefinition(node, context);
        if (def != null && !generate) {
            return def.getDefInstance(args, indexes);
        }

        // now see if it's a complex name
        if (n > 1 && node.isComplex()) {
            NameNode prefix = (NameNode) node.getChild(0);
            NameNode suffix;
            if (n == 2) {
                suffix = (NameNode) node.getChild(1);
            } else {
                suffix = new ComplexName(node, 1, n);
            }
            ArgumentList prefixArgs = prefix.getArguments();
            ParameterList prefixParams = null;
            List<Index> prefixIndexes = prefix.getIndexes();
            Definition prefixDef = null;
            if (prefixArgs == null) {
                String nm = prefix.getName();
                String fullNm = getFullNameInContext(context) + "." + nm;
                Holder holder = context.getDefHolder(nm, fullNm, null, prefixIndexes, false);
                if (holder != null && holder.def != null && holder.def.getDurability() != DYNAMIC && isDynamic() && !((BentoNode) holder.def).isDynamic() && !holder.def.equals(context.getDefiningDef())) {
                    prefixDef = holder.def;
                    prefixArgs = holder.args;
                }
            }
            if (prefixDef == null) {
                DefinitionInstance defInstance = (DefinitionInstance) getChild(prefix, prefixArgs, prefix.getIndexes(), parentArgs, context, false, trySuper, parentObj);
                if (defInstance != null) {
                    prefixDef = defInstance.def;
                }
            }
            if (prefixDef != null) {
                if (prefixDef instanceof ExternalDefinition) {
                    ExternalDefinition externalDef = (ExternalDefinition) prefixDef;
                    prefixDef = externalDef.getDefForContext(context, args);
                }
                
                prefixParams = prefixDef.getParamsForArgs(prefixArgs, context);
            	
                // if the prefix definition is an alias, look up the aliased
                // definition and use that instead.
                if (prefixDef.isAliasInContext(context)) {
                    NameNode alias = prefixDef.isParamAlias() ? prefixDef.getParamAlias() : prefixDef.getAliasInContext(context);
                    ArgumentList aliasArgs = alias.getArguments();
                    List<Index> aliasIndexes = alias.getIndexes();

                    //                    int pushedParams = 0;
//                    boolean local = prefixDef.getAccess() == Definition.LOCAL_ACCESS;
//                    if (local) {
//                        pushedParams = (prefixParams == null ? 0 : prefixParams.size());
//                        for (int i = 0; i < pushedParams; i++) {
//                            context.pushParam((DefParameter) prefixParams.get(i), prefixArgs.get(i));
//                        }
//                    } else {
//                        context.push(instantiatedDef, prefixParams, prefixArgs, false);
//                    }

                    Definition aliasDef = null;
                    Context.Entry aliasEntry = context.getParameterEntry(alias, false);
                    if (aliasEntry == null) {
                        for (Definition owner = prefixDef.getOwner(); owner != null; owner = owner.getOwner()) {
                            Definition ownerInContext = owner.getSubdefInContext(context);
                            if (ownerInContext != null && !ownerInContext.equalsOrExtends(this)) {
                                DefinitionInstance defInstance = (DefinitionInstance) ownerInContext.getChild(alias, aliasArgs, aliasIndexes, parentArgs, context, false, trySuper, parentObj);
                                if (defInstance != null && defInstance.def != null) {
                                    aliasDef = defInstance.def;
                                    break;
                                }
                            }
                        }
                    }

                    if (aliasEntry != null) {
                        prefixDef = aliasEntry.def;
                        prefixParams = aliasEntry.params;
                        prefixArgs = aliasEntry.args;
                        prefixIndexes = aliasIndexes;

                    } else if (aliasDef != null) {
                        prefixDef = aliasDef;
                        prefixArgs = aliasArgs;
                        prefixParams = prefixDef.getParamsForArgs(prefixArgs, context);
                        prefixIndexes = null;
                    }

//                    if (local) {
//                        for (int i = 0; i < pushedParams; i++) {
//                           context.popParam();
//                        }
//                    } else {
//                        context.pop();
//                    }
                }
                if (prefixDef != null) {
                    try {
                        context.push(prefixDef, prefixParams, prefixArgs);
                        
                        Object child = prefixDef.getChild(suffix, args, indexes, parentArgs, context, generate, trySuper, parentObj);
    
                        if (!generate && child == null && prefixDef.isAliasInContext(context)) {
                            ComplexName childName = new ComplexName(prefixDef.getAliasInContext(context), suffix);
                            Definition externalDef = getExternalDefinition(childName, context);
                            if (externalDef != null) {
                                child = externalDef.getDefInstance(childName.getArguments(), childName.getIndexes());
                            }
                        }
                        if (child != null && child instanceof DefinitionInstance) {
                            //Definition childDef = ((DefinitionInstance) child).def;
                            //if (childDef.isAliasInContext(context)) {
                            //    Instantiation aliasInstance = childDef.getAliasInstanceInContext(context);
                            //    Definition aliasDef = aliasInstance.getDefinition(context, childDef);
                            //    child = aliasDef.getDefInstance(aliasInstance.getArguments(), aliasInstance.getIndexes());
                            //}
                        }
                        if (child == null && isAlias()) {
                            return super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                        } else if (child instanceof Definition) {
                            return ((Definition) child).getDefInstance(args, indexes);
                        } else {
                            return child;
                        }
                    } finally {
                        context.pop();
                    }
                }
            }
        }

        // is this a parameter?
        ParameterList params = getParamsForArgs(args, context);
        if (params != null && params.size() > 0) {
            Iterator<DefParameter> it = params.iterator();
            while (it.hasNext()) {
                DefParameter param = it.next();
                if (param.getName().equals(node.getName())) {
                    def = param;
                    break;
                }
            }
        }

        // next see if this is a fully named definition or an immediate child
        if (def == null) {
            def = getExplicitDefinition(node, args, context);
            //if (def != null) {
            //    def = def.getUltimateDefinition(context);
            //}
        }
        
        // if not, then try supertypes or alias.
        if (def == null) {
            // try alias
            if (isAlias()) {
                // look up the definition in the aliased definition

                //avoid recursion
                String aliasName = getAlias().getFirstPart().getName();
                if (!aliasName.equals(node.getName()) && !aliasName.equals(Name.THIS)  && !aliasName.equals(Name.OWNER)) {
                    
                    Instantiation aliasInstance = getAliasInstance();
                    Definition aliasDef = aliasInstance.getDefinition(context, this);

                    // if it's null, it's a problem, but not our problem.  We won't abort the
                    // current operation just because we found an undefined construction.  That
                    // will happen when/if the program tries to instantiate it.
                    
                    if (aliasDef != null) {
                        ArgumentList aliasArgs = aliasInstance.getArguments();
                        ParameterList aliasParams = aliasDef.getParamsForArgs(aliasArgs, context, false);
                        context.push(instantiatedDef, aliasParams, aliasArgs, false);
                        Object child = aliasDef.getChild(node, args, null, parentArgs, context, generate, trySuper, parentObj);
                        context.pop();
                        if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                            return child;
                        }
                    }
                }
            }

            // not an alias; see if it is a construction that defines the child
            AbstractNode contents = getContents();
            if (contents instanceof Construction) {
                Construction construction = ((Construction) contents).getUltimateConstruction(context);
            
                if (construction instanceof Instantiation) {
                    Definition contentDef = ((Instantiation) construction).getDefinition(context, this);
                    ArgumentList contentArgs = null;
                    ParameterList contentParams = null;
    
                    if (contentDef == null || contentDef == this) {
                        Type contentType = ((Instantiation) construction).getType(context, this);
                        if (contentType != null) {
                            contentDef = contentType.getDefinition();
                            if (contentDef != null) {
                                contentArgs = ((Instantiation) construction).getArguments(); // contentType.getArguments(context);
                                contentParams = contentDef.getParamsForArgs(contentArgs, context, false);
                            }
                        }
                    }
    
                    if (contentDef != null) {
                        context.push(contentDef, contentParams, contentArgs, false);
                        try {
    
                            Object child = context.getDescendant(contentDef, contentArgs, new ComplexName(node), generate, parentObj);
                            
                          //  Object child = contentDef.getChild(node, null, context, generate, trySuper);
                            if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                                return child;
                            }
                        } finally {
                            context.pop();
                        }
                    }
                } else  {
                    Type type = construction.getType(context, this);
                    if (type != null) {
                        Definition runtimeDef = type.getDefinition();
                        if (runtimeDef != null && runtimeDef.canHaveChildDefinitions()) {
                            Object child = runtimeDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                            if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                                return child;
                            }
                        }
                    }
                }
            }
            
            // no luck yet; try supertypes if trySuper is true
            //
            // There's a problem with this code: it ignores the args when getting the superdefinition,
            // which means that if there are multiple super definitions it may not pick the right one.
            if (trySuper) {
                NamedDefinition nd = getSuperDefinition();
                if (nd != null) {
                    Type st = getSuper(context);
                    ArgumentList superArgs = st.getArguments(context);
                    // if one of the super arguments is the name we're looking for, avoid
                    // calling getParamsForArgs, which could lead to infinite recursion
                    List<ParameterList> paramLists = nd.getParamLists();
                    boolean foundSame = false;
                    if (paramLists != null && paramLists.size() > 1) {
                        int numArgs = superArgs.size();
                        for (int i = 0; i < numArgs; i++) {
          
                            Construction superArg;
                            try {
                                superArg = superArgs.get(i);
                            } catch (Exception e) {
                                System.err.println("Exception in ComplexDefinition.getChild looking for supArg");
                                continue;
                            }
                            if (superArg instanceof Instantiation) {
                                BentoNode ref = ((Instantiation) superArg).getReference();
                                if (node.equals(ref)) {
                                    vlog("...can't look up " + node.getName() + " in supertype " + st.getName());
                                    foundSame = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!foundSame) {
                        ParameterList superParams = nd.getParamsForArgs(superArgs, context, false);
                        context.push(instantiatedDef, superParams, superArgs, false);
                        try {
                            Object child = nd.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                            if ((!generate && child != null) || (generate && child != UNDEFINED)) {
                                return child;
                            }
                        } finally {
                            context.pop();
                        }
                    }
                }

                // finally look to see if this is a de-aliased definition
                if (context.size() > 1) {
                    try {
                        context.unpush();
                        Definition precedingDef = context.peek().def;
                        if (precedingDef.isAlias() && getName().equals(precedingDef.getAlias().getName())) {
                            nd = precedingDef.getSuperDefinition();
                            if (!nd.equals(this) && nd.hasChildDefinition(node.getName())) {
                                Type superSt = precedingDef.getSuper();
                                ArgumentList superSuperArgs = superSt.getArguments(context);
                                ParameterList superSuperParams = nd.getParamsForArgs(superSuperArgs, context);
                                context.unpop(nd, superSuperParams, superSuperArgs);
                                try {
                                    return nd.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
                                } finally {
                                    context.repop();
                                }
                            }
                        }
                    } finally {
                        context.repush();
                    }
                }
                
                // add code to see what the parent type would be if instantiated, and if
                // the result is an external type, instantiate the parent then try the child.
            }
            return (generate ? UNDEFINED : null);
        }

        // if the reference has one or more indexes, and the definition is an
        // array definition, get the appropriate element in the array.
        if (indexes != null && indexes.size() > 0) {
            int numPushes = 0;
            try {
                while (def.isReference() && !(def instanceof CollectionDefinition)) {
                    params = def.getParamsForArgs(args, context);
                    context.push(def, params, args, false);
                    numPushes++;
                    Instantiation refInstance = (Instantiation) def.getContents();
                    Definition childDef = null;
                    
                    // this is to get indexed aliases to work right.  A bit hacky.
                    if (def instanceof ElementReference) {
                        Definition elementDef = ((ElementReference) def).getElementDefinition(context);
                        childDef = (elementDef != null ? elementDef : (Definition) refInstance.getDefinition(context));  // lookup(context, false);
                    } else {
                        childDef = (Definition) refInstance.getDefinition(context);  // lookup(context, false);
                    }
                    if (childDef != null) {
                        // if the ref is complex, track back through prefix supers and aliases
                        BentoNode ref = refInstance.getReference();
                        if (ref instanceof ComplexName) {
                            NameNode refName = (NameNode) ref;
                            ArgumentList refArgs = args;
                            n = ref.getNumChildren();
                            while (n > 1) {
                                NameNode prefix = (NameNode) refName.getChild(0);
                                refName = new ComplexName(refName, 1, n);
                                n--;
                                
                                ArgumentList prefixArgs = prefix.getArguments();
                                Instantiation prefixInstance = new Instantiation(prefix, def);
                                Definition prefixDef = (Definition) prefixInstance.getDefinition(context);  // lookup(context, false);
                                numPushes += context.pushSupersAndAliases(def, refArgs, prefixDef);
                                def = prefixDef;
                                refArgs = prefixArgs;
                            }
                            numPushes += context.pushSupersAndAliases(def, refArgs, childDef);
                            
                        }
                        def = childDef;
                    } else {
                        def = null;
                        break;
                    }
                    args = refInstance.getArguments();
                }
            
                CollectionDefinition collectionDef = null;
                if (def != null && def instanceof CollectionDefinition) {
                    collectionDef = (CollectionDefinition) def;
    
                } else {
                    while (numPushes-- > 0) {
                        context.pop();
                    }
    
                    // find array definition
                    for (NamedDefinition nd = getSuperDefinition(); nd != null; nd = nd.getSuperDefinition()) {
                        if (nd instanceof ComplexDefinition) {
                            def = ((ComplexDefinition) nd).getExplicitDefinition(node, args, context);
                            if (def != null && def instanceof CollectionDefinition) {
                                collectionDef = (CollectionDefinition) def;
                                break;
                            }
                        }
                    }
                    if (collectionDef == null) {
                        Definition owner = getOwner();
                        Iterator<Context.Entry> it = context.iterator();
                        while (it.hasNext()) {
                            Context.Entry entry = (Context.Entry) it.next();
                            Definition defcon = entry.def;
                            // avoid infinite recursion
                            if (!defcon.equals(this)) {
                                def = defcon.getChildDefinition(node, args, null, parentArgs, context);
                                if (def != null && def instanceof CollectionDefinition) {
                                    collectionDef = (CollectionDefinition) def;
                                    break;
                                }
                            }
                        }
                        if (collectionDef == null) {   // not in the class hierarchy, try the container hierarchy
                            it = context.iterator();
                            while (it.hasNext()) {
                                Context.Entry entry = (Context.Entry) it.next();
                                Definition defcon = entry.def;
                                // avoid infinite recursion
                                if (!defcon.equals(this)) {
                                    for (owner = defcon.getOwner(); owner != null; owner = owner.getOwner()) {
                                        def = owner.getChildDefinition(node, args, null, parentArgs, context);
                                        if (def != null && def instanceof CollectionDefinition) {
                                            collectionDef = (CollectionDefinition) def;
                                            break;
                                        }
                                    }
                                    if (collectionDef != null) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (collectionDef == null) {
                        log(node.getName() + " not found.");
                        return null;
                    }
                }
     
                //collectionDef = new SubcollectionDefinition(collectionDef);
    
                //List<Index> indexes = node.getIndexes();
                while (numPushes-- > 0) {
                    context.pop();
                }
                def = collectionDef.getElementReference(context, args, indexes);
                indexes = null;
                
            } finally {
                while (numPushes-- > 0) {
                    context.pop();
                }
            }
        }
        if (def == null && isAlias()) {
            return super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj);
 
        } else if (generate) {
            if (def == null) {
                return UNDEFINED;
            } else {
                return def.instantiate(args, indexes, context);
            }

        } else {
            return ((Definition) def).getDefInstance(args, indexes);
        }
    }

    protected Definition getExplicitDefinition(NameNode node, ArgumentList args, Context context) throws Redirection {
        Definition def = getExplicitChildDefinition(node);
        if (def != null) {
            if (def.isFormalParam()) {

                // we should allow DefParameters if this is being called in scope,
                // but I haven't figured the best way to do that
                def = null;
               
            } else if (def instanceof NamedDefinition && args != null) {
                NamedDefinition ndef = (NamedDefinition) def;
                int numUnpushes = 0;
                try {
                    NamedDefinition argsOwner = (NamedDefinition) args.getOwner();
                    if (argsOwner != null) {
                        int limit = context.size() - 1;
                        while (numUnpushes < limit) {
                            NamedDefinition nextdef = (NamedDefinition) context.peek().def;
                            if (argsOwner.equals(nextdef) || argsOwner.isSubDefinition(nextdef)) {
                                break;
                            }
                            numUnpushes++;
                            context.unpush();
                        }
                        // if we didn't find the owner in the stack, put it back the way it came
                        if (numUnpushes == limit) {
                            while (numUnpushes-- > 0) {
                                context.repush();
                            }
                        }
                    }
                    def = ndef.getDefinitionForArgs(args, context);

                } finally {
                    while (numUnpushes-- > 0) {
                        context.repush();
                    }
                }
            }
        }
        return def;
    }

//    protected Definition getExternalDefinition(NameNode node, Context context) {
//        return getSite().getExternalDefinition(this, node, DefaultType.TYPE, context);
//    }

    public Definition getExplicitChildDefinition(NameNode node) {
        if (definitions == null) {
            vlog(getFullName() + " has no definition table, cannot look up " + node.getName());
            return null;
        }
        return definitions.getDefinition(this, node);
    }
}

