
/* requires Java 8 or newer to compile and run (major release 52 or newer)
 * uses jgrapht implementation for graphs http://jgrapht.org/
 * https://github.com/jgrapht/jgrapht
 * jgrapht-core-1.0.0.jar included in src/ folder
 * 
 * read batch_payment.txt and build user list and connections
 * read stream_payment.txt one line at a time and check if 
 * payer and recipient are separated by more than 1,2,or 4 degrees of separation
 * save the value of the transactions made in objects for both payer and recipient

 * output results to output1.txt output2.txt output3.txt
 */

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jgrapht.*;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.*;

import java.lang.String;

public class FraudUserStat {

	// graph of users, and backup graph
	// pseudograph allows for more than 1 edge per vertex
	private static UndirectedGraph<String, DefaultEdge> userGraph = new Pseudograph<String,DefaultEdge>(DefaultEdge.class);
	private static UndirectedGraph<String, DefaultEdge> userGraphBackup = new Pseudograph<String,DefaultEdge>(DefaultEdge.class);


	// User tree for storing other stats of users
	private static TreeMap<String, User> userList = new TreeMap<String, User>();

	// args[0] = "batch_payment.txt" args[1] = "stream_payment.txt"
	public static void main(String[] args){

		String batch_payment = args[0];
		String stream_payment = args[1];
		long startBuild = System.currentTimeMillis();
		System.out.println("Building user graph....");
		buildUserGraph(batch_payment);
		System.out.println("Done");
		long endBuild = System.currentTimeMillis();
		System.out.println("Checking for fraud...");
		long startFraud = System.currentTimeMillis();
		try{
			// feature 1 check if 1 degree of separation between payer and recipient
			System.out.println(" test 1");
			checkFraud(stream_payment, "paymo_output/output1.txt",1);
			// feature 2 check if <=2 degree of separation between payer and recipient
			resetUserGraph();
			System.out.println(" test 2");
			checkFraud(stream_payment, "paymo_output/output2.txt",2);
			// feature 3 check if <=4 degree of separation between payer and recipient
			resetUserGraph();
			System.out.println(" test 3");
			checkFraud(stream_payment, "paymo_output/output3.txt",4);
		}
		catch (IOException e3) {
			e3.printStackTrace();
		}
		// only print out user stats if args has 3rd argument indicating how many to print out

		long endFraud = System.currentTimeMillis();
		if (args.length > 2) {
			printOutUserObjectStats(Integer.parseInt(args[2]));
		}
		System.out.println("done" + " it took: " +(endBuild-startBuild)+ "ms to parse batch_payments and "+ (endFraud - startFraud) + "ms to process stream_payments for all 3 tests");
	}


	// read in batch_payments.csv
	// put user id and transaction recipient id into userGraph data structure
	private static void buildUserGraph(String filename){
		Scanner scan = makeScanner(filename);
		String transaction = "";

		// scan starts after header line, check makeScanner to change this
		while(scan.hasNext()){
			transaction = scan.nextLine();
			String[] elements = transaction.split(",");

			// check if transaction column 0 contains valid date,
			// skip transactions that do not since they are invalid
			// check if payer and recipient are the same
			if(validDate(elements[0])){
				//System.out.println(transaction);
				String payer = elements[1];
				String recipient = elements[2];
				// add payer and recipient ID to graph
				addUserToGraph(payer);
				retreiveUser(payer).runningAverageSent(Double.parseDouble(elements[3]));
				addUserToGraph(recipient);
				retreiveUser(recipient).runningAverageReceived(Double.parseDouble(elements[3]));

				// add connection between payer and recipient
				addEdgeToGraph(payer, recipient);
				//System.out.println(transaction);
			}

		}
		scan.close();
		Graphs.addGraph(userGraphBackup, userGraph);
	}

	// read in stream_payment.csv
	// check if distance between payer and recipient is greater than maxDistance (using dijkstra)
	// write to output the results of fraudcheck
	// ???update graph with new transaction made???
	private static void checkFraud(String filename, String outputName, int maxDistance) throws IOException{
		Scanner scan = makeScanner(filename);
		BufferedWriter bw = makeFileWritter(outputName);
		String transaction = "";

		while(scan.hasNext()){
			transaction = scan.nextLine();
			String[] elements = transaction.split(",");

			// check if first column of transaction is valid date, if not skip that transaction as it is not a real transaction
			if (validDate(elements[0])) {

				String payer = elements[1];
				String recipient = elements[2];
				
				double pathLength = getLength(payer,recipient, maxDistance);
				//System.out.println(pathLength);
				if(pathLength > 4){
					// did not pass fraud
					//System.out.println(transaction + " " + pathLength);
					bw.write(String.format("unverified\n"));
					
					// update payment stats in User object of payer and recipient
					retreiveUser(payer).runningAverageSent(Double.parseDouble(elements[3]));
					retreiveUser(recipient).runningAverageReceived(Double.parseDouble(elements[3]));
				}
				else {
					// passed fraud 
					bw.write(String.format("trusted\n"));
					// update payment stats in User object of payer and recipient					
					retreiveUser(payer).runningAverageSent(Double.parseDouble(elements[3]));
					retreiveUser(recipient).runningAverageReceived(Double.parseDouble(elements[3]));
				}
			}
		}
		bw.close();
		scan.close();
	}

	// use dijkstra to compute shortest path between payer and recipient
	// maxDistance is max degrees of separation to search
	// connect payer and recipient
	// returns +infinity if exceeds maximum degrees of separation to search
	// returns +infinity if payer or recipient are not connected
	// payer and recipient may not be in graph add them to graph
	private static double getLength(String payer, String recipient, int maxDistance){
		double theLength=Double.POSITIVE_INFINITY;
		try{
			DijkstraShortestPath<String,DefaultEdge> pathLength = new DijkstraShortestPath<String, DefaultEdge>(userGraph,payer,recipient,maxDistance);
			theLength = pathLength.getPathLength();
			addEdgeToGraph(payer,recipient);
		}
		catch (IllegalArgumentException e2){
			//System.out.println("New edge detected");
			// no connection between payer and recipient
			// or either payer or recipient not in graph
			if (!userGraph.containsVertex(payer)){
				addUserToGraph(payer);
			}
			if (!userGraph.containsVertex(recipient)){
				addUserToGraph(recipient);
			}
			addEdgeToGraph(payer,recipient);
			//DijkstraShortestPath<String,DefaultEdge> pathLength = new DijkstraShortestPath<String, DefaultEdge>(userGraph,payer,recipient,4);
			//theLength = Double.POSITIVE_INFINITY;
		}
		return theLength;
	}

	// add a user id to the userGraph
	private static void addUserToGraph(String id){
		userGraph.addVertex(id);
		if (!userList.containsKey(id)) {
			userList.put(id, new User(id));
		}
	}


// add id1 (payer) and id2 (recipient) edge to graph
private static void addEdgeToGraph(String id1, String id2){
	userGraph.addEdge(id1,id2);
}

// load a csv and make a scanner to read its contents
private static Scanner makeScanner(String filename){
	Scanner scan = null;
	File input = new File(filename);
	input.getAbsolutePath();
	try {
		scan = new Scanner(new FileReader(input));
	} 
	catch (FileNotFoundException e1) {
		e1.printStackTrace();
	}

	scan.nextLine(); // skip header
	return scan;
}

// check if column 0 of transaction is valid date
// use strict parsing to ensure dates that don't follow pattern are discarded
private static boolean validDate(String date){
	DateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	format.setLenient(false);
	try {
		Date dateObject = format.parse(date);
		return true;
	} catch (ParseException e) {
		//System.out.println("skipping invalid entry " + date);
		return false;
	}
}

// retrieves User object from userList
private static User retreiveUser(String id){
	return userList.floorEntry(id).getValue();
}

// make bufferedWritter for output.txt
private static BufferedWriter makeFileWritter(String filename){
	BufferedWriter bw = null;
	try {
		bw = new BufferedWriter(new FileWriter(filename));
	} 
	catch (IOException e4) {
		e4.printStackTrace();
	}
	return bw;
}

// reset userGraph to the state it was in when batch_payment finished reading in
// to prepare for next test
// this is done by dereferencing the old user graph and assigning the nodes and edges
// from the backup graph to userGraph
private static void resetUserGraph(){
	userGraph = new Pseudograph<String,DefaultEdge>(DefaultEdge.class);
	Graphs.addGraph(userGraph,userGraphBackup);
}


// print out first howMany user object statistics
private static void printOutUserObjectStats(int howMany){
	int foo = 0;
	Set<String> userIDSet = userList.keySet();
	for (String id: userIDSet){
		if (foo < howMany) {
			System.out.println("User: " +id+ " average payment is " +userList.get(id).getAverageSent() + ", total payment is: " + userList.get(id).getTotalSent());
			foo++;
		}
		else {
			return;
		}
	}
}
}
