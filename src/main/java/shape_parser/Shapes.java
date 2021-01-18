package shape_parser;

import java.util.*;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.engine.Targets;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sys.JenaSystem;

/** A collection of shapes as output by the SHACL parser.
 * Usage:
 * <pre>
 *    Shapes myShapes = Shapes.parse(graph);
 * </pre>
 */
public class Shapes implements Iterable<Shape> {
    static { JenaSystem.init(); }

    private final Collection<Shape> rootShapes;
    private final Map<Node, Shape> shapes;
    private final Graph shapesGraph;
    private final Targets targets;

    /** Parse the model and return the shapes connected to the targets. */
    public static Shapes parse(Model model) {
        return parse(model.getGraph());
    }

    /** Parse the graph and return the shapes connected to the targets. */
    public static Shapes parse(Graph graph) {
        Targets targets = ShapesParser.targets(graph);
        Map<Node, Shape> shapes = new HashMap<>();
        Collection<Shape> rootShapes = ShapesParser.parseShapes(graph, targets, shapes);
        return new Shapes(graph, shapes, rootShapes, targets);
    }

    public static Shapes parse(String fileOrURL) {
        Graph g = RDFDataMgr.loadGraph(fileOrURL);
        return parse(g);
    }

    /**
     *  Parse the graph and also include all declared node and property shapes, whether connected to the targets or not.
     */
    public static Shapes parseAll(Graph graph) {
        Shapes shapes = parse(graph);
        Collection<Shape> declShapes = ShapesParser.declaredShapes(graph, shapes.shapes);
        declShapes.forEach(shape->{
            if ( ! shapes.getRootShapes().contains(shape) )
                shapes.rootShapes.add(shape);
        });
        return shapes;
    }

    public static Shapes parseAll(String fileOrURL) {
        Graph g = RDFDataMgr.loadGraph(fileOrURL);
        return parseAll(g);
    }

    /** Do not call directly.*/
    private Shapes(Graph shapesGraph, Map<Node, Shape> shapes, Collection<Shape> rootShapes, Targets targets) {
        this.rootShapes = rootShapes;
        this.shapes = shapes;
        this.shapesGraph = shapesGraph;
        this.targets = targets;
    }

    public boolean isEmpty() {
        return shapes.isEmpty();
    }

    public Collection<Shape> getRootShapes() {
        return rootShapes;
    }

    public Shape getShape(Node node) {
        return shapes.get(node);
    }

    public Map<Node,Shape> getShapeMap() {
        return shapes;
    }

    public Graph getGraph() {
        return shapesGraph;
    }

    public Targets getTargets() {
        return targets;
    }

    public int numShapes() {
        return shapes.values().size();
    }

    public int numRootShapes() {
        return rootShapes.size();
    }

    @Override
    public Iterator<Shape> iterator() {
        return rootShapes.iterator();
    }
}
