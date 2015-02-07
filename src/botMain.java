import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.jibble.pircbot.NickAlreadyInUseException;

public class botMain {
	
	public static void main(String[] args) throws Exception {
		Bot brobot = new Bot();
		
		brobot.setVerbose(true);
		
		try {
			
			brobot.connect(brobot.botSettings.getProperty("irc_server"),Integer.parseInt(brobot.botSettings.getProperty("server_port")));
			
		} catch(SocketException se) {
			
			while(brobot.isConnected() == false)
			{
				Thread.sleep(5000);
				brobot.connect(brobot.botSettings.getProperty("irc_server"),Integer.parseInt(brobot.botSettings.getProperty("server_port")));
			}
			
		} catch(UnknownHostException uhe) {
			while(brobot.isConnected() == false)
			{
				Thread.sleep(5000);
				brobot.connect(brobot.botSettings.getProperty("irc_server"),Integer.parseInt(brobot.botSettings.getProperty("server_port")));
			}			
		} catch(IOException ioe) {
			while(brobot.isConnected() == false)
			{
				Thread.sleep(5000);
				brobot.connect(brobot.botSettings.getProperty("irc_server"),Integer.parseInt(brobot.botSettings.getProperty("server_port")));
			}		
		} catch(NickAlreadyInUseException naiue) {
			brobot.changeNick("somebodystolemynick");
			brobot.sendRawLine("/msg nickserv ghost " + brobot.botSettings.getProperty("bot_password"));
		}
	}
	
}