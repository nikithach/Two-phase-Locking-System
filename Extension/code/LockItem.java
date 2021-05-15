
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class LockItem {
	private String itemName;
    PriorityQueue<String> readLockTransId = new PriorityQueue();
    private String writeLockTransId;
    private String lockState;
    Queue<Operation> waitingLockTrans = new LinkedList();

    public String getItemName() {
        return this.itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getWriteLockTransId() {
        return this.writeLockTransId;
    }

    public void setWriteLockTransId(String writeLockTransId) {
        this.writeLockTransId = writeLockTransId;
    }

    public String getLockState() {
        return this.lockState;
    }

    public void setLockState(String lockState) {
        this.lockState = lockState;
    }

}
