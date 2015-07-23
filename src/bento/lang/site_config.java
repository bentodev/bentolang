/* Bento
 *
 * $Id: site_config.java,v 1.4 2007/11/12 14:28:08 sthippo Exp $
 *
 * Copyright (c) 2005-2007 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

/**
 * This interface corresponds to the site_config object, defined in the
 * default site_config.bento file.  It represents a website configuration --
 * name, bentopath, base directory for file-based resources, and
 * network address.
 */
public interface site_config {

    /** Returns the name of the site. **/
    public String name();
    
    /** The directories and/or files containing the Bento source
     *  code for this site.
     **/
    public String bentopath();
    
    /** The directories and/or files containing the Bento source
     *  code for core.
     **/
    public String corepath();
    
    /** The directories and/or files containing the Bento source
     *  code specific to this site (not including core).
     **/
    public String sitepath();

        /** If true, directories found in bentopath are searched recursively
     *  for Bento source files.
     **/
    public boolean recursive();
    
    /** The base directory for file-based resources. **/
    public String filepath();

    /** The files first setting.  If true, the server should look for files 
     *  before Bento objects to satisfy a request.  If false, the server 
     *  should look for Bento objects first, and look for files only when no 
     *  suitable Bento object by the requested name exists.
     */
    public boolean files_first();
}


