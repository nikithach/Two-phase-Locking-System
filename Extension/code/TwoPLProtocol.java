
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.stream.Collectors;

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.Schema;

/*============Execution Steps:==================
Compilation of java files:
	Navigate to the code folder in command prompt
	javac *.java
Execution:
	java TwoPLProtocol  
	Enter the input file path when prompted
	Note: The final input and output files are placed in final input n output folder.*/

public class TwoPLProtocol {

	// Initializing the lock table and transaction table
	static HashMap<String, TransactionItem> transactionTable = new HashMap();
	static HashMap<String, LockItem> lockTable = new HashMap();
	static int timeStamp = 0;
	static String scheme = null;
	static boolean isBlocked = false;

	// Methods for performing read, write, end operations
	public static void readInput(String operation, String tId, String dataItem) {

		switch (operation) {

		case "b":

			// 1.begin operation
			beginTransaction(tId);
			break;

		case "r":

			// 2.read operation
			Operation op = new Operation("Read", dataItem, tId);
			readTransaction(tId, dataItem, op);
			break;

		case "w":

			// 3.write operation
			Operation opr = new Operation("Write", dataItem, tId);
			writeTransaction(tId, dataItem, opr);
			break;

		case "e":

			// 4.end operation
			Operation oper = new Operation("End", dataItem, tId);
			endTransaction(tId, dataItem, oper);
			break;

		default:

			String printMsg = "Invalid Operation Detected : " + operation;
			System.out.println(printMsg);
		}
	}

	private static void endTransaction(String tId, String dataItem, Operation opr) {
		TransactionItem tr = transactionTable.get(tId);
		String trState = tr.getTransactionState();
		LockItem lck = null;
		if (trState.equals("Active")) {
			// If the transaction is active, Commit the transaction on receiving end
			// operation
			tr.setTransactionState("Commit");
			System.out.println("Transaction T" + tId + " is committed and all the locks are released.");
			HashSet<String> itemLocks = tr.itemsHeld;
			Iterator var17 = itemLocks.iterator();
			// Committing requires releasing the locks
			while (var17.hasNext()) {
				String item = (String) var17.next();
				lck = lockTable.get(item);
				releaseLock(tr, lck);
			}

			return;
		} else {
			// If the transaction is not active, the transaction is blocked (i.e, waiting)
			// on receiving end operation
			if (trState.equals("Blocked")) {
				System.out.println("Transaction T" + tr.getTransactionId() + " is already BLOCKED");
				// Block transaction is called
				blockTransaction(tr, opr);
			} else if (trState.equals("Abort")) {
				System.out.println("Transaction T" + tId + " is already aborted");
				return;
			}

			return;
		}

	}

	private static void writeTransaction(String tId, String dataItem, Operation op) {

		TransactionItem tr = transactionTable.get(tId);
		String trState = tr.getTransactionState();
		LockItem l = null;
		if (!trState.isEmpty() && !(trState == "") && trState.equalsIgnoreCase("Active")) {
			if (lockTable.containsKey(dataItem)) {
				l = lockTable.get(dataItem);
				if (l.getLockState() == null || l.getLockState().isEmpty() || l.getLockState().equalsIgnoreCase("")) {
					// Obtaining write lock
					l.setLockState("Write");
					l.setWriteLockTransId(tId);
					tr.itemsHeld.add(dataItem);
					System.out.println(dataItem + " write locked by T" + tId + ": Lock table record for " + dataItem
							+ " is created with mode W (T" + tId + " holds lock).");
					return;
				} else {
					// The item is already read or write locked so resolve the conflict
					if (l.getLockState().equals("Read")) {
						readWrite(dataItem, tr, l, op);
					} else if (l.getLockState().equals("Write")) {
						if (!l.getWriteLockTransId().equals(tr.getTransactionId())) {
							System.out.println("Write Write conflict between T" + tr.getTransactionId() + " and T"
									+ l.getWriteLockTransId());
							TransactionItem lockTrans = transactionTable.get(l.getWriteLockTransId());
							deadlockPrevention(dataItem, tr, lockTrans, l, op);
						}
					}
				}

			} else {
				// If there is not entry for data item in lock table, create one
				l = new LockItem();
				l.setItemName(dataItem);
				l.setLockState("Read");
				l.readLockTransId.add(tId);
				System.out.println(dataItem + " write locked by T" + tId + ": Lock table record for " + dataItem
						+ " is created with mode W (T" + tId + " holds lock).");
				tr.itemsHeld.add(dataItem);
				lockTable.put(dataItem, l);
				return;
			}

		} else {
			if (!trState.isEmpty() && !(trState == "") && trState.equalsIgnoreCase("Blocked")) {
				System.out.println("Transaction T" + tr.getTransactionId() + " is already BLOCKED");
				if (!lockTable.containsKey(dataItem)) {
					LockItem lck = new LockItem();
					lck.setItemName(dataItem);
					lck.setLockState((String) null);
					lck.setWriteLockTransId((String) null);
					lockTable.put(dataItem, lck);
					System.out.println("Entry for DataItem " + dataItem + " has been made in the lock table");
				}

				blockTransaction(tr, op);
			} else {
				System.out.println("Transaction T" + tId + " is already aborted");
			}

			return;
		}
	}

