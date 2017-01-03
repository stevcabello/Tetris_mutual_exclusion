//Creates a Peer Server 

var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);

var PythonShell = require('python-shell');


io.on('connection', function(socket){
  socket.on('new connection', function(msg){ // save peer in list of peers

	   	var options = {
	  		mode: 'text',
	  		args: [msg]
		};

		PythonShell.run('savepeeraddr.py', options ,function (err,results) {
		  if (err) throw err;
		  console.log('ipaddr :', results);
		});

  });


  socket.on('get peers list', function(msg){ //peer wants to get list of peers

   		var res = "";
	   	var options = {
	  		mode: 'text',
	  		args: [msg]
		};

		PythonShell.run('getpeerslist.py', options ,function (err,results) {
		  if (err) throw err;

		  io.emit('server message',msg+"#"+results) //The selectedalgorithm is added to the server message
		});
  });



   socket.on('disconnect peer', function(msg){ //peer disconnects from network
	   	
	   	var options = {
	  		mode: 'text',
	  		args: [msg]
		};

		PythonShell.run('disconnectpeer.py', options ,function (err,results) {
		  if (err) throw err;
		  console.log('peers list :', results);
		});
  });


});



http.listen(3000, function(){
  console.log('listening on *:3000');
});


