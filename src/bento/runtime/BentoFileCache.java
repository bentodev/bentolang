/* Bento
 *
 * $Id: BentoFileCache.java,v 1.3 2015/05/14 12:35:17 sthippo Exp $
 *
 * Copyright (c) 2006-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;

import java.io.*;
import java.util.*;

/**
 * A BentoFileCache stores values in the form of Bento source code.
 * 
 * 
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

abstract public class BentoFileCache extends AbstractMap {

    File file;
    String siteName;
 
    /** Constructs a file-based persistent cache.
     */
    public BentoFileCache(File file, String siteName) {
    }

}

