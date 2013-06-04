import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.jibble.pircbot.*;

public class botMain {
	
	public static void main(String[] args) throws Exception {
		Bot brobot = new Bot();
		
		brobot.setVerbose(true);
		
		try {
			
			brobot.connect("irc.digitalirc.org");
			
		} catch(SocketException se) {
			
			while(brobot.isConnected() == false)
			{
				Thread.sleep(5000);
				brobot.connect("irc.digitalirc.org");
			}
			
		} catch(UnknownHostException uhe) {
			
			brobot.connect("irc.digitalirc.org");
			
		}
	}
	
}