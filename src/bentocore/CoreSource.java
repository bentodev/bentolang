/* Bento
 *
 * $Id: CoreSource.java,v 1.6 2014/07/15 14:25:46 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bentocore;


/**
 * This is a static convenience class for autoloading core source.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public final class CoreSource {
    public static String[] corePaths = { "core.bento", "core_ui.bento", "core_js.bento", "core_platform_java.bento", "core_sandbox.bento" };

    public static String[] getCorePaths() {
        return corePaths;
    }

}
