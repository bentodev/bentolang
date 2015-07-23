/* Bento
 *
 * $Id: bento_processor.java,v 1.5 2014/11/01 19:49:43 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.Map;

/**
 * This interface corresponds to the Bento bento_processor object, called by Bento code
 * to obtain environment information and compile Bento code at runtime.
 */
public interface bento_processor {
    /** Returns the name of this processor (generally the name of a class or
     *  interface).
     **/
    public String name();

    /** The highest Bento version number supported by this processor. **/
    public String version();

    /** Properties associated with this processor. **/
    public Map<String, Object> props();

    /** Compile the Bento source files found at the locations specified in <code>bentopath</code>
     *  and return a bento_domain object.  If a location is a directory and <code>recursive</code>
     *  is true, scan subdirectories recursively for Bento source files.  If <code>autoloadCore</code>
     *  is true, and the core definitions required by the system cannot be found in the files
     *  specified in <code>bentopath</code>, the processor will attempt to load the core
     *  definitions automatically from a known source (e.g. from the same jar file that the
     *  processor was loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public bento_domain compile(String siteName, String bentopath, boolean recursive, boolean autoloadCore);

    /** Compile Bento source code passed in as a string and return a bento_domain object.  If
     *  <code>autoloadCore</code> is true, and the core definitions required by the system cannot
     *  be found in the passed text, the processor will attempt to load the core definitions
     *  automatically from a known source (e.g. from the same jar file that the processor was
     *  loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public bento_domain compile(String siteName, String bentotext, boolean autoloadCore);

    /** Compile Bento source code passed in as a string and merge the result into the specified
     *  bento_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(bento_domain domain, String bentotext) throws Redirection;
    
    /** Returns the domain type.  The default for the primary domain is "site".
     */
    public String domain_type();
}


