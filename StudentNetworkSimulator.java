/* Modified by Keith Downie for CS 4480 PA 2 */
import java.util.ArrayList;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

	
	/* Member variables for side A */
	private Packet currentlySending;
	private int seqNumA;
	private int timeout = 100;
	
	/* Member variables for side A */
	private int corruptPacketsB;
	private int ackNumB;
	
	/* Member variables for side B */
    /* Used to keep and report statistics */
	private int packetsSentA;
	private int packetsResentA;
	private int corruptPacketsA;
	private int packetsLost;
	private double sentTime;
	private ArrayList<Double> RTT;
	private int acksSent;
	private int acksResent;
	
	/* Function to print out the current stats to the console */
	private void printStats() {
		System.out.println("Number of original packets sent from A: " + packetsSentA);
		System.out.println("Number of original acks sent from B: " + acksSent);
		System.out.println("Number of packets resent from A: " + packetsResentA);
		System.out.println("Number of acks resent from B: " + acksResent);
		System.out.println("Total number of packets that were lost: " + packetsLost);
		System.out.println("Total number of packets that were corrupted: " + (corruptPacketsA+corruptPacketsB));
		
		/* Calculate the percentage of packets lost and round then to 4 decimal places */
		double original = (double)(packetsLost)/((packetsSentA+packetsResentA+acksSent+acksResent));
		int factor = 10000;
		int scaled_and_rounded = (int)(original * factor + 0.5);
		double rounded = (double)scaled_and_rounded / factor;
		System.out.println("Percentage of packets lost: " + rounded);
		
		/* Calculate the percentage of packets corrupted and round to 4 decimal places */
		original = (double)(corruptPacketsA+corruptPacketsB)/((packetsSentA+packetsResentA+acksSent+acksResent)-(packetsLost));
		scaled_and_rounded = (int)(original * factor + 0.5);
		rounded = (double)scaled_and_rounded / factor;
		System.out.println("Percentage of packets corrupted: " + rounded);
		
		/* Calculate the average RTT and round to 4 decimal places */
		original = 0;
		for (int i = 0; i < RTT.size(); i++) {
			original += (RTT.get(i)/RTT.size());
		}
		scaled_and_rounded = (int)(original * factor + 0.5);
		rounded = (double)scaled_and_rounded / factor;
		System.out.println("Average Round Trip Time: " + rounded);
		System.out.println(" ");
	}

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	// Used to make sure that nothing is currently sending.
    	// If something is sending, specs say to drop the message.
    	if (currentlySending == null) {
    		// Increment for stat keeping
    		packetsSentA++;
    		
    		// Set up packet variables
    		String payload = message.getData();
    		// Calculate checksum
    		int checksum = seqNumA;
    		for (int i = 0; i < payload.length(); i++) {
    			checksum += payload.charAt(i);
    		}
    		
    		// Create new packet and send it out.
    		currentlySending = new Packet (seqNumA, 0, checksum, payload);
    		toLayer3(A, currentlySending);
    		startTimer(A, timeout);
    		// Set the current time for stat keeping.
    		sentTime = getTime();

    		// If more than one packet has been sent, print out to console.
    		if (packetsSentA > 1) {    			
    			printStats();
    		}

    	}
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
    	// Calculate the checksum to compare against the packet checksum.
    	int checksum = packet.getAcknum() + packet.getSeqnum();
    	String payload = packet.getPayload();
    	for (int i = 0; i < payload.length(); i++) {
    		checksum += payload.charAt(i);
    	}
    	
    	// If they don't match, the packet has been corrupted.
    	if (checksum != packet.getChecksum()) {
    		// Increment for stat keeping.
    		corruptPacketsA++;
    		packetsLost--;
    		// Packet was still received, so add the RTT time.
    		RTT.add(getTime() - sentTime);
    		// Let the timer take care of resending.
    		return;
    	}
    	
    	// Stop the timeout from occuring.
    	stopTimer(A);
    	// Make sure that the ack was acknowledging the correct number. 
    	if (packet.getAcknum() == seqNumA) {
    		// If so, set the new seqNum and null the sending variable.
    		seqNumA = (seqNumA == 1) ? 0 : 1;
    		currentlySending = null;
    		RTT.add(getTime() - sentTime);
    	}
    	else {
    		// If not, a packet was lost. Resend the current packet and start the timer.
    		packetsResentA++;
    		RTT.add(getTime() - sentTime);
    		toLayer3(A, currentlySending);
    		startTimer(A, timeout);
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	// Set up the stat variables
    	sentTime = getTime();
    	packetsResentA++;
    	packetsLost++;
    	// Resend the current packet and start the timer again.
    	toLayer3(A, currentlySending);
    	startTimer(A, timeout);
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	seqNumA = 0;
    	packetsSentA = 0;
    	packetsResentA = 0;
    	corruptPacketsA = 0;
    	packetsLost = 0;
    	RTT = new ArrayList<Double>();
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    	// Calculate the checksum coming from A.
    	int checksum = packet.getAcknum() + packet.getSeqnum();
    	String payload = packet.getPayload();
    	for (int i = 0; i < payload.length(); i++) {
    		checksum += payload.charAt(i);
    	}
    	
    	// An error was thrown when there was no payload to the packet,
    	// so a variable was added in order to have a payload.
    	String m;
    	// If the checksum and seqNum match, the packet is correct.
    	if (checksum == packet.getChecksum() && packet.getSeqnum() != ackNumB) {
    		acksSent++;
    		// Send an ack for the current packet.
    		toLayer5(B, packet.getPayload());
    		// Set B to listen for the next number.
    		ackNumB = packet.getSeqnum();
    		m = "s";
    	}
    	else {
    		// There was a problem.
    		// Check if it was a corruption for stat keeping.
    		if (checksum != packet.getChecksum()) {
    			corruptPacketsB++;
    		}
    		
    		acksResent++;
    		m = "c";
    	}
    	
    	// Send the ack to A
    	Packet p = new Packet(0, ackNumB, ackNumB + m.charAt(0), m);
		toLayer3(B, p);
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
    	ackNumB = 2;

    	acksSent = 0;
    	acksResent = 0;
    	corruptPacketsB = 0;
    }
}