	private static void beginTransaction(String tId) {
		if (transactionTable.containsKey(tId)) {
			System.out.println("Error: The transaction has already started");
		} else {
			// Adding the transaction entry in the transaction table
			TransactionItem tr = new TransactionItem();
			tr.setTransactionId(tId);
			tr.setTimeStamp(++timeStamp);
			tr.setTransactionState("Active");
			transactionTable.put(tId, tr);
			System.out
					.println("Begin T" + tr.getTransactionId() + ": " + "Record is added to transaction table with Tid="
							+ tr.getTransactionId() + " and TS(T" + tr.getTransactionId() + ")=" + tr.getTimeStamp()
							+ ". T" + tr.getTransactionId() + " state=" + tr.getTransactionState());
		}
	}

	private static void readTransaction(String tId, String dataItem, Operation op) {

		TransactionItem tr = transactionTable.get(tId);
		String trState = tr.getTransactionState();
		LockItem l = null;

		if (!trState.isEmpty() && !(trState == "") && trState.equalsIgnoreCase("Active")) {
			if (lockTable.containsKey(dataItem)) {
				l = lockTable.get(dataItem);
				if (l.getLockState() == null || l.getLockState().isEmpty() || l.getLockState().equalsIgnoreCase("")) {
					// Obtaining read lock
					l.setLockState("Read");
					l.readLockTransId.add(tId);
					tr.itemsHeld.add(dataItem);
					System.out.println(dataItem + " read locked by T" + tId + ": Lock table record for " + dataItem
							+ " is created with mode R (T" + tId + " holds lock).");
					return;
				} else {

					if (l.getLockState().equals("Read")) {
						// The data item is read locked by one or more transactions, this transaction
						// also acquires read lock
						l.readLockTransId.add(tId);
						tr.itemsHeld.add(dataItem);
						String transList = l.readLockTransId.stream().map(Object::toString)
								.collect(Collectors.joining(", T"));
						System.out.println(dataItem + " read locked by T" + tId + ": Lock table record for " + dataItem
								+ " is updated (T" + transList + " hold R lock on " + dataItem + ")");

					} else if (l.getLockState().equals("Write")) {
						// The item is already write locked, resolve the conflict using wound wait
						if (!l.getWriteLockTransId().equals(tr.getTransactionId())) {
							System.out.println("Write Read conflict between T" + tr.getTransactionId() + " and T"
									+ l.getWriteLockTransId());
							TransactionItem lockTrans = transactionTable.get(l.getWriteLockTransId());
							deadlockPrevention(dataItem, tr, lockTrans, l, op);
						}
					}
				}

			} else {
				// If there is not entry for data item in lock table, create one
				l = new LockItem();
				l.setItemName(dataItem);
				l.setLockState("Read");
				l.readLockTransId.add(tId);
				System.out.println(dataItem + " read locked by T" + tId + ": Lock table record for " + dataItem
						+ " is created with mode R (T" + tId + " holds lock).");
				tr.itemsHeld.add(dataItem);
				lockTable.put(dataItem, l);
				return;
			}

		} else {
			if (!trState.isEmpty() && !(trState == "") && tr.getTransactionState().equals("Blocked")) {
				System.out.println("Transaction T" + tr.getTransactionId() + " is already BLOCKED");
				if (!lockTable.containsKey(dataItem)) {
					LockItem lck = new LockItem();
					lck.setItemName(dataItem);
					lck.setLockState((String) null);
					lck.setWriteLockTransId((String) null);
					lockTable.put(dataItem, lck);
					System.out.println("Entry for DataItem " + dataItem + " has been made in the lock table");
				}

				blockTransaction(tr, op);
			} else {
				System.out.println("Transaction T" + tId + " is already aborted");
			}

			return;
		}
	}

