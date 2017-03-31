import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by user on 3/31/17.
 * Bidirectional BFS for detecting 4th degree connections
 * 1. construct 1st degree graph
 * 2. construct 2nd degree graph
 * 3. for given pair of payer and recipient check for at least 1 common element in their graphs
 */
public class BidirectionalFraudHash {
    private static HashMap<String, HashSet<String>> userGraph = new HashMap<>();
    private static HashMap<String, HashSet<String>> userGraphBackup = new HashMap<>();
    private static int maxDistance= 0;
    private static HashSet<String> visited = new HashSet<>();
    public static void main(String[] args){

        String batch_payment = args[0];
        String stream_payment = args[1];
        //int maxDistance = args[3];
        System.out.println("Building user graph....");
        long startBuild = System.currentTimeMillis();
        buildUserGraph(batch_payment);
        System.out.println("Done: building 1st and 2nd degree connection graph took: " + (System.currentTimeMillis()-startBuild));
        System.out.println("Checking for fraud...");
        long startFraud = System.currentTimeMillis();
        try{
            // feature 1 check if 1 degree of separation between payer and
            System.out.println(" test 1");
            maxDistance = 3;
            checkFraud(stream_payment, "paymo_output/output1.txt");
            // feature 2 check if <=2 degree of separation between payer and recipient
//            resetUserGraph();
//            System.out.println(" test 2");
//            maxDistance = 1;
//            checkFraud(stream_payment, "paymo_output/output2.txt");
//            // feature 3 check if <=4 degree of separation between payer and recipient
//            resetUserGraph();
//            System.out.println(" test 3");
//            maxDistance = 3;
//            checkFraud(stream_payment, "paymo_output/output3.txt");
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
//                System.out.println(transaction);
                String payer = elements[1];
                String recipient = elements[2];
                // add payer and recipient ID to graph
                addUserToGraph(payer);
                addUserToGraph(recipient);

                // add connection between payer and recipient
                addEdgeToGraph(payer, recipient);
                addEdgeToGraph(recipient,payer);
                //System.out.println(transaction);
            }
        }
        scan.close();
        System.out.println("Start building 2nd degree connections");
        buildDeg2Graph(); //2nd degree connections graph
        userGraphBackup = new HashMap<>(userGraph);
    }

    // build 2nd degree graph, loop through each neighbor of a user and add neighbors connections to users connections

    private static void buildDeg2Graph(){
        for(String key: userGraph.keySet()){
            HashSet<String> tempSet = new HashSet<>();
            for(String neighbor: userGraph.get(key) ){
                tempSet.addAll(userGraph.get(neighbor));
            }
            userGraph.get(key).addAll(tempSet);
            tempSet.clear();
        }
    }


    // read in stream_payment.csv
    // check if distance between payer and recipient is greater than maxDistance (using dijkstra)
    // write to output the results of fraudcheck
    // ???update graph with new transaction made???
    private static void checkFraud(String filename, String outputName) throws IOException{
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
                visited.clear();
                // reverse the boolean here since we want false intersections to be truly suspicious
                boolean isSuspicious = !checkSuspicious(payer,recipient);
//                System.out.println(isSuspicious);
                if(isSuspicious){
                    // did not pass fraud
//                    System.out.println(transaction);
                    bw.write(String.format("unverified\n"));
                }
                else {
                    // passed fraud
                    bw.write(String.format("trusted\n"));
                }
                // update graph
//                addUserToGraph(payer);
//                addUserToGraph(recipient);
                addEdgeToGraph(payer,recipient);
                addEdgeToGraph(recipient,payer);
            }
        }
        bw.close();
        scan.close();
    }

    private static boolean checkSuspicious(String payer, String recipient){
        // check if payer and recipient are new
        if(!userGraph.containsKey(payer))
            addUserToGraph(payer);
        if(!userGraph.containsKey(recipient))
            addUserToGraph(recipient);

        // check for at least one common element in their connection sets
        return intersection(userGraph.get(payer), userGraph.get(recipient));

    }

    //returns true if at least one element in small is found in big
    private static boolean intersection(Set<String> small, Set<String> big){
        if(small.size()>big.size()) {
            Set<String> temp = small;
            small = big;
            big = temp;
        }
        for(String key: small){
            if(big.contains(key))
                return true;
        }
        return false;
    }

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
