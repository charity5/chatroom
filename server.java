
import java.util.*;
import java.io.*;
import java.net.*;

public class server {
	public static ArrayList<User> allUser = new ArrayList<User>();          //allUser store all info
	public static ArrayList<User> online = new ArrayList<User>();           //online only store online or offline state of each user

	public static void main(String[] args) throws IOException {
		int portNumber = Integer.parseInt(args[0]);
		//int portNumber = Integer.parseInt("8090");
		ServerSocket serverSeSocket = new ServerSocket(portNumber);
		System.out.println("The server is listening ");
		saveuserpass();
		boolean flag = true;
		while (flag) {
			try {
				Socket serverLiSocket = serverSeSocket.accept();
				InetAddress IPAddress = serverLiSocket.getInetAddress();
				String targetIP = IPAddress.getHostAddress();
				
				int clientPort;
				BufferedReader in = new BufferedReader(new InputStreamReader(
						serverLiSocket.getInputStream()));
				PrintWriter out = new PrintWriter(
						serverLiSocket.getOutputStream());
				String receiveWord = in.readLine();
				String[] splitWord = null;
				splitWord = receiveWord.split(" ");

				if (splitWord[0].equals("userName")                        
						&& (!(splitWord.length == 5))) {
					out.println("fail! Username or password incorrect.");
					out.flush();
					continue;
				}
				if (splitWord[0].equals("userName")
						&& splitWord[2].equals("password")) {              //User authentication
					String userName = splitWord[1];
					String password = splitWord[3];
					int index = searchname(userName);                           //return the index in allUser ArrayList
					if (index == -1) {
						out.println("fail! Username doesn't exsit.");
						out.flush();
					} else {                                                   //if in block period
						Calendar logintime = Calendar.getInstance();
						boolean block = false;
						if (allUser.get(index).blocktime > logintime
								.getTimeInMillis()) {
							block = true;
						}
						Boolean repeatLog = false;
						for (int i = 0; i < online.size(); i++) {              //repeat login, force original logout
							if (online.get(i).userName.equals(userName)) {
								repeatLog = true;
								Socket tempSocket = new Socket(
										online.get(i).IP,
										online.get(i).portNumber);
								PrintWriter tmpOut = new PrintWriter(
										tempSocket.getOutputStream());
								tmpOut.println("#forcelogout");
								tmpOut.flush();
								tempSocket.close();
								online.remove(i);
							}
						}

						if (allUser.get(index).passWord.equals(password)         //login successfully
								&& !block) {
							out.println("login successfully");
							out.flush();							
							
							for (int i = 0; i < online.size(); i++) {              
								Boolean block2 = false;
								int allUserNum = searchname(online.get(i).userName);
								for (int j = 0; j < allUser.get(allUserNum).blockList
										.size(); j++) {
									if (allUser.get(allUserNum).blockList
											.get(j).equals(userName)) {
										block2 = true;
									}
								}
								if (!block2 && !repeatLog) {                         //notify user's online if not blocked
									Socket tempSocket2 = new Socket(
											online.get(i).IP,
											online.get(i).portNumber);
									PrintWriter tmpOut2 = new PrintWriter(
											tempSocket2.getOutputStream());
									tmpOut2.println(userName + " is online");
									tmpOut2.flush();
									tempSocket2.close();
								}
							}

							String portNum = splitWord[4];                            //store login user's info
							clientPort = Integer.parseInt(portNum);
							User newonlineUser = new User(userName, clientPort,
									targetIP, logintime);
							online.add(newonlineUser);                             
							allUser.get(index).portNumber = clientPort;
							allUser.get(index).IP = targetIP;
							allUser.get(index).loginTime = logintime;
							allUser.get(index).trytime = 0;
							User tempUser = allUser.get(index);
      
							for (int i = 0; i < tempUser.offlineMessage.size(); i++) {    //show offline message								
								String[] offWord = tempUser.offlineMessage.get(
										i).split("%");
								String fromUserName = offWord[0];
								String message = offWord[1];								
								Socket tempSocket = new Socket(tempUser.IP,
										tempUser.portNumber);
								PrintWriter tmpOut = new PrintWriter(
										tempSocket.getOutputStream());
								tmpOut.println("[message] " + fromUserName
										+ ": " + message);
								tmpOut.flush();
								tempSocket.close();								
							}
						} else if (block) {                                  //still in block period
							out.println("fail! You have been bloked");
							out.flush();
						} else if (allUser.get(index).trytime == 3) {        //try times > 3
							allUser.get(index).blocktime = logintime
									.getTimeInMillis() + 60000;              // block for 60 seconds
							out.println("fail! You have benn blocked. Please wait for 10 seconds.");
							out.flush();
							allUser.get(index).trytime = 1;
						} else {
							System.out.println("fail");
							out.println("fail! password wrong.");
							out.flush();
							allUser.get(index).trytime++;
						}
					}

				} else if (splitWord[0].equals("LIVE")) {                       //heart beat check
					String fromUserName = splitWord[splitWord.length - 1];
					User fromUser;
					System.out.println(Arrays.toString(splitWord));
					Calendar currentTime = Calendar.getInstance();
					for (int i = 0; i < online.size(); i++) {
						if (online.get(i).userName.equals(fromUserName)) {
							fromUser = online.get(i);
							fromUser.loginTime = currentTime;
							break;
						}
					}
					for (int i = 0; i < online.size(); i++) {
						if (currentTime.getTimeInMillis()
								- online.get(i).loginTime.getTimeInMillis() > 30000) {
							String offUserName = online.get(i).userName;
							online.remove(i);
							for (int k = 0; k < online.size(); k++) {
								Boolean block = false;
								int index = searchname(online.get(k).userName);
								for (int j = 0; j < allUser.get(index).blockList
										.size(); j++) {
									if (allUser.get(index).blockList.get(j)
											.equals(offUserName)) {
										block = true;
									}
								}
								if (!block) {                             //notify one's offline if not blocked
									Socket tempSocket = new Socket(
											online.get(k).IP,
											online.get(k).portNumber);
									PrintWriter tmpOut = new PrintWriter(
											tempSocket.getOutputStream());
									tmpOut.println(offUserName + " is offline");
									tmpOut.flush();
									tempSocket.close();
								}
							}
						}
					}
				} else if (splitWord[0].equals("logout")) {               //command "logout"
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}

					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					if (!(splitWord.length == 2)) {
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					else {
						tmpOut.println("#logout");
						tmpOut.flush();
						tempSocket.close();
						for (int i = 0; i < online.size(); i++) {
							if (online.get(i).userName.equals(fromUserName)) {
								online.remove(i);
							}
						}
						for (int i = 0; i < online.size(); i++) {
							Boolean block = false;
							int allUserNum = searchname(online.get(i).userName);
							for (int j = 0; j < allUser.get(allUserNum).blockList
									.size(); j++) {
								if (allUser.get(allUserNum).blockList.get(j)
										.equals(fromUserName)) {
									block = true;
								}
							}
							if (!block) {                                                 //notify one's logout if not blocked
								Socket tempSocket2 = new Socket(
										online.get(i).IP,
										online.get(i).portNumber);
								PrintWriter tmpOut2 = new PrintWriter(
										tempSocket2.getOutputStream());
								tmpOut2.println(fromUserName + " is offline");
								tmpOut2.flush();
								tempSocket2.close();
							}
						}
						continue;
					}
				}

				else if (splitWord[0].equals("broadcast")) {                  //command "broadcast"
					User fromUser = null;
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName
								.equals(splitWord[splitWord.length - 1])) {
							fromUser = allUser.get(i);
						}
					}
					if (splitWord.length < 3) {
						Socket tempSocket = new Socket(fromUser.IP,
								fromUser.portNumber);
						PrintWriter tmpOut = new PrintWriter(
								tempSocket.getOutputStream());
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}

					String message = splitWord[1];
					for (int i = 2; i < splitWord.length - 1; i++) {
						message += " " + splitWord[i];
					}
					for (int i = 0; i < online.size(); i++) {
						String userNameTmp = online.get(i).userName;
						User userTmp = null;
						for (int k = 0; k < allUser.size(); k++) {
							if (allUser.get(k).userName.equals(userNameTmp)) {
								userTmp = allUser.get(k);
							}
						}
						Boolean block = false;
						for (int j = 0; j < userTmp.blockList.size(); j++) {
							if (userTmp.blockList.get(j).equals(
									fromUser.userName)) {
								block = true;
							}
						}
						if (block) {
							continue;
						}
						int tempPort = userTmp.portNumber;
						Socket tempSocket = new Socket(userTmp.IP, tempPort);
						PrintWriter tmpOut = new PrintWriter(
								tempSocket.getOutputStream());
						tmpOut.println("[Broadcast] "
								+ splitWord[splitWord.length - 1] + ": "
								+ message);
						tmpOut.flush();
						tempSocket.close();
					}
				}

				else if (splitWord[0].equals("message")) {                 //command "message <user> <message> 
					Boolean block = false;
					Boolean onlineflag = false;
					Boolean nameExist = false;
					User toUser = null;
					String toUserName = splitWord[1];
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(toUserName)) {
							toUser = allUser.get(i);
							nameExist = true;
						}
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					if (splitWord.length < 4) {
						Socket tempSocket = new Socket(fromUser.IP,
								fromUser.portNumber);
						PrintWriter tmpOut = new PrintWriter(
								tempSocket.getOutputStream());
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (!nameExist) {
						Socket tempSocket = new Socket(fromUser.IP,
								fromUser.portNumber);
						PrintWriter tmpOut = new PrintWriter(
								tempSocket.getOutputStream());
						tmpOut.println("username to be sent doesn't exist");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (fromUserName.equals(toUserName)) {
						Socket tempSocket = new Socket(fromUser.IP,
								fromUser.portNumber);
						PrintWriter tmpOut = new PrintWriter(
								tempSocket.getOutputStream());
						tmpOut.println("can't send message to yourself");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					for (int i = 0; i < toUser.blockList.size(); i++) {
						System.out.println(toUser.blockList.get(i));
						if (toUser.blockList.get(i).equals(fromUserName)) {
							block = true;
							Socket tempSocket = new Socket(fromUser.IP,
									fromUser.portNumber);
							PrintWriter tmpOut = new PrintWriter(
									tempSocket.getOutputStream());
							tmpOut.println("You have be blocked by "
									+ toUserName);
							tmpOut.flush();
							tempSocket.close();
							continue;
						}
					}
					
					String message = splitWord[2];                        //into send message 
					for (int i = 3; i < splitWord.length - 1; i++) {
						message += " " + splitWord[i];
					}
					for (int i = 0; i < online.size(); i++) {
						if (online.get(i).userName.equals(toUserName)) {
							onlineflag = true;
						}
					}
					if (onlineflag) {
						if (!block) {
							Socket tempSocket = new Socket(toUser.IP,
									toUser.portNumber);
							PrintWriter tmpOut = new PrintWriter(
									tempSocket.getOutputStream());
							tmpOut.println("[message] " + fromUserName + ": "
									+ message);
							tmpOut.flush();
							tempSocket.close();
							Socket tempSocket2 = new Socket(fromUser.IP,
									fromUser.portNumber);
							PrintWriter tmpOut2 = new PrintWriter(
									tempSocket2.getOutputStream());
							tmpOut2.println("[message] " + fromUserName + ": "
									+ message);
							tmpOut2.flush();
							tempSocket2.close();
						}
					} else {                                             //store in offline message
						String offlineMessage = fromUserName + "%" + message;
						toUser.offlineMessage.add(offlineMessage);
						Socket tempSocket2 = new Socket(fromUser.IP,
								fromUser.portNumber);
						PrintWriter tmpOut2 = new PrintWriter(
								tempSocket2.getOutputStream());
						tmpOut2.println(toUserName
								+ " is offline now. Message will be sent when "
								+ toUserName + " is online.");
						tmpOut2.flush();
						tempSocket2.close();
					}
				}

				else if (splitWord[0].equals("block")) {               //command "block <user>"
					Boolean block = false;
					User toUser = null;
					String toUserName = splitWord[1];
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(toUserName)) {
							toUser = allUser.get(i);
							block = true;
						}
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					if (!(splitWord.length == 3)) {
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (!block) {
						tmpOut.println("block username does't exist");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					for (int i = 0; i < fromUser.blockList.size(); i++) {
						if (fromUser.blockList.get(i).equals(toUserName)) {
							tmpOut.println(toUserName + " already be blocked");
							tmpOut.flush();
							tempSocket.close();
							break;
						}
					}
					if (block) {
						tmpOut.println("You have blocked " + toUserName);
						tmpOut.flush();
						fromUser.blockList.add(toUserName);
						tempSocket.close();
					}
				}

				else if (splitWord[0].equals("unblock")) {            //command "unblock <user>"
					Boolean block = false;
					User toUser = null;
					String toUserName = splitWord[1];
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(toUserName)) {
							toUser = allUser.get(i);
							block = true;
						}
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					if (!(splitWord.length == 3)) {
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (!block) {
						tmpOut.println("unblock username does't exist");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					Boolean notBlock = false;
					for (int i = 0; i < fromUser.blockList.size(); i++) {
						if (fromUser.blockList.get(i).equals(toUserName)) {
							notBlock = true;
						}
					}
					if (!notBlock) {
						tmpOut.println(toUserName + " not be blocked yet");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (block) {
						tmpOut.println("You have unblocked " + toUserName);
						tmpOut.flush();
						fromUser.blockList.remove(toUserName);
						tempSocket.close();
					}
				}

				else if (splitWord[0].equals("online")) {                 //command "online"
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					if (!(splitWord.length == 2)) {
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					String onlineList = "All online users: ";
					for (int i = 0; i < online.size(); i++) {
						Boolean block = false;
						for (int j = 0; j < online.get(i).blockList.size(); j++) {
							if (online.get(i).blockList.get(j).equals(
									fromUserName)) {
								block = true;
							}
						}
						if (!block) {
							onlineList += online.get(i).userName + " ";
						}
					}
					tmpOut.println(onlineList);
					tmpOut.flush();
					tempSocket.close();
				}

				else if (splitWord[0].equals("getaddress")) {         //command "getaddress <user>"
					Boolean userExist = false;
					Boolean block = false;
					User toUser = null;
					String toUserName = splitWord[1];
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(toUserName)) {
							toUser = allUser.get(i);
							userExist = true;
						}
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					if (!(splitWord.length == 3)) {
						tmpOut.println("input message format uncorrect");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					if (!userExist) {
						tmpOut.println(toUserName + " doesn't exist");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					Boolean onlineCheak = false;
					for (int i = 0; i < online.size(); i++) {
						if (online.get(i).userName.equals(toUserName)) {
							onlineCheak = true;
						}
					}
					if (!onlineCheak) {
						tmpOut.println(toUserName
								+ " is offline. Don't have available IP and port number now.");
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					for (int i = 0; i < toUser.blockList.size(); i++) {
						if (toUser.blockList.get(i).equals(fromUserName)) {
							block = true;
							continue;
						}
					}
					if (block) {
						tmpOut.println("You have been blocked by " + toUserName);
						tmpOut.flush();
						tempSocket.close();
						continue;
					}
					else {
						String portNumStr = Integer.toString(toUser.portNumber);
						tmpOut.println("#portNumber%" + toUserName + "%"
								+ toUser.IP + "%" + portNumStr + "%");
						tmpOut.flush();
						tempSocket.close();
					}
				}
				else {
					User fromUser = null;
					String fromUserName = splitWord[splitWord.length - 1];
					for (int i = 0; i < allUser.size(); i++) {
						if (allUser.get(i).userName.equals(fromUserName)) {
							fromUser = allUser.get(i);
						}
					}
					Socket tempSocket = new Socket(fromUser.IP,
							fromUser.portNumber);
					PrintWriter tmpOut = new PrintWriter(
							tempSocket.getOutputStream());
					tmpOut.println("input message format uncorrect");
					tmpOut.flush();
					tempSocket.close();
					continue;
				}
				serverLiSocket.close();

			} catch (Exception e) {
				e.printStackTrace();
				// System.err.println(e);
				// continue;
			}
		}
		serverSeSocket.close();
	}

	public static class User {
		public String userName;
		public String passWord;
		public int portNumber;
		public int trytime;
		public String IP;
		public long blocktime;
		public Socket socket;
		public Calendar loginTime;

		public ArrayList<String> blockList = new ArrayList<String>();
		public ArrayList<String> offlineMessage = new ArrayList<String>();

		public User(String username, String password) {
			this.userName = username;
			this.passWord = password;
			this.trytime = 1;
			this.blocktime = 0;
		}
		public User(String username, int portnumber, String IP,
				Calendar logintime) {
			this.userName = username;
			this.portNumber = portnumber;
			this.loginTime = logintime;
			this.IP = IP;
			this.trytime = 1;
		}
		public void setBlocktime(Calendar time) {
			this.blocktime = time.getTimeInMillis();
		}
	}

	public static void saveuserpass() throws FileNotFoundException {       //input use_password information
		File file = new File("/Users/sulin/Desktop/use_pass.txt");
		Scanner input = new Scanner(file);
		while (input.hasNext()) {
			User newUser = new User(input.next(), input.next());
			allUser.add(newUser);
		}
		input.close();
	}

	public static void listUser() throws Exception {
		System.out.println("All saved users:");
		for (int i = 0; i < allUser.size(); i++) {
			System.out.println(allUser.get(i).userName + " ");
		}

	}

	public static int searchname(String username) {
		int index = -1;
		for (int i = 0; i < allUser.size(); i++) {
			if (allUser.get(i).userName.equals(username)) {
				index = i;
				break;
			}
		}
		return index;
	}

}