	private static void blockTransaction(TransactionItem tr, Operation op) {
		if (!op.operation.equals("End")) {
			LockItem lock = (LockItem) lockTable.get(op.dataItem);
			lock.waitingLockTrans.add(op);
			System.out.println("Transaction T" + tr.getTransactionId()
					+ " is blocked and this operation has been added to waiting list. T" + tr.getTransactionId()
					+ " is waiting");
		} else {
			System.out.println("Transaction T" + tr.getTransactionId()
					+ " is blocked and its 'End' operation has been added to its waiting list. T"
					+ tr.getTransactionId() + " is waiting");
		}

		if (tr.waitingOperations.remove(op)) {
			System.out.println("Operation was already blocked");
		}

		tr.waitingOperations.add(op);

	}

	private static void woundWait(String dataItem, TransactionItem trans, TransactionItem lockTrans, LockItem l,
			Operation op) {
		int ti = trans.getTimeStamp();
		int tj = lockTrans.getTimeStamp();

		// tj is younger transaction
		if (ti < tj) {
			// The younger transaction is aborted i.e, Tj is aborted
			// Tj is aborted and releases locks
			System.out.println("T" + lockTrans.getTransactionId() + " is aborted and releases "
					+ lockTable.get(dataItem).getLockState() + " lock from item " + dataItem);
			lockTrans.setTransactionState("Abort");
			abortTransaction(lockTrans);
			if (op.operation.equals("Read")) {
				l.setWriteLockTransId((String) null);
				l.setLockState("Read");
				l.readLockTransId.add(trans.getTransactionId());
				System.out.println("Transaction T" + trans.getTransactionId()
						+ " has been appended to readlock list for data item " + dataItem
						+ ". So, it has acquired 'Read Lock' on " + dataItem);
			} else {
				l.readLockTransId.poll();
				l.setLockState("Write");
				l.setWriteLockTransId(trans.getTransactionId());
				System.out.println("Transaction T" + trans.getTransactionId()
						+ " has been granted 'Write Lock' for data item " + dataItem);
			}

			trans.itemsHeld.add(dataItem);
		} else {
			// ti is blocked
			System.out.println("T" + ti + " 'BLOCKED'");
			trans.setTransactionState("Blocked");
			blockTransaction(trans, op);
		}
	}

	private static void abortTransaction(TransactionItem ts) {
		HashSet<String> itemsLocked = ts.itemsHeld;
		for (String i : itemsLocked) {
			LockItem l = (LockItem) lockTable.get(i);
			releaseLock(ts, l);
		}

	}

	private static void releaseLock(TransactionItem ts, LockItem l) {
		TransactionItem becameActive;
		if (l.getLockState().equals("Read")) {
			l.readLockTransId.remove(ts.getTransactionId());

			if (l.readLockTransId.size() == 1) {

				l.setLockState((String) null);
			}

			becameActive = grantLock(l);
			if (becameActive != null) {
				startBlockedOps(becameActive);
			}

		} else {
			l.setWriteLockTransId((String) null);
			l.setLockState((String) null);
			becameActive = grantLock(l);
			if (becameActive != null) {
				startBlockedOps(becameActive);
			}
		}

	}

