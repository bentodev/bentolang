/* Bento
*
* $Id: Table.java,v 1.11 2014/05/05 13:15:25 sthippo Exp $
*
* Copyright (c) 2007-2014 by bentodev.org
*
* Use of this code in source or compiled form is subject to the
* Bento Poetic License at http://www.bentodev.org/poetic-license.html
*/

package bento.runtime;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import bento.lang.BentoArray;
import bento.lang.Core;
import bento.lang.Definition;
import bento.lang.Initializer;
import bento.lang.Redirection;
import bento.lang.Resolver;
import bento.lang.Site;
import bento.lang.Validater;
import bento.parser.BentoParser;
import bento.parser.ParseException;
import bento.parser.ParsedCollectionDefinition;

/**
 *  An external runtime utility class for operations on Bento tables.
 * 
 *  @author mash
 *
 */

public class Table {
    
    public static Object get(Object tableObject, Object key) {
        if (tableObject instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) tableObject;
            return map.get(key.toString());
        } else {
            return null;
        }
    }

    public static void set(Object tableObject, Object key, Object element) {
        if (tableObject == null) {
             throw new NullPointerException("bento.runtime.Table.set called with null table");
        }
        if (key == null) {
            throw new NullPointerException("bento.runtime.Table.set called with null key");
        }
                
        if (tableObject instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) tableObject;
            map.put(key.toString(), element);
        }
    }

    public static int size(Object tableObject) {
        if (tableObject instanceof Map<?,?>) {
            Map<?,?> map = (Map<?,?>) tableObject;
            return map.size();
        } else {
            return 0;
        }
    }

    public static Object copy(Object tableObject) {
        if (tableObject instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new HashMap<String, Object>((Map<String, Object>) tableObject);
            return map;
        } else {
            return null;
        }
    }
    
    public static void clear(Object tableObject) {
        if (tableObject instanceof Map<?,?>) {
            Map<?,?> map = (Map<?,?>) tableObject;
            map.clear();
        }        
    }

    public static Object parse(Context context, String str) throws ParseException, Redirection {
        try {
            BentoParser parser = new BentoParser(new StringReader("parse{}=" + str));
            Definition owner = context.getDefiningDef();
            
            ParsedCollectionDefinition collectionDef = (ParsedCollectionDefinition) parser.CollectionDefinition();
            collectionDef.setOwner(owner);
            collectionDef.init();
            
            Site site = owner.getSite();
            Core core = site.getCore();
            collectionDef.jjtAccept(new Initializer(core, site, true), owner);
            collectionDef.jjtAccept(new Resolver(), null);
            Validater validater = new Validater();
            collectionDef.jjtAccept(validater, null);
            String problems = validater.spoolProblems();
            if (problems.length() > 0) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Problems parsing array: " + problems);
            }
            
            // should there be a Linker?

            return collectionDef.getTable(context, null, null);
            
        } catch (Redirection r) {
            System.out.println("Redirection parsing table: " + r);
            throw r;
        } catch (Exception e) {
            System.out.println("Exception parsing table: " + e);
            e.printStackTrace();
            throw new Redirection(Redirection.STANDARD_ERROR, "Exception parsing table: " + e);
        }
    }

}