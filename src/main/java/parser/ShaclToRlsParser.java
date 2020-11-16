package parser;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.engine.TargetType;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.engine.constraint.MaxCount;
import org.apache.jena.shacl.engine.constraint.MinCount;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.sparql.path.Path;



import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Alt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class ShaclToRlsParser {
	private static int maxAllDifferent = 0;
	private static Set<Integer> minCountSet = new HashSet<Integer>();
	private static Set<Integer> maxCountSet = new HashSet<Integer>();
	
	public static void generateRlsFiles(String shaclFilepath, String rlsFilepath) {
		rlsFilepath = "src/main/resources/" + rlsFilepath;
		String rulesString = generateRulesString(shaclFilepath);
		try {
	      File myObj = new File(rlsFilepath);
	      if (myObj.createNewFile()) {
	        System.out.println("File created: " + myObj.getName());
	      } else {
	        System.out.println("File already exists.");
	      }
	    } catch (IOException e) {
	      System.out.println("An error occurred when creating file " + rlsFilepath);
	      e.printStackTrace();
	    }
		
		try {
	      FileWriter myWriter = new FileWriter(rlsFilepath);
	      myWriter.write(rulesString);
	      myWriter.close();
	      System.out.println("Successfully wrote to the file.");
	    } catch (IOException e) {
	      System.out.println("An error occurred.");
	      e.printStackTrace();
	    }		
	}
	
	public static String generateRulesString(String filepath) {
		String res = "";
		
		ArrayList<String> rules = generateRulesList(filepath);
		ArrayList<String> groundRules =  generateGroundRules();
		
		res += String.join("\n",groundRules);
		res += String.join(".\n",rules) + ".";
		return res;
	}
	
	private static ArrayList<String> generateGroundRules()  {
		ArrayList<String> rules = new ArrayList<String>();
		File file = new File("src/main/resources/data/groundRules.rls"); 
		  
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String st; 
			while ((st = br.readLine()) != null) {
			  rules.add(st); 
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return rules;
	}
	
	public static ArrayList<String> generateRulesList(String filepath) {
		ArrayList<String> rules = new ArrayList<String>();		
		Graph shapesGraph = RDFDataMgr.loadGraph(filepath);		   
        Shapes shapes = Shapes.parse(shapesGraph);
			
		Iterator<Shape> iter = shapes.iterator();
        while(iter.hasNext()){
        	Shape myShape = iter.next();
        	rules.add(generateShapeFact(myShape));
        	rules.addAll(generateTargetRules(myShape));
        	rules.addAll(generateHoldConstraintRules(myShape));
        	if(myShape instanceof PropertyShape) {
        		rules.addAll(generateReachableRules((PropertyShape) myShape));
        	}        	
        }
        rules.addAll(generatePredefinedRulesMinCount());
    	rules.addAll(generatePredefinedRulesMaxCount());
    	rules.addAll(generatePredefinedRulesAllDifferent(maxAllDifferent));
        Collections.sort(rules);
        
        return rules;
	}
	
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
	
	private static String generateShapeFact(Shape shape) {
		return "shape(<" + shape.getShapeNode() + ">)";
	}
	
	private static ArrayList<String> generateTargetRules(Shape shape) {
		String shapeURI = "<" + shape.getShapeNode() + ">";
		ArrayList<String> rules = new ArrayList<String>();
		
		ArrayList<Target> targets = new ArrayList<Target>(shape.getTargets());
		Iterator<Target> iter = targets.iterator();
		
		while(iter.hasNext()) {
			Target target = iter.next();
			TargetType targetType = target.getTargetType();
			
			switch (targetType) {
		       case targetNode:
		    	  rules.add(parseTargetNode(shapeURI,target));
		          break;
		                    
		       case targetClass:
		    	  rules.add(parseTargetClass(shapeURI,target));
		          break;
		          
		       case implicitClass:
		    	   rules.add(parseImplicitClass(shapeURI,target));
		          break;
		                         
		       case targetSubjectsOf: 
		    	  rules.add(parseTargetSubjectsOf(shapeURI,target));
		          break;
		                        
		       case targetObjectsOf:
		    	   rules.add(parseTargetObjectsOf(shapeURI,target));
		          break;	       
		     }			
		}		
		return rules;		
	}
	
	private static String parseTargetNode(String shapeURI, Target target) {
		String nodeURI = ShLib.displayStr(target.getObject());
		return "applicable(" + shapeURI + ", " + nodeURI + ")";
	}
	
	private static String parseTargetClass(String shapeURI, Target target) {
		String classURI = ShLib.displayStr(target.getObject());
		return "applicable(" + shapeURI + ", ?node) :- instanceOf(?node, " + classURI + ")";
	}
	
	private static String parseImplicitClass(String shapeURI, Target target) {
		String classURI = ShLib.displayStr(target.getObject());
		return "applicable(" + shapeURI + ", ?node) :- instanceOf(?node, " + classURI + ")";
	}
	
	private static String parseTargetSubjectsOf(String shapeURI, Target target) {
		String propertyURI = ShLib.displayStr(target.getObject());
		return "applicable(" + shapeURI + ", ?node) :- subject(?node, " + propertyURI + ")";
	}
	
	private static String parseTargetObjectsOf(String shapeURI, Target target) {
		String propertyURI = ShLib.displayStr(target.getObject());
		return "applicable(" + shapeURI + ", ?node) :- object(?node, " + propertyURI + ")";
	}

	private static ArrayList<String> generateReachableRules(PropertyShape shape) {
		ArrayList<String> rules = new ArrayList<String>();
		String shapeURI = "<" + shape.getShapeNode() + ">";
		Path path = shape.getPath();
		String head = "reachable(?node,?destNode," + shapeURI + ") :- ";
		
		if(path instanceof P_Link) {
			String body = "data(?node," + path + ",?destNode)";
			rules.add(head+body);
		}
		
		else if (path instanceof P_Seq) {
			ArrayList<String> predicates = getAllMatches(path.toString(),"<[^><]*>");
			ArrayList<String> atoms = new ArrayList<String>();
			
			//initiate node sequence list
			ArrayList<String> nodes = new ArrayList<String>();
			nodes.add("?node");
			for (int i=1; i< predicates.size(); i++) {
				nodes.add("?node"+i);
			}
			nodes.add("?destNode");
			
			//create rules
			for (int i=0; i< predicates.size(); i++) {
				atoms.add("data(" + nodes.get(i) + "," + predicates.get(i) + "," + nodes.get(i+1) + ")");
			}
			
			String body = String.join(", ",atoms);
			rules.add(head+body);
		}	
		else if (path instanceof P_Alt) {
			ArrayList<String> predicates = getAllMatches(path.toString(),"<[^><]*>");
			for (String predicate : predicates) {
				String body = "data(?node," + predicate + ",?destNode)";
				rules.add(head+body);
			}			
		}
		else if (path instanceof P_Inverse) {
			String body = "data(?destNode," + path.toString().substring(1) + ",?node)"; 
			rules.add(head+body);
		}
		
		return rules;
	}
	
	
	private static ArrayList<String> generateHoldConstraintRules(Shape shape) {
		ArrayList<String> rules = new ArrayList<String>();
		ArrayList<String> tails = new ArrayList<String>();
		Collection<Constraint> constraints = shape.getConstraints();
		Iterator<Constraint> iter = constraints.iterator();
		while(iter.hasNext()) {
			Constraint constraint = iter.next();
			String tail = generateTailByConstraint(shape, constraint);			
			tails.add(tail);
		}		
		String shapeURI = "<" + shape.getShapeNode() + ">";
		
		if(!tails.isEmpty()) {
			rules.add("holdConstraints(?node," + shapeURI + ") :- " + String.join(", ",tails));
		}
		return rules;		
	}
	
	
	private static String generateTailByConstraint(Shape shape, Constraint constraint) {
		if (constraint instanceof ClassConstraint) {
			return generateTailByClassConstraint(shape,(ClassConstraint) constraint);			
		}
		else if(constraint instanceof MinCount) {
			return generateAtomByMincountConstraint(shape, (MinCount) constraint);			
		}
		else if(constraint instanceof MaxCount) {
			return generateAtomByMaxcountConstraint(shape, (MaxCount) constraint);			
		}
		else {
			return "";
		}
	}
	
	private static String generateTailByClassConstraint(Shape shape, ClassConstraint constraint) {
		String classURI = ShLib.displayStr(constraint.getExpectedClass());
		String shapeURI = "<" + shape.getShapeNode() + ">";
		if(shape instanceof PropertyShape) {
			return "holdPathClassConstraint(?node," + shapeURI + "," + classURI + ")";	
		}else{
			return "instanceOf(?node," + classURI + ")";
		}
	}
	
	private static String generateAtomByMincountConstraint(Shape shape, MinCount constraint) {
		String shapeURI = "<" + shape.getShapeNode() + ">";
		int minCount = Integer.parseInt(constraint.toString().replaceAll("[^0-9]", ""));
		
		//set the highest minCount value
		if (minCount > maxAllDifferent) {
			maxAllDifferent = minCount;
		}
		
		//update minCountSet
		minCountSet.add(minCount);
		
		//generate atom string		
		return ("minCountConstraint" + minCount + "(?node," + shapeURI + ")" );		
	}
	
	private static String generateAtomByMaxcountConstraint(Shape shape, MaxCount constraint) {
		String shapeURI = "<" + shape.getShapeNode() + ">";
		int maxCount = Integer.parseInt(constraint.toString().replaceAll("[^0-9]", ""));
		
		//set the highest maxCount value
		if (maxCount+1 > maxAllDifferent) {
			maxAllDifferent = maxCount;
		}
		
		//update maxCountSet
		maxCountSet.add(maxCount);
		
		//generate atom string		
		return ("maxCountConstraint" + maxCount + "(?node," + shapeURI + ")" );		
	}
	
	private static ArrayList<String> generatePredefinedRulesMinCount(){
		ArrayList<String> rules = new ArrayList<String>();
		Iterator<Integer> itr = minCountSet.iterator();
		
		while(itr.hasNext()) {
			int minCount = itr.next();
			String head = "minCountConstraint" + minCount + "(?node,?shape)";
			ArrayList<String> body = new ArrayList<String>();
			ArrayList<String> nodes = new ArrayList<String>();
			
			for(int i=1; i<=minCount; i++) {
				body.add("reachable(?node,?node" + i + ",?shape)");
				nodes.add("?node" + i);
			}
			
			//generate allDifferent only if the minCount is greater than 1
			if(minCount > 1) {
				body.add("allDifferent" + minCount + "(" + String.join(",", nodes) + ")");
			}		
			
			rules.add(head + " :- " + String.join(", ", body));
		}
		
		return rules;
	}
	
	private static ArrayList<String> generatePredefinedRulesMaxCount(){
		ArrayList<String> rules = new ArrayList<String>();
		Iterator<Integer> itr = maxCountSet.iterator();
		
		while(itr.hasNext()) {
			int maxCount = itr.next();
			String head = "maxCountConstraint" + maxCount + "(?node,?shape)";
			String body = "node(?node), shape(?shape), ~minCounConstraint" + (maxCount+1) + "(?node,?shape)";
			rules.add(head + " :- " + body);
		}
		return rules;
	}
	
	private static ArrayList<String> generatePredefinedRulesAllDifferent(int maxNum){
		ArrayList<String> rules = new ArrayList<String>();
		ArrayList<String> nodes = new ArrayList<String>();
		
		
		rules.add("allDifferent2(?node1,?node2) :- node(?node1), node(?node2), ~equal(?node1,?node2)");
		nodes.add("?node1");
		nodes.add("?node2");
		
		for(int i=3; i <= maxNum; i++) {
			ArrayList<String> body = new ArrayList<String>(); 
			body.add("allDifferent" + (i-1) + "(" + String.join(",", nodes) + ")");
			body.add("node(?node" + i + ")");
			nodes.add("?node" + i);
			String head = "allDifferent" + i + "(" + String.join(",", nodes) + ")";
			
			for(int j=0; j < i-1; j++) {
				body.add("~equal(" + nodes.get(j) + "," + nodes.get(nodes.size()-1) + ")");
			}			
			rules.add(head + " :- " + String.join(", ", body));
		}
		return rules;	
	}
	
	private static ArrayList<String> getAllMatches(String text, String regex) {
        ArrayList<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while(m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }
	

}
