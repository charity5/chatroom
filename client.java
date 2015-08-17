
import java.util.*;
import java.io.*;
import java.net.*;



public class client {
	public static ArrayList<userPortNum> allUser = new ArrayList<userPortNum>();

	public static void main(String[] args) {
		int PortNumber;
		client.AddShutdownHookSample shutDown = new client.AddShutdownHookSample();        // exit using control +c
																					
		for (PortNumber = 1024; PortNumber < 8090; PortNumber++) {                         // for each client generate a new port number																	
			try {
				ServerSocket ListenSocket = new ServerSocket(PortNumber);
				boolean judge = true;                                                     
				//String hostName = "127.0.0.1";
				String hostName=args[0];                                               //input IP address
				//int serverPort = Integer.parseInt("8090");
				int serverPort=Integer.parseInt(args[1]);                              //input server port number
				String UserName = null;
				while (judge) {   //when judge==false, jump out of user name password while loop
					shutDown.attachShutDownHook();
					System.out.println("username:");
					BufferedReader br = new BufferedReader(
							new InputStreamReader(System.in));
					String userName = br.readLine();
					System.out.println("password:");
					String password = br.readLine();
					
					Socket clientSocket = new Socket(hostName, serverPort);
					PrintWriter out = new PrintWriter(
							clientSocket.getOutputStream());
					BufferedReader in = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));

					out.println("userName " + userName + " password "
							+ password + " " + PortNumber);
					out.flush();
					
					String receiveWord = in.readLine();
					String[] splitWord = null;
					splitWord = receiveWord.split(" ");
					if (!splitWord[0].equals("fail!")) {
						judge = false;
						UserName = userName;
					}
					
					System.out.print(receiveWord+"\n");
					
					clientSocket.close();
				}// end of while judge loop

				client.heartbeatsend beat = new client.heartbeatsend(hostName,          //heart beat thread
						UserName, serverPort);				
				if (!judge) {
					client.ClientThread listenThread = new client.ClientThread(
							ListenSocket);
					listenThread.start();
					beat.start();
				}// end of if judge loop
				
				while (!judge) {                  //star chat while loop                                                                    
					BufferedReader br = new BufferedReader(
							new InputStreamReader(System.in));
					String chatWord = br.readLine();
					Boolean privateCheack = false;
					String[] splitWord = chatWord.split(" ");
					if (splitWord[0].equals("private")) {        //implement command "private"
						if(splitWord.length<3){
							System.out
							.println("input format uncorrect ");
					         continue;
						}
						
						
						privateCheack = true;
						String toUserName = splitWord[1];
						String fromUserName = UserName;
						int length = splitWord[0].length()
								+ splitWord[1].length() + 2;
						String message = chatWord.substring(length);
						String userIP = null;
						int userPortNum = 0;
						Boolean addressGet = false;                
						for (int i = 0; i < allUser.size(); i++) {
							if (allUser.get(i).userName.equals(toUserName)) {
								addressGet = true;
								userIP = allUser.get(i).IP;
								userPortNum = allUser.get(i).portNmu;
							}
						}
						if (!addressGet) {
							System.out
									.println("Don't have IP or port number of "
											+ toUserName
											+ ". Use command-getaddress first.");
							continue;
						}
						Socket tempSocket = new Socket(userIP, userPortNum);
						PrintWriter tempOut = new PrintWriter(
								tempSocket.getOutputStream());
						if (addressGet) {
							tempOut.println("[private message] " + fromUserName
									+ ": " + message);
							tempOut.flush();
							tempSocket.close();
							System.out.println("[private message] "+fromUserName
									+ ":" + message);
							continue;
						}
						continue;
					}

					if (!privateCheack) {
						Socket clientSocket = new Socket(hostName, serverPort);
						PrintWriter out = new PrintWriter(
								clientSocket.getOutputStream());
						out.println(chatWord + " " + UserName);
						out.flush();
						clientSocket.close();
					}
				}// end of while !judge loop;
				ListenSocket.close();
				beat.RequestStop();
			} catch (BindException e) {
				continue;
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}// end of for loop
	}// end of main

	public static class ClientThread extends Thread {            //listen thread
		public ServerSocket socket;
		public String portNumStr;
		public BufferedReader in;
		public PrintWriter out;
		public ClientThread(ServerSocket listensocket) {
			this.socket = listensocket;
		}
		public void run() {
			try {
				while (true) {
					Socket Threadlistensocket = this.socket.accept();
					BufferedReader in = new BufferedReader(
							new InputStreamReader(
									Threadlistensocket.getInputStream()));
					String receiveWord = in.readLine();
					String[] splitWord = receiveWord.split("%");
					String command = splitWord[0];
					if (command.equals("#logout")) {                    //command logout, client response
						System.out.println("Logout successfully!");
						break;
					}
					if (command.equals("#forcelogout")) {               //force logout by replicated login
						System.out
								.println("You account have been used in another place.");
						System.out.println("force logout");
						break;
					}
					if (command.equals("#portNumber")) {                //command "getaddress", store portnumber
						String userName = splitWord[1];
						String IP = splitWord[2];
						String portNumStr = splitWord[3];
						userPortNum tempPort = new userPortNum(userName, IP,
								portNumStr);
						allUser.add(tempPort);
						System.out.println(userName
								+ " portNumber and IP store successfully");
					} else {
						System.out.println(receiveWord);
					}
					Threadlistensocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class userPortNum {       //class to store port number
		String userName;
		String IP;
		int portNmu;
		
		public userPortNum(String userName, String IP, String portNumStr) {
			this.userName = userName;
			this.IP = IP;
			this.portNmu = Integer.parseInt(portNumStr);
		}
	}

	public static class heartbeatsend extends Thread {    //class to generate live signal
		public boolean flag;
		public int portNumber;
		public String hostName;
		public String sourcename;

		public heartbeatsend(String hostName, String username, int portNumber) {
			flag = true;
			this.portNumber = portNumber;
			this.hostName = hostName;
			this.sourcename = username;
		}
		public void run() {
			try {
				while (flag) {
					Socket beat = new Socket(hostName, portNumber);
					PrintWriter out = new PrintWriter(beat.getOutputStream());
					out.println("LIVE " + this.sourcename);
					out.flush();
					beat.close();
					sleep(10000);                         //every 10 seconds send live signal
				}
			} catch (Exception e) {
			}
		}
		public void RequestStop() {
			this.flag = false;
		}
	}

	public static class AddShutdownHookSample {            //exit using control +c
		public void attachShutDownHook() {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.println("end!");
				}
			});
			//System.out.println("Program start");
		}
	}
	
}
