/* Bento
 *
 * $Id: Redirection.java,v 1.13 2014/12/15 14:10:26 sthippo Exp $
 *
 * Copyright (c) 2002-2017 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import bento.runtime.Context;

/**
 *  A Redirection is thrown by a Bento <code>redirect</code> statement.  This
 *  interrupts the construction of the page and leads to a HTTP redirect.
 */
public class Redirection extends Throwable {
    
    private static final long serialVersionUID = 1L;

    public static final String STANDARD_ERROR = "error";
    public static final String STANDARD_ERROR_PAGE = "/error_page";
    public static final String STANDARD_ERROR_DIV = "/$error_div";
    
    public static final int SERVER_ERROR_STATUS = 500;

    private int status = 307;  // 307 == temporary redirect
    private String location;
    private String message;
    private ResolvedInstance instance;
    
    public Redirection(Instantiation instance, Context context) {
        super();
        this.instance = new ResolvedInstance(instance, context, false);
        location = null;
        message = null;
        bento.runtime.SiteBuilder.vlog("Creating redirection to instance: " + instance.getName());
    }
    
    public Redirection(String location) {
        super();
        this.location = location;
        message = null;
        instance = null;
        bento.runtime.SiteBuilder.vlog("Creating redirection to location: " + location);
    }

    public Redirection(String location, String message) {
        super(message);
        this.location = location;
        this.message = message;
        bento.runtime.SiteBuilder.vlog("Creating redirection to location: " + location + " with message: " + message);
    }

    public Redirection(int status, String location, String message) {
        super(message);
        this.status = status;
        this.location = location;
        this.message = message;
        bento.runtime.SiteBuilder.vlog("Creating redirection to location: " + location + " with message: " + message + " and status: " + status);
    }

    public int getStatus() {
        return status;
    }

    public String getLocation() {
        if (instance != null) {
            return instance.getString();
        } else {
            return location;
        }
    }

    public String getMessage() {
        return message;
    }

    public void setLocation(String location) {
    	this.location = location;
    }
}
