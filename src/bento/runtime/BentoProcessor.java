/* Bento
 *
 * $Id: BentoProcessor.java,v 1.5 2005/06/30 04:20:56 sthippo Exp $
 *
 * Copyright (c) 2003-2005 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;


/**
 * This interface extends bento_server (and therefore bento_processor) to include methods
 * to adjust the behavior of a Bento processor.
 */
public interface BentoProcessor extends bento_server {

    /** Sets the files first option.  If this flag is present, then the server looks for
     *  files before bento objects to satisfy a request.  If not present, the server looks
     *  for bento objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst);

    /** Sets the base directory where the server should read and write files. **/
    public void setFileBase(String fileBase);
}


