/* Bento
 *
 * $Id: Validater.java,v 1.7 2014/11/01 19:49:43 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */


package bento.lang;

import bento.runtime.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Validater extends BentoVisitor {

    public final static int MAX_TYPE_LEVELS = 10;

    public static void vlog(String str) {
        SiteBuilder.vlog(str);
    }

    public Validater() {}
    
    private List<String> problems = new ArrayList<String>();

     public Object handleNode(BentoNode node, Object data) {
        if (node instanceof NamedDefinition) {
            NamedDefinition ndef = (NamedDefinition) node;

            // check and correct aliases.  This also checks for identities.
            try {
                ndef.checkAlias();
            } catch (Throwable t) {
                throw new ValidateException(t.toString());
            }

            // check for circular inheritance hierarchy
            Type supertype = ndef.getSuper();
            if (supertype != null) {
                vlog("Validating ancestors of " + ndef.getFullName());
                int n = checkSupers(ndef, supertype, 0);
                if (n == -1) {
                    String message = ndef.getFullName() + " is circularly defined.";
                    problems.add(message);

                } else if (n >= MAX_TYPE_LEVELS) {
                    String message = "Inheritance hierarchy for " + ndef.getFullName() + " has more than " + MAX_TYPE_LEVELS + " levels; probably circular.";
                    problems.add(message);
                    vlog(message);
                }
            }
        }
        return super.handleNode(node, data);
    }

    private int checkSupers(NamedDefinition ndef, Type st, int n) {
        while (n <= MAX_TYPE_LEVELS) {
            if (st == null) {
                break;
            } else if (st instanceof TypeList) {
                Iterator<Type> it = ((TypeList) st).iterator();
                int m = 0;
                while (it.hasNext()) {
                    int k = checkSupers(ndef, it.next(), n);
                    if (k == -1 || k >= MAX_TYPE_LEVELS) {
                        m = k;
                        break;
                    }
                    m = Math.max(m, k);
                }
                n = m;
                break;
            }
            Definition def = st.getDefinition();
            if (def == null) {
                break;
            } else if (def.equals(ndef)) {
                n = -1;
                break;
            }
            st = def.getSuper();
            n++;
        }
        return n;
    }


    public List<String> getProblems() {
        return problems;
    }
    
    public String spoolProblems() {
        StringBuffer sb = new StringBuffer();
        
        for (String problem : problems) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(problem);
        }
       
        return sb.toString(); 
    }
    
    public static class ValidateException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ValidateException(String message) {
            super(message);
        }
    }

    
}

