/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

//package for simpler date format
import java.text.SimpleDateFormat;

public class WebWorker implements Runnable
{
private String filename = ""; //create string to store file name
private String code = ""; //create string to store code variable
private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os,"text/html");
      writeContent(os);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header. 
* Modified from original: if "GET" found, split request line to obtain path. 
**/
private void readHTTPRequest(InputStream is)
{

   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));

   while (true) {
      try {
         while (!r.ready()) 
            Thread.sleep(1);

         line = r.readLine();
         System.err.println("Request line: ("+line+")");
	 String[] path = line.split(" "); //create array to store path from request, split request line
         if(path[0].equals("GET"))
            filename = path[1];//store file path in filename String
         if (line.length()==0) 
         break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;

}

/**
* Write the HTTP header lines to the client network connection.
* Modified from original to include the proper HTTP response which depends on whether or not the file is found.
*
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   
   //create new file, see if it exists
   File f = new File("."+filename);
   if (f.exists())
      code = "200 OK";
   else
      code = "404 Not Found";
   //os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write(("HTTP/1.1 "+ code +"\n").getBytes());  //dynamically using code string instead of fixed 200
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* Modified read an html file and output it to the output stream. Replacing tags specified in assignment.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{

   String line;
   //read from the file from GET request
   BufferedReader r;
   //get the current date
   Date today = new Date();
   DateFormat day = new SimpleDateFormat("MM/dd/yyyy");
   
      try {	
         r = new BufferedReader(new FileReader(new File("."+filename)));
         // read line from html file
         while ((line = r.readLine()) != null)
         { 
               //replace date tag with current date
		 if(line.contains("<cs371date>")){
	 		line = line.replaceAll("<cs371date>", day.format(today));
		 }
               //replace server tag with message
        	 if(line.contains("<cs371server>")){
	 		line = line.replaceAll("<cs371server>", "Hello, World! Server");
		 }
        	 os.write(line.getBytes()); //output html line to web server
         
         }//end while
         r.close();
     
      }//end try  
      
      catch (IOException e) {

        //If not found, displays message
      	os.write("<html><head></head><body>".getBytes());
        os.write("<h3>404 Page not found</h3>".getBytes());
        os.write("</body></html>".getBytes());

      }//end catch

     os.close();

}
} // end class
