package example;

import java.io.IOException;

import org.semanticweb.vlog4j.parser.ParsingException;

import rlsgenerator.NodeTaggingRlsParser;
import rlsgenerator.ShapeGraphRlsParser;

public class ParseShaclToRlsFile {
	
	public static void main(String[] args) throws IOException, ParsingException {
		String shaclFilepath = "parsing-example/shapes_graph.ttl";
		String rlsFilepath = "parsing-example/shapes_graph.rls";
		String dataGraph = "parsing-example/data_graph.ttl";
		String nodeTaggingRls = "parsing-example/node_tagging.rls";	
		
		System.out.println("SHAPES INFORMATIONS");	
		ShapeGraphRlsParser.printShapesInformations(shaclFilepath);
		ShapeGraphRlsParser.generateRlsFile(shaclFilepath, rlsFilepath);
		
		NodeTaggingRlsParser.generateRlsFiles(dataGraph, nodeTaggingRls);
	}
}
