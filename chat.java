import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class chat {

    // To chose whether to function as a client or as a server.
    public static void main(String[] args) {

        System.out.println("Press 1 for Server and Press 2 for client");
        Scanner sc = new Scanner(System.in);
        int opt = sc.nextInt();
        if (opt == 1) {
            System.out.println("Enter port number to listen");
            sc = new Scanner(System.in);
            String line = sc.nextLine();
            String[] arr = line.split("\\s");
            MultiThreadChatServerSync.main(arr);
        } else if (opt == 2) {
            System.out.println("Enter server address and port number to connect client");
            sc = new Scanner(System.in);
            String line = sc.nextLine();
            String[] arr = line.split("\\s");
            MultiThreadChatClient.main(arr);
        } else {
            System.out.println("Enter valid choice");
        }
    }

}


/*
 * A chat server that delivers private messages between users.
 */
class MultiThreadChatServerSync {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;
    private String serverAdd = null;
    private String serverPort = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 20;
    private static final clientThread[] threads = new clientThread[maxClientsCount];
    private static HashMap<String, ArrayList<String>> Mapper = new HashMap<String, ArrayList<String>>();

    public static boolean checkConnection(String client1, String client2) {
        if (Mapper.containsKey(client1) && Mapper.get(client1).contains(client2) == true) {
            return true;
        }
        return false;
    }

    public static void setConnection(String client1, String client2) {
        if (Mapper.containsKey(client1) == false) {
            Mapper.put(client1, new ArrayList<String>());
        }
        if (Mapper.containsKey(client2) == false) {
            Mapper.put(client2, new ArrayList<String>());
        }
        if (Mapper.get(client1).contains(client2) == false) {
            Mapper.get(client1).add(client2);
        }
        if (Mapper.get(client2).contains(client1) == false) {
            Mapper.get(client2).add(client1);
        }

    }

    public static void remConnection(String client1, String client2) {
        if (Mapper.containsKey(client1) && Mapper.get(client1).contains(client2) == true) {
            Mapper.get(client1).remove(client2);
        }
        if (Mapper.containsKey(client2) && Mapper.get(client2).contains(client1) == true) {
            Mapper.get(client2).remove(client1);
        }
    }

