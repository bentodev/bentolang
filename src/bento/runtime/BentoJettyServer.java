/* Bento
 *
 * $Id: BentoJettyServer.java,v 1.9 2015/01/30 14:18:07 sthippo Exp $
 *
 * Copyright (c) 2010-2015 bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;
import bento.parser.Node;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Standalone Bento-programmable HTTP server
 *
 * This HTTP server is based on the Jetty HTTP server.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class BentoJettyServer extends Server implements BentoStandaloneServer {

    /* These are passed in to the constructor */
	private BentoServer bentoServer;

	/* These are optional and set via setter methods */
	private String virtualHost = null;
    private boolean filesFirst = false;
    private String address = null;
    
	private BentoHandler bentoHandler = null;
	
	/* if this is non-null, then something went wrong.  Responses are disabled. */
	private Exception exception = null;

	public BentoJettyServer() {}
	
	public void setServer(BentoServer bentoServer) {
        this.bentoServer = bentoServer;
    }

    public void startServer() throws Exception {
        try {
        	BentoSite mainSite = bentoServer.getMainSite();
        	
            // Get the addresses to listen to
            Connector[] connectors = null;
            Object serverAddr[] = null;

            String addr = bentoServer.getSpecifiedAddress();
            if (addr == null) {
                serverAddr = mainSite.getPropertyArray("listen_to");
            } else {
                serverAddr = new Object[1];
                serverAddr[0] = addr;
            }


            if (serverAddr == null || serverAddr.length == 0) {
                connectors = new Connector[1];
                HttpConfiguration httpConfiguration = new HttpConfiguration();
                ServerConnector connector = new ServerConnector(this, new HttpConnectionFactory(httpConfiguration));
                if (virtualHost != null) {
                    connector.setHost(virtualHost);
                }
                address = connector.getHost();
                connectors[0] = connector;
            } else {
            	address = null;
                int numConnectors = serverAddr.length;
                connectors = new Connector[numConnectors];
                for (int i = 0; i < serverAddr.length; i++) {
                    int port = 80;
                    String host = virtualHost;
                    addr = serverAddr[i].toString();
                    int ix = addr.indexOf(':');
                    if (ix >= 0) {
                        port = Integer.parseInt(addr.substring(ix + 1));
                        if (ix > 0) {
                            host = addr.substring(0, ix);
                        }
                    }
                    HttpConfiguration httpConfiguration = new HttpConfiguration();
                    ServerConnector connector = new ServerConnector(this, new HttpConnectionFactory(httpConfiguration));
                    connector.setPort(port);
                    if (host != null) {
                        connector.setHost(host);
                    }
                    connectors[i] = connector;
                    if (address == null) {
                    	address = addr;
                    } else {
                        address = address + ", " + addr;
                    }
                }
            }

            HandlerCollection handlers = new HandlerCollection();

            String fileBase = mainSite.getProperty("file_base", ".");
            if (filesFirst) {
                ResourceHandler fileHandler = new FileHandler();
                fileHandler.setResourceBase(fileBase);
                handlers.addHandler(fileHandler);
            }
            
            String contextPath = mainSite.getProperty("context_path", "/");
            bentoHandler = new BentoHandler(bentoServer, contextPath);
            handlers.addHandler(bentoHandler);

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase(fileBase);
            handlers.addHandler(resourceHandler);

            handlers.addHandler(new BentoDefaultHandler());

            setConnectors(connectors);
            setHandler(handlers);
            start();
            join();

        } catch (Exception e) {
            exception = e;
            throw e;
        }
    }

    public void stopServer() throws Exception {
        Connector[] connectors = getConnectors();
        for (Connector c: connectors) {
            if (c instanceof ServerConnector) {
                ((ServerConnector) c).close();
            }
        }
        stop();
    }
    
    static void link(Node[] parseResults) {
        for (int i = 0; i < parseResults.length; i++) {
            parseResults[i].jjtAccept(new SiteLoader.Linker(), null);
        }
    }

//    public void addSite(BentoSite site) {
//        if (bentoHandler != null) {
//            bentoHandler.addSite(site);
//        }
//    }
    


    /** Gets the setting of the files first option.  If true, the server looks for
     *  files before bento objects to satisfy a request.  If false, the server looks
     *  for bento objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public boolean getFilesFirst() {
        return filesFirst;
    }

    /** Set the files first option.  If this flag is present, then the server looks for
     *  files before bento objects to satisfy a request.  If not present, the server looks
     *  for bento objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst) {
        this.filesFirst = filesFirst;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    /** Sets the optional virtual host name.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public class BentoHandler extends ServletContextHandler {
 
        ServletHolder holder = null;

        public BentoHandler(BentoServer bentoServer, String contextPath) {
            super(SESSIONS);
            setContextPath(contextPath);
            if (bentoServer.getMainSite() != null && exception == null) {
                holder = new ServletHolder(bentoServer);
                addServlet(holder, "/*");
            }
        }

        // problem with jetty 6.1.3
        public String getContextPath() {
            String path = super.getContextPath();
            return (path == null ? "" : path);
        }
        
        public synchronized void doStart() throws Exception {
            if (holder != null) {
                super.doStart();
            } else {
                System.out.println("Can't start BentoHandler because the site was not initialized due to an error.");
            }
        }

        public void doHandle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

        	Map<String, BentoSite> sites = bentoServer.getSiteMap();
        	BentoSite mainSite = bentoServer.getMainSite();
        	
            if (sites != null) {
                String ru = pathInContext;
                int ix = ru.indexOf('/');
                while (ix == 0) {
                    ru = ru.substring(1);
                    ix = ru.indexOf('/');
                }
                if (ix < 0) {
                    if (sites.containsKey(ru)) {
                        mainSite = (BentoSite) sites.get(ru);
                    }
                } else if (ix > 0) {
                    String siteName = ru.substring(0, ix);
                    if (sites.containsKey(siteName)) {
                        mainSite = (BentoSite) sites.get(siteName);
                    }
                }
            }
            
            String pageName = mainSite.getPageName(pathInContext);
            if (mainSite.canRespond(pageName)) {
                try {
                    System.out.println("== BentoHandler: request path " + pathInContext);
                    super.doHandle(pathInContext, baseRequest, httpRequest, httpResponse);
                } catch (Exception e) {
                    System.out.println("== BentoHandler: exception handling request " + e.toString());
                    httpResponse.reset();
                    baseRequest.setHandled(false);
                }
            } else {
                System.out.println("== BentoHandler: cannot respond to " + pathInContext);
                httpResponse.reset();
                baseRequest.setHandled(false);
            }
            
        }
    }

    public class FileHandler extends ResourceHandler {

    	public void handle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int dispatch) throws IOException, ServletException {
    		// handle only files; pass directories through
            System.out.println("== FileHandler: request path " + pathInContext);
            if (pathInContext != null && pathInContext.length() > 0 && !pathInContext.endsWith("/")) {
            	super.handle(pathInContext, baseRequest, httpRequest, httpResponse);
            }
        }
    }

    public class BentoDefaultHandler extends DefaultHandler {

        public BentoDefaultHandler() {
            setServeIcon(false);
        }
        
        public void handle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int dispatch) throws IOException, ServletException {
            System.out.println("== DefaultHandler: request path " + pathInContext);
            super.handle(pathInContext, baseRequest, httpRequest, httpResponse);
        }
    }
    
    
 
}
