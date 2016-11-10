# LiWallet

Alvin Li
axl002 {dot} ta [at] gmail (dot) com

Submission for Insight Data Engineering coding challenge Digital Wallet

Nov 9 2016


Dependencies:

This program is coded in Java. It requires Java 8 or newer to run (major release 52 or higher).
It uses the jgrapht core library implementation of graphs (http://jgrapht.org/)
The jgrapht-core-1.0.0.jar is included in src folder

run.sh:

Shell script that compiles the program and the runs the program with the parameters "paymo_input/batch_payment.txt" "paymo_input/stream_payment.txt" "1"
The first 2 arguments direct the program to the location of the input data.
The third argument is for demonstrating the transaction statistics tracking ability by printing to console 1 example of a users transaction statistics after completing all tests

src overview:
FraudUserStat.java
This is the fraud detaction program and also contains the main method to run.


The buildUserGraph() method reads in batch_payment.txt.
Each line is parsed into a String array containing (in order) the date, the payer id, the recipient id, the money amount, and the description.
The payer and recipient IDs are used to build a pseudograph where each vertex is the string of a paymo user id and each edge is a 1st degree connection between two users. 
The pseudograph is an undirected graph that allows each vertex to have more than 2 edges to other vertexes. It also allows edges that loops back to the same vertex.
In addition, information about which "payer user" sent what amount of money to which "recipient user" is stored in a list of User objects. 
The list of user objects is stored as a java TreeMap with the key being the sting of the user ID and the value being a User object containing a paymo user's transaction statistics.

The validDate() method provides protection against potentially invalid transaction entries I check if the first column (when delimited by "," character) of each line of transactions contains a valid date object when parsed by the Java simpleDataFormat using the pattern ""yyyy-MM-dd kk:mm:ss".
This means any line that does not match the date format "yyyy-MM-dd kk:mm:ss" exactly will be considered invalid and skipped over.
This input validation occurs for both processing of batch_payment and stream_payment.

The checkFraud() method processes each transaction in stream_payment.txt. 
It calls the getLength() method which uses the jgrapht implementation of dijkstra's algorithm to compute the weighed distance between the graph vertex of the payer in the transaction and the graph vertex of the recipient in the transcation.
According to specifications all edges of the graph have a weight of 1, and thus a distance of 1 represents 1 degree of separation between two users.
A distance of 3 represents 3 degrees of separation between two users, etc.

Feature 1 requires that transaction marked untrusted if the recipient is greater than 1 degree of separation away. 
I restricted dijkstra's algorithm search radius to 1, this means only recipients within 1 distance unit away form the payer will be searched.
In otherwords only recipients that the payer has made payments to in the past will be within the search radius.
If the recipient is not found then the transaction is marked as untrusted.

Feature 1 requires that transaction marked untrusted if the recipient is greater than 2 degrees of separation away. 
I restricted the search radius to 2 for this feature. 
This limits the search to recipients that a payer has directly made payments to in the past as well as the users the recipients of the payer have made prior payments to.
Again if the recipient is not found the transaction is marked as untrusted.

Feature 3 requires that transaction marked untrusted if the recipient is greater than 4 degrees of separation away.
The same approach is used only this time the search radius is set to 4. Only users within 4 degrees of separation from the payer will be searched.
If the recipient is not found the transaction is marked as untrusted.

As part of the general requirements, during processing of stream_payment every transaction that is processed creates a first degree edge between payer and recipient. 
This means if another transaction between the two users occurs further down in stream_payment, it will have a distance of 1 and be marked trusted under all 3 feature requirements.
It also means that more linear payment chains such as "a->b->c->d->e" will become more interconnected if for example a makes a payment to "c".

The output of the program are saved as txt files in paymo_output folder.

User.java
The purpose of this class is to provide a starting point for additional fraud detection methods based on user transaction statistics.
For example one could compute the average and standard deviation of a user's payments and issue a warning when a new payment exceeds the average plus twice the standard deviation.

jgrapht-core-1.0.0.jar
This contains the core implementation of graphs by jgrapht. It is necessary for my program.

FraudProcess.java
This is identical to FraudUserStat.java but does not contain any user transaction statistics tracking. This was the original code I developed.

Currently this object stores:
how much money total a user has sent and received
the average money amount sent and received
the total number of sent transactions and received transactions

To calculate the average a moving average algorithm is used.


insight_testsuite overview:
Under the tests directory, I have added 4 additional tests I used.

test-2-7-degree builds a linear payment chain for 8 users. 
It tests how the degrees of separation change if a user at the end of the chain makes a payment to a user in the middle of the chain.

test-3-invalid-info includes 3 lines of invalid entries among 3 lines of valid transaction data in stream_payment.
It tests how the program handles invalid input- including Strings that are dates but do not match the exact date pattern described above.

test-4-empty-batch contains an empty batch_payment file and tests the building of a user graph data structure from only stream_payment data and how connections change as transactions are processed.

test-5-empty-stream contains an empty stream_payment file and tests if the program produces empty output files for these situations.


Repository Contents:

├──LiWallet
	├── README.md 
	├── run.sh
	├── src
	│  	└── FraudUserStat.java
	│  	└── User.java
	│  	└── jgrapht-core-1.0.0.jar
	│  	└── FraudProcess.java	
	├── paymo_input
	│   └── batch_payment.txt
	|   └── stream_payment.txt
	├── paymo_output
	│   └── output1.txt
	|   └── output2.txt
	|   └── output3.txt
	└── insight_testsuite
	 	   ├── run_tests.sh
		   └── tests
	        	└── test-1-paymo-trans
        		│   ├── paymo_input
        		│   │   └── batch_payment.txt
        		│   │   └── stream_payment.txt
        		│   └── paymo_output
        		│       └── output1.txt
        		│       └── output2.txt
        		│       └── output3.txt
        		└── your-own-test
            		 ├── paymo_input
        		     │   └── batch_payment.txt
        		     │   └── stream_payment.txt
        		     └── paymo_output
        		         └── output1.txt
        		         └── output2.txt
        		         └── output3.txt
        		└── test-2-7-degree
            		 ├── paymo_input
        		     │   └── batch_payment.txt
        		     │   └── stream_payment.txt
        		     └── paymo_output
        		         └── output1.txt
        		         └── output2.txt
        		         └── output3.txt
        		└── test-3-invalid-info
            		 ├── ... same...
        		└── test-4-empty-batch
            		 ├── ...
        		└── test-5-empty-stream
            		 ├── ...
        		└── your-own-test
            		 ├── ...      		         

