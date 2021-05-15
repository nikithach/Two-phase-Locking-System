

public class Operation {

	String operation;
	String dataItem;
	String transId;
	boolean opCompleted;

	public Operation(String operation, String dataItem, String transId) {
		this.operation = operation;
		this.dataItem = dataItem;
		this.transId = transId;
		this.opCompleted = false;


	}
}