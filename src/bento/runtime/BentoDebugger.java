/* Bento
 *
 * $Id: BentoDebugger.java,v 1.2 2010/11/29 14:20:05 sthippo Exp $
 *
 * Copyright (c) 2010 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;

/**
 * This interface defines a debugging api for Bento.  All of the methods in this
 * interface are callback methods, called by the Bento server before and after 
 * various steps in its operation.  
 */
public interface BentoDebugger {
      public void getting(Chunk chunk, Context context);
      public void constructed(Chunk chunk, Context context, Object data);
      public void retrievedFromCache(String name, Context context, Object data);

      public void setFocus();
      public void close();
}