	private static void startBlockedOps(TransactionItem ts) {
		label1: while (true) {
			if (ts.getTransactionState().equals("Active") && !ts.waitingOperations.isEmpty()) {
				Operation op = ts.waitingOperations.poll();
				if (op.opCompleted) {
					continue;
				}
				String dataItem = op.dataItem;
				String tId = ts.getTransactionId();
				LockItem lck;
				if (op.operation.equals("Read")) {
					readTransaction(tId, dataItem, op);
					op.opCompleted = true;
					continue;
				}

				if (op.operation.equals("Write")) {
					lck = lockTable.get(dataItem);
					writeTransaction(tId, dataItem, op);
					op.opCompleted = true;
					continue;

				}

				ts.setTransactionState("Commit");
				System.out.println(
						"Transaction T" + ts.getTransactionId() + " is committed and all the locks are released.");
				HashSet<String> itemLocks = ts.itemsHeld;
				Iterator var5 = itemLocks.iterator();

				while (true) {
					if (!var5.hasNext()) {
						continue label1;
					}

					String item = (String) var5.next();
					lck = lockTable.get(item);
					releaseLock(ts, lck);
				}
			}

			return;

		}

	}

	private static TransactionItem grantLock(LockItem lock) {
		TransactionItem ts = null;

		while (!lock.waitingLockTrans.isEmpty()) {
			Operation op = lock.waitingLockTrans.poll();
			ts = transactionTable.get(op.transId);
			String txnState = ts.getTransactionState();
			if (!txnState.equals("Abort") && !op.opCompleted) {
				ts.setTransactionState("Active");
				System.out.println("Transaction T" + ts.getTransactionId() + " is unblocked");
				if (lock.getLockState() == null) {
					if (op.operation.equals("Read")) {
						lock.setLockState("Read");
						lock.readLockTransId.add(ts.getTransactionId());
						System.out.println("Transaction T" + ts.getTransactionId()
								+ " has been appended to readlock list for data item " + lock.getItemName()
								+ ". So, it has acquired 'Read Lock' on " + lock.getItemName());
						ts.itemsHeld.add(lock.getItemName());
						op.opCompleted = true;
					} else {
						lock.setLockState("Write");
						lock.setWriteLockTransId(ts.getTransactionId());
						System.out.println("Transaction T" + ts.getTransactionId()
								+ " has been granted 'Write Lock' for data item " + lock.getItemName());
						ts.itemsHeld.add(lock.getItemName());
						op.opCompleted = true;
					}
					break;
				}
				readWrite(op.dataItem, ts, lock, op);
				if (!ts.getTransactionState().equals("Blocked") || lock.waitingLockTrans.size() <= 1) {
					if (ts.getTransactionState().equals("Blocked") && lock.waitingLockTrans.size() == 1) {
						ts = null;
					}
					break;
				}
			}
		}

		return ts;
	}

