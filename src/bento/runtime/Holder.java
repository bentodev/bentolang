/* Bento
*
* $Id: Holder.java,v 1.14 2014/01/14 14:42:45 sthippo Exp $
*
* Copyright (c) 2002-2014 by bentodev.org
*
* Use of this code in source or compiled form is subject to the
* Bento Poetic License at http://www.bentodev.org/poetic-license.html
*/

package bento.runtime;

import bento.lang.Definition;
import bento.lang.ArgumentList;
import bento.lang.ResolvedInstance;

/**
 *  A simple container for associating a definition with an instantiated
 *  object.  A Holder is useful for lazy instantiation (avoiding instantiation
 *  till the last possible moment) and for providing name, type, source code
 *  and other metadata about the object that can be obtained from the 
 *  definition   
 *
 * 
 *  @author mash
 *
 */

public class Holder {
    public Definition nominalDef;
    public ArgumentList nominalArgs;
    public Definition def;
    public ArgumentList args;
    public Object data;
    public ResolvedInstance resolvedInstance;
    
    public Holder() {
        this(null, null, null, null, null, null, null);
    }
    
    public Holder(Definition nominalDef, ArgumentList nominalArgs, Definition def, ArgumentList args, Context context, Object data, ResolvedInstance resolvedInstance) {
        this.nominalDef = nominalDef;
        this.nominalArgs = nominalArgs;
        this.def = def;
        this.args = args;
        this.data = data;
if (data instanceof BentoObjectWrapper) {
 System.out.println("Holder 48");    
}
        this.resolvedInstance = resolvedInstance;
    }

    public String toString() {
        return "{ nominalDef: "
             + (nominalDef == null ? "(null)" : nominalDef.getName())
             + "\n  nominalArgs: "
             + (nominalArgs == null ? "(null)" : nominalArgs.toString())
             + "\n  def: "
             + (def == null ? "(null)" : def.getName())
             + "\n  args: "
             + (args == null ? "(null)" : args.toString())
             + "\n  data: "
             + (data == null ? "(null)" : data.toString())
             + "\n  resolvedInstance: "
             + (resolvedInstance == null ? "(null)" : resolvedInstance.getName())
             + "\n}";
    }

}