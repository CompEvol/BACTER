/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package bacter;

import bacter.util.parsers.ExtendedNewickBaseVisitor;
import bacter.util.parsers.ExtendedNewickLexer;
import bacter.util.parsers.ExtendedNewickParser;
import beast.core.*;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Conversion graph based around the clonal frame.")
@Citation("Tim Vaughan, Alexei Drummond and Nigel French, "
        + "'Phylogenetic inference\n for bacteria in BEAST 2'. "
        + "In preparation.")
public class ConversionGraph extends Tree {
    
    /**
     * Unlike Trees, Conversion graphs require an alignment (or at least
 the length of an alignment) to be specified so that the regions of
 the alignment affected by recombination events can be recorded.
     */
    public Input<List<Locus>> lociInput = new Input<>(
            "locus",
            "Locus associated with graph.",
            new ArrayList<>());

    public Input<String> fromStringInput = new Input<>(
            "fromString",
            "Initialise ARG from string representation.");

    /**
     * List of recombinations on graph.
     */
    protected Map<Locus, List<Conversion>> convs;
    protected Map<Locus, List<Conversion>> storedConvs;

    /**
     * Event and region lists.
     */
    protected Map<Locus, RegionList> regionLists;
    protected CFEventList cfEventList;
    protected ACGEventList acgEventList;

    protected List<Locus> loci;
    protected int totalSequenceLength;
    
    @Override
    public void initAndValidate() throws Exception {

        convs = new HashMap<>();
        storedConvs = new HashMap<>();

        if (lociInput.get().isEmpty())
                throw new RuntimeException("Must specify at least one locus " +
                        "as an input to ConversionGraph.");

        loci = lociInput.get();

        // Sort alignment list lexographically in order of BEASTObject IDs
        loci.sort((l1, l2) -> l1.getID().compareTo(l2.getID()));

        totalSequenceLength = 0;
        for (Locus locus : loci) {
            convs.put(locus, new ArrayList<>());
            storedConvs.put(locus, new ArrayList<>());
            totalSequenceLength += locus.getSiteCount();
        }
        
        if (fromStringInput.get() != null) {
            fromString(fromStringInput.get());
        }

        regionLists = new HashMap<>();
        for (Locus locus : loci)
            regionLists.put(locus, new RegionList(this, locus));

        cfEventList = new CFEventList(this);
        acgEventList = new ACGEventList(this);

        super.initAndValidate();
    }

    /**
     * Retrieve total length of all sequence alignments.
     *
     * @return total sequence length
     */
    public int getTotalSequenceLength() {
        return totalSequenceLength;
    }

    /**
     * Retrieve locus associated with this ACG having a given BEASTObject ID.
     *
     * @param id id of alignment to retrieve
     * @return locus, or null if no locus matches.
     */
    public Locus getLocusByID(String id) {
        for (Locus locus : loci)
            if (locus.getID().equals(id))
                return locus;

        return null;
    }

    /**
     * Retrieve alignments associated with this conversion graph.
     *
     * @return list of associated alignments
     */
    public List<Locus> getLoci() {
        return loci;
    }
    
    /**
     * Add conversion to graph, ensuring conversion list
     * remains sorted.
     *
     * @param conv conversion to add
     */
    public void addConversion(Conversion conv) {
        startEditing(null);
        
        conv.setConversionGraph(this);

        Locus locus = conv.getLocus();

        int i;
        for (i=0; i<convs.get(locus).size(); i++)
            if (convs.get(locus).get(i).startSite>conv.startSite)
                break;
        
        convs.get(locus).add(i, conv);
    }
    
    /**
     * Remove recombination from graph.
     *
     * @param conv conversion to remove.
     */
    public void deleteConversion(Conversion conv) {
        startEditing(null);
        
        convs.get(conv.getLocus()).remove(conv);
    }
    
    /**
     * Retrieve list of conversions associated with given locus.
     *
     * @param locus locus with which conversions are associated
     * @return List of conversions.
     */
    public List<Conversion> getConversions(Locus locus) {
        return convs.get(locus);
    }

    /**
     * Obtain number of conversion events associated with given locus.
     *
     * @param locus locus with which conversions are associated.
     * @return Number of conversions.
     */
    public int getConvCount(Locus locus) {
        return convs.get(locus).size();
    }