	private static LockItem readWrite(String dataItem, TransactionItem ts, LockItem lock, Operation op) {

		if (!lock.readLockTransId.isEmpty()) {
			String lock_transId;
			TransactionItem lock_trans;

			// read lock has only 1 operation
			if (lock.readLockTransId.size() == 1) {
				lock_transId = (String) lock.readLockTransId.peek();
				lock_trans = transactionTable.get(lock_transId);

				// compare transaction id's in both tables
				if (lock_transId.equals(ts.getTransactionId())) {
					lock.setLockState("Write");
					lock.setWriteLockTransId(ts.getTransactionId());
					lock.readLockTransId.poll();
					System.out.println("Read lock upgraded to write lock for item " + dataItem + " by T"
							+ ts.getTransactionId() + ", lock table updated to mode W");
				} else {
					System.out.println("Read Write conflict between T" + ts.getTransactionId() + "and T"
							+ (String) lock.readLockTransId.peek());
					deadlockPrevention(dataItem, ts, lock_trans, lock, op);
				}
			} else {

				// String transList =
				// lock.readLockTransId.stream().map(Object::toString).filter(id ->
				// !id.equalsIgnoreCase(ts.getTransactionId())).collect(Collectors.joining(",
				// T"));
				String id = ts.getTransactionId();

				System.out.println("Read Write conflict between T" + ts.getTransactionId()
						+ " and multiple Transactions in readlock list");
				// lock_transId = (String)lock.readLockTransId.peek();

				// ---------------
				isBlocked = false;
				if (scheme.equalsIgnoreCase("waitDie")) {
					PriorityQueue<String> holdingTransIds1 = lock.readLockTransId;

					Iterator value = holdingTransIds1.iterator();

					// Displaying the values after iterating through the queue
					// System.out.println("The iterator values are: ");
					while (value.hasNext()) {
						TransactionItem tx = transactionTable.get(value.next());
						if (tx.getTransactionId() != id) {
							deadlockPrevention(dataItem, ts, tx, lockTable.get(dataItem), op);
						}

					}
				} else if (scheme.equalsIgnoreCase("cautiousWaiting")) {
					/*
					 * PriorityQueue<String> holdingTransIds2 = lock.readLockTransId; Iterator
					 * value1 = holdingTransIds2.iterator(); while (value1.hasNext()) {
					 * TransactionItem tx = transactionTable.get(value1.next());
					 * if(tx.getTransactionId() != id) { deadlockPrevention(dataItem, ts, tx,
					 * lockTable.get(dataItem), op); }
					 * 
					 * }
					 */
					deadlockPrevention(dataItem, ts, null, lockTable.get(dataItem), op);

				} else {

					lock_transId = (String) lock.readLockTransId.peek();
					if (Integer.parseInt(ts.getTransactionId()) <= Integer.parseInt(lock_transId)) {
						if (Integer.parseInt(ts.getTransactionId()) == Integer.parseInt(lock_transId)) {
							lock.readLockTransId.poll();
						}

						System.out.println("Lock will be upgraded to 'Write Lock' from 'Read Lock' for data item "
								+ dataItem + " on transaction id - " + ts.getTransactionId());

						while (!lock.readLockTransId.isEmpty()) {
							lock_transId = (String) lock.readLockTransId.peek();
							lock_trans = transactionTable.get(lock_transId);
							woundWait(dataItem, ts, lock_trans, lock, op);
						}
					}
				}
				// List<TransactionItem> txList = new ArrayList<TransactionItem>
				/*
				 * for(int j=0;j<holdingTransIds1.size();j++) { TransactionItem tx =
				 * transactionTable.get(); if(tx.getTransactionId() != id) {
				 * deadlockPrevention(dataItem, ts, tx, lockTable.get(dataItem), op); }
				 * 
				 * }
				 */

			}
			/*
			 * else { lock_trans = transactionTable.get(lock_transId);
			 * deadlockPrevention(dataItem, ts, lock_trans, lock, op); }
			 */ }

		return lock;
	}

	private static void deadlockPrevention(String dataItem, TransactionItem tr, TransactionItem lockTrans, LockItem l,
			Operation op) {
		switch (scheme) {

		case "woundWait":
			woundWait(dataItem, tr, lockTrans, l, op);
			break;
		case "waitDie":
			waitDie(dataItem, tr, lockTrans, l, op);
			break;
		case "cautiousWaiting":
			cautiousWaiting(dataItem, tr, lockTrans, l, op);
			break;
		default:
			System.out.println("==========================================wrong Input=====================================");

		}

	}

	private static void cautiousWaiting(String dataItem, TransactionItem tr, TransactionItem lockTrans, LockItem l,
			Operation op) {
		String ti = tr.getTransactionState();
		// String tj = lockTrans.getTransactionState();
		List<String> holdingTransIds = null;

		if (l.getLockState().equalsIgnoreCase("Read")) {
			holdingTransIds = new ArrayList<String>(l.readLockTransId);
		} else if (l.getLockState().equalsIgnoreCase("Write")) {
			holdingTransIds = new ArrayList<String>();
			holdingTransIds.add(l.getWriteLockTransId());
		}

		List<String> status = holdingTransIds.stream().map(x -> transactionTable.get(x).getTransactionState())
				.collect(Collectors.toList());

		if (status.contains("Blocked")) {
			System.out.println("T" + tr.getTransactionId() + " is aborted and releases "
					+ lockTable.get(dataItem).getLockState() + " lock from item " + dataItem);
			tr.setTransactionState("Abort");
			abortTransaction(tr);
		} else {

			isBlocked = true;
			System.out.println("T" + tr.getTransactionId() + " 'BLOCKED'");
			tr.setTransactionState("Blocked");
			blockTransaction(tr, op);

		}
		// boolean isBlocked = false;
		/*
		 * List<String> holdingTransIds = new ArrayList<String>(l.readLockTransId);
		 * 
		 * 
		 * if(l.getLockState().equalsIgnoreCase("Write") && l.getWriteLockTransId() !=
		 * null){ holdingTransIds.add(l.getWriteLockTransId()); }
		 * 
		 * for(String h: holdingTransIds) { String status = null; TransactionItem hTrans
		 * = transactionTable.get(h); if(h!=tr.getTransactionId()) { status =
		 * hTrans.getTransactionState(); if(status == "Blocked") { isAtleastOneBlocked =
		 * true;
		 * 
		 * } } }
		 */
		// If lockTrans id blocked, abort tr else block tr
		/*
		 * if (tj.equalsIgnoreCase("Blocked") || isAtleastOneBlocked ) {
		 * System.out.println("T"+tr.getTransactionId()+" is aborted and releases " +
		 * lockTable.get(dataItem).getLockState() + " lock from item "+dataItem);
		 * tr.setTransactionState("Abort"); abortTransaction(tr); }else { //ti is
		 * blocked
		 * 
		 * if(!isBlocked) { isBlocked = true; System.out.println("T" +
		 * tr.getTransactionId() + " 'BLOCKED'"); tr.setTransactionState("Blocked");
		 * blockTransaction(tr, op); } }
		 */

	}

