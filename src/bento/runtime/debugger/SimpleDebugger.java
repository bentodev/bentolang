/* Bento
 *
 * $Id: SimpleDebugger.java,v 1.2 2014/11/10 20:43:04 sthippo Exp $
 *
 * Copyright (c) 2010-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime.debugger;

import bento.lang.Chunk;
import bento.runtime.BentoDebugger;
import bento.runtime.Context;

public class SimpleDebugger implements BentoDebugger {
	
	private SimpleDebuggerFrame frame;
	
	public SimpleDebugger() {}

	public void constructed(Chunk chunk, Context context, Object data) {
	}

	public void getting(Chunk chunk, Context context) {
	}

	public void retrievedFromCache(String name, Context context, Object data) {
	}

	public void close() {
	}

	public void setFocus() {
	}

}
