package example;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.util.FileManager;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.shacl.lib.ShLib;

public class Example {
	public static void main(String[] args) {
		Graph shapesGraph = RDFDataMgr.loadGraph("data/shapes.ttl");
		
//		FileManager.get().addLocatorClassLoader(Example.class.getClassLoader());
//        InputStream in = FileManager.get().open("data/shapes.ttl");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//        String line = null;
//        Model model = ModelFactory.createDefaultModel();
//        model.read(in, null, "TURTLE");
//        model.write(System.out, "TURTLE");
//        
        Shapes shapes = Shapes.parse(shapesGraph);
        
        Iterator<Shape> iter = shapes.iterator();
        while(iter.hasNext()){
        	Shape myShape = iter.next();
        	System.out.println(myShape);
        	System.out.println("does it has any target? " + myShape.hasTarget());
        	System.out.println(myShape.getTargets().getClass());
        	System.out.println("is it node shape? " + (myShape instanceof NodeShape));
        	System.out.println("is it property shape? " + (myShape instanceof PropertyShape));
        	System.out.println(myShape.getConstraints().get(0).getClass());
        	System.out.println("\n");
        	
        };
        
        ArrayList<String> rulesAndFacts = new ArrayList<String>();
        
        iter = shapes.iterator();
        while(iter.hasNext()){
        	Shape myShape = iter.next();
        	rulesAndFacts.add(generateShapeFact(myShape));
        	rulesAndFacts.addAll(generateTargetNodeFact(myShape));
        	rulesAndFacts.addAll(generateApplicableFacts(myShape));
        	rulesAndFacts.add(generateHoldConstraintRules(myShape));
        }        
        
        Collections.sort(rulesAndFacts);
        System.out.println(String.join(".\n",rulesAndFacts) + ".");

	}
	
	public static String generateShapeFact(Shape shape) {
		return "shape(<" + shape.getShapeNode() + ">)";
	}
	
	public static ArrayList<String> generateTargetNodeFact(Shape shape) {
		ArrayList<String> facts = new ArrayList<String>();
		
		ArrayList<Target> targets = new ArrayList<Target>(shape.getTargets());
		Iterator<Target> iter = targets.iterator();
		while(iter.hasNext()) {
			Target target = iter.next();
			String nodeURI = ShLib.displayStr(target.getObject());
			facts.add("targetNode(" + nodeURI + ")");
		}
		
		return facts;		
	}
	
	public static ArrayList<String> generateApplicableFacts(Shape shape) {
		ArrayList<String> facts = new ArrayList<String>();
		String shapeURI = "<" + shape.getShapeNode() + ">";
		
		ArrayList<Target> targets = new ArrayList<Target>(shape.getTargets());
		Iterator<Target> iter = targets.iterator();
		while(iter.hasNext()) {
			Target target = iter.next();
			String nodeURI = ShLib.displayStr(target.getObject());
			facts.add("applicable(" + nodeURI + ", " + shapeURI + ")");
		}
		
		return facts;		
	}
	
	public static String generateHoldConstraintRules(Shape shape) {
		ArrayList<String> tails = new ArrayList<String>();
		Collection<Constraint> constraints = shape.getConstraints();
		Iterator<Constraint> iter = constraints.iterator();
		while(iter.hasNext()) {
			Constraint constraint = iter.next();
			String tail = generateTailByConstraint(constraint);
			tails.add(tail);
		}
		
		String shapeURI = "<" + shape.getShapeNode() + ">";
		
		return "holdConstraints(?node," + shapeURI + ") :- " + String.join(", ",tails);			
	}
	
	public static String generateTailByConstraint(Constraint constraint) {
		if (constraint instanceof ClassConstraint) {
			return generateTailByClassConstraint((ClassConstraint) constraint);			
		}
		else {
			return "";
		}
	}
	
	public static String generateTailByClassConstraint(ClassConstraint constraint) {
		String classURI = ShLib.displayStr(constraint.getExpectedClass());
		return "instanceOf(?node," + classURI + ")";
	}
	
}
