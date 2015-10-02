/* Bento
 *
 * $Id: ComplexType.java,v 1.47 2015/05/31 17:11:44 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.*;

/**
* Base class for types.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.47 $
*/

public class ComplexType extends AbstractType {

   private List<Dim> dims = null;
   private ArgumentList args = null;


   public ComplexType() {
       super();
   }

   public ComplexType(Definition def, String typename) {
       super();
       setChild(0, new NameNode(typename));
       setName(typename);
       setDefinition(def);
       dims = new EmptyList<Dim>();
       args = new ArgumentList(new EmptyList<Construction>());
   }

   public ComplexType(Definition def, String typename, List<Dim> dims, ArgumentList args) {
       super();
       this.dims = (dims == null ? new EmptyList<Dim>() : dims);
       this.args = (args == null ? new ArgumentList(new EmptyList<Construction>()) : args);

       setChild(0, new NameNode(typename));
       setName(typename);
       setDefinition(def);
   }

   public ComplexType(Type baseType, List<Dim> additionalDims, ArgumentList args) {
       setName(baseType.getName());
       this.args = (args == null ? new ArgumentList(new EmptyList<Construction>()) : args);
       dims = baseType.getDims();
       if (dims == null) {
           dims = additionalDims;

       } else if (additionalDims != null) {
           dims = Context.newArrayList(dims);
           dims.addAll(additionalDims);
       }
   }


   /** Find the definition associated with this type.
    */
   public void resolve() {
       if (getDefinition() != null) {
           return;
       }
       BentoNode parent = getParent();
       boolean allowCircular = (parent instanceof Expression);

       Definition owner = getOwner();
       while (owner != null) {
           owner = owner.getOwner();
           if (owner instanceof ComplexDefinition) {
               break;
           }
       }
       if (owner == null) {
           throw new NullPointerException("ComplexType '" + getName() + "' has no owner, cannot resolve");
       }

       ComplexDefinition container = (ComplexDefinition) owner;
       owner = getOwner();

       String checkName = this.getName();
       vlog("Resolving type " + checkName + " in " + owner.getFullName() + "...");

       Definition def = null;
       while ((def == null || (def.equals(owner) && !allowCircular)) && container != null) {
           def = container.getExplicitChildDefinition(this);
           
           // if not found, check superdefinitions
           if (def == null || (def.equals(owner) && !allowCircular)) {
               NamedDefinition superdef = container.getSuperDefinition(null);
               while (superdef != null) {
                   def = superdef.getExplicitChildDefinition(this);
                   if (def != null && (!def.equals(owner) || allowCircular)) {
                       break;
                   }
                   superdef = superdef.getSuperDefinition(null);
               }
               
           }
           container = (ComplexDefinition) container.getOwner();
       }

       // avoid circular type definitions
       if (def != null && !allowCircular && owner.equals(def)) {
           def = null;
       }

       // if not found yet, look for explicit or external definition
       if (def == null) {
           DefinitionTable defTable = ((NamedDefinition) owner).getDefinitionTable();
           def = defTable.getDefinition((NamedDefinition) owner, this);
           // avoid circular type definitions
           if (def != null && !allowCircular && owner.equals(def)) {
               def = null;
           }
       }

       if (def != null) {
           setDefinition(def);

           if (def.getAccess() == Definition.LOCAL_ACCESS) {
               vlog("   ...type " + checkName + " refers to local definition " + def.getFullName());
           } else if (checkName.equals(def.getFullName())) {
               vlog("   ...type " + checkName + " is an explicit definition reference");
           } else {
               vlog("   ...type " + checkName + " refers to class definition " + def.getFullName());
           }

       } else {
           vlog("   ...type " + checkName + " could not be resolved");
       }

   }

   public Value convert(Value val) {
       // TO DO: convert by calling the definition
       // with the passed type as a single parameter

//       try {
           if (getNumChildren() == 1) {
               BentoNode node = getChild(0);
               if (node instanceof Type) {
                   Type type = (Type) node;
                   return type.convert(val);
               }
           }
           List<Construction> args = Context.newArrayList(1, Construction.class);
           args.add((Construction) val);
           Definition def = getDefinition();
           Instantiation instance = new Instantiation(def, new ArgumentList(args), null);
           instance.setOwner(getOwner());
           Value result = val;
           try {
               result = instance.getValue(new Context(def.getSite()));
           } catch (Redirection r) {
               vlog("Error converting value to " + getName() + ": " + r.getMessage());
           }
           return result;

//       } catch (ClassCastException cce) {
//           throw new Redirection(Redirection.STANDARD_ERROR, "don't know how to convert a " + val.getValueClass().getName() + " to a " + getName());
//       }
   }

   public String getName() {
       
       String name = null;
       BentoNode node = null;
       int numChildren = getNumChildren();
       if (numChildren > 0) {
           for (int i = 0; i < numChildren; i++) {
               node = getChild(i);
               // In a type name, Any (i.e. *) is an argument list stand in, not 
               // part of a compound name 
               if (node instanceof Any) {
                   break;
               } else if (node instanceof Name) {
                   String n = ((Name) node).getName();
                   if (name == null) {
                       name = n;
                   } else {
                       name = name + '.' + n;
                   }
               } else {
                   break;
               }
           }
       } else {
           name = super.getName();
       }
       return name;
   }

   public List<Dim> getDims() {
       if (dims == null) {
           int len = getNumChildren();
           int ix;
           BentoNode node = null;
           for (ix = 0; ix < len; ix++) {
               node = getChild(ix);
               if (node instanceof Dim) {
                   break;
               }
           }
           int num = len - ix;
           if (num == 0) {
               dims = new EmptyList<Dim>();
           } else if (num == 1) {
               dims = new SingleItemList<Dim>((Dim) node);
           } else {
               dims = Context.newArrayList(num, Dim.class);
               dims.add((Dim) node);
               for (++ix; ix < len; ix++) {
                   dims.add((Dim) getChild(ix));
               }
           }
       }
       return dims;
   }


   public ArgumentList getArguments(Context context) {
       if (args == null) {
           int len = getNumChildren();
           if (len > 0) {
               BentoNode node = getChild(len - 1);
               if (node instanceof ArgumentList) {
                   args = (ArgumentList) node;
               }
           }
           if (args == null) {
               args = new ArgumentList(new EmptyList<Construction>());
           }
       }
       if (args.size() >= 1 && args.get(0) instanceof Any) {
           //return context.peek().args;
           
           ParameterList params = context.peek().params;
           int len = (params == null ? 0 : params.size());
           ArgumentList argsFromParams = new ArgumentList(len);
           Definition owner = getOwner();
           for (DefParameter param: params) {
               Instantiation instance = new Instantiation(param.getNameNode(), owner);
               argsFromParams.add(instance);
           }
           return argsFromParams;
           
       }
       return args;
   }

   /** Returns the type, not including dimensions, represented by
    *  this complex type.
    */
   public Type getBaseType() {
       int len = getNumChildren();
       int n;
       for (n = 0; n < len; n++) {
           if (getChild(n) instanceof Dim) {
               break;
           }
       }
       if (n == 0) {
           return DefaultType.TYPE;
       } else if (n == 1) {
           BentoNode node = getChild(0);
           if (node instanceof Type) {
               return ((Type) node).getBaseType();
           } else if (len == 1) { // && (dims == null || dims.size() == 0)) {  // no dimensions
               Definition def = getDefinition();
               if (def != null) {
            	   if (def instanceof CollectionDefinition) {
                       return ((CollectionDefinition) def).getElementType();
                   } else {
            	       Type st = def.getSuper();
            	       if (st != null && st.isCollection()) {
            	            return st.getBaseType();
                       }
                   }
               }
               return this;

           } else {
               ComplexType baseType = new ComplexType();
               baseType.copyChildren(this, 0, 1);
               baseType.setOwner(getOwner());
               baseType.resolve();
               return baseType.getBaseType();
           }
       } else if (len == n) { // && (dims == null || dims.size() == 0)) {  // no dimensions
           Definition def = getDefinition();
           if (def != null && def.isCollection()) {
               return ((CollectionDefinition) def).getElementType();
           } else {
               return this;
           }
       } else {
           ComplexType baseType = new ComplexType();
           baseType.copyChildren(this, 0, n);
           baseType.setOwner(getOwner());
           baseType.resolve();
           return baseType.getBaseType();
       }
   }

