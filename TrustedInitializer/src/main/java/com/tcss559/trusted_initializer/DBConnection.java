package com.tcss559.trusted_initializer;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

/**
 * Establishes connection to the database.
 * 
 * @author: Ming Hoi Lam, Ibrahim Diabate, matthew Subido
 *
 */

public class DBConnection {

	private static String userName = "diabai";
	private static String password = "paparazi7!";
	private static String serverName = "diabaidatabase.cxn0dqrsv9q9.us-east-1.rds.amazonaws.com";
	private static Connection sConnection;

	// Creates once instance of the connection to be reused in the different
	// places in the
	// system.
	private static void createConnection() throws SQLException {
		Properties connectionProps = new Properties();
		connectionProps.put("user", userName);
		connectionProps.put("password", password);
		sConnection = DriverManager.getConnection("jdbc:mysql://" + serverName + "/" + userName + "?user=" + userName
				+ "&password=" + password + "&useSSL=false");

		// For debugging -
		System.out.println("Connected to database...");
	}

	/**
	 * Returns a connection to the database so that queries can be executed.
	 * 
	 * @return Connection to the database
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		if (sConnection == null) {
			System.out.println("GET CONNECTION()");
			createConnection();
		}
		String query = "USE diabai;";
		executeQuery(query);
		return sConnection;
	}

	/**
	 * Close the connection to the database when done.
	 * 
	 * @throws SQLException
	 */
	public static void closeConnection() throws SQLException {
		if (sConnection != null && !sConnection.isClosed()) {
			sConnection.close();
		}
	}

