/* Bento
 *
 * $Id: BentoStandaloneServer.java,v 1.4 2015/02/02 04:36:20 sthippo Exp $
 *
 * Copyright (c) 2010-2015 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

/**
 * Interface for standalone Bento-programmable HTTP server
 *
 * This allows BentoServer to avoid a dependency on a specifig implementation (e.g. Jetty)
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public interface BentoStandaloneServer {

	public void setServer(BentoServer bentoServer);

	public void startServer() throws Exception;

    public void stopServer() throws Exception;
    
    public boolean isRunning();

    /** Gets the setting of the files first option.  If true, the server looks for
     *  files before bento objects to satisfy a request.  If false, the server looks
     *  for bento objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public boolean getFilesFirst();

    /** Set the files first option.  If this flag is present, then the server looks for
     *  files before bento objects to satisfy a request.  If not present, the server looks
     *  for bento objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst);

    /** Gets the optional virtual host name.
     */
    public String getVirtualHost();

    /** Sets the optional virtual host name.
     */
    public void setVirtualHost(String virtualHost);


}
