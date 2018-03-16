package com.tcss559.trusted_initializer;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is the share request part of the web service.
 * It handle call from the client side for share requesting.
 * @author Ming Hoi Lam, Ibrahim Diabate, Matthew Subido
 * 
 */
@Path("/share")
public class ShareRequest {

	/**
	 * This method take request in XML format and interact with the 
	 * database to finish the share request.
	 * @param username
	 * @param password
	 * @param otherUsername
	 * @param theField
	 * @return XML response
	 * @throws SQLException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@POST
	@Produces(MediaType.TEXT_XML)
	@Consumes(MediaType.TEXT_XML)
	@Path("share_request")
	public String requestShare(String theRequest)
			throws SQLException, ParserConfigurationException, SAXException, IOException {
		String username, password, usernamePartner;
		String responseMessage = "<value>";

		String responseDescription;
		int responseCode;

		BigInteger field = null;
		int verificationCode = 0;

		// Print for debugging
		System.out.println("Share request method called");

		// Convert String to XML
		Document xmlRequest = parseXML(theRequest);

		username = xmlRequest.getElementsByTagName("username").item(0).getTextContent();
		password = xmlRequest.getElementsByTagName("password").item(0).getTextContent();
		usernamePartner = xmlRequest.getElementsByTagName("partner").item(0).getTextContent();
		String fieldString = xmlRequest.getElementsByTagName("field").item(0).getTextContent();

		// Check to make sure that xml object has username and password.
		if (username.equals("") || password.equals("")) {
			responseCode = 2;
			responseDescription = "Missing username or password";
			username = "NULL";
		} else if (usernamePartner.equals("")) {
			responseCode = 2;
			responseDescription = "Missing partner's username";
		} else if (fieldString.equals("")) {
			responseCode = 2;
			responseDescription = "Missing field";
		} else if (username.equals(usernamePartner)) {
			responseCode = 2;
			responseDescription = "You cannot request share to yourself";
			
		} else {
			field = new BigInteger(fieldString);

			// Communicate with database to verify username and password.
			verificationCode = DBConnection.verifyUser(username, password);

			switch (verificationCode) {
			case 1:
				responseCode = 1;
			case 2:
				responseCode = 2;
				responseDescription = "Account doesn't exist";
				break;
			case 3:
				responseCode = 2;
				responseDescription = "Incorrect password";
				break;
			case 4:
				responseCode = 2;
				responseDescription = "Fail to verify due to system error";
				break;
			default:
				responseCode = 2;
				responseDescription = "Fail to verify due to system error";
				break;
			}
		}

		// If user is verified, continue.
		if (verificationCode == 1) {
			int isPartnerExist = DBConnection.isPresent(usernamePartner);
			
			// If partner username exists in the database, continue.
			if (isPartnerExist == 1) {
				
				// If the database already has a stored share for this partner, return the share to the partner
				if (DBConnection.hasStoredShares(username, usernamePartner)) {
					responseCode = 3;
					BigInteger[] ints = DBConnection.getShares(username, usernamePartner);
					responseMessage += "<responseCode>" + responseCode + "</responseCode>"
							+ "<responseDescription>Share already exists between you and your partner\nHere is your share</responseDescription>";
					responseMessage += "<u>" + ints[0].toString() + "</u>" + "<v>" + ints[1].toString() + "</v>" + "<w>"
							+ ints[2].toString() + "</w>";
					System.out.printf("U=%s, V=%s, W=%s\n\n", ints[0].toString(), ints[1].toString(),
							ints[2].toString());
					
					
					DBConnection.removeShares(username,  usernamePartner);
				// If the partner has already made a share request, show error.
				} else if (DBConnection.hasStoredShares(usernamePartner, username)) {
					responseCode = 2;
					responseMessage += "<responseCode>" + responseCode + "</responseCode>"
							+ "<responseDescription>Share already exists between you and your partner</responseDescription>";
				} else {
					// Else start computing the share.
					SecureRandom r = new SecureRandom();
					BigInteger u, v, w, ua, ub, va, vb, wa, wb;
					do {
						u = nextBigInteger(field, r);
					} while (u.min(field.divide(BigInteger.TEN)).equals(u));
					do {
						v = nextBigInteger(field, r);
					} while (v.min(field.divide(BigInteger.TEN)).equals(v));
					w = u.multiply(v).mod(field);
					do {
						ua = nextBigInteger(u, r);
						ub = u.subtract(ua).mod(field);
					} while (ua.min(field.divide(BigInteger.TEN)).equals(ua));
					do {
						va = nextBigInteger(v, r);
						vb = v.subtract(va).mod(field);
					} while (va.min(field.divide(BigInteger.TEN)).equals(va));
					do {
						wa = nextBigInteger(w, r);
						wb = w.subtract(wa).mod(field);
					} while (wa.min(field.divide(BigInteger.TEN)).equals(wa));
					BigInteger[] reply = new BigInteger[] { ua, va, wa };
					BigInteger[] store = new BigInteger[] { ub, vb, wb };
					System.out.printf("U=%s, V=%s, W=%s\n\n", reply[0], reply[1], reply[2]);
					System.out.printf("U=%s, V=%s, W=%s\n\n", store[0], store[1], store[2]);

					String result = DBConnection.storeShares(usernamePartner, username, store);
					if (result.startsWith("Error storing shares: ")) {
						responseCode = 2;
						responseMessage += "<responseCode>" + responseCode + "</responseCode>"
								+ "<responseDescription>Error Storing shares</responseDescription>";
					} else {
						responseCode = 1;
						responseMessage += "<responseCode>" + responseCode + "</responseCode>"
								+ "<responseDescription>Successfully request share</responseDescription>";
						responseMessage += "<u>" + reply[0].toString() + "</u>" + "<v>" + reply[1].toString() + "</v>"
								+ "<w>" + reply[2].toString() + "</w>";
					}
				}
			} else if (isPartnerExist == 2) {
				responseCode = 2;
				responseMessage += "<responseCode>" + responseCode + "</responseCode>"
						+ "<responseDescription>Share already exists for this user</responseDescription>";
			} else {
				responseCode = 2;
				responseMessage += "<responseCode>" + responseCode + "</responseCode>" + "<responseDescription>"
						+ "Error" + "</responseDescription>";
			}
		} else {
			responseMessage += "<responseCode>" + responseCode + "</responseCode>" + "<responseDescription>"
					+ responseDescription + "</responseDescription>";
		}

		responseMessage += "<username>" + username + "</username>";
		responseMessage += "</value>";
		// Return response in XML type.
		return responseMessage;
	}

	/**
	 * Computing next BigInteger.
	 * @param theField
	 * @param theRandom
	 * @return BigInteger result.
	 */
	private static BigInteger nextBigInteger(final BigInteger theField, final Random theRandom) {
		BigInteger result;
		do {
			result = new BigInteger(theField.bitLength(), theRandom);
		} while (result.equals(BigInteger.ZERO) || result.max(theField).equals(result));
		return result;
	}

	/**
	 * Parse the string request to xml request
	 * @param theRequest
	 * @return XML request
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private Document parseXML(String theRequest) throws ParserConfigurationException, SAXException, IOException {
		// Parsing String to XML for better data handling.
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		//
		InputSource source = new InputSource(new StringReader(theRequest));
		return builder.parse(source);
	}
}