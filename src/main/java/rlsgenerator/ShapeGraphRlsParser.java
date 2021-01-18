package rlsgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
//import org.apache.jena.shacl.*;
import shape_parser.Shapes;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.engine.TargetType;
//import org.apache.jena.shacl.engine.constraint.*;
import constraint.*;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;

/**
 * @author syahbima
 *
 */

public class ShapeGraphRlsParser {
	
	private static Set<Integer> minCountConstraintFlag = new HashSet<Integer>();
	private static Set<Integer> maxCountConstraintFlag = new HashSet<Integer>();
	private static Set<Integer> andConstraintFlag = new HashSet<Integer>();
	private static Set<Integer> orConstraintFlag = new HashSet<Integer>();
	private static Set<Integer> xoneConstraintFlag = new HashSet<Integer>();
	private static Set<Shape> parsedShape = new HashSet<Shape>();
	private static Queue<Shape> shapeQueue  = new LinkedList<Shape>();
	private static PathExpressionParser pathExpressionParser = new PathExpressionParser();
	
	public static void printShapesInformations(String filepath) {
		Graph shapesGraph = RDFDataMgr.loadGraph(filepath);		   
        Shapes shapes = Shapes.parse(shapesGraph);
        
        Iterator<Shape> iter = shapes.iterator();
        while(iter.hasNext()){
        	Shape myShape = iter.next();
        	System.out.println(myShape);
        	System.out.println("targets : " + myShape.getTargets());
        	System.out.println("is it node shape? " + (myShape instanceof NodeShape));
        	System.out.println("is it property shape? " + (myShape instanceof PropertyShape));
        	System.out.println("Property Shapes List : " + (myShape.getPropertyShapes()));
        	System.out.println("constraints: " + myShape.getConstraints());
        	System.out.println("\n");
        } 
        
	}
	
	
	
