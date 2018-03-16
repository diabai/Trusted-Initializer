import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player {
	
	public final BigInteger FIELD;
	
	private final int myPort;
	
	private final InetAddress myPartner;
	
	private final int myPartnerPort;
	
	private final BigInteger mySecret;
	
	private BigInteger[] myShares;
	
	private BigInteger myGivenShare;
	
	private BigInteger[] myInitValues;
	
	private BigInteger[] myGivenResults;
	
	private BigInteger myGivenProductShare;
	
	private boolean runServer;
	
	private boolean isAlice;
	
	private SecureRandom myRandom;
	
	public Player(final int thePort, final InetAddress theOtherIP, final int theOtherPort, final BigInteger theSecret,
			final boolean alice, final String theField) {
		myRandom = new SecureRandom();
		myPort = thePort;
		myPartnerPort = theOtherPort;
		myPartner = theOtherIP;
		mySecret = theSecret;
		BigInteger index;
		do {
			index = new BigInteger(mySecret.bitLength(), myRandom);
		} while (index.equals(BigInteger.ZERO) || index.max(mySecret).equals(index));
		System.out.printf("My Values: %s, %s\n", index, mySecret.subtract(index));
		myShares = new BigInteger[] {index, mySecret.subtract(index)};
		myGivenShare = null;
		myGivenResults = new BigInteger[] {null, null};
		myGivenProductShare = null;
		runServer = true;
		isAlice = alice;
		FIELD = new BigInteger(theField);
	}
	
	public BigInteger computeProduct() {
		Thread t = new Thread(new PlayerServer());
		t.start();
		
		requestInitialization();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sendShare(myShares[1], 1);
		
		while (myGivenShare == null) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}; // Wait for response.
		
		System.out.printf("Received Share %s!\n\n", myGivenShare);
		System.out.printf("Calculating shares of D%s and E%s...\n", isAlice ? 'a' : 'b', isAlice ? 'a' : 'b');
		BigInteger[] myResults = new BigInteger[2];
		if (isAlice) {
			myResults[0] = myShares[0].subtract(myInitValues[0]).mod(FIELD);
			myResults[1] = myGivenShare.subtract(myInitValues[1]).mod(FIELD);
		} else {
			myResults[0] = myGivenShare.subtract(myInitValues[0]).mod(FIELD);
			myResults[1] = myShares[0].subtract(myInitValues[1]).mod(FIELD);
		}
		System.out.printf("My shares of D%s, E%s: ", isAlice ? 'a' : 'b', isAlice ? 'a' : 'b');
		System.out.printf("%s, %s!\n\n", myResults[0], myResults[1]);
		sendShare(myResults, 2);
		
		while (myGivenResults[0] == null) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}; // Wait for response.
		
		System.out.printf("Received Shares %s, %s!\n\n", myGivenResults[0], myGivenResults[1]);
		System.out.println("Calculating values of D and E...");
		final BigInteger d = myGivenResults[0].add(myResults[0]).mod(FIELD);
		final BigInteger e = myGivenResults[1].add(myResults[1]).mod(FIELD);
		System.out.printf("Final values: D=%s, E=%s\n", d, e);
		System.out.println("Computing share of final secret...");
		BigInteger productShare;
		if (isAlice) {
			productShare = d.multiply(e).mod(FIELD);
			productShare = productShare.add(myInitValues[0].multiply(e).mod(FIELD));
			productShare = productShare.add(myInitValues[1].multiply(d)).mod(FIELD);
			productShare = productShare.add(myInitValues[2]).mod(FIELD);
		} else {
			productShare = myInitValues[0].multiply(e).mod(FIELD);
			productShare = productShare.add(myInitValues[1].multiply(d)).mod(FIELD);
			productShare = productShare.add(myInitValues[2]).mod(FIELD);
		}
		
		sendShare(productShare, 3);
		
		while (myGivenProductShare == null) {
			try {
				Thread.sleep(1000);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}; // Wait for response.
		
		System.out.printf("Received Product Share %s!\n", myGivenProductShare);
		System.out.println("Computing final secret...");
		
		runServer = false;
		
		return myGivenProductShare.add(productShare).mod(FIELD);
	}
	
	private void requestInitialization() {
		Scanner s = new Scanner(System.in);
		System.out.print("Enter first share: ");
		String u = s.next();
		System.out.print("\nEnter second share: ");
		String v = s.next();
		System.out.print("\nEnter third share: ");
		String w = s.next();
		System.out.println();
		myInitValues = new BigInteger[]{new BigInteger(u), new BigInteger(v), new BigInteger(w)};
		s.close();
	}
	
	private void sendShare(final Object theObject, final int theData) {
		try {
			Socket initSocket = new Socket(myPartner, myPartnerPort);
			ObjectOutputStream oos = new ObjectOutputStream(initSocket.getOutputStream());
			Map<String, Object> map = new HashMap<>();
			map.put("share", theObject);
			map.put("type", theData);
			oos.writeObject(map);
			System.out.printf("Share %s sent!\n", theObject.toString());
			initSocket.close();
		} catch (final IOException e) {
			sendShare(theObject, theData);
		}
	}
	
	public static void main(final String[] theArgs) {
		final int port = Integer.parseInt(theArgs[1]);
		final BigInteger secret = new BigInteger(theArgs[0]);
		InetAddress partner = null;
		final int partnerPort = Integer.parseInt(theArgs[3]);
		try {
			partner = InetAddress.getByName(theArgs[2]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean alice = theArgs[4].equals("A");
		String field = theArgs[5];
		Player p = new Player(port, partner, partnerPort, secret, alice, field);
		System.out.println("Player Initialized!\n\nComputing Product...\n");
		System.out.printf("Final Secret = %s\n", p.computeProduct());
		System.exit(0);
	}
	
	private class PlayerServer implements Runnable {

		@Override
		public void run() {
			try {
				ServerSocket ss = new ServerSocket(myPort);
				Lock myLock = new ReentrantLock();
				do {
					Socket s = ss.accept();
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								while (!myLock.tryLock());
								if (s.getInetAddress().equals(myPartner)) {
									ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
									@SuppressWarnings("unchecked") //TODO Check it with if statement later.
									Map<String, Object> map = (HashMap<String, Object>) ois.readObject();
									if (map != null) {
//										System.out.println(map);
										if ((Integer) map.get("type") == 1) {
											myGivenShare = (BigInteger) map.get("share");
										} else  if ((Integer) map.get("type") == 2) {
											myGivenResults = (BigInteger[]) map.get("share");
										} else if ((Integer) map.get("type") == 3) {
											myGivenProductShare = (BigInteger) map.get("share");
											runServer = false;
										}
									}
								}
								s.close();
								myLock.unlock();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					t.start();
				} while (runServer);
				ss.close();
			} catch (Exception e) {
				e.printStackTrace();
				run();
			}
		}
	}
}
