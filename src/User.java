/* 
 * class for holding statistics on paymo users
 * currently contains average sent and received amounts
 * and total sent and received amounts
 */

public class User{
	
	private String id;
	private double averageSent=0;
	private double averageReceived=0;
	
	private double totalSent = 0;
	private double totalReceived = 0;
	
	private int numberSent=0;
	private int numberReceived=0;
	
	public User(String id){
		this.id = id;
	}
	
	public void runningAverageSent(double transaction){
		numberSent++;
		totalSent += transaction;
		averageSent = averageSent + (transaction - averageSent)/numberSent;
		
	}
	
	public void runningAverageReceived(double transaction){
		numberReceived++;
		totalReceived += transaction;
		averageReceived = averageReceived + (transaction - averageReceived)/averageReceived;
		
	}
	
	public String getID(){
		return id;
	}
	
	public double getAverageSent(){
		return averageSent;
	}

	public double getAverageReceived(){
		return averageReceived;
	}
	
	public double getTotalSent(){
		return totalSent;
	}
	
	public double getTotalReceived(){
		return totalReceived;
	}
	
	public int getNumSent(){
		return numberSent;
	}
	
	public int getNumReceived(){
		return numberReceived;
	}
}
