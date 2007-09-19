package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ParameterConverter;
import org.esa.beam.framework.gpf.internal.DefaultParameterConverter;
import org.esa.beam.framework.gpf.internal.OperatorContextInitializer;
import org.esa.beam.framework.gpf.internal.ParameterInjector;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;

/**
 * The <code>GraphProcessor</code> is responsible for executing processing
 * graphs.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 4.1
 */
public class GraphProcessor {

    private List<GraphProcessingObserver> observerList;
    private Logger logger;


    /**
     * Creates a new instance og {@code GraphProcessor}.
     */
    public GraphProcessor() {
        observerList = new ArrayList<GraphProcessingObserver>();
        logger = Logger.getAnonymousLogger();
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets a logger.
     *
     * @param logger a logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds an observer to this graph propcessor. {@link GraphProcessingObserver}s are informed about
     * processing steps of the currently running processing graph.
     *
     * @param processingObserver the observer
     * @see GraphProcessingObserver
     */
    public void addObserver(GraphProcessingObserver processingObserver) {
        observerList.add(processingObserver);
    }

    /**
     * Gets all observers currentyl attached to this {@code GraphProcessor}.
     *
     * @return the observers
     */
    public GraphProcessingObserver[] getObservers() {
        return observerList.toArray(new GraphProcessingObserver[observerList.size()]);
    }

    /**
     * Executes the given {@link Graph}.
     *
     * @param graph the {@link Graph}
     * @param pm    a progress monitor. Can be used to signal progress.
     * @throws GraphException if any error occrues during execution
     */
    public void executeGraph(Graph graph, ProgressMonitor pm) throws GraphException {
        GraphContext graphContext;
        try {
            pm.beginTask("Executing processing graph", 100);
            graphContext = createGraphContext(graph, SubProgressMonitor.create(pm, 10));
            executeGraphContext(graphContext, SubProgressMonitor.create(pm, 90));
            disposeGraphContext(graphContext);
        } finally {
            pm.done();
        }
    }

    /**
     * Creates an {@link GraphContext} for the given {@link Graph}.
     *
     * @param graph the {@link Graph} to create the {@link GraphContext} for
     * @param pm    a progress monitor. Can be used to signal progress.
     * @return the created {@link GraphContext}
     * @throws GraphException if any error occrues during creation of the context, e.g. the graph is empty
     */
    public GraphContext createGraphContext(Graph graph, ProgressMonitor pm) throws GraphException {

        if (graph.getNodeCount() == 0) {
            throw new GraphException("Empty graph.");
        }

        try {
            pm.beginTask("Creating processing graph context", 100);
            GraphContext graphContext = new GraphContext(graph, logger);
            initNodeDependencies(graphContext);
            pm.worked(10);
            initOutput(graphContext, SubProgressMonitor.create(pm, 90));
            return graphContext;
        } finally {
            pm.done();
        }
    }

    /**
     * Disposes the given {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to dispose
     */
    public void disposeGraphContext(GraphContext graphContext) {
        Deque<NodeContext> initNodeContextDeque = graphContext.getInitNodeContextDeque();
        while (!initNodeContextDeque.isEmpty()) {
            NodeContext nodeContext = initNodeContextDeque.pop();
            nodeContext.getOperator().dispose();
            nodeContext.getTargetProduct().dispose();
        }
    }

    /**
     * Executes the given {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to execute
     * @param pm           a progress monitor. Can be used to signal progress.
     * @throws GraphException if any error occrues during execution
     */
    public void executeGraphContext(GraphContext graphContext, ProgressMonitor pm) throws GraphException {
        fireProcessingStarted(graphContext);

        Rectangle rectangleUnion = new Rectangle();
        NodeContext[] outputNodeContexts = graphContext.getOutputNodeContexts();
        for (int i = 0; i < outputNodeContexts.length; i++) {
            NodeContext outputNodeContext = outputNodeContexts[i];
            Product targetProduct = outputNodeContext.getTargetProduct();
            rectangleUnion.add(getProductBounds(targetProduct));
        }
        Dimension defaultTileSize = JAI.getDefaultTileSize();
        int numXTiles = MathUtils.ceilInt(rectangleUnion.width / (double) defaultTileSize.width);
        int numYTiles = MathUtils.ceilInt(rectangleUnion.height / (double) defaultTileSize.height);
        // todo - FIXMEEEE!
        // use per-image tile def in order to iter correctly over all target products
        pm.beginTask("Computing raster data...", numXTiles * numYTiles);
        for (int tileY = 0; tileY < numYTiles; tileY++) {
            for (int tileX = 0; tileX < numXTiles; tileX++) {
                if (pm.isCanceled()) {
                    break;
                }
                Rectangle tileRectangle = new Rectangle(tileX * defaultTileSize.width,
                                                        tileY * defaultTileSize.height,
                                                        defaultTileSize.width,
                                                        defaultTileSize.height);
                fireTileStarted(graphContext, tileRectangle);
                for (NodeContext nodeContext : outputNodeContexts) {
                    Product targetProduct = nodeContext.getTargetProduct();
                    if (getProductBounds(targetProduct).intersects(tileRectangle)) {
                        if (nodeContext.getOperatorImplementationInfo().isComputeAllBandsMethodImplemented()) {
                            Band band = targetProduct.getBandAt(0);
                            forceTileComputation(band, tileX, tileY);
                        } else {
                            for (Band band : targetProduct.getBands()) {
                                forceTileComputation(band, tileX, tileY);
                            }
                        }
                    }
                }
                fireTileStopped(graphContext, tileRectangle);
                pm.worked(1);
            }
        }
        pm.done();
        fireProcessingStopped(graphContext);
    }

    private Rectangle getProductBounds(Product targetProduct) {
        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        Rectangle rectangle = new Rectangle(rasterWidth, rasterHeight);
        return rectangle;
    }

    private void forceTileComputation(Band band, int tileX, int tileY) {
        final RenderedImage image = band.getImage();
        /////////////////////////////////////////////////////////////////////
        //
        // GPF pull-processing is triggered here!!!
        //
        image.getTile(tileX, tileY);
        //
        /////////////////////////////////////////////////////////////////////
    }

    private void initNodeDependencies(GraphContext graphContext) throws GraphException {
        Graph graph = graphContext.getGraph();
        for (Node node : graph.getNodes()) {
            for (NodeSource source : node.getSources()) {
                Node sourceNode = graph.getNode(source.getSourceNodeId());
                if (sourceNode == null) {
                    throw new GraphException("Missing source. Node Id: " + node.getId()
                            + " Source Id: " + source.getSourceNodeId());
                }
                graphContext.getNodeContext(sourceNode).incrementReferenceCount();
                source.setSourceNode(sourceNode);  // todo - use getNodeContext()
            }
        }
    }

    private void initOutput(GraphContext graphContext, ProgressMonitor pm) throws GraphException {
        final int outputCount = graphContext.getOutputCount();
        try {
            pm.beginTask("Creating output products", outputCount);
            for (Node node : graphContext.getGraph().getNodes()) {
                NodeContext nodeContext = graphContext.getNodeContext(node);
                if (nodeContext.isOutput()) {
                    initNodeContext(graphContext, nodeContext, SubProgressMonitor.create(pm, 1));
                    graphContext.addOutputNodeContext(nodeContext);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void initNodeContext(GraphContext graphContext, final NodeContext nodeContext, ProgressMonitor pm) throws
            GraphException {
        try {
            NodeSource[] sources = nodeContext.getNode().getSources();
            pm.beginTask("Creating operator", sources.length + 4);

            if (nodeContext.isInitialized()) {
                return;
            }

            for (NodeSource source : sources) {
                NodeContext sourceNodeContext = graphContext.getNodeContext(source.getSourceNode());
                initNodeContext(graphContext, sourceNodeContext, SubProgressMonitor.create(pm, 1));
                nodeContext.addSourceProduct(source.getName(), sourceNodeContext.getTargetProduct());
            }

            OperatorContextInitializer.initOperatorContext(nodeContext, new Xpp3DomParameterInjector(nodeContext), pm);

            // register nodeContext for correct disposal
            graphContext.getInitNodeContextDeque().addFirst(nodeContext);
        } catch (OperatorException e) {
            throw new GraphException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }


    private void fireProcessingStarted(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStarted(graphContext);
        }
    }

    private void fireProcessingStopped(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStopped(graphContext);
        }
    }

    private void fireTileStarted(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStarted(graphContext, rect);
        }

    }

    private void fireTileStopped(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStopped(graphContext, rect);
        }
    }


    private static class Xpp3DomParameterInjector implements ParameterInjector {

        private final NodeContext nodeContext;

        public Xpp3DomParameterInjector(NodeContext nodeContext) {
            this.nodeContext = nodeContext;
        }

        public void injectParameters(Operator operator) throws OperatorException {
            Xpp3Dom configuration = nodeContext.getNode().getConfiguration();
            if (configuration != null) {
                if (operator instanceof ParameterConverter) {
                    ParameterConverter converter = (ParameterConverter) operator;
                    try {
                        converter.setParameterValues(operator, configuration);
                    } catch (Throwable t) {
                        throw new OperatorException(t);
                    }
                } else {
                    DefaultParameterConverter defaultParameterConverter = new DefaultParameterConverter();
                    defaultParameterConverter.setParameterValues(operator, configuration);
                }
            }
        }
    }

    private static class TCL implements TileComputationListener {
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY, Raster tile) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY, Throwable situation) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
