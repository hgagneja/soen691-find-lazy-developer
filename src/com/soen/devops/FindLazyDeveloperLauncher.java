package com.soen.devops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;

import com.soen.devops.LazyDeveloperInstance.LazinessType;

public class FindLazyDeveloperLauncher {

	private static final HashSet<LazyDeveloperInstance> nodes = new HashSet<LazyDeveloperInstance>();

	private static final String[] extensions = new String[]{"java"};
	
	public static void main(String[] args) throws IOException, CoreException {
		
		File dir = new File(args[0]);
		
		List<File> javaFiles = (List<File>) FileUtils.listFiles(dir, extensions, true);
		
		for(File src : javaFiles){
			astParse(src);
		}
		
		System.out.println("***********Summary**************");
		for(LazyDeveloperInstance ldi : nodes){
			System.out.println(ldi);
		}
	}
	
	private static String readFileToString(File file) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(file));
 
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
 
		reader.close();
 
		return  fileData.toString();	
	}
	
	private static void astParse(File file) throws IOException{
		
		String fileData = readFileToString(file);
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileData.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
 
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		Map<String,Integer> lineComments = new HashMap<String,Integer>();
		
		for(Comment c : (List<Comment>) cu.getCommentList()){
			c.accept(new ASTVisitor() {
				
				public boolean visit(LineComment cNode){
					int start = cNode.getStartPosition();
					int end = start + cNode.getLength();
					String comment = fileData.substring(start, end);
					Integer pos = cu.getLineNumber(cNode.getStartPosition());
					if(comment.contains("TODO") || comment.contains("FIXME"))
						lineComments.put(file.getName()+"::"+comment, pos);
					return true;
				}
				
				public boolean visit(BlockComment bNode) {
					int start = bNode.getStartPosition();
					int end = start + bNode.getLength();
					String comment = fileData.substring(start, end);
					Integer pos = cu.getLineNumber(bNode.getStartPosition());
					if(comment.contains("TODO") || comment.contains("FIXME"))
						lineComments.put(file.getName()+"::"+comment, pos);
					return true;
				}
				
			});
		}
		
		cu.accept(new ASTVisitor() {
			
			public boolean visit(CatchClause node){
				List<ASTNode> statments =  node.getBody().statements();
				LazinessType lazyType = null;
				
				int start = cu.getLineNumber(node.getStartPosition());
				int end = cu.getLineNumber(node.getStartPosition()+node.getLength());
				
				int lineNo = start;
				
				if(statments.isEmpty()){
					lazyType = LazinessType.EMPTY_CATCH;
				}
				if(node.getException().getType().toString().equals("Exception")){
					boolean flagAbort = false;
					
					for(ASTNode stat : statments){
						if(stat.toString().contains("System.exit")){
							flagAbort = true;
						}
					}
					
					if(flagAbort == true){
						lazyType = LazinessType.OVER_CATCH;
					}
				}
					
					
				for(Entry<String, Integer> e :lineComments.entrySet()){
					if(e.getValue()>start && e.getValue()<end){
						nodes.add(new LazyDeveloperInstance(file.getName(), LazinessType.TODO_CATCH, e.getValue()));
					}
				}
					
				
				if(lazyType!=null){
					nodes.add(new LazyDeveloperInstance(file.getName(), lazyType, cu.getLineNumber(node.getStartPosition())));
				}
				
				return true;
			}
			
		});
		
	}
	
}