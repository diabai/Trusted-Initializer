package com.tcss559.trusted_initializer;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;

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
 * This class is the account part of the web service. It is responsible for
 * registering and validating account.
 * @author Ming Hoi Lam, Ibrahim Diabate, matthew Subido
 *
 */
@Path("/account")
public class Account {

	/**
	 * This method handle the registration process. It will use DBConnection
	 * to add user account to the database.
	 * @param theRequest
	 * @return XML response
	 * @throws SQLException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	@POST
	@Produces(MediaType.TEXT_XML)
	@Consumes(MediaType.TEXT_XML)
	@Path("registration")
	public String registerAccount(String theRequest) throws Exception {
		String username, password;
		String responseMessage = "<value>";

		String responseDescription;
		int responseCode;

		// Print for debugging
		System.out.println("Registration method called");

		// Convert String to XML
		Document xmlRequest = parseXML(theRequest);

		username = xmlRequest.getElementsByTagName("username").item(0).getTextContent();
		password = xmlRequest.getElementsByTagName("password").item(0).getTextContent();

		// Check to make sure that xml object has username and password.
		if (username.equals("") || password.equals("")) {
			responseCode = 0;
			responseDescription = "Missing username or password";
			username = "NULL";
		} else {
			String result[] = DBConnection.addUser(username, password);
			if (result[0].equals("1")) {
				responseCode = 1;
				responseDescription = result[1];
			} else {
				responseCode = 0;
				responseDescription = result[1];
			}
		}

		// Return response in XML format.
		responseMessage += "<responseCode>" + responseCode + "</responseCode>" + "<responseDescription>"
				+ responseDescription + "</responseDescription>" + "<username>" + username + "</username>";
		responseMessage += "</value>";
		return responseMessage;
	}

	/**
	 * This class is responsible of validating login information, 
	 * which are username and password.
	 * @param theRequest
	 * @return XML response
	 * @throws SQLException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@POST
	@Produces(MediaType.TEXT_XML)
	@Consumes(MediaType.TEXT_XML)
	@Path("validation")
	public String validateLogin(String theRequest)
			throws SQLException, ParserConfigurationException, SAXException, IOException {
		String username, password;
		String responseMessage = "<value>";

		String responseDescription;
		int responseCode;

		// Print for debugging
		System.out.println("Validation method called");

		// Convert String to XML
		Document xmlRequest = parseXML(theRequest);

		username = xmlRequest.getElementsByTagName("username").item(0).getTextContent();
		password = xmlRequest.getElementsByTagName("password").item(0).getTextContent();

		// Check to make sure that xml object has username and password.
		if (username.equals("") || password.equals("")) {
			responseCode = 0;
			responseDescription = "Missing username or password";
			username = "NULL";
		} else {
			// Communicate with database to verify username and password.
			int verificationCode = DBConnection.verifyUser(username, password);

			responseCode = verificationCode;

			switch (verificationCode) {
			case 1:
				responseDescription = "Validated";
				break;
			case 2:
				responseDescription = "Account doesn't exist";
				break;
			case 3:
				responseDescription = "Incorrect password";
				break;
			case 4:
				responseDescription = "Fail to verify due to system error";
				break;
			default:
				responseDescription = "Fail to verify due to system error";
				break;
			}
		}

		// Return response in XML format.
		responseMessage += "<responseCode>" + responseCode + "</responseCode>" + "<responseDescription>"
				+ responseDescription + "</responseDescription>" + "<username>" + username + "</username>";
		responseMessage += "</value>";
		return responseMessage;
	}

	/**
	 * Test method
	 * 
	 * @param name
	 * @return
	 */
	@POST
	@Produces(MediaType.TEXT_XML)
	@Consumes(MediaType.TEXT_XML)
	@Path("/test")
	public String Test(String theString) {
		System.out.println("Test Method Called: " + theString);
		return "Successful";

	}

	/**
	 * This method is responsible for parsing String request into XML response.
	 * @param theRequest
	 * @return request in XML format
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