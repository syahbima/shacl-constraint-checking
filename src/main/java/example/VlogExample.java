package example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.vlog4j.core.model.api.PositiveLiteral;
import org.semanticweb.vlog4j.core.reasoner.KnowledgeBase;
import org.semanticweb.vlog4j.core.reasoner.QueryResultIterator;
import org.semanticweb.vlog4j.core.reasoner.Reasoner;
import org.semanticweb.vlog4j.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.vlog4j.parser.ParsingException;
import org.semanticweb.vlog4j.parser.RuleParser;

public class VlogExample {
	public static void main(String[] args) throws ParsingException, IOException {
		String shaclFilepath = "data/myshapes.ttl";
		String rlsFilepath = "vlog-example/example.rls";
		String dataGraphPath = "data/data_example.ttl";
		
		Graph dataGraph = RDFDataMgr.loadGraph(dataGraphPath);
		writeGraphToNT(dataGraph, "data/data_graph.nt");	
		
		final KnowledgeBase kb = RuleParser.parse(new FileInputStream("src/main/resources/" + rlsFilepath));
//		RuleParser.parseInto(kb, "@source data[3] : load-rdf('src/main/resources/vlog-example/data_graph.nt') .");
		
		try (final Reasoner reasoner = new VLogReasoner(kb)) {
			// reasoner.setLogLevel(LogLevel.INFO); // < uncomment for reasoning details
			reasoner.reason();

			PositiveLiteral query;

			// Run the
			query = RuleParser.parsePositiveLiteral("myRules(?node1,node2)");
			System.out.println("\nNode which are valid for all shape:");
			try (final QueryResultIterator answers = reasoner.answerQuery(query, true)) {
				answers.forEachRemaining(answer -> System.out.println(answer.getTerms().get(0) + "," + answer.getTerms().get(1)));
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