	/**
	 * This method will generate VLOG Rules from specific SHACL file (RDF format)
	 * and write it to specific RLS file. If the RLS file not existed, this method
	 * will create new RLS file.
	 * 
	 * @param shaclFile SHACL file path in RDF format
	 * @param rlsFile RLS file path to be written by the output
	 */
	public static void generateRlsFile(String shaclFile, String rlsFile) {
		rlsFile = "src/main/resources/" + rlsFile;
		String rulesString = generateRulesString(shaclFile);
		try {
	      File myObj = new File(rlsFile);
	      myObj.createNewFile();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
		
		try {
	      FileWriter myWriter = new FileWriter(rlsFile);
	      myWriter.write(rulesString);
	      myWriter.close();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }		
	}
	
	/**
	 * This method will generate VLOG Rules from specific SHACL file (RDF format)
	 * into String. 
	 * 
	 * @param shaclFile
	 * @return VLOG rules concatenated in a single String
	 */
	public static String generateRulesString(String shaclFile) {	
		ArrayList<String> rules = generateRulesList(shaclFile);

		return String.join("\n",rules);
	}
	
	/**
	 * This method will generate VLOG Rules from specific SHACL file (RDF format)
	 * into Array List of VLOG rules statements. 
	 * 
	 * @param shaclFile
	 * @return Array List of Vlog Rules
	 */
	public static ArrayList<String> generateRulesList(String shaclFile) {
		ArrayList<String> rules = new ArrayList<String>();		
		Graph shapesGraph = RDFDataMgr.loadGraph(shaclFile);		   
        Shapes shapes = Shapes.parse(shapesGraph);
		
        //adding shape to the queue
		Iterator<Shape> iter = shapes.iterator();
        while(iter.hasNext()){
        	Shape shape = iter.next();
        	shapeQueue.add(shape);
        }
        
        while(!shapeQueue.isEmpty()) {
        	Shape shape = shapeQueue.poll();
        	if(parsedShape.contains(shape)) {
        		continue;
        	}
        	rules.addAll(generateRulesFromSingleShape(shape));
        	parsedShape.add(shape);
        }        
        if(!minCountConstraintFlag.isEmpty()) rules.addAll(generateMinCountConstraintRules());
        if(!maxCountConstraintFlag.isEmpty()) rules.addAll(generateMaxCountConstraintRules());
        if(!minCountConstraintFlag.isEmpty()) rules.addAll(generateAllDifferentRules());
        if(!andConstraintFlag.isEmpty()) rules.addAll(generateAndConstraintRules());
        if(!orConstraintFlag.isEmpty()) rules.addAll(generateOrConstraintRules());
        if(!xoneConstraintFlag.isEmpty())rules.addAll(generateXoneConstraintRules());
        rules.addAll(pathExpressionParser.generateIsPathExpressionRules());
        rules.addAll(pathExpressionParser.generateReachableRules());

        Collections.sort(rules);        
        return rules;
	}
	
	private static ArrayList<String> generateRulesFromSingleShape(Shape shape) {
		ArrayList<String> rules = new ArrayList<String>();	
		String ShapeIRI = getNodeRepresentation(shape.getShapeNode());
		rules.add("isShape(" + ShapeIRI + ").");
		// TODO check implicit shape does it have target or not
		rules.addAll(generateFocusNodeOfRules(shape));
		rules.add(generateValueNodeHoldsAllConstraintsRules(shape));
		
		if(shape instanceof NodeShape) {
			rules.add(generateFocusNodeSatisfiesShapeRules((NodeShape) shape));
		}
		else if (shape instanceof PropertyShape) {
			PropertyShape propertyShape = (PropertyShape) shape;
			Path path = propertyShape.getPath();
			
			rules.add(generateFocusNodeSatisfiesShapeRules(propertyShape));
			rules.add(pathExpressionParser.generatePathExpressionOfRules(path, propertyShape));
			
		}
		return rules;
	}
	
	private static ArrayList<String> generateFocusNodeOfRules(Shape shape) {
		String shapeIRI = getNodeRepresentation(shape.getShapeNode());
		ArrayList<String> rules = new ArrayList<String>();		
		ArrayList<Target> targets = new ArrayList<Target>(shape.getTargets());
		Iterator<Target> iter = targets.iterator();
		
		while(iter.hasNext()) {
			Target target = iter.next();
			TargetType targetType = target.getTargetType();
			String targetURI = ShLib.displayStr(target.getObject());
			
			switch (targetType) {
		       case targetNode:
		    	  rules.add("focusNodeOf(" + targetURI + ", " + shapeIRI + ").");
		          break;		                    
		       case targetClass:
		    	  rules.add("focusNodeOf(?node," + shapeIRI + ") :- instanceOf(?node, " + targetURI + ").");
		          break;		          
		       case implicitClass:
		    	   rules.add("focusNodeOf(?node," + shapeIRI + ") :- instanceOf(?node, " + targetURI + ").");
		          break;		                         
		       case targetSubjectsOf: 
		    	  rules.add("focusNodeOf(?node," + shapeIRI + ") :- subject(?node, " + targetURI + ").");
		          break;		                        
		       case targetObjectsOf:
		    	   rules.add("focusNodeOf(?node," + shapeIRI + ") :- object(?node, " + targetURI + ").");
		          break;	       
		     }			
		}		
		return rules;		
	}
	
	private static String generateValueNodeHoldsAllConstraintsRules(Shape shape) {
		String ShapeIRI = getNodeRepresentation(shape.getShapeNode());
		String head = "valueNodeHoldsAllConstraints(?valueNode," + ShapeIRI + ")";
		List<PropertyShape> propertyShapes = shape.getPropertyShapes();
		
		ArrayList<String> body = new ArrayList<String>();
		body.add("isNode(?valueNode)");
		
		//parse sh property constraint 
		for (PropertyShape propertyShape : propertyShapes) {
			String shapeIRI = "<" + propertyShape.getShapeNode() + ">";
			shapeQueue.add(propertyShape);
			body.add("holdsPropertyConstraint(?valueNode," + shapeIRI + ")");
		}
		
		Collection<Constraint> constraints = shape.getConstraints();
		Iterator<Constraint> iter = constraints.iterator();
		
		while(iter.hasNext()) {
			// TODO CREATE IMPLEMENTATION TO PARSE EVERY CONSTRAINT PARAMETER
			Constraint constraint = iter.next();
			String value = "DUMMYVALUE";
			ArrayList<String> dummyList = new ArrayList<String>();
			dummyList.add("SHAPE1");
			dummyList.add("SHAPE2");
			dummyList.add("SHAPE3");
			
			if(constraint instanceof ClassConstraint) {
				//TODO
				String classIRI = ((ClassConstraint) constraint).getValue();
				body.add("holdsClassConstraint(?valueNode," + classIRI + ")");
			}
			else if(constraint instanceof DatatypeConstraint) {
				//TODO
				String datatypeIRI = ((DatatypeConstraint) constraint).getValue();
				body.add("holdsDatatypeConstraint(?valueNode," + datatypeIRI + ")");
			}
			else if (constraint instanceof NodeKindConstraint) {
				//TODO
				String nodeKind = ((NodeKindConstraint) constraint).getValue();
				body.add("holdsNodeKindConstraint(?valueNode," + nodeKind + ")");
			}
			else if (constraint instanceof ShNot)  {
				//TODO
				Shape valueShape = ((ShNot) constraint).getOther();
				String shapeIRI = "<" + valueShape.getShapeNode() + ">";
				shapeQueue.add(valueShape);					
				
				body.add("holdsNotConstraint(?valueNode," + shapeIRI + ")");
			}
			else if (constraint instanceof ShAnd)  {
				List<Shape> valueShapes = ((ShAnd) constraint).getOthers();
				shapeQueue.addAll(valueShapes);
				andConstraintFlag.add(valueShapes.size());
				
				List<String> shapesIRI = valueShapes.stream()
						   .map(object -> "<" + object.getShapeNode() + ">")
						   .collect(Collectors.toList());			
				body.add("holdsAndConstraint"+ valueShapes.size() +"(?valueNode," + String.join(",", shapesIRI) + ")");				
			}
			else if (constraint instanceof ShOr)  {
				//TODO
				List<Shape> valueShapes = ((ShOr) constraint).getOthers();
				shapeQueue.addAll(valueShapes);
				orConstraintFlag.add(valueShapes.size());
				
				List<String> shapesIRI = valueShapes.stream()
						   .map(object -> "<" + object.getShapeNode() + ">")
						   .collect(Collectors.toList());
				
				body.add("holdsOrConstraint"+ valueShapes.size() +"(?valueNode," +  String.join(",", shapesIRI) + ")");
				
			}
			else if (constraint instanceof ShXone)  {
				//TODO
				List<Shape> valueShapes = ((ShXone) constraint).getOthers();
				shapeQueue.addAll(valueShapes);
				xoneConstraintFlag.add(valueShapes.size());
				
				List<String> shapesIRI = valueShapes.stream()
						   .map(object -> "<" + object.getShapeNode() + ">")
						   .collect(Collectors.toList());
				
				body.add("holdsXoneConstraint"+ valueShapes.size() +"(?valueNode," + String.join(",", dummyList)+ ")");
				
			}
			else if (constraint instanceof ShNode)  {
				Shape valueShape = ((ShNode) constraint).getOther();
				String shapeIRI = "<" + valueShape.getShapeNode() + ">";
				shapeQueue.add(valueShape);
				body.add("holdsNodeConstraint(?valueNode," + shapeIRI + ")");
			}
		}		
		return head + " :- " + String.join(", ", body) + ".";
	}
	
	private static String generateFocusNodeSatisfiesShapeRules(NodeShape shape) {
		String ShapeIRI = getNodeRepresentation(shape.getShapeNode());
		String head = "focusNodeSatisfiesShape(?focusNode," + ShapeIRI + ")";
		String body = "valueNodeHoldsAllConstraints(?focusNode," + ShapeIRI + ")";
		
		return head + " :- " + body + ".";		
	}
	
	private static String generateFocusNodeSatisfiesShapeRules(PropertyShape shape) {
		String ShapeIRI = getNodeRepresentation(shape.getShapeNode());
		String head = "focusNodeSatisfiesShape(?focusNode," + ShapeIRI + ")";
		ArrayList<String> body = new ArrayList<String>();
		
		body.add("pathExpressionOf(?shapePathExpression," + ShapeIRI + ")");
		body.add("isNode(?focusNode)");
		body.add("~someReachableNodeViolateConstraint(?focusNode," + ShapeIRI + ")" );
		
		Collection<Constraint> constraints = shape.getConstraints();
		Iterator<Constraint> iter = constraints.iterator();
		
		while(iter.hasNext()) {
			Constraint constraint = iter.next();
			String valueInt = constraint.toString().replaceAll("[^0-9]", "");
			String value = "DUMMYVALUE";
			if(constraint instanceof MinCount) {
				body.add("holdsMinCount" + valueInt + "Constraint(?focusNode,?shapePathExpression)");
				minCountConstraintFlag.add(Integer.parseInt(valueInt));
			}
			else if(constraint instanceof MaxCount) {
				body.add("holdsMaxCount" + valueInt + "Constraint(?focusNode,?shapePathExpression)");
				maxCountConstraintFlag.add(Integer.parseInt(valueInt));
				minCountConstraintFlag.add(Integer.parseInt(valueInt)+1);
			}
			else if (constraint instanceof EqualsConstraint) {
				Node propertyNode = ((EqualsConstraint) constraint).getProperty();
				Path propertyPath = new P_Link(propertyNode);
				String pathString = pathExpressionParser.getPathRepresentation(propertyPath);
				body.add("holdsEqualsConstraint(?focusNode,?shapePathExpression," + pathString + ")");
			}
			else if (constraint instanceof DisjointConstraint)  {
				Node propertyNode = ((EqualsConstraint) constraint).getProperty();
				Path propertyPath = new P_Link(propertyNode);
				String pathString = pathExpressionParser.getPathRepresentation(propertyPath);
				
				body.add("holdsDisjointConstraint(?focusNode,?shapePathExpression," + pathString + ")");
			}
			
		}
		
		
		return head + " :- " + String.join(", ", body) + ".";
	}
	
	private static ArrayList<String> generateMinCountConstraintRules(){
		ArrayList<String> rules = new ArrayList<String>();
		Iterator<Integer> itr = minCountConstraintFlag.iterator();
		
		while(itr.hasNext()) {
			int cardinality = itr.next();
			String head = "holdMinCount" + cardinality + "Constraint(?focusNode,?pathExpression)";
			ArrayList<String> body = new ArrayList<String>();
			ArrayList<String> nodeVariables = new ArrayList<String>();
			
			for(int nodeNum=1; nodeNum <= cardinality; nodeNum++) {
				String nodeVar = "?node" + nodeNum;
				body.add("reachable(?focusNode," + nodeVar + ",?pathExpression)");
				nodeVariables.add(nodeVar);
			}		
			body.add("allDifferent" + cardinality + "(" + String.join(",", nodeVariables) + ")");
			
			rules.add(head + " :- " + String.join(", ", body) + ".");		
		}		
		return rules;
	}
	
	private static ArrayList<String> generateMaxCountConstraintRules(){
		ArrayList<String> rules = new ArrayList<String>();
		Iterator<Integer> itr = maxCountConstraintFlag.iterator();
		
		while(itr.hasNext()) {
			int cardinality = itr.next();
			String head = "holdMaxCount" + cardinality + "Constraint(?focusNode,?pathExpression)";
			ArrayList<String> body = new ArrayList<String>();
			
			body.add("isNode(?focusNode)");
			body.add("isPathExpression(?pathExpression)");
			body.add("~holdMinCount" + (cardinality + 1) + "Constraint(?focusNode,?pathExpression)");
		
			rules.add(head + " :- " + String.join(",", body) + ".");		
		}
		
		return rules;		
	}
	
	private static ArrayList<String> generateAllDifferentRules(){
		ArrayList<String> rules = new ArrayList<String>();
		rules.add("allDifferent(?node1) :- isNode(?node1).");
		int maxCardinality = Collections.max(minCountConstraintFlag);
		
		ArrayList<String> nodeVariables = new ArrayList<String>();
		nodeVariables.add("?node1");
		
		for(int cardinality=2; cardinality<=maxCardinality; cardinality++) {
			nodeVariables.add("?node" + cardinality);
			String head = "allDifferent" + cardinality + "(" + String.join(",", nodeVariables) + ")";
			ArrayList<String> body = new ArrayList<String>();
			
			List<String> subList = nodeVariables.subList(1, nodeVariables.size());
			body.add("isNode(?node1)");
			body.add("allDifferent" + (cardinality-1) + "(" + String.join(",", subList) + ")");
			
			for (String nodeVar : subList) {
				body.add("~equal(?node1," + nodeVar + ")");
			}
			rules.add(head + " :- " + String.join(", ", body) + ".");
		}
		
		return rules;	
	}
	
	private static ArrayList<String> generateAndConstraintRules(){
		ArrayList<String> rules = new ArrayList<String>();
		rules.add("holdsAndConstraint1(?node,?shape1) :- focusNodeSatisfiesShape(?node,?shape1).");
		int maxCardinality = Collections.max(andConstraintFlag);
		ArrayList<String> shapeVariables = new ArrayList<String>();
		shapeVariables.add("?shape1");
		
		for(int cardinality=2; cardinality<=maxCardinality; cardinality++) {
			shapeVariables.add("?shape" + cardinality);
			String head = "holdsAndConsraint" + cardinality + "(?node," + String.join(",", shapeVariables) + ")";
			ArrayList<String> body = new ArrayList<String>();
			//sublist contain shape variables from index 0 to n-1
			List<String> subList = shapeVariables.subList(0, shapeVariables.size()-1);
			body.add("holdsAndConsraint" + (cardinality-1) + "(?node," + String.join(",", subList) + ")");
			body.add("focusNodeSatisfiesShape(?node,?shape" + cardinality + ")" );
			rules.add(head + " :- " + String.join(", ", body) + ".");
		}		
		return rules;
	}
	
	private static ArrayList<String> generateOrConstraintRules(){
		ArrayList<String> rules = new ArrayList<String>();
		rules.add("holdsOrConstraint1(?node,?shape1) :- focusNodeSatisfiesShape(?node,?shape1).");
		int maxCardinality = Collections.max(orConstraintFlag);
		ArrayList<String> shapeVariables = new ArrayList<String>();
		shapeVariables.add("?shape1");
		
		for(int cardinality=2; cardinality<=maxCardinality; cardinality++) {
			shapeVariables.add("?shape" + cardinality);
			String head = "holdsOrConstraint" + cardinality + "(?node," + String.join(",", shapeVariables) + ")";
			ArrayList<String> body1 = new ArrayList<String>();
			ArrayList<String> body2 = new ArrayList<String>();			
			//sublist contain shape variables from index 0 to n-1
			List<String> subList = shapeVariables.subList(0, shapeVariables.size()-1);
			
			for (String nodeVar : subList) {
				body1.add("isShape(" + nodeVar + ")");
			}
			body1.add("focusNodeSatisfiesShape(?node,?shape" + cardinality + ")");
			rules.add(head + " :- " + String.join(", ", body1) + ".");
			
			body2.add("isShape(?shape" + cardinality + ")");
			body2.add("holdsOrConsraint" + (cardinality-1) + "(?node," + String.join(",", subList) + ")");
			rules.add(head + " :- " + String.join(", ", body2) + ".");
		}
		
		return rules;
	}
	
	private static ArrayList<String> generateXoneConstraintRules(){
		//TODO
		ArrayList<String> rules = new ArrayList<String>();
		rules.add("holdsXoneConstraint1(?node,?shape1) :- focusNodeSatisfiesShape(?node,?shape1).");
		int maxCardinality = Collections.max(xoneConstraintFlag);
		ArrayList<String> shapeVariables = new ArrayList<String>();
		shapeVariables.add("?shape1");
		
		for(int cardinality=2; cardinality<=maxCardinality; cardinality++) {
			shapeVariables.add("?shape" + cardinality);
			String head = "holdsXoneConstraint" + cardinality + "(?node," + String.join(",", shapeVariables) + ")";
			ArrayList<String> body1 = new ArrayList<String>();
			ArrayList<String> body2 = new ArrayList<String>();			
			//sublist contain shape variables from index 0 to n-1
			List<String> subList = shapeVariables.subList(0, shapeVariables.size()-1);
			
			for (String nodeVar : subList) {
				body1.add("isShape(" + nodeVar + ")");
			}
			body1.add("~holdsOrConstraint" + (cardinality-1) + "(?node," +  String.join(",", subList) + ")");
			body1.add("focusNodeSatisfiesShape(?node,?shape" + cardinality + ")");
			rules.add(head + " :- " + String.join(",", body1) + ".");
			
			body2.add("isShape(?shape" + cardinality + ")");
			body2.add("holdsXoneConsraint" + (cardinality-1) + "(?node," + String.join(",", subList) + ")");
			body2.add("~focusNodeSatisfiesShape(?node,?shape" + cardinality + ")");
			rules.add(head + " :- " + String.join(", ", body2) + ".");
		}
		
		return rules;	
	}
	
	private static ArrayList<String> generateReachableRules(PropertyShape shape) {
		ArrayList<String> rules = new ArrayList<String>();
		String ShapeIRI = getNodeRepresentation(shape.getShapeNode());
		Path path = shape.getPath();
		String pathString = "dummyPath";
		String head = "reachable(?node,?node2," + pathString + ")"  ;
		
		if(path instanceof P_Link) {
			String body = "data(?node," + path + ",?destNode)";
			rules.add(head + " :- " + body + ".");
		}
		
		else if (path instanceof P_Seq) {
			ArrayList<String> predicates = getAllRegexMatches(path.toString(),"<[^><]*>");
			ArrayList<String> body = new ArrayList<String>();
			
			//initiate node sequence list
			ArrayList<String> nodes = new ArrayList<String>();
			nodes.add("?node");
			for (int i=1; i< predicates.size(); i++) {
				nodes.add("?node"+i);
			}
			nodes.add("?destNode");
			
			//create rules
			for (int i=0; i< predicates.size(); i++) {
				body.add("data(" + nodes.get(i) + "," + predicates.get(i) + "," + nodes.get(i+1) + ")");
			}
			
			rules.add(head + " :- " + String.join(", ",body) + ".");
		}	
		else if (path instanceof P_Alt) {
			ArrayList<String> predicates = getAllRegexMatches(path.toString(),"<[^><]*>");
			for (String predicate : predicates) {
				String body = "data(?node," + predicate + ",?destNode)";
				rules.add(head + " :- " + body + ".");
			}			
		}
		else if (path instanceof P_Inverse) {
			String body = "data(?destNode," + path.toString().substring(1) + ",?node)"; 
			rules.add(head + " :- " + body + ".");
		}
		
		return rules;
	}
	

	private static ArrayList<String> getAllRegexMatches(String text, String regex) {
        ArrayList<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while(m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }
	
	public static String getNodeRepresentation(Node node) {
		if (node.isURI()) {
			return "<" + node.toString() + ">";
		}
		else if(node.isBlank()) {
			return "<" + node.toString() + ">";
		}
		return node.toString();
	}

}
