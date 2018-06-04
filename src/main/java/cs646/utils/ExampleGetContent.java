package cs646.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExampleGetContent {
	
	public static void main( String[] args ) throws Exception {
		
		String pathIndex = "index_robust04/";
		
		LuceneQLSearcher searcher = new LuceneQLSearcher( pathIndex );
		
		String content = searcher.index.document( 0).get( "content" );
		content = content.replaceAll("\n"," ");
		System.out.println("content: "+content);

		String c1 = content.replaceAll("( )+","@");
		System.out.println("c1: "+c1);

		String c2 = c1.replaceAll("@", " ").trim();
		System.out.println("c2: "+c2);



		searcher.close();
		
	}
	
}