	public static String[] addUser(String username, String password) throws SQLException {
		if (isPresent(username) == 2) { // not present

			String sql = "insert into Clients (mName, mPassword) values " + "(?, ?); ";
			if (sConnection == null) {
				try {
					sConnection = DBConnection.getConnection();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			PreparedStatement preparedStatement = null;
			try {
				preparedStatement = sConnection.prepareStatement(sql);
				preparedStatement.setString(1, username);
				preparedStatement.setString(2, password);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				return new String[] { "0", "Error adding a new client: " + e.getMessage() };
			}
			return new String[] { "1", "Client added successfully" };
		} else { // user name is already present
			return new String[] { "2", "Client already exists in the DB" };
		}
	}

	/**
	 * Checks whether the username and password pair is present in the DB.
	 * 
	 * @param username
	 * @param password
	 * @return true if username and password match else false.
	 * @throws SQLException
	 */
	public static int verifyUser(String username, String password) throws SQLException {

		int statusCode = -1;

		if (sConnection == null) {
			sConnection = getConnection();
		}
		Statement stmt = null;
		String query = "SELECT mName, mPassword " + "FROM Clients WHERE mName = '" + username + "';";

		try {
			stmt = sConnection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				String name = rs.getString("mName");
				String pass = rs.getString("mPassword");
				// Not needed but can't hurt to check
				if (name.equalsIgnoreCase(username) && pass.equals(password)) {
					statusCode = 1; // Verified
				} else if (name.equalsIgnoreCase(username) && (!pass.equals(password))) {
					statusCode = 3; // invalid password
				}
			} else {
				statusCode = 2;
			}
		} catch (SQLException e) {
			statusCode = 4;
			e.printStackTrace();
			System.out.println(e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
		return statusCode;
	}

	/**
	 * 
	 * @param p2
	 * @param p1
	 * @return true if p2 has stored shares with p1 as its partner
	 * @throws SQLException
	 */
	public static boolean hasStoredShares(String p2, String p1) throws SQLException {

		boolean result = false;
		HashMap<String[], BigInteger[]> shares = getSharesData();

		for (String[] arr : shares.keySet()) {
			if ((arr[0].equals(p2)) && (arr[1].equals(p1)) && (shares.get(arr).length > 0)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Gets player 2's user names and shares.
	 * 
	 * @return a HashMap of all usernames keys representing Player2 and their shares
	 *         stored in BigInteger arrays.
	 * @throws SQLException
	 */
	public static HashMap<String[], BigInteger[]> getSharesData() throws SQLException {

		HashMap<String[], BigInteger[]> mMap = new HashMap<>();

		if (sConnection == null) {
			sConnection = DBConnection.getConnection();
		}
		Statement stmt = null;
		String query = "select mName, mPartner, mShare1, mShare2, mShare3 " + "from Shares;";

		try {
			stmt = sConnection.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			BigInteger[] mArray = new BigInteger[3];
			while (rs.next()) {
				String name = rs.getString("mName");
				String partner = rs.getString("mPartner");
				String share1 = rs.getString("mShare1");
				String share2 = rs.getString("mShare2");
				String share3 = rs.getString("mShare3");

				mArray[0] = new BigInteger(share1);
				mArray[1] = new BigInteger(share2);
				mArray[2] = new BigInteger(share3);

				// p2 at first index, p1 at second
				String[] p2AndPartner = { name, partner };
				mMap.put(p2AndPartner, mArray); // Player2's username and his
												// shares
			}
			// Clear the table Shares
			// String clearTableQuery = "TRUNCATE TABLE Shares;";
			// executeQuery(clearTableQuery);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
		return mMap;
	}

	/**
	 * Removes shares from table, user and partner will remain.
	 * 
	 * @param p1
	 * @param p2
	 */

	public static void removeShares(String p1, String p2) {

		if (sConnection == null) {
			try {
				sConnection = DBConnection.getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		String q = "";

		q = "DELETE FROM Shares WHERE mName = '" + p1 + "' AND mPartner = '" + p2 + "';";
		PreparedStatement preparedStatement = null;
		try {

			preparedStatement = sConnection.prepareStatement(q);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Clears all tables of the database
	 * 
	 * @throws SQLException
	 */
	public static void clearAllTables() throws SQLException {

		if (sConnection == null) {
			sConnection = DBConnection.getConnection();
		}

		String[] queries = { "TRUNCATE TABLE Shares", "TRUNCATE TABLE Clients" };

		for (int i = 0; i < queries.length; i++) {
			PreparedStatement preparedStatement = null;
			try {
				preparedStatement = sConnection.prepareStatement(queries[i]);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param p2
	 * @param p1
	 * @return stored shares (p2's shares) if p1 is p2's partner.
	 * @throws SQLException
	 */
	public static BigInteger[] getShares(String p2, String p1) throws SQLException {

		BigInteger[] result = null;
		if (hasStoredShares(p2, p1)) {
			// For each p2, get its username, its partern username and p2's
			// shares
			HashMap<String[], BigInteger[]> mMap = getSharesData();

			for (String[] arr : mMap.keySet()) {
				if (arr[0].equals(p2) && arr[1].equals(p1)) {
					result = mMap.get(arr);
				}
			}

		}
		return result;
	}

	/**
	 * Stores shares for P2 from P1
	 * 
	 * @param p2
	 * @param p1
	 * @param shares
	 * @return
	 */
	public static String storeShares(String p2, String p1, BigInteger[] shares) {

		String sql = "INSERT INTO Shares (mName, mPartner, mShare1, mShare2, mShare3) VALUES " + "(?, ?, ?, ?, ?);";
		if (sConnection == null) {
			try {
				sConnection = DBConnection.getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = sConnection.prepareStatement(sql);
			preparedStatement.setString(1, p2);
			preparedStatement.setString(2, p1);
			preparedStatement.setString(3, shares[0].toString());
			preparedStatement.setString(4, shares[1].toString());
			preparedStatement.setString(5, shares[2].toString());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return "Error storing shares: " + e.getMessage();
		}
		return "Shares stored successfully";
	}

	/**
	 * Helper method to quickly execute a query.
	 * 
	 * @param query
	 */
	private static void executeQuery(String query) {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = sConnection.prepareStatement(query);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks whether a username already exists in the DB. 0 error, 1 present, 2 not
	 * present
	 * 
	 * @param username
	 *            the user's name to check
	 * @throws SQLException
	 */
	public static int isPresent(String username) throws SQLException {

		int statusCode = 0;
		if (sConnection == null) {
			try {
				sConnection = DBConnection.getConnection();
			} catch (SQLException e) {
				System.out.println("Status code " + statusCode + ": " + e);
				e.printStackTrace();
			}
		}

		String q = "SELECT mName FROM Clients WHERE mName = '" + username + "';";
		Statement stmt = null;
		try {
			stmt = sConnection.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			if (rs.next()) {
				statusCode = 1;
			} else {
				statusCode = 2;
			}
		} catch (SQLException e) {

			System.out.println("Status code " + statusCode + ": " + e);
			System.out.println("Stack trace: ");
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}

		return statusCode;
	}

}