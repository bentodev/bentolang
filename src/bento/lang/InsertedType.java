/* Bento
 *
 * $Id: InsertedType.java,v 1.5 2015/04/13 13:09:10 sthippo Exp $
 *
 * Copyright (c) 2006-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

import java.util.*;

/**
 * An InsertedType is a wrapper for a type that is injected into a class hierarchy.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class InsertedType extends NameNode implements Type {

    private Type insertedType;
    private Type supertype;
    private NamedDefinition insertedDefinition;
    
    public InsertedType(Type insertedType, Type supertype) {
        super(insertedType.getName());
        this.insertedType = insertedType;
        this.supertype = supertype;
        insertedDefinition = new InsertedDefinition(this, null);
    }
    
    Type getInsertedType() {
        return insertedType;
    }
    
    Type getSuperType() {
        return supertype;
    }

    /** Returns true if the passed value is an instance of the inserted type or the supertype.
     *  specified context.
     */
    public boolean isInstance(Object obj, Context context) {
        return (insertedType.isInstance(obj, context) || supertype.isInstance(obj, context));
    }

    /** Returns true if the inserted type is external. */
    public boolean isExternal() {
        return insertedType.isExternal();
    }

    /** Returns true if the inserted type is primitive. */
    public boolean isPrimitive() {
        return insertedType.isPrimitive();
    }

    /** Returns true if the passed type is the same as or is a supertype of the inserted
     *  type or the supertype.
     */
    public boolean isTypeOf(Type type, Context context) {
        return (insertedType.isTypeOf(type, context) || supertype.isTypeOf(type, context));
    }

    public boolean isTypeOf(String typeName) {
        return (insertedType.isTypeOf(typeName) || supertype.isTypeOf(typeName));
    }


    /** Returns true if the passed type is the same as or is a component of
     *  this type.
     */
    public boolean includes(Type type) {
        return (insertedType.includes(type) || supertype.includes(type));
    }

    /** Returns true if a type of the passed name is the same as or is a component of
     *  this type. 
     */
    public boolean includes(String typeName) {
        return (insertedType.includes(typeName) || supertype.includes(typeName));
    }

    public Type[] getChildTypes() {
        return insertedType.getChildTypes();
    }

    public Type[] getPersistableChildTypes() {
        return insertedType.getPersistableChildTypes();
    }

    /** Converts a value of an arbitrary type to a value of this type. */
    public Value convert(Value val) {
        return insertedType.convert(val);
    }

    /** Returns the dimensions associated with this type, or an empty list
     *  if this is not a collection type.
     */
    public List getDims() {
        return insertedType.getDims();
    }

    /** Returns true if the type represents a collection.
     */
    public boolean isCollection() {
        return insertedType.isCollection();
    }

    /** Returns the collection (array or table) type this type 
     * represents or is a subtype of, if any, else null.
     */
    public Type getCollectionType() {
        return insertedType.getCollectionType();
    }   
        
    /** Returns true if this type represents an array. */
    public boolean isArray() {
        return insertedType.isArray();
    }

    /** Returns the array type this type represents or is a subtype of, if any, else null. */
    public Type getArrayType() {
        return insertedType.getArrayType();
    }
    
    /** Returns true if this type represents a table. */
    public boolean isTable() {
        return insertedType.isTable();
    }

    /** Returns the table type this type represents or is a subtype of, if any, else null. */
    public Type getTableType() {
        return insertedType.getTableType();
    }

    
    /** Returns the base type, not including dimensions, represented by this type. */
    public Type getBaseType() {
        return insertedType.getBaseType();
    }
    
    /** Returns true if this type can be the supertype of a type with the specified parameters. **/
    public boolean canBeSuperForParams(ParameterList params, Context context) {
        return insertedType.canBeSuperForParams(params, context);
    }


    /** Returns the arguments associated with this type, or an empty list
     *  if this type has no associated arguments.
     */
    public ArgumentList getArguments(Context context) {
        return insertedType.getArguments(context);
    }

    public Definition getDefinition() {
        return insertedDefinition;
    }

    public Class getTypeClass(Context context) {
        return insertedType.getTypeClass(context);
    }

    public int levelsBelow(Type type, Context context) {
        return Math.min(insertedType.levelsBelow(type, context), supertype.levelsBelow(type, context) + 1);
    }

    public boolean inheritsCollection() {
        return insertedType.inheritsCollection();
    }

    public Type getSuper() {
        return insertedDefinition.getSuper();
    }
}

class InsertedDefinition extends NamedDefinition {
    private InsertedType type;

    public InsertedDefinition(InsertedType t, Context context) {
        super(t.getInsertedType().getDefinition(), context);
        this.type = t;
        setSuper(type.getSuperType());
    }


    /** Returns <code>true</code> unless one of the definitions in the list
     *  is abstract.
     */
    public boolean isAbstract(Context context) {
       return (isAbstract(context) || type.getSuperType().getDefinition().isAbstract(context));
    }


    /** Returns a TypeList created from the supertypes of all the definitions in the list
     *  that have non-null supertypes.
     */
    public Type getSuper() {
        return type.getSuperType();
    }


    /** Returns true if any of the definitions return true.
     */
    public boolean isSuper(Type t) {
        return (isSuper(t) || ((NamedDefinition) type.getSuperType().getDefinition()).isSuper(t));
    }

    /** Returns true if any of the definitions return true.
     */
    public boolean isSuperType(String name) {
        return (isSuperType(name) || type.getSuperType().getDefinition().isSuperType(name));
    }
}
