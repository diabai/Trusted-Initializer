// Ming Hoi Lam, Ibrahim Diabate, Matthew Subido

//call web service to validate account.
function validateAccount() {
	$("#errorLabel").text("");

	var username = document.getElementById("loginUsernameField").value;
	var password = document.getElementById("loginPasswordField").value;
	
	var key = ["username", "password"];
	var value = [username, password];
	
	if (username === "") {
		alert("Username cannot be empty");
	} else if (password === "") {
		alert("Password cannot be empty");
	} else {		
    	$.ajax({
		    type: "POST",
		    url: 'http://localhost:8080/account/validation',
		    data: parseXML(key, value),
		    success: function(data) {
		    	var responseCode;
		    	var responseDescription;
		    	
		   		$(data).find("value").each(function(){
		   			responseCode = $(this).find("responseCode").text();
		   			responseDescription = $(this).find("responseDescription").text();
		   			
		   		}); 
		   		
		   		if (responseCode == 1) {
		   			storeCookie("username", username);
		   			storeCookie("password", password);
		   			window.location.href = "share_request.html";
		   		} else {
		 			$('#errorLabel').html(responseDescription);
		   			
		   		}	
		     },
		    contentType: "text/xml",
		    dataType: "xml",
		});
	}
}

//call web service to register account.
function registerAccount() {
	$("#errorLabel").text("");


	var username = document.getElementById("registrationUsernameField").value;
	var password = document.getElementById("registrationPasswordField").value;
	var confirmPassword = document.getElementById("registrationConfirmPasswordField").value;
	
	if (username === "") {
		alert("Username cannot be empty");
	} else if (password === "") {
		alert("Password cannot be empty");
	} else if (confirmPassword === "") {
			alert("Confirm password cannot be empty");
	} else if (username.length < 6 || username.length > 14) {
		alert("Username has to be between 6 and 14 characters");
	} else if (password.length < 8 || password.length > 16) {
		alert("Password has to be between 8 and 16 characters");
	} else if (password !== confirmPassword) {
		alert("Confirm password has to be the same as password");
	
	} else {
		var key = ["username", "password"];
		var value = [username, password];
	
		$.ajax({
		    type: "POST",
		    url: 'http://localhost:8080/account/registration',
		    data: parseXML(key, value),
		    success: function(data) {
		    	var responseCode;
		    	var responseDescription;
		    	
		   		$(data).find("value").each(function(){
		   			responseCode = $(this).find("responseCode").text();
		   			responseDescription = $(this).find("responseDescription").text();
		   		}); 
		   		
		   		if (responseCode == 1) {
		   			window.location.href = "index.html";
		   		} else {
		   	 		$("#errorLabel").text(responseDescription);
		   		}

		     },
		    contentType: "text/xml",
		    dataType: "xml",
		});
		
	}
}

// Make Rest request to web service to request share.
function requestShare() {
	$("#errorLabel").text("");

	var username = retrieveCookie("username");
	var password = retrieveCookie("password");

	var partnerUsername = document.getElementById("sharePartnerUsernameField").value;
	var field = document.getElementById("shareFieldField").value;
	
	if (username === "") {
		alert("Error with your login session. Please login again");
	} else if (password === "") {
		alert("Error with your login session. Please login again");
	} else if (partnerUsername === "") {
		alert("Partner username cannot be empty");
	} else if (field === "") {
		alert("field cannot be empty");
	} else {
		var key = ["username", "password", "partner", "field"];
		var value = [username, password, partnerUsername, field];
	
		$.ajax({
		    type: "POST",
		    url: 'http://localhost:8080/share/share_request',
		    data: parseXML(key, value),
		    success: function(data) {
		    	var responseCode;
		    	var responseDescription;
		    	var u;
		    	var v;
		    	var w;
		   		$(data).find("value").each(function(){
		   			responseCode = $(this).find("responseCode").text();
		   			responseDescription = $(this).find("responseDescription").text();
		   			u = $(this).find("u").text();
		   			v = $(this).find("v").text();
		   			w = $(this).find("w").text();
		   			
		   		}); 
		   				   		
		   		if (responseCode == 1) { 
					var myPanel = document.getElementById('infoPanel');
		   			while (myPanel.firstChild) {
		   				myPanel.removeChild(myPanel.firstChild);
		   			
		   			}
		   			
					var infoDiv = document.createElement('div');
					infoDiv.id = 'infoDiv';
					var uDiv = document.createElement('div');
					var vDiv = document.createElement('div');
					var wDiv = document.createElement('div');
					var warnDiv = document.createElement('div');
					warnDiv.id = 'warningLabel';
					
					
					infoDiv.innerHTML += responseDescription + "\nHere is your share..."
					uDiv.innerHTML += "u: " + u;
					vDiv.innerHTML += "v: " + v;
					wDiv.innerHTML += "w: " + w;
					warnDiv.innerHTML += "\nPlease write down your share"
						+ "because we don't store your share in our database";
						
					document.getElementById('infoPanel').appendChild(infoDiv);	
					document.getElementById('infoPanel').appendChild(uDiv);
					document.getElementById('infoPanel').appendChild(vDiv);
					document.getElementById('infoPanel').appendChild(wDiv);
					document.getElementById('infoPanel').appendChild(warnDiv);	   			
		   		} else if (responseCode == 2) {
		   			$("#errorLabel").text(responseDescription);
		   			
		   		} else if (responseCode == 3){
		   			var myPanel = document.getElementById('infoPanel');
		   			while (myPanel.firstChild) {
		   				myPanel.removeChild(myPanel.firstChild);
		   			
		   			}
		   			
					var infoDiv = document.createElement('div');
					infoDiv.id = 'infoDiv';
					var uDiv = document.createElement('div');
					var vDiv = document.createElement('div');
					var wDiv = document.createElement('div');
					var warnDiv = document.createElement('div');
					warnDiv.id = 'warningLabel';
					
					infoDiv.innerHTML += responseDescription + "\nHere is your share..."
					uDiv.innerHTML += "u: " + u;
					vDiv.innerHTML += "v: " + v;
					wDiv.innerHTML += "w: " + w;
					warnDiv.innerHTML += "\nPlease write down your share "
						+ "because we don't store your share in our database";
						
					document.getElementById('infoPanel').appendChild(infoDiv);	
					document.getElementById('infoPanel').appendChild(uDiv);
					document.getElementById('infoPanel').appendChild(vDiv);
					document.getElementById('infoPanel').appendChild(wDiv);
					document.getElementById('infoPanel').appendChild(warnDiv);   			
		   		}

		     },
		    contentType: "text/xml",
		    dataType: "xml",
		});
	}
}

// Parsing data to XML.
function parseXML(key, value) {
	var requestMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	
	requestMessage += "<value>";
	for (var i = 0; i < key.length; i++) {
		requestMessage += "<" + key[i] + ">" + value[i] + "</" + key[i] + ">\n";
	
	}
	requestMessage += "</value>";

	return requestMessage;
}

// Store username and password cookie.
function storeCookie(key, value) {
	document.cookie = key + "=" + value + ";";
}


// Retrieve username and password cookie.
function retrieveCookie(key) {
	var value = "";
		
	var decodedCookie = decodeURIComponent(document.cookie);
	var cookieArray = decodedCookie.split('; ');
	
	for (var i = 0; i < cookieArray.length; i++) {
		var cookie = cookieArray[i].split('=');
		
		if (cookie[0] === key) {
			value = cookie[1];		
			break;
		}
	}
	
	return value;
}