    // Method to return the private IP addresses of the server( all Network Interfaces)
    public static ArrayList<String> getIP() {
        ArrayList<String> ipList = new ArrayList<String>();
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (i.getHostAddress().startsWith("10.") || i.getHostAddress().startsWith("172.") || i.getHostAddress().startsWith("192.")) {
                        ipList.add(i.getHostAddress());
                    }
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(MultiThreadChatServerSync.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ipList;
    }

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 5454;

        if (args == null || args.length < 1) {
            System.out.println("Now using port number=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        /* Open a server socket on the portNumber (default 5454). */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         */
        new serverThread(portNumber + "").start();
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}

class serverThread extends Thread {

    private String port = null;

    public serverThread(String portNumber) {
        port = portNumber;
    }

    public void run() {
        while (true) {
            Scanner sc = new Scanner(System.in);
            String command = sc.nextLine();
            if (command.equals("myip")) {
                ArrayList<String> ipList = MultiThreadChatServerSync.getIP();
                for (String ip : ipList) {
                    System.out.println(ip);
                }
            }
            if (command.equals("myport")) {
                System.out.println(port);
            }
        }
    }

}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room The thread routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class clientThread extends Thread {

    //private String clientName = null;
    private String clientID = null;
    private String clientAddr = null;
    private String clientFull = null;
    private int clientPort = 0;
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {
            /*
       * Create input and output streams for this client.
             */

            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            String name;
            /* Welcome the new the client. */
            clientAddr = clientSocket.getInetAddress().getHostAddress();
            clientPort = clientSocket.getPort();
            clientFull = clientAddr + ":" + clientPort; // get the full address and port
            os.println("Welcome " + clientFull
                    + " to our chat room.\nTo leave enter /exit in a new line.");
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientID = (i + 1) + "";
                        break;
                    }
                }
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        threads[i].os.println("*** A new user " + clientFull
                                + " entered the chat room !!! ***");
                    }
                }
            }
            /* Start the conversation. */
            try {
                while (true) {
                    String line = "";

                    line = is.readLine();
                    if (line.startsWith("exit")) {
                        break;
                    }

                    // help menu
                    if (line.startsWith("help")) {
                        this.os.println("-> On Server:\n"
                                + "*Enter 'myip' and 'myport' to get Server IP Address and Port respectively\n\n"
                                + "->On Client:\n"
                                + "* list : Get all the client's ip addresses and ports\n"
                                + "* connect <Client IP Address> <Client Port> = To connect to a client\n"
                                + "* send <Client ID> <Message> = To send message to a client\n"
                                + "* terminate <Client IP Address>= To terminate connection to a client <Client Port>\n"
                                + "* exit : To disconnect from Server ");
                        continue;
                    }
                    //List all connection associated with ID
                    if (line.startsWith("list")) {
                        this.os.println("ID" + "  " + "IP Address" + "  " + "Port");
                        for (clientThread ct : threads) {
                            if (ct != null && ct.clientFull != null) {
                                this.os.println(ct.clientID + "  " + ct.clientAddr + "  " + ct.clientPort);
                            }
                        }
                        continue;
                    }
                    
                    // Connect to a desired server as a client
                    if (line.startsWith("connect")) {
                        String[] words = line.split("\\s", 3);
                        synchronized (this) {
                            MultiThreadChatServerSync.setConnection(this.clientFull, words[1] + ":" + words[2]);
                            this.os.println("Connected!");
                        }
                        continue;
                    }
                    
                    // not sure if it works
                    if (line.startsWith("terminate")) {
                        String[] words = line.split("\\s", 3);
                        
                        // synchronized the thread
                        synchronized (this) {
                            MultiThreadChatServerSync.remConnection(this.clientFull, words[1] + ":" + words[2]);
                            this.os.println("Terminated!");
                        }
                        continue;
                    }

                    /* Sent message to the given client. */
                    if (line.startsWith("send")) {
                        String[] words = line.split("\\s", 3);
                        if (words.length > 2 && words[2] != null) {
                            words[2] = words[2].trim();
                            if (!words[2].isEmpty()) {
                            		// synchronized the thread to continue
                                synchronized (this) {
                                    for (int i = 0; i < maxClientsCount; i++) {
                                        if (threads[i] != null && threads[i] != this
                                                && MultiThreadChatServerSync.checkConnection(this.clientFull, threads[i].clientFull)
                                                && threads[i].clientFull != null
                                                && threads[i].clientID.equals(words[1])) {
                                            threads[i].os.println("<" + clientFull + "> " + words[2]);
                                            /* Show this message to let the client know the private message was sent. */
                                             
                                            this.os.println("<" + clientFull + "> " + words[2]);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.os.println("Invalid command! type 'help' for more options");
                    }
                }
            } catch (Exception e) {
            }
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientFull != null) {
                        threads[i].os.println("*** The user " + clientFull
                                + " is leaving the chat room !!! ***");
                    }
                }
            }
            // client exits
            os.println("*** Bye " + clientFull + " ***"); 

            
            /* Clean up. Set the current thread variable to null so that a new clien could be accepted by the server. */
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }
     
            /* Close the output stream, close the input stream, close the socket. */
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }
}

///////////////////////////////Client Side /////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////

class MultiThreadChatClient implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;
    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args) {

        // The default port.
        int portNumber = 2222;
        // The default host.
        String host = "localhost";

        if (args == null || args.length < 2) {
            System.out.println("Now using host=" + host + ", portNumber=" + portNumber);
        } else {
            host = args[0];
            portNumber = Integer.valueOf(args[1]).intValue();
        }

        /*
     * Open a socket on a given host and port. Open input and output streams.
         */
        try {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host "
                    + host);
        }

        /*
     * If everything has been initialized then we want to write some data to the
     * socket we have opened a connection to on the port portNumber.
         */
        if (clientSocket != null && os != null && is != null) {
            try {

                /* Create a thread to read from the server. */
                new Thread(new MultiThreadChatClient()).start();
                while (!closed) {
                    os.println(inputLine.readLine().trim());
                }
                /*
         * Close the output stream, close the input stream, close the socket.
                 */
                os.close();
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    /*
   * Create a thread to read from the server. 

     */
    @SuppressWarnings("deprecation")
	public void run() {

        String responseLine = "";
        try {

            while ((responseLine = is.readLine()) != null) {
                System.out.println(responseLine);
                if (responseLine.indexOf("Thank you for using this Chat!") != -1) {
                    break;
                }
            }
            closed = true;
        } catch (Exception e) {
            System.err.println("IOException:  " + e);
        }
    }
}