   public Class<?> getTypeClass(Context context) {
       Class<?> typeClass = null;
       Type baseType = getBaseType();
       if (baseType instanceof PrimitiveType) {
           typeClass = baseType.getTypeClass(context);
       } else {
           Definition def = getDefinition();
           if (def == null) {
               typeClass = String.class;
           } else {
               // if the definition is an alias, look for an external definition at the
               // end of the alias chain, and use that class as the type class
               
               if (def.isAlias() && context != null) {
                   int numPushes = 0;
                   try {
                       Definition aliasDef = def;
                       ArgumentList aliasArgs = args;
                       ParameterList aliasParams = def.getParamsForArgs(args, context);
                       while (aliasDef.isAlias()) {
                           context.push(aliasDef, aliasParams, aliasArgs, false);
                           numPushes++;
                           Instantiation aliasInstance = aliasDef.getAliasInstance();
                           aliasDef = (Definition) aliasInstance.getDefinition(context);
                           if (aliasDef == null || aliasDef instanceof ExternalDefinition) {
                               break;
                           }
                           aliasArgs = aliasInstance.getArguments();   // aliasInstance.getUltimateInstance(context).getArguments();
                           aliasParams = aliasDef.getParamsForArgs(aliasArgs, context);
                       }
                       if (aliasDef != null && aliasDef instanceof ExternalDefinition) {
                           def = aliasDef;
                       }
                       
                   } catch (Throwable t) {
                       ;
                   } finally {
                       while (numPushes-- > 0) {
                           context.pop();
                       }
                   }
                   
               
               }    
               if (def instanceof ExternalDefinition) {
                   typeClass = ((ExternalDefinition) def).getExternalClass(context);
               } else {
                   Type superType = def.getSuper();
                   if (superType != null) {
                       return superType.getTypeClass(context);
                   } else {
                       typeClass = String.class;
                   }
               }
           }
       }
       List<Dim> dims = getDims();
       if (dims.size() > 0) {
           String className;
           if (typeClass.isPrimitive() || typeClass.equals(String.class)) {

               className = "Ljava.lang.Object;";

            /********************
               if (valueClass.equals(Boolean.TYPE)) {
                   className = "Z";
               } else if (valueClass.equals(Byte.TYPE)) {
                   className = "B";
               } else if (valueClass.equals(Character.TYPE)) {
                   className = "C";
               } else if (valueClass.equals(Integer.TYPE)) {
                   className = "I";
               } else if (valueClass.equals(Short.TYPE)) {
                   className = "S";
               } else if (valueClass.equals(Long.TYPE)) {
                   className = "J";
               } else if (valueClass.equals(Float.TYPE)) {
                   className = "F";
               } else if (valueClass.equals(Double.TYPE)) {
                   className = "D";
               } else {
                   className = "Ljava.lang.String";
               }
       ************************/
           } else {
                className = 'L' + typeClass.getName() + ';';
           }

           Iterator<Dim> it = dims.iterator();
           while (it.hasNext()) {
               Dim dim = it.next();
               if (dim.isTable()) {
                   if (it.hasNext()) {
                       className = "Ljava.util.Map;";
                   } else {
                       className = "java.util.Map";
                   }
               } else {
                   className = '[' + className;
               }
           }
           try {
               typeClass = Class.forName(className);
           } catch (ClassNotFoundException cnfe) {
               log("Unable to load class " + className);
               typeClass = (new Object[0]).getClass();
           }
       }
       return typeClass;
   }
}
