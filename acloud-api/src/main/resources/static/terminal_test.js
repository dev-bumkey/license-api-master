var socket = null;
function setConnected(connected) {
	$("#connect").prop("disabled", connected);
	$("#disconnect").prop("disabled", !connected);
	if (connected) {
		$("#conversation").show();
	} else {
		$("#conversation").hide();
	}
	$("#greetings").html("");
}

function connect() {
	var clusterSeq = $("#clusterSeq").val();
	var namespace = $("#namespace").val();
	var podName = $("#podName").val();
	var containerName = $("#containerName").val();

	//connect to stomp where stomp endpoint is exposed
	socket = new WebSocket("ws://localhost:8080/terminal/"+clusterSeq+"/"+namespace+"/"+podName+"/"+containerName);

    socket.onopen = function(data){
		console.log("onopen : ", data);
	}
    socket.onmessage = function(data){
        console.log("onmessage : \n", data.data);
        var msg = JSON.parse(data.data);
        showTerminal(msg.Data)
    }

    setConnected(true);
}

function disconnect() {
	if (socket != null) {
        socket.close();
	}
	setConnected(false);
	console.log("Disconnected");
}

function sendCommand(keyCode) {
	var jsonData = {
		"Op": "stdin",
		"Data": ""+keyCode
	};
    socket.send(jsonData);
}

function showTerminal(message) {
	//var msg = JSON.parse(message);
	$("#terminal").append("<tr><td> " + message.replace(/(\[\d;\d\d?m|\[m)/g,"").replace(/(\r\n)/gi, "<br>") + "</td></tr>");
}

$(function() {
	$("form").on('submit', function(e) {
		e.preventDefault();
	});
	$("#connect").click(function() {
		connect();
	});
	$("#disconnect").click(function() {
		disconnect();
	});
	$("#command").keydown(function(e){
		sendCommand(e.keyCode);
	});
	$("#send").click(function() {
		alert("Not supported!")
        // sendCommand();
	});
});
