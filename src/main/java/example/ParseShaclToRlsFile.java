package example;

import java.io.IOException;

import org.semanticweb.vlog4j.parser.ParsingException;

import parser.NodeTaggingParser;
import parser.ShaclToRlsParser;

public class ParseShaclToRlsFile {
	
	public static void main(String[] args) throws IOException, ParsingException {
		String shaclFilepath = "data/myshapes.ttl";
		String rlsFilepath = "data/outputRules.rls";
		
		System.out.println(NodeTaggingParser.generateRulesString(rlsFilepath));	
		
		
//		ShaclToRlsParser.printShapesInformations(shaclFilepath);	
//		ShaclToRlsParser.generateRlsFiles(shaclFilepath, rlsFilepath);
		
//		System.out.println(ShaclToRlsParser.generateRulesString(shaclFilepath));
//		System.out.println( ShaclToRlsParser.generateGroundRules());
	}
}
