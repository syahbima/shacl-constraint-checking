package parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.semanticweb.vlog4j.core.model.api.PositiveLiteral;
import org.semanticweb.vlog4j.core.reasoner.KnowledgeBase;
import org.semanticweb.vlog4j.core.reasoner.QueryResultIterator;
import org.semanticweb.vlog4j.core.reasoner.Reasoner;
import org.semanticweb.vlog4j.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.vlog4j.parser.ParsingException;
import org.semanticweb.vlog4j.parser.RuleParser;

public class NodeTaggingParser {
	
	public static void generateRlsFiles(String shaclFilepath, String rlsFilepath) throws ParsingException, IOException {
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
	
	public static String generateRulesString(String filepath) throws ParsingException, IOException {
		String res = "";
		ArrayList<String> rules = generateRulesList(filepath);
		
		res += String.join(".\n",rules) + ".";
		return res;
	};
	
	
	public static ArrayList<String> generateRulesList(String filepath) throws ParsingException, IOException{
		ArrayList<String> rules = new ArrayList<String>();
		ArrayList<String> nodes = extractNodesFromDataGraph(filepath);
		
		rules.addAll(genereateNodeKindTaggingFact(nodes));
		
		Collections.sort(rules);
		
		return rules;
								
	};
	
	public static ArrayList<String> extractNodesFromDataGraph(String filepath) throws ParsingException, IOException{
		ArrayList<String> nodes = new ArrayList<String>();
		final KnowledgeBase kb = RuleParser.parse("node(?node) :- data(?node,?property,?anotherNode).\n" + "node(?node) :- data(?anotherNode,?property,?node).");
		RuleParser.parseInto(kb, "@source data[3] : load-rdf('src/main/resources/data/data_graph.nt') .");
		
		try (final Reasoner reasoner = new VLogReasoner(kb)) {
			reasoner.reason();
			
			PositiveLiteral query;

			query = RuleParser.parsePositiveLiteral("node(?node)");
			try (final QueryResultIterator answers = reasoner.answerQuery(query, true)) {
				answers.forEachRemaining(answer -> nodes.add(answer.getTerms().get(0).toString()));
			}
		}
		        
        return nodes;
	}
	
	private static ArrayList<String> genereateNodeKindTaggingFact(ArrayList<String> nodes){
		ArrayList<String> facts = new ArrayList<String>();
		Iterator<String> itr = nodes.iterator();
		
		while(itr.hasNext()) {
			String node = itr.next();
			if(node.matches("^_:.*")) {
				facts.add("isBlankNode(" + node + ")");
			}
			else if(node.matches("<[^><]*>")){
				facts.add("isIRI(" + node + ")");
			}
			else {
				facts.add("isLiteral(" + node + ")");
				facts.add(genereateDatatypeTaggingFact(node));
			}
		}
		return facts;
	}
	
	private static String genereateDatatypeTaggingFact(String node){
		if(node.matches("^\".*\"$")) {
			return "hasDatatype(" + node + ",<http://www.w3.org/2001/XMLSchema#strings>)";				
		}
		else if (node.matches("[0-9]*")) {
			return "hasDatatype(" + node + ",<http://www.w3.org/2001/XMLSchema#integer>)";
		}
		else if (node.matches(".*\\^\\^.*")) {
			Pattern pattern = Pattern.compile(".*\\^\\^(.*)$");
			Matcher matcher = pattern.matcher(node);
			if (matcher.find()) {
				return "hasDatatype(" + node + "," + matcher.group(1) + ")";
		    }
			else {
				return "illType(" + node + ")";
			}
		}else if(node.matches(".*@.*")) {
			return "hasDatatype(" + node + ",<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>)";
		}
		return "illType(" + node + ")";
	}
	
	private static void writeGraphToNT(Graph graph, String filepath) {
		try {
			filepath = "src/main/resources/" + filepath;
			OutputStream fos = new FileOutputStream(filepath);
			RDFDataMgr.write(fos, graph, Lang.NTRIPLES);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