	private static void waitDie(String dataItem, TransactionItem trans, TransactionItem lockTrans, LockItem l,
			Operation op) {
		int ti = trans.getTimeStamp();
		int tj = lockTrans.getTimeStamp();

		// ti is younger transaction
		if (ti > tj) {
			// The transaction is aborted i.e, ti is aborted
			// T3 is aborted and releases R lock from item Y
			System.out.println("T" + trans.getTransactionId() + " is aborted and releases "
					+ lockTable.get(dataItem).getLockState() + " lock from item " + dataItem);
			trans.setTransactionState("Abort");
			abortTransaction(trans);
			if (op.operation.equals("Read")) {
				l.setWriteLockTransId((String) null);
				l.setLockState("Read");
				l.readLockTransId.add(lockTrans.getTransactionId());
				System.out.println("Transaction T" + lockTrans.getTransactionId()
						+ " has been appended to readlock list for data item " + dataItem
						+ ". So, it has acquired 'Read Lock' on " + dataItem);
			} else {
				l.readLockTransId.poll();
				l.setLockState("Write");
				l.setWriteLockTransId(lockTrans.getTransactionId());
				System.out.println("Transaction T" + lockTrans.getTransactionId()
						+ " has been granted 'Write Lock' for data item " + dataItem);
			}

			lockTrans.itemsHeld.add(dataItem);
		} else { // ti is blocked
			System.out.println("T" + ti + " 'BLOCKED'");
			trans.setTransactionState("Blocked");
			blockTransaction(trans, op);
		}

	}

	public static void main(String[] args) {

		// scanner object for taking input
		Scanner sc = new Scanner(System.in);
		String selectedScheme = null;
		String path = null;
		String line = null;

		// Transaction related variables
		String operation = null;
		String tId = null;
		String dataItem = null;

		// Reading the input file
		System.out.println("Please enter the input transation file path:");

		path = sc.nextLine();

		while (scheme == null) {
			System.out.println(
					"Please choose the deadlock prevention scheme. \n1.Wound-Wait\n2.Wait-Die\n3.Cautious Waiting");
			selectedScheme = sc.next();
			switch (selectedScheme) {
			case "1":
			case "Wound-Wait":
				scheme = "woundWait";
				break;

			case "2":
			case "Wait-Die":
				scheme = "waitDie";
				break;

			case "3":
			case "Cautious Waiting":
				scheme = "cautiousWaiting";
				break;
			default:
				System.out.println("Invalid selection");
			}

		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			try {
				while ((line = br.readLine()) != null) {
					// Remove empty spaces in lines
					line = line.replace(" ", "");
					line = line.trim();
					System.out.println("\n" + line);
					operation = line.substring(0, 1);
					tId = line.substring(1, 2);
					if (line.length() > 3)
						dataItem = line.substring(3, 4);
					System.out.println("TID:" + tId + ",operation:" + operation + ",dataItem:" + dataItem);
					readInput(operation, tId, dataItem);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.out.println("Unable to find the file. Please check the file path");
			e.printStackTrace();
		}

	}

}
