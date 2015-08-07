/* Bento
 *
 * $Id: Core.java,v 1.15 2011/09/16 21:56:03 sthippo Exp $
 *
 * Copyright (c) 2002-2011 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.lang;

import java.util.*;

/**
 * The Core is the owner of all sites.  It establishes a global namespace.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.15 $
 */
public class Core extends Site {

    private Map<String, Site> siteTable = new HashMap<String, Site>();
    private Map<String, DefinitionTable> defTableTable = new HashMap<String, DefinitionTable>();

    public Core() {
        super("core");
        setNewDefinitionTable();
        siteTable.put("core", this);
    }

    /** Returns an empty string.  */
    public String getFullName() {
        return "";
    }

    public void addSite(Site site) {
        site.setOwner(this);

        String name = site.getName();
        Site oldSite = getSite(name);
        if (oldSite == null) {
            log("Adding site " + name + " to core");
            siteTable.put(name, site);
            
        } else if (oldSite instanceof MultiSite) {
            log("Adding site " + name + " to multisite");
            ((MultiSite) oldSite).addSite(site);
            
        } else {
            log("Site " + name + " exists; creating new multisite");
            MultiSite newSite = new MultiSite(oldSite);
            newSite.addSite(site);
            newSite.setOwner(this);
            siteTable.put(name, newSite);
        }
    }

    public Site getSite(String name) {
        return (Site) siteTable.get(name);
    }

    public Iterator<Site> getSites() {
        return siteTable.values().iterator();
    }

    public Map<String, Site> sites() {
        return siteTable;
    }

    static class MultiSite extends Site {
        
        private List<Site> siteList = new ArrayList<Site>(5); 
        
        public MultiSite(Site site) {
            super(site.getName());
            setDefinitionTable(site.getDefinitionTable());
            siteList.add(site);
        }
        
        
        public void addSite(Site site) {
            siteList.add(site);
        }
        
        public AbstractNode getContents() {
            List<Definition> list = new ArrayList<Definition>();
            Iterator<Site> it = siteList.iterator();
            while (it.hasNext()) {
                Site site = it.next();
                Definition[] defs = site.getDefinitions();
                for (int i = 0; i < defs.length; i++) {
                    if (isOwnerOf(defs[i])) {
                        list.add(defs[i]);
                    }
                }
            }
            int n = list.size();
            AbstractNode[] nodes = new AbstractNode[n];
            nodes = (AbstractNode[]) list.toArray(nodes);
            return new BentoBlock(nodes);
        }
        
        public List<Name> getAdoptedSiteList() {
            List<Name> adopts = new ArrayList<Name>();
            Iterator<Site> it = siteList.iterator();
            while (it.hasNext()) {
                Site site = it.next();
                List<Name> adoptedSiteList = site.getAdoptedSiteList();
                if (adoptedSiteList != null) {
                    adopts.addAll(adoptedSiteList);
                }
            }
            return (adopts.size() > 0 ? adopts : null);
        }
        
        public List<ExternStatement> getExternList() {
            List<ExternStatement> externs = new ArrayList<ExternStatement>();
            Iterator<Site> it = siteList.iterator();
            while (it.hasNext()) {
                Site site = it.next();
                List<ExternStatement> externList = site.getExternList();
                if (externList != null) {
                    externs.addAll(externList);
                }
            }
            return (externs.size() > 0 ? externs : null);
        }
        
        public List<KeepStatement> getKeeps() {
            List<KeepStatement> keeps = new ArrayList<KeepStatement>();
            Iterator<Site> it = siteList.iterator();
            while (it.hasNext()) {
                Site site = it.next();
                List<KeepStatement> siteKeeps = site.getKeeps();
                if (siteKeeps != null) {
                    keeps.addAll(siteKeeps);
                }
            }
            return (keeps.size() > 0 ? keeps : null);
        }

        
        protected boolean isOwnerOf(Definition def) {
            Definition owner = def.getOwner();
            Iterator<Site> it = siteList.iterator();
            while (it.hasNext()) {
                Site site = it.next();
                if (owner.equals(site)) {
                    return true;
                }
            }
            return owner.equals(this);
        }        
    }
    
    public Core getCore() {
    	return this;
    }
    	
    public boolean equals(Object obj) {
        if (obj instanceof Core) {
            return equals((Definition) obj, null);
        } else {
            return false;
        }
    }

    public String toString(String prefix) {
        String str = '\n' + prefix + "core " + getContents().toString(prefix) + '\n';
        return str;
    }

	public Map<String, DefinitionTable> getDefTableTable() {
		return defTableTable;
	}
}