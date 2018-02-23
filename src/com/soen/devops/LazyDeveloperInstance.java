package com.soen.devops;

public class LazyDeveloperInstance {

	String srcFile;
	
	LazinessType lazyType;
	
	int lineNumber;
	
	public LazyDeveloperInstance(String srcFile, LazinessType lazyType, int lineNumber) {
		this.srcFile = srcFile;
		this.lazyType = lazyType;
		this.lineNumber = lineNumber;
	}

	public enum LazinessType {
		EMPTY_CATCH,
		OVER_CATCH,
		TODO_CATCH;
	}
	
	public String toString(){
		return "Source File : "+
					this.srcFile+
						"; Laziness Type : "+
							this.lazyType+
								"; Line Number : "+
									this.lineNumber;
	}
	
}
