package pl.edu.agh.kt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseTimeChecker implements Runnable {
	Logger logger = LoggerFactory.getLogger(SdnLabListener.class); 
	private final ServerInstance server;
	
	private static final Random RANDOM = new Random();
	
	public ResponseTimeChecker(ServerInstance server) {
		this.server = server;
	}

	@Override
	public void run() {
		HttpURLConnection connection = null;
	    try {
            	/*
	             URL url = new URL("http://" + server.getIp() + ":" + server.getPort() + "/i_do_not_exist");
	             connection = (HttpURLConnection) url.openConnection();

	             long start = System.currentTimeMillis();
	             String jsonResponse = myInputStreamReader(connection.getInputStream());

	             long finish = System.currentTimeMillis();
	             long totalTime = finish - start;
	             
	             server.setLastResponseTime(totalTime);
	             logger.info("Total Time for page load - " + totalTime);
	             */
            	
            	server.addLastResponseTime(RANDOM.nextInt(50) + 50L);
	         } catch (Exception e) {
	             e.printStackTrace();
	         } finally {
	           //  connection.disconnect();
	         }
		
	}
	

	public static String myInputStreamReader(InputStream in) throws IOException {

         StringBuilder sb = null;
         try {
             InputStreamReader reader = new InputStreamReader(in);
             sb = new StringBuilder();
             int c = reader.read();
             while (c != -1) {
                 sb.append((char) c);
                 c = reader.read();
             }
             reader.close();
             return sb.toString();

         } catch (Exception e) {
             e.printStackTrace();
         } finally {
         }
         return sb.toString();
     }
}
