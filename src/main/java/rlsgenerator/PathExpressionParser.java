package rlsgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;

public class PathExpressionParser {

	private Map<Path,String> pathExpressionMapping;
	private Map<String,Set<Path>> parsedPath;
	
	public PathExpressionParser() {
		pathExpressionMapping = new HashMap<Path, String>();
		parsedPath = new HashMap<String,Set<Path>>();
		parsedPath.put("isPathExpression", new HashSet<Path>());
		parsedPath.put("reachable", new HashSet<Path>());
		
	}
	
	public String getPathRepresentation(Path path) {
		if(!pathExpressionMapping.containsKey(path)) {
			pathExpressionMapping.put(path, "PATH" + (pathExpressionMapping.size() + 1));
		}
		return pathExpressionMapping.get(path);
	}
	
	public String generatePathExpressionOfRules(Path path, PropertyShape shape) {
		String pathGroundTerms = getPathRepresentation(path);
		String shapeIRI = getNodeRepresentation(shape.getShapeNode());
		return "pathExpressionOf(" + pathGroundTerms + "," + shapeIRI + ").";
	}
	
	private String generateIsPathExpressionRules(Path path) {
		if(parsedPath.get("isPathExpression").contains(path)) {
			return "";			
		}
		
		String pathGroundTerms = getPathRepresentation(path);
		parsedPath.get("isPathExpression").add(path);
		return "isPathExpression(" + pathGroundTerms + ").";
	}
	
	public ArrayList<String> generateIsPathExpressionRules() {
		ArrayList<String> rules = new ArrayList<String>();
		for (Path path : pathExpressionMapping.keySet()) {
			rules.add(generateIsPathExpressionRules(path));
		}
		return rules;		
	}	
	
	private ArrayList<String> generateReachableRules(Path path) {
		ArrayList<String> rules = new ArrayList<String>();
		if(parsedPath.get("reachable").contains(path)) {
			return rules;			
		}
		
		String pathGroundTerms = getPathRepresentation(path);
		String head = "reachable(?node,?anotherNode," + pathGroundTerms + ")";
		
		if(path instanceof P_Link) {
			String body = "dataGraphHas(?node," + path.toString() + ",?anotherNode)";
			rules.add(head + " :- " + body + ".");
		}
		
		else if (path instanceof P_Seq) {
			ArrayList<String> pathsIRI = getAllMatches(path.toString(),"<[^><]*>");
			ArrayList<String> body = new ArrayList<String>();
			
			//initiate node variable sequence
			ArrayList<String> nodeVariables = new ArrayList<String>();
			nodeVariables.add("?node");
			for (int i=1; i< pathsIRI.size(); i++) {
				nodeVariables.add("?node"+i);
			}
			nodeVariables.add("?anotherNode");
			
			//generate sequence of atom in body
			for (int i=0; i< pathsIRI.size(); i++) {
				body.add("dataGraphHas(" + nodeVariables.get(i) + "," + pathsIRI.get(i) + "," + nodeVariables.get(i+1) + ")");
			}
			rules.add(head + " :- " + String.join(", ",body) + ".");
		}	
		else if (path instanceof P_Alt) {
			ArrayList<String> pathsIRI = getAllMatches(path.toString(),"<[^><]*>");
			for (String pathIRI : pathsIRI) {
				String body = "dataGraphHas(?node," + pathIRI + ",?anotherNode)";
				rules.add(head + " :- " + body + ".");
			}			
		}
		else if (path instanceof P_Inverse) {
			String body = "dataGraphHas(?anotherNode," + path.toString().substring(1) + ",?node)"; 
			rules.add(head + " :- " + body + ".");
		}
		
		parsedPath.get("reachable").add(path);
		return rules;
	}
	
	public ArrayList<String> generateReachableRules() {
		ArrayList<String> rules = new ArrayList<String>();
		for (Path path : pathExpressionMapping.keySet()) {
			rules.addAll(generateReachableRules(path));
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
