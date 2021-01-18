package example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.semanticweb.vlog4j.core.model.api.PositiveLiteral;
import org.semanticweb.vlog4j.core.reasoner.KnowledgeBase;
import org.semanticweb.vlog4j.core.reasoner.QueryResultIterator;
import org.semanticweb.vlog4j.core.reasoner.Reasoner;
import org.semanticweb.vlog4j.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.vlog4j.parser.ParsingException;
import org.semanticweb.vlog4j.parser.RuleParser;

import rlsgenerator.NodeTaggingRlsParser;
import rlsgenerator.ShapeGraphRlsParser;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class ConstraintCheckingDemo {
	
	public static void main(String[] args) throws ParsingException, IOException {
		String shaclFilepath = "parsing-example/shapes_graph.ttl";
		String rlsFilepath = "parsing-example/shapes_graph.rls";
		String dataGraph = "parsing-example/data_graph.ttl";
		String nodeTaggingRls = "parsing-example/node_tagging.rls";	
		
		System.out.println("SHAPES INFORMATIONS");	
		ShapeGraphRlsParser.printShapesInformations(shaclFilepath);
		ShapeGraphRlsParser.generateRlsFile(shaclFilepath, rlsFilepath);		
		NodeTaggingRlsParser.generateRlsFiles(dataGraph, nodeTaggingRls);
		
		Graph shapesGraph = RDFDataMgr.loadGraph(dataGraph);
		writeGraphToNT(shapesGraph, "data/data_graph.nt");	
		

		final KnowledgeBase kb = RuleParser.parse(new FileInputStream("src/main/resources/parsing-example/predefined.rls"));
		RuleParser.parseInto(kb, new FileInputStream("src/main/resources/" + rlsFilepath));
		RuleParser.parseInto(kb, new FileInputStream("src/main/resources/" + nodeTaggingRls));
		RuleParser.parseInto(kb, "@source dataGraphHas[3] : load-rdf('src/main/resources/data/data_graph.nt') .");
		
		try (final Reasoner reasoner = new VLogReasoner(kb)) {
			// reasoner.setLogLevel(LogLevel.INFO); // < uncomment for reasoning details
			reasoner.reason();

			PositiveLiteral query;
			
			query = RuleParser.parsePositiveLiteral("dataGraphSatisfiesShapeGraph(?value)");
			System.out.println("\nDoes Data Graph Satifies Shapes Graph?");
			try (final QueryResultIterator answers = reasoner.answerQuery(query, true)) {
				answers.forEachRemaining(answer -> System.out.println(answer.getTerms().get(0)));
			}
		}
	}
	
	public static void writeGraphToNT(Graph graph, String filepath) {
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
