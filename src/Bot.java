// TODO
// Move commands into seperate class files.
// Make more modular. e.g. load class files from a dir
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

public class Bot extends PircBot {
	
	private Map<String,String> cmds;
	public Bot() {
		this.setName("BroBot");
		cmds = new HashMap<String, String>();
		cmds.put(".help","");
		cmds.put(".cmds", "Shows list of commands.");
		cmds.put(".nsfw", "Gives you some fun nsfw");
		cmds.put(".shorten", "Shortens URLS using goo.gl");
		cmds.put(".add", "Name says it all, type .add help for more information");
		cmds.put(".identify", "Identifies nick");
	}
	
	public void onDisconnect() {
		while(this.isConnected() == false) {
				try {
					this.reconnect();
				} catch (NickAlreadyInUseException naiue) {
					this.changeNick("needtofigureouthowtoghostwithbot");
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (IrcException ie) {
					ie.printStackTrace();
				}
		}
		
	}
	
	public void onMessage(String channel, String sender, String login, 
						String hostname, String message) {
		//test for help, we are going about it differently
		if(message.startsWith(".help")) {
			String[] args = message.split(" ");
			if(args.length > 1) {
				showHelp(args, channel);
			} else {
				sendMessage(channel, "To get help with a specific command type .help *command*. To see a list of commands available type .cmds");
			}
			return;
		}
		
		//use this for regular commands to keep it nice and clean
		if(cmds.containsKey(message.split(" ")[0])) {
			doCommand(message,channel,sender);
		}
		
		/* regex alts
		 * 
		 * (http?s://)?(www\\.)?(youtube\\.com/|youtu\\.be/)watch\\?v=[a-zA-Z0-9-_]{11}
		 * 
		 * (?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9-_]{11}(&(.+))?
		 * 
		 * https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*
		 */
		String pattern = "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9-_]{11}(&(.+))?";
		Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		if(message.matches(compiledPattern.toString())) {
			try {
				URL url = new URL(message);
				URLConnection conn = url.openConnection();
				InputStream is = conn.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				
				HTMLEditorKit htmlkit = new HTMLEditorKit();
				
				HTMLDocument htmlDoc = (HTMLDocument) htmlkit.createDefaultDocument();
				htmlkit.read(br, htmlDoc, 0);
				
				String title = (String) htmlDoc.getProperty(HTMLDocument.TitleProperty);
				
				sendMessage(channel, title);
				
				br.close();
				isr.close();
				is.close();
			}
			catch (Exception ex) {
				System.out.print(ex.getCause() + "|| " + ex.getMessage());
			}
		}
	}
	
	public void onConnect() {
		this.identify("zombies");
		this.joinChannel("#zombienation");
	}
	
	private void doCommand(String cmd, String chan, String sender) {
		if(cmd.equals(".cmds")) {
			
			String msg = "";
			for(String s : cmds.keySet()) {
				msg += s + ", ";
			}
			msg = msg.substring(0, msg.length() - 1);
			sendMessage(chan,msg);
			
		} else if(cmd.equals(".nsfw")) {
			sendMessage(chan, getNSFW());
			
		} else if(cmd.contains(".shorten")) {
			String[] args = cmd.split(" ");
			if(args[1] != "") {
				sendMessage(chan, ShortenURL(args[1]));
			} else { sendMessage(chan, "There is no URL to Shorten"); }
			
		} else if(cmd.contains(".add")) { 
			String[] args = cmd.split(" ");
			if(checkAdmin(sender)) {
				switch(args[1]) {
					case "tits":
						try { // TODO check file for duplicates. 
							FileWriter pw = new FileWriter("tits.txt",true);
							BufferedWriter bw = new BufferedWriter(pw);
							bw.newLine();
							bw.append(args[2]);
							bw.close();
							pw.close();
							
							sendMessage(chan, "nsfw added!");
						} catch (Exception ex) {
							sendMessage(chan, "There was an error reading tits");
						}
						break;
					
					case "admin":
						try {
							FileWriter pw = new FileWriter("admins.txt",true);
							BufferedWriter bw = new BufferedWriter(pw);
							bw.newLine();
							bw.append(args[2]);
							bw.close();
							pw.close();
							
							sendMessage(chan, "admin added!");
						} catch (Exception ex) {
							sendMessage(chan, "There was an error reading admins");
						}
						break;
				}
			} else {
				sendMessage(chan, "You cannot use this command. NOT SPECIAL ENOUGH!");
			}
		
		} else if (cmd.contains(".identify")) {
			if(checkAdmin(sender)) {
				this.identify("zombies"); // TODO Make config option in admin.txt e.g. each admin should have their own PW
			} else {
				sendMessage(chan, "Fak Aff. Still not special enough!");
			}
			
		} else {
			sendMessage(chan, "wha gwan me bredren! dis be an error!");
		}			
	}
	
	private void showHelp(String[] args, String channel) {
		String msg = "";
		
		switch(args[1]) {
			case "chachin": // TODO add a shitlist file so this isn't hard coded in
				msg = "There is no help for chachin. He is a poor lost soul! :("; 
				break;
			
			case "cmds":
				msg = cmds.get("." + args[1]).toString();
				break;
				
			case "nsfw":
				msg = cmds.get("." + args[1]).toString();
				break;
				
			case "shorten":
				msg = cmds.get("." + args[1]).toString();
				break;
			
			case "add":
				msg = cmds.get("." + args[1]).toString();
				break;
			
			default:
				msg = "That is not a command! Type .cmds if you are confused.";
				break;
		}
		
		sendMessage(channel,msg);
	}
	
	private String getNSFW() {
		try {
			FileInputStream fstream = new FileInputStream("tits.txt");
			DataInputStream dstream = new DataInputStream(fstream);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(dstream));
			List<String> temp = new ArrayList<String>();
			String link;
			while ((link = br.readLine()) != null) {
				temp.add(link);
			}
			
			dstream.close();
			fstream.close();
			
			Random rand = new Random();
			return temp.get(rand.nextInt(temp.size()));
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String ShortenURL(String LongURL) { // TODO FIX IT ALL
		/*//url aint working look into it
		String googURL = "https://www.googleapis.com/urlshortener/v1/url?key=AIzaSyBdMcLKfQrYqL-8FCtwe0PZU9z2GGmdvPw";
		
		String shortURL = "";
		
		try {
			URLConnection uconn = new URL(googURL).openConnection();
			uconn.setDoOutput(true);
			uconn.setRequestProperty("Content-Type", "application/json");
			
			OutputStreamWriter osw = new OutputStreamWriter(uconn.getOutputStream());
			osw.write("{\"longUrl\":\"" + LongURL + "\"}");
			osw.flush();
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(uconn.getInputStream()));
			CharBuffer cbuff = CharBuffer.allocate(1024);
			//JSONObject json = new JSONObject(cbuff.toString());
			//shortURL = json.get("id").toString();
			
			osw.close();
			rd.close();*/
			return "SHIT FUCKIN SUCKS ITLL WORK WHEN I GET TO IT";
		/*} catch (MalformedURLException mue) {
			return mue.getMessage();
		} catch (IOException ioe) {
			return ioe.getMessage();
		} /*catch (JSONException je) {
			return je.getMessage();
		}*/
		
		//return shortURL;
	}
	
	private Boolean checkAdmin(String sender) {
		try{
			FileInputStream fstream = new FileInputStream("admins.txt");
			DataInputStream dstream = new DataInputStream(fstream);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(dstream));
			List<String> temp = new ArrayList<String>();
			String nick;
			while ((nick = br.readLine()) != null) {
				temp.add(nick);
			}
			
			dstream.close();
			
			if(temp.contains(sender)) {
				return true;
			} else {
				return false;
			}
		} catch(Exception ex) {
			return false;
		}
	}
}