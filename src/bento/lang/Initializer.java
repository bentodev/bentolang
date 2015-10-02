/* Bento
 *
 * $Id: Initializer.java,v 1.35 2015/06/09 13:15:29 sthippo Exp $
 *
 * Copyright (c) 2002-2014 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */


package bento.lang;

import bento.runtime.*;

import java.util.Map;

public class Initializer extends BentoVisitor {

    private Core core;
    private Site site;
    private boolean allowOverwrite = false;

    public Initializer(Core core) {
        this.core = core;
        site = core;
    }

    public Initializer(Core core, Site site) {
        this.core = core;
        this.site = site;
    }

    public Initializer(Core core, Site site, boolean allowOverwrite) {
        this.core = core;
        this.site = site;
        this.allowOverwrite = allowOverwrite;
    }

    public Object handleNode(BentoNode node, Object data) {
        NamedDefinition def = (NamedDefinition) data;
        ((AbstractNode) node).setOwner(def);
        NamedDefinition subdef = def;

        try {

            if (node instanceof Core) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting core is not supported.");
                }
                subdef = core;
                if (node != core) {
                    ((Core) node).setDefinitionTable(core.getDefinitionTable());
                    core.addContents((Site) node);
                }

            } else if (def == core && node instanceof Site) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting site is not supported.");
                }
                site = (Site) node;
                String name = site.getName();
                Map<String, DefinitionTable> defTableTable = core.getDefTableTable();
                DefinitionTable defTable = (DefinitionTable) defTableTable.get(name);
                if (defTable != null) {
                	site.setDefinitionTable(defTable);
                } else {
                	defTable = site.setNewDefinitionTable();
                	defTableTable.put(name, defTable);
                }
                subdef = site;

            } else if (node instanceof ExternStatement) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting extern is not supported.");
                }
                site.addExtern((ExternStatement) node);

            } else if (node instanceof AdoptStatement) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting adopt is not supported.");
                }
                site.addAdopt((AdoptStatement) node);

            } else if (node instanceof KeepStatement) {
                def.addKeep((KeepStatement) node);

            } else if (node instanceof InsertStatement) {
                def.addInsert((InsertStatement) node);

            } else if (node instanceof AbstractConstruction) {
                if (((AbstractConstruction) node).hasSub()) {
                    def.setHasSub(true);
                }
                if (((AbstractConstruction) node).hasOver()) {
                    def.setHasOver(true);
                }
                if (((AbstractConstruction) node).hasNext()) {
                    def.setHasNext(true);
                }
                if (node instanceof Instantiation) {
                    Instantiation instance = (Instantiation) node;
                    if (instance.getParent() instanceof ConcurrentBentoBlock) {
                        instance.setConcurrent(true);
                    } else {
                        ArgumentList args = instance.getArguments();
                        if (args != null && args.isConcurrent()) {
                            instance.setConcurrent(true);
                        }
                    }
                }

            } else if (node instanceof Type) {
                if (Name.THIS.equals(node.getName())) {
                    BentoNode sibling = node.getNextSibling();
                    
                    ((NameNode) node).setName(sibling.getName());    
                }
            
            } else if (node instanceof NamedDefinition) {
                subdef = (NamedDefinition) node;
                subdef.setDefinitionTable(def.getDefinitionTable());
                //if (def.isGlobal() && subdef.getDurability() == Definition.IN_CONTEXT) {
                //    subdef.setDurability(Definition.GLOBAL);
                //}
                
                // anonymous collections nested in collections represent multidimensional
                // collections; as such, their supertype is their owner's supertype
                if (node instanceof CollectionDefinition && def instanceof CollectionDefinition && ((CollectionDefinition) node).getSuper() == null) {
                    CollectionDefinition anonCollection = (CollectionDefinition) node;
                    anonCollection.setSuper(def.getSuper());
                }
            }
            super.handleNode(node, subdef);

            if (def == core && node instanceof Site && !(node instanceof Core)) {
                // if a site by this name already exists, it's not overwritten; a new
                // multi-site object containing all the content from both sites is built 
                core.addSite((Site) node);
                subdef = core.getSite(node.getName());
            }

            if (subdef != def && subdef.getName().length() > 0) {
                // sites automatically overwrite because that's how multifile sites
                // get handled
                boolean overwrite = allowOverwrite || (subdef instanceof Site);
                def.addDefinition(subdef, overwrite);
            }
        
            

        } catch (Exception e) {
            SiteBuilder.log("Error handling node " + node.getName() + " owned by " + (def instanceof Core ? "core" : def.getFullName()) + ": " + e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }

        return data;
    }
}
