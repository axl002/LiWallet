import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Created by user on 3/29/17.
 * checks for 4th degree connections between payer and recipient
 * if recipient not in 4th degree mark transaction as suspicious
 * add recipient into 1st degree connections of payer
 */
public class FraudProcessHashMap {

    private static HashMap<String, HashSet<String>> userGraph = new HashMap<>();
    private static HashMap<String, HashSet<String>> userGraphBackup = new HashMap<>();
    public static void main(String[] args){

        String batch_payment = args[0];
        String stream_payment = args[1];
        //int maxDistance = args[3];
        System.out.println("Building user graph....");
        buildUserGraph(batch_payment);
        System.out.println("Done");
        System.out.println("Checking for fraud...");
        long startFraud = System.currentTimeMillis();
        try{
            // feature 1 check if 1 degree of separation between payer and
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
        long endFraud = System.currentTimeMillis();
        System.out.println("done" + " " + (endFraud - startFraud));
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
                System.out.println(transaction);
                String payer = elements[1];
                String recipient = elements[2];
                // add payer and recipient ID to graph
                addUserToGraph(payer);
                addUserToGraph(recipient);

                // add connection between payer and recipient
                addEdgeToGraph(payer, recipient);
                //System.out.println(transaction);


            }

        }
        scan.close();
        userGraphBackup = new HashMap<>(userGraph);
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

                boolean isSuspicious = isConnected(userGraph.get(payer),recipient, maxDistance);
                System.out.println(isSuspicious);
                if(isSuspicious){
                    // did not pass fraud
                    //System.out.println(transaction + " " + pathLength);
                    bw.write(String.format("unverified\n"));
                }
                else {
                    // passed fraud
                    bw.write(String.format("trusted\n"));
                }
            }
        }
        bw.close();
        scan.close();
    }

    // loop through each element of set and check for presence of recipient
    // stop once depth exceeds 3
    // if recipient found then not suspicious return false
    // if recipient not found then suspicious return true
    private static boolean isConnected(HashSet<String> connections, String recipient, int depth){
        if(depth>3) return true;
        if(connections.contains(recipient)){
            return false;
        }else{
            for(String key: (String[])connections.toArray()){
                if(isConnected(userGraph.get(key), recipient, depth+1)){
                    return false;
                }
            }
        }
        return true;
    }
    // use dijkstra to compute shortest path between payer and recipient
    // maxDistance is max degrees of separation to search
    // connect payer and recipient
    // returns +infinity if exceeds maximum degrees of separation to search
    // returns +infinity if payer or recipient are not connected
    // payer and recipient may not be in graph add them to graph
//	private static double getLength(String payer, String recipient, int maxDistance){
//		double theLength=Double.POSITIVE_INFINITY;
//		try{
//			DijkstraShortestPath<String,DefaultEdge> pathLength = new DijkstraShortestPath<String, DefaultEdge>(userGraph,payer,recipient,maxDistance);
//			theLength = pathLength.getPathLength();
//			addEdgeToGraph(payer,recipient);
//		}
//		catch (IllegalArgumentException e2){
//			//System.out.println("New edge detected");
//			// no connection between payer and recipient
//			// or either payer or recipient not in graph
//			if (!userGraph.containsVertex(payer)){
//				addUserToGraph(payer);
//			}
//			if (!userGraph.containsVertex(recipient)){
//				addUserToGraph(recipient);
//			}
//			addEdgeToGraph(payer,recipient);
//			//DijkstraShortestPath<String,DefaultEdge> pathLength = new DijkstraShortestPath<String, DefaultEdge>(userGraph,payer,recipient,4);
//			//theLength = Double.POSITIVE_INFINITY;
//		}
//		return theLength;
//	}

    // add a user id to the userGraph
    private static void addUserToGraph(String id){
        if(!userGraph.containsKey(id)){
            userGraph.put(id, new HashSet<String>());
        }
    }

    // add id1 (payer) and id2 (recipient) edge to graph
    private static void addEdgeToGraph(String id1, String id2){
        userGraph.get(id1).add(id2);
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
    private static void resetUserGraph(){
        userGraph = new HashMap<>(userGraphBackup);
    }
}
