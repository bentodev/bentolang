/* Bento
 *
 * $Id: Resolver.java,v 1.3 2015/07/04 17:41:13 sthippo Exp $
 *
 * Copyright (c) 2002-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */


package bento.lang;

import bento.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class Resolver extends BentoVisitor {

    public Resolver() {}
    
    @SuppressWarnings("unchecked")
	public Object handleNode(BentoNode node, Object data) {
        if (node instanceof ForStatement) {
            ParameterList params = ((ForStatement) node).getParameters();
            if (data == null) {
                super.handleNode(node, params);
            } else {
                List<DefParameter> oldList = (List<DefParameter>) data;
                ArrayList<DefParameter> newList = new ArrayList<DefParameter>(oldList.size() + params.size());
                newList.addAll(oldList);
                newList.addAll(params);
                super.handleNode(node, new ParameterList(newList));
            }
            return data;
        } else if (node instanceof Instantiation) {
            ((Instantiation) node).resolve(data);
        } else if (node instanceof Type) {
            ((Type) node).resolve();
        } else if (node instanceof NamedDefinition) {
            // resolve children first
            super.handleNode(node, data);
            ((NamedDefinition) node).resolveKeeps();
            ((NamedDefinition) node).resolveInserts();
            return data;
        }
        return super.handleNode(node, data);
    }
}