    /**
     * Obtain total number of conversion events.
     *
     * @return Number of conversions.
     */
    public int getTotalConvCount() {
        int convCount = 0;
        for (Locus locus : loci)
            convCount += convs.get(locus).size();

        return convCount;
    }

    /**
     * Obtain index of conversion when conversions are listed in order
     * of alignment and start site.
     *
     * @param conv conversion whose index is required
     * @return Conversion index
     */
    public int getConversionIndex(Conversion conv) {
        int index = 0;
        for (Locus locus : getLoci()) {
            if (locus == conv.getLocus()) {
                index += getConversions(locus).indexOf(conv);
                break;
            } else
                index += getConvCount(locus);
        }

        return index;
    }

    /**
     * Get list of contiguous regions having fixed marginal trees
     * associated with given locus.
     *
     * @param locus locus with which regions are associated
     * @return list of regions
     */
    public List<Region> getRegions(Locus locus) {
        return regionLists.get(locus).getRegions();
    }

    /**
     * Obtain number of contiguous single-tree regions associated with
     * given locus.
     *
     * @param locus locus with which regions are associated
     * @return Number of regions.
     */
    public int getRegionCount(Locus locus) {
        return regionLists.get(locus).getRegions().size();
    }

    /**
     * Obtain ordered list of events that make up the clonal frame.  Used
     * for ACG probability density calculations and for various state proposal
     * operators.
     * 
     * @return List of events.
     */
    public List<CFEventList.Event> getCFEvents() {
        return cfEventList.getCFEvents();
    }

    /**
     * Obtain ordered list of events that make up the ACG.  Used
     * for ACG probability density calculations and for various state proposal
     * operators.
     * 
     * @return List of events.
     */
    public List<ACGEventList.Event> getACGEvents() {
        return acgEventList.getACGEvents();
    }
    
    /**
     * @return Total length of all edges in clonal frame.
     */
    public double getClonalFrameLength() {
        double length = 0.0;
        for (Node node : m_nodes) {
            if (node.isRoot())
                continue;
            length += node.getLength();
        }
        
        return length;
    }

    /**
     * Check validity of conversions.  Useful for probability densities
     * over the ACG to decide whether to return 0 based on an unphysical
     * state.
     * 
     * @return true if all conversions are valid w.r.t. clonal frame.
     */
    public boolean isInvalid() {
        for (Locus locus : loci) {
            for (Conversion conv : convs.get(locus)) {
                if (!conv.isValid()) {
                    return true;
                }
                if (conv.getStartSite() < 0
                        || conv.getStartSite() >= locus.getSiteCount()
                        || conv.getEndSite() < 0
                        || conv.getEndSite() >= locus.getSiteCount()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Locus locus : loci) {
            for (Conversion conv : getConversions(locus)) {
                sb.append(String.format("[&%s,%d,%d,%s,%d,%d,%s] ",
                        locus.getID(),
                        conv.node1.getNr(),
                        conv.startSite,
                        String.valueOf(conv.height1),
                        conv.node2.getNr(),
                        conv.endSite,
                        String.valueOf(conv.height2)));
            }
        }
        sb.append(super.toString());

        // Unfortunately, we must behave differently if we're being
        // called by toXML().
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        if (ste[2].getMethodName().equals("toXML"))
            return sb.toString().replaceAll("&", "&amp;");
        else
            return sb.toString();
    }
    
    /**
     * Load ACG from string representation.  This is the same representation
     * used for XML state restoration.
     *
     * @param str string representation of ACG
     */
    public void fromString(String str) {
        
        // Extract clonal frame and recombination components of string
        Pattern cfPattern = Pattern.compile("^[^\\(]*(\\(.*)$");
        Matcher cfMatcher = cfPattern.matcher(str);
        
        if (!cfMatcher.find())
            throw new RuntimeException("Error parsing ACG state string.");
        
        // Process clonal frame
        String sNewick = cfMatcher.group(cfMatcher.groupCount());
        try {
            TreeParser parser = new TreeParser();
            parser.thresholdInput.setValue(1e-10, parser);
            parser.offsetInput.setValue(0, parser);
            setRoot(parser.parseNewick(sNewick));
        } catch (Exception ex) {
            Logger.getLogger(ConversionGraph.class.getName()).log(Level.SEVERE, null, ex);
        }

        initArrays();
        
        Pattern convPattern = Pattern.compile("\\[&([^]]*)]");
        Matcher convMatcher = convPattern.matcher(str);
        
        // Process recombinations
        for (Locus locus : getLoci())
            convs.get(locus).clear();

        while(convMatcher.find()) {
            String [] elements = convMatcher.group(1).split(",");

            Locus locus = getLocusByID(elements[0]);
            if (locus == null)
                throw new RuntimeException("Uknown locus id "
                        + elements[0] + ".  Aborting.");

            Node node1 = getNode(Integer.parseInt(elements[1]));
            int startLocus = Integer.parseInt(elements[2]);
            double height1 = Double.parseDouble(elements[3]);
            
            Node node2 = getNode(Integer.parseInt(elements[4]));
            int endLocus = Integer.parseInt(elements[5]);
            double height2 = Double.parseDouble(elements[6]);

            Conversion conv = new Conversion(
                    node1, height1,
                    node2, height2,
                    startLocus, endLocus, this, locus);
            
            addConversion(conv);
        }

        if (isInvalid()) {
            throw new IllegalArgumentException(
                    "Invalid ACG read from string. Aborting.");
        }
    }
    
    @Override
    public void fromXML(final org.w3c.dom.Node node) {
        fromString(node.getTextContent());
    }

    @Override
    public ConversionGraph copy() {
        ConversionGraph acg = new ConversionGraph();

        acg.setID(getID());

        acg.index = index;
        acg.root = root.copy();
        acg.nodeCount = nodeCount;
        acg.internalNodeCount = internalNodeCount;
        acg.leafNodeCount = leafNodeCount;

        acg.initArrays();

        acg.m_taxonset = m_taxonset;
        
        acg.convs = new HashMap<>();
        acg.storedConvs = new HashMap<>();

        acg.loci = loci;
        for (Locus locus : getLoci()) {
            acg.convs.put(locus, new ArrayList<>());
            for (Conversion conv : convs.get(locus)) {
                Conversion convCopy = conv.getCopy();
                convCopy.setConversionGraph(acg);
                convCopy.setNode1(acg.m_nodes[conv.getNode1().getNr()]);
                convCopy.setNode2(acg.m_nodes[conv.getNode2().getNr()]);
                acg.convs.get(locus).add(convCopy);
            }

            acg.storedConvs.put(locus, new ArrayList<>());
            for (Conversion conv : storedConvs.get(locus)) {
                Conversion convCopy = conv.getCopy();
                convCopy.setConversionGraph(acg);
                convCopy.setNode1(acg.m_nodes[conv.getNode1().getNr()]);
                convCopy.setNode2(acg.m_nodes[conv.getNode2().getNr()]);
                acg.storedConvs.get(locus).add(convCopy);
            }
        }

        return acg;
    }

    /**
     * Use another StateNode to configure this ACG.  If the other StateNode
     * is merely a tree, only the clonal frame is configured.
     * 
     * @param other StateNode used to configure ACG
     */
    @Override
    public void assignFrom(StateNode other) {
        super.assignFrom(other);
        
        if (other instanceof ConversionGraph) {
            ConversionGraph acg = (ConversionGraph)other;

            loci = acg.getLoci();
        
            convs.clear();
            storedConvs.clear();
            for (Locus locus : loci) {
                convs.put(locus, new ArrayList<>());
                storedConvs.put(locus, new ArrayList<>());
                for (Conversion conv : acg.getConversions(locus)) {
                    Conversion convCopy = conv.getCopy();
                    convCopy.setConversionGraph(this);
                    convCopy.setNode1(m_nodes[conv.getNode1().getNr()]);
                    convCopy.setNode2(m_nodes[conv.getNode2().getNr()]);
                    convs.get(locus).add(convCopy);
                }
            }

            if (cfEventList == null)
                cfEventList = new CFEventList(this);

            if (acgEventList == null)
                acgEventList = new ACGEventList(this);

            regionLists.clear();
            for (Locus locus : loci) {
                regionLists.put(locus, new RegionList(this, locus));
            }
        }
    }
    
    @Override
    public void assignFromFragile(StateNode other) {
        super.assignFromFragile(other);

        if (other instanceof  ConversionGraph) {
            ConversionGraph acg = (ConversionGraph) other;

            loci = acg.getLoci();

            convs.clear();
            storedConvs.clear();
            for (Locus locus : loci) {
                convs.put(locus, new ArrayList<>());
                storedConvs.put(locus, new ArrayList<>());
                for (Conversion conv : acg.getConversions(locus)) {
                    Conversion convCopy = conv.getCopy();
                    convCopy.setConversionGraph(this);
                    convCopy.setNode1(m_nodes[conv.getNode1().getNr()]);
                    convCopy.setNode2(m_nodes[conv.getNode2().getNr()]);
                    convs.get(locus).add(convCopy);
                }
            }

            if (cfEventList == null)
                cfEventList = new CFEventList(null);

            if (acgEventList == null)
                acgEventList = new ACGEventList(this);

            regionLists.clear();
            for (Locus locus : loci)
                regionLists.put(locus, new RegionList(this, locus));
        }
    }
    
    /**
     * Obtain extended Newick representation of ACG.  Includes Nexus metadata
     * on hybrid leaf nodes describing the alignment sites affected by the
     * conversion event.
     * 
     * @return Extended Newick string.
     */
    public String getExtendedNewick() {

        return extendedNewickTraverse(root, false) + ";";
    }

    /**
     * Obtain extended Newick representation of ACG, including only those
     * conversions which attach to CF edges above non-root nodes.
     * Includes Nexus metadata on hybrid leaf nodes describing the alignment
     * sites affected by the conversion event.
     *
     * @return Extended Newick string.
     */
    public String getTrimmedExtendedNewick() {

        return extendedNewickTraverse(root, true) + ";";
    }
    
    private String extendedNewickTraverse(Node node,
                                          boolean intraCFOnly) {
        StringBuilder sb = new StringBuilder();
        
        // Determine sequence of events along this node.
        class Event {
            boolean isArrival;
            double time;
            Conversion conv;
            
            public Event(boolean isArrival, double time, Conversion conv) {
                this.isArrival = isArrival;
                this.time = time;
                this.conv = conv;
            }
        }
        List<Event> events = new ArrayList<>();
        for (Locus locus : getLoci()) {
            for (Conversion conv : getConversions(locus)) {
                if (intraCFOnly && conv.node2.isRoot())
                    continue;

                if (conv.node1 == node)
                    events.add(new Event(false, conv.getHeight1(), conv));
                if (conv.node2 == node)
                    events.add(new Event(true, conv.getHeight2(), conv));
            }
        }
        
        // Sort events from oldest to youngest.
        Collections.sort(events, (Event e1, Event e2) -> {
            if (e1.time>e2.time)
                return -1;
            else
                return 1;
        });

        // Process events.
        
        int cursor = 0;
        
        double lastTime;
        if (node.isRoot())
            lastTime = Double.POSITIVE_INFINITY;
        else
            lastTime = node.getParent().getHeight();

        for (Event event : events) {

            double thisLength;
            if (Double.isInfinite(lastTime))
                thisLength = 0.0;
            else
                thisLength = lastTime - event.time;
            
            if (event.isArrival) {
                String meta =  String.format("[&conv=%d, region={%d,%d}, locus=\"%s\"]",
                                convs.get(event.conv.getLocus()).indexOf(event.conv),
                                event.conv.getStartSite(),
                                event.conv.getEndSite(),
                                event.conv.getLocus().getID());

                sb.insert(cursor, "(,#" + getConversionIndex(event.conv)
                        + meta
                        + ":" + (event.conv.height2-event.conv.height1)
                        + "):" + thisLength);
                cursor += 1;
            } else {
                sb.insert(cursor, "()#" + getConversionIndex(event.conv)
                        + ":" + thisLength);
                cursor += 1;
            }
            
            lastTime = event.time;
        }
        
        // Process this node and its children.

        if (!node.isLeaf()) {
            String subtree1 = extendedNewickTraverse(node.getChild(0), intraCFOnly);
            String subtree2 = extendedNewickTraverse(node.getChild(1), intraCFOnly);
            sb.insert(cursor, "(" + subtree1 + "," + subtree2 + ")");
            cursor += subtree1.length() + subtree2.length() + 3;
        }
        
        double thisLength;
        if (Double.isInfinite(lastTime))
            thisLength = 0.0;
        else
            thisLength = lastTime - node.getHeight();
        sb.insert(cursor, (node.getNr() + taxaTranslationOffset)
                + ":" + thisLength);
        
        return sb.toString();
    }

    /**
     * Read in an ACG from a string in extended newick format.  Assumes
     * that the network is stored with exactly the same metadata as written
     * by the getExtendedNewick() method.
     *
     * @param string extended newick representation of ACG
     */
    public void fromExtendedNewick(String string) {

        // Spin up ANTLR
        ANTLRInputStream input = new ANTLRInputStream(string);
        ExtendedNewickLexer lexer = new ExtendedNewickLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExtendedNewickParser parser = new ExtendedNewickParser(tokens);
        ParseTree parseTree = parser.tree();

        Map<String, Conversion> convIDMap = new HashMap<>();
        Node root = new ExtendedNewickBaseVisitor<Node>() {

            @Override
            public Node visitNode(@NotNull ExtendedNewickParser.NodeContext ctx) {

                double branchLength = Double.parseDouble(ctx.post().length.getText());

                if (!ctx.node().isEmpty() && ctx.post().hybrid() != null) {
                    Node node =  visitNode(ctx.node(0));
                    node.setHeight(node.getHeight() + branchLength);
                    return node;
                }

                if (ctx.node().isEmpty() && ctx.post().hybrid() != null) {
                    Conversion conv = new Conversion();
                    conv.setStartSite(Integer.parseInt(ctx.post().meta()
                            .attrib(1).attribValue().vector().attribValue(0).getText()));
                    conv.setEndSite(Integer.parseInt(ctx.post().meta()
                            .attrib(1).attribValue().vector().attribValue(1).getText()));
                    conv.setLocus(new Locus(ctx.post().meta().attrib(2).attribValue().getText(),
                            conv.getSiteCount()));

                    convIDMap.put(ctx.post().hybrid().INT().getText(), conv);
                    return null;
                }

                for (ExtendedNewickParser.NodeContext nodeCtx : ctx.node()) {

                }
            }
        }.visit(parseTree);

    }

    /*
    * StateNode implementation
    */
    
    @Override
    protected void store () {
        super.store();
        
        for (Locus locus : getLoci()) {
            storedConvs.get(locus).clear();

            for (Conversion conv : convs.get(locus)) {
                Conversion convCopy = new Conversion();

                convCopy.setLocus(conv.getLocus());
                convCopy.setStartSite(conv.getStartSite());
                convCopy.setEndSite(conv.getEndSite());
                convCopy.setHeight1(conv.getHeight1());
                convCopy.setHeight2(conv.getHeight2());

                convCopy.setNode1(m_storedNodes[conv.getNode1().getNr()]);
                convCopy.setNode2(m_storedNodes[conv.getNode2().getNr()]);

                convCopy.setConversionGraph(this);

                storedConvs.get(locus).add(convCopy);
            }
        }
    }
    
    @Override
    public void restore() {
        super.restore();
        
        Map<Locus, List<Conversion>> tmp = storedConvs;
        storedConvs = convs;
        convs = tmp;

        cfEventList.makeDirty();
        acgEventList.makeDirty();
        for (Locus locus : loci)
            regionLists.get(locus).makeDirty();
    }

    @Override
    public void startEditing(Operator operator) {
        if (state != null)
            super.startEditing(operator);

        if (cfEventList != null)
            cfEventList.makeDirty();

        if (acgEventList != null)
            acgEventList.makeDirty();

        if (regionLists != null)
            for (RegionList regionList : regionLists.values())
                regionList.makeDirty();
    }
    
    /*
     * Loggable implementation.
     */

    @Override
    public void log(int nSample, PrintStream out) {
        ConversionGraph arg = (ConversionGraph) getCurrent();
        
        out.print(String.format("tree STATE_%d = [&R] %s",
                nSample, arg.getExtendedNewick()));
    }
}
