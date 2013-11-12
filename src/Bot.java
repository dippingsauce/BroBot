import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.Properties;
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
	private List<Links> LinkList = new ArrayList<Links>();
	private List<Command> CmdList = new ArrayList<Command>();
	public Properties botSettings = new Properties();
	
	public Bot() {
		//Load settings from config file
		File f = new File("Config.ini");
		if(f.exists() && f.length() > 0) {
			try {
				botSettings.load(new FileInputStream(f));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// check to make sure bot_nick is set right in the settings
		// then set the name to it, if its not set correctly the name
		// will be "propertyError"
		if(botSettings.getProperty("bot_nick") != "") {
			this.setName(botSettings.getProperty("bot_nick"));
		} else {
			this.setName("propertyError");
		}
		
		
		//Set all the commands (this is temporary i might throw this in the command class to clean things up)
		Command c = new Command();
		c.setCmdName(".help");
		c.setDescription("This Command is to help users use the bot.");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".commands");
		c.setDescription("Gives you a list of all the valid commands");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".nsfw");
		c.setDescription("Sends a random nsfw link. FAP AWAY!");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".shorten");
		c.setDescription(".shorten <url> | Shortens specified URL using goo.gl");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(true);
		c.setEnabled(false);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".identify");
		c.setDescription("Identifies the bot.");
		c.setUserFlags(Command.Flags.MOD);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".add");
		c.setDescription("for nsfw (.add tits <link> <tags>(optional)); for brolinx (.add brolinx <link> <genre>(optional)) genre and tags use tag formatting. no spacing just comma seperated");
		c.setUserFlags(Command.Flags.MOD);
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".brolinx");
		c.setDescription("Gives you some sick ass bro links. You can enter a genre right after it for more specific brolinks.");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".count");
		c.setDescription("Shows the count of all the links in the 'database'");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		
		// deprecated way need to get this shit out
		cmds = new HashMap<String, String>();
		cmds.put(".help","");
		cmds.put(".cmds", "Shows list of commands.");
		cmds.put(".nsfw", "Gives you some fun nsfw");
		cmds.put(".shorten", "Shortens URLS using goo.gl");
		cmds.put(".add", "for nsfw (.add tits <link> <tags>(optional)); for brolinx (.add brolinx <link> <genre>(optional)) genre and tags use tag formatting. no spacing just comma seperated");
		cmds.put(".identify", "Identifies bot nick");
		cmds.put(".brolinx", "Gives you some sick ass bro links. You can enter a genre right after it for more specific brolinks.");
		cmds.put(".count", "Shows the count of all the links in the 'database'");
		cmds.put(".importnsfw", "This will import nsfw from file tits.txt");
		cmds.put(".exportlinks", "for all(.exportlinks all <filename>) for nsfw(.exportlinks nsfw <filename>) for brolinx(.exportlinks brolinx <filename>) Exports selected cateogry links to text file.");
		
		//imports database file into object list
		f = new File("links.txt");
		if(f.exists() && f.length() != 0) {
			try{
				FileInputStream fis = new FileInputStream("links.txt");
				ObjectInputStream instream = new ObjectInputStream(fis);
				LinkList = (ArrayList<Links>) instream.readObject();
				instream.close();
				fis.close();
				
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void onDisconnect() {
		while(this.isConnected() == false) {
				try {
					Thread.sleep(5000);
					this.reconnect();
				} catch (NickAlreadyInUseException naiue) {
					this.changeNick("needtofigureouthowtoghostwithbot");
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (IrcException ie) {
					ie.printStackTrace();
				} catch(InterruptedException ie) {
					ie.printStackTrace();
				}
		}
		
	}
	
	public void onMessage(String channel, String sender, String login, 
						String hostname, String message) {

		// This will confirm the incoming message is a command
		if(message.startsWith(".")) {
			//for whatever reason we cant have a loop in here so at this point
			//we know it SHOULD be a command and will call a helper function
			doCommand(message,channel,sender);
		}
		
		
		if(Boolean.parseBoolean(botSettings.getProperty("youtube_announce"))) {
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
			if(message.trim().matches(compiledPattern.toString())) {
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
	}
	
	public void onConnect() {
		if(Boolean.parseBoolean(botSettings.getProperty("auto_identify"))) {
			this.identify(botSettings.getProperty("bot_password"));
		}
		this.joinChannel("#" + botSettings.getProperty("bot_chan"));
	}
	
	private void doCommand(String cmd, String chan, String sender) {
		String[] args = cmd.split(" ");
		for(Command c:CmdList) {
			if(c.getCmdName() == args[0].toLowerCase()) {
				cmd = args[0];
			}
		}

		if(cmd.equals(".cmds")) {
			
			String msg = "";
			for(String s : cmds.keySet()) {
				msg += s + ", ";
			}
			msg = msg.substring(0, msg.length() - 1);
			sendMessage(chan,msg);
			
		} else if(cmd.equals(".nsfw")) {
			sendMessage(chan, getNSFW());
			
		} else if(cmd.contains(".brolinx")) {
			if(args.length > 1 && !args[1].isEmpty()) {
				sendMessage(chan, getBrolinx(args[1]));
			} else {
				sendMessage(chan, getBrolinx());
			}
		} else if(cmd.contains(".shorten")) {
			if(args[1] != "") {
				sendMessage(chan, ShortenURL(args[1]));
			} else { sendMessage(chan, "There is no URL to Shorten"); }
			
		} else if(cmd.contains(".add")) {
			if(checkAdmin(sender)) {
				switch(args[1]) {
					case "tits":
						try {
							/* old method
							FileWriter pw = new FileWriter("tits.txt",true);
							BufferedWriter bw = new BufferedWriter(pw);
							bw.newLine();
							bw.append(args[2]);
							bw.close();
							pw.close();
							*/
							Links l = new Links(Links.SourceCategory.NSFW,args[2]);
							if(DupeCheck(l.getLink())) {
								LinkList.add(l);
							} else {
								sendMessage(chan, "This link is already in the database. Please try a different link.");
								return;
							}
							
							
							if(SaveList() == true) {
								sendMessage(chan, "nsfw added!");
							} else {
								sendMessage(chan, "Tits Exploded! Retry perhaps?");
							}
							
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
						
					case "brolinx":
						try {
							Links l = new Links(Links.SourceCategory.YOUTUBE,args[2]);
							if(args[3] != null) {
								l.setArgs(args[3]);
							}
							
							LinkList.add(l);
							if(SaveList() == true) {
								sendMessage(chan, "Broin' Out...SUCCESSFUL!");
							}
							
						} catch (Exception e) {
							sendMessage(chan, "Fucking errors!");
						}
						break;
				}
			} else {
				sendMessage(chan, "You cannot use this command. NOT SPECIAL ENOUGH!");
			}
		
		} else if (cmd.contains(".identify")) {
			if(checkAdmin(sender)) {
				this.identify("zombies");
			} else {
				sendMessage(chan, "Fak Aff. Still not special enough!");
			}
			
		} else if (cmd.contains(".count")) {
			sendMessage(chan, "There are " + Integer.toString(getLinkCount(Links.SourceCategory.NSFW)) + " NSFW links, " + Integer.toString(getLinkCount(Links.SourceCategory.YOUTUBE)) + " Youtube links, and " + Integer.toString(getLinkCount(Links.SourceCategory.FUNNYS)) + " LOLOL links.");
			
		} else if(cmd.contains(".exportlinks")) {
			if(checkAdmin(sender)) {				
				try {
					FileWriter pw = new FileWriter(args[2] + ".txt",true);
					BufferedWriter bw = new BufferedWriter(pw);

					switch(args[1]) {
						case "nsfw":
							for(Links l: LinkList){
								if(l.getCat() == Links.SourceCategory.NSFW) {
									bw.write(l.getLink());
									bw.newLine();
								}
							}
							break;
							
						case "brolinx":
							for(Links l: LinkList){
								if(l.getCat() == Links.SourceCategory.YOUTUBE) {
									bw.write(l.getLink());
									bw.newLine();
								}
							}
							break;
							
						case "all":
							for(Links l: LinkList){
								bw.write(l.getLink());
								bw.newLine();
							}
							break;
					}
					
					bw.close();
					pw.close();
				} catch(Exception ex) {
					sendMessage(chan, "Error somewhere along the way :(");
				}
			}
		
		}else if(cmd.contains(".importnsfw")) {
			if(checkAdmin(sender)) {
				try{
					FileInputStream fstream = new FileInputStream("tits.txt");
					DataInputStream dstream = new DataInputStream(fstream);
					
					BufferedReader br = new BufferedReader(new InputStreamReader(dstream));
					String link;
					int counter = 0;
					while ((link = br.readLine()) != null) {
						if(DupeCheck(link)) {
							Links l = new Links(Links.SourceCategory.NSFW, link);
							LinkList.add(l);
							counter++;
						} 
					}
					
					if(SaveList()) {
						sendMessage(chan, Integer.toString(counter) + " Links imported successfully.");
					} else {
						sendMessage(chan, "There was a problem saving the list.");
					}
					
					dstream.close();
					fstream.close();
					
					if(counter > 0) {
						FileOutputStream writer = new FileOutputStream("tits.txt");
						writer.write((new String().getBytes()));
						writer.close();
					}
					
				} catch (Exception ex) {
					sendMessage(chan, ex.getMessage());
				}
			} else {
				sendMessage(chan, "Fak Aff. Still not special enough!");
			}
		} else {
			sendMessage(chan, "wha gwan me bredren! dis be an error!");
		}			
	}
	
	private int getLinkCount(Links.SourceCategory cat) {
		int i = 0;
		
		for(Links l : LinkList) {
			if(l.getCat() == cat) {
				i++;
			}
		}
		
		return i;
	}
	
	private Boolean DupeCheck(String link) {
		for(Links l : LinkList) {
			if(l.getLink().equalsIgnoreCase(link)) {
				return false;
			}
		}
		
		return true;
	}

	private void showHelp(String[] args, String channel) {
		String msg = "";
		
		switch(args[1]) {
			case "chachin":
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
			/* old method
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
			*/
			List<String> temp = ReturnList(Links.SourceCategory.NSFW);
			
			Random rand = new Random();
			return temp.get(rand.nextInt(temp.size()));
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String getBrolinx(String... args) {
		try {
			List<String> temp;
			if(args.length == 0) {
				temp = ReturnList(Links.SourceCategory.YOUTUBE);
			} else {
				temp = ReturnList(Links.SourceCategory.YOUTUBE, args);
			}
			
			Random rand = new Random();
			return temp.get(rand.nextInt(temp.size()));
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String ShortenURL(String LongURL) {
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
	
	private Boolean SaveList() {
		try {
			FileOutputStream fos = new FileOutputStream("links.txt");
			ObjectOutputStream outstream = new ObjectOutputStream(fos);
			outstream.writeObject(LinkList);
			outstream.close();
			fos.close();
			return true;
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private List<String> ReturnList (Links.SourceCategory cat, String... args) {
		if(cat == Links.SourceCategory.NSFW) {
			List<String> temp = new ArrayList<String>();
			for(Links l:LinkList) {
				if(l.getCat() == Links.SourceCategory.NSFW) {
					temp.add(l.getLink());
				}
			}
			
			return temp;
		} else if(cat == Links.SourceCategory.YOUTUBE) {
			List<String> temp = new ArrayList<String>();
			if(args.length > 0) {
				for(Links l:LinkList) {
					if(l.getCat() == Links.SourceCategory.YOUTUBE) {
						if(l.getArgs().equalsIgnoreCase(args[0])) {
							temp.add(l.getLink());
						}
					}
				}
				return temp;
			} else {
				for(Links l:LinkList) {
					if(l.getCat() == Links.SourceCategory.YOUTUBE) {
						temp.add(l.getLink());
					}
				}
				return temp;
			}
		}
		
		return null;
	}
}