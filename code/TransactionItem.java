

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class TransactionItem {
	private String transactionId;
    private int timeStamp;
    private String transactionState;
    HashSet<String> itemsHeld = new HashSet();
    Queue<Operation> waitingOperations = new LinkedList();

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTransactionState() {
        return this.transactionState;
    }

    public void setTransactionState(String transactionState) {
        this.transactionState = transactionState;
    }
}
