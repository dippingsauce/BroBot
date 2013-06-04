import java.net.SocketException;
import java.net.UnknownHostException;

import org.jibble.pircbot.*;

@SuppressWarnings("unused")
public class botMain {
	
	public static void main(String[] args) throws Exception {
		Bot brobot = new Bot();
		
		brobot.setVerbose(true);
		
		try {
			
			brobot.connect("irc.silentzombies.com");
			
		} catch(SocketException se) {
			
			brobot.connect("irc.digitalwizardry.org");
			
		} catch(UnknownHostException uhe) {
			
			brobot.connect("irc.digitalwizardry.org");
			
		}
	}
	
}