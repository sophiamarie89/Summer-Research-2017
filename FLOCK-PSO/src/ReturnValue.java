/*
to simplify return value of given node on tree to avoid issues
with holding both booleans and doubles
 */

public class ReturnValue {

	private double valueDouble = 0;
	private boolean valueBoolean = false;
	private String valueString = null;

	public ReturnValue(){
		valueDouble = 0;
		valueBoolean = false;
		this.valueString = null;
	}

	public ReturnValue(double valueDouble){
		this.valueDouble = valueDouble;
		valueBoolean = false;
		this.valueString = null;
	}


	public ReturnValue(boolean valueBoolean){
		valueDouble = 0;
		this.valueBoolean = valueBoolean;
		this.valueString = null;
	}

	public ReturnValue(String valueString){
		valueDouble = 0;
		valueBoolean = false;
		this.valueString = valueString;
	}


	public void setReturnVal(double valueDouble) {
		this.valueDouble = valueDouble;		
	}

	public void setReturnVal(boolean valueBoolean) {
		this.valueBoolean = valueBoolean;		
	}

	public void setReturnVal(String valueString) {
		this.valueString = valueString;		
	}

	public double getReturnVal(double doubleFlag) {
		return valueDouble;
	}

	public boolean getReturnVal(boolean booleanFlag) {
		return valueBoolean;
	}

	public String getReturnVal(String stringFlag) {
		return valueString;
	}

}