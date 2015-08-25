This is a chatroom.


1. Programming design and data structure
This chatroom is implemented using Java, and contains two java files：server.java and client.java. 
The general design is to use Serversocket to listen the data send from other side, and use Socket with corresponding port number to send the data to the other side. The client side also need a thread to implement both listen data from server and send input words to the server simultaneously. One key point is  to close the socket once the one message has been sent and create the socket again when sending another message, in this way, we can achieve non-persistence connection. 

Data structure:
Server side has a class User to store the info of user such as password, port number, IP. It also has two array lists blackList and  offlineMessage to store the corresponding info for each user. On the basic of User class, server side creates two ArrayList allUser and online. allUser array list store the information of all user, and online array list only store the online and offline state of the user.
Client side has a class UserPorNum to store the IP and portNumber of user to implement the private command later. Besides, it has a hearbeastsend class in extend of class thread to send live signal every 30 second.


2. Explanation of source code
In server side, it first creates a ServerSocket serverSeSocket and use accept() to listen the data send from client side. Then it has several parallel if statements to deal with different commands. In each of these if statements, once the server finish dealing with different command, it needs to create a tempSocket with client’s IP and portNumber to send data back to client side, and this tempSocket should be closed as long as the data is sent. So as the ServerSocket, it should be closed every time it finish receiving one message. And all these are included in a while loop to achieve continually listening data from client side. In this way, we can achieve the non-persistence connection.

In client side, the login part is first a separated while loop. The username and password along with client’s port number is first sent to server in one message, and after receiving the successful login information, the client side jump out of this while loop and go into the chatting part. In the chatting part, it first create a thread with SeverSocket to continuously listen the data from server. Also, it has BufferedReader to get the input words from the client and several if statements to implement some command like private. The same as server side, after each listening or sending data, the socket closed, and all these are included in a while loop.

More detailed explanation of source code is noted in the code.


3. Instructions
Run code on terminal:
Put client.java, server.java and makefile in one fold, open it in terminal.
>make

Start server:
>java server 8090
Open new terminal to start client:
>java client 127.0.0.1 8090
Then the chatroom program starts.  
When need to start another client, open new terminal and type
>java client 127.0.0.1 8090

client terminal can be graceful exit using control +c
>^Cend!
>end!

The command formats are just as the assignment required, also shown in sample run.


4. Sample commands
User Authentication:
>username:
>columbia
>password:
>116bway
>login successfully
Fail cases including “input message format incorrect”, “password wrong”,”Username does’t exist”, “You have been blocked”.

message:
client1(columbia) 
>message network hi
>[message] columbia: hi
>[message] network: how are you?
client2(network)  
>[message] columbia: hi
>message columbia how are you?
>[message] network: hi
                  
broadcast:
>broadcast who want to go out?
>[Broadcast] columbia: who want to go out?

online:
>online
>All online users: columbia network

block:
client columbia:
>block network
>You have blocked columbia
client network:
>message columbia hi
>You have be blocked by columbia

getaddress:
>getaddress columbia
>columbia portNumber and IP store successfully

private:
>private columbia this is a private line
>[private message] network: this is a private line
