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
import java.util.Arrays;
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
	private Map<String,Command.Flags> UserLevels = new HashMap<String,Command.Flags>();
	public Properties botSettings = new Properties();
	
	public Bot() {
		initData();
		// check to make sure bot_nick is set right in the settings
		// then set the name to it, if its not set correctly the name
		// will be "propertyError"
		if(botSettings.getProperty("bot_nick") != "") {
			this.setName(botSettings.getProperty("bot_nick"));
		} else {
			this.setName("propertyError");
		}
		
		// deprecated way need to get this shit out
		cmds = new HashMap<String, String>();
		cmds.put(".importnsfw", "This will import nsfw from file tits.txt");
		cmds.put(".exportlinks", "for all(.exportlinks all <filename>) for nsfw(.exportlinks nsfw <filename>) for brolinx(.exportlinks brolinx <filename>) Exports selected cateogry links to text file.");
	}
	
	@SuppressWarnings("unchecked")
	private void initData() {
		// add all our commands to the CmdList (still need to think of the right place to put this)
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
		c.setDescription("Sends a random fappage links. Optionally add a tag at the end to get more specific links (.nsfw <tag>)");
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
		c.setDescription("To add a user (.add user <nick> <level>) current levels: admin,mod,user; for nsfw (.add tits <link> <tags>(optional)); for brolinx (.add brolinx <link> <genre>(optional)) genre and tags use tag formatting. no spacing just comma seperated");
		c.setUserFlags(Command.Flags.MOD);
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".brolinx");
		c.setDescription("Gives you some sick ass bro links. Optionally add a tag at the end to get more specific links (.brolinx <tag>)");
		c.setUserFlags(Command.Flags.ALL);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".funnies");
		c.setDescription("Gives you some funny links. Optionally add a tag at the end to get more specific links (.funnies <tag>)");
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
		
		//imports Links file into our LinkList
		f = new File("links.txt");
		if(f.exists() && f.length() != 0) {
			try{
				FileInputStream fis = new FileInputStream(f);
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
		
		// Load users and levels from Users.txt
		f = new File("users.txt");
		if(f.exists() && f.length() != 0) {
			try{
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream instream = new ObjectInputStream(fis);
				UserLevels = (Map<String,Command.Flags>) instream.readObject();
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
		} else if(f.length() <= 0) {
			UserLevels.put(botSettings.getProperty("owner_nick").toLowerCase(), Command.Flags.ADMIN);
			SaveList("users");
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
			if(c.getCmdName().equals(args[0])) {
				//delete this variable once all methods are switched over
				cmd = args[0];
				//Make sure the command is enabled before we even start
				if(c.isEnabled() == false) {
					sendMessage(chan,"This command is disabled.");
					return;
				}
				
				//Check to see if user has permission
				if(hasPermission(sender,c.getUserFlags()) == false) {
					sendMessage(chan,"You don't have the proper permissions to use this command.");
					return;
				}
				
				switch(args[0]) {
					case ".help":
						showHelp(args,chan);
						break;
						
					case ".commands":
						showCommands(chan);
						break;
						
					case ".nsfw":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.NSFW,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.NSFW));
							break;
						}
						
					case ".brolinx":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.YOUTUBE,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.YOUTUBE));
							break;
						}
						
					case ".funnies":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.FUNNYS,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.FUNNYS));
							break;
						}
						
					case ".identify":
						this.identify("zombies");
						break;
						
					case ".count":
						sendMessage(chan,getLinkCount());
						break;
						
					case ".add":
						if (UserLevels.get(sender.toLowerCase()) == Command.Flags.ADMIN) {
							sendMessage(chan,addCommand(args,true));
						} else {
							sendMessage(chan,addCommand(args,false));
						}
						
						break;
				}
			}
		}

// -------- all that remains for the old system ---------------
		if(cmd.contains(".exportlinks")) {
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
		
		} else if(cmd.contains(".importnsfw")) {
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
					
					if(SaveList("links")) {
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
			// 2 commands remain to be switched over
		}
// ----------------------------------------------------------------
	}
	
// ------------------- Functions for commands ------------------------------------	
	private void showHelp(String[] args, String channel) {
		String msg = "Help requested! If you need help with a specific command; Try .help <command name> or .commands to see a list of available commands.";
		
		if(args.length > 1) {
			for(Command c:CmdList) {
				if(c.getCmdName().endsWith(args[1])) {
					msg = c.getDescription();
				}
			}
		}
		
		sendMessage(channel,msg);
	}
	
	private void showCommands(String chan) {
		String lst = "";
		
		for(Command c:CmdList) {
			if(c.isHidden() == false) {
				lst += c.getCmdName() + ", ";
			}
		}
		
		lst = lst.substring(0,lst.length() - 2);
		sendMessage(chan,lst);
	}
	
	private String getLinks(Links.SourceCategory cat,String[]... args) {
		try {
			List<String> temp = null;
			
			if(args.length > 0) {
				temp = ReturnList(cat, args);
			} else {
				temp = ReturnList(cat);
			}
			
			Random rand = new Random();
			if(!temp.isEmpty()) {
				return temp.get(rand.nextInt(temp.size()));
			}
			
			return "Unfortunately, I couldn't locate any links that were up your alley.";
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private List<String> ReturnList(Links.SourceCategory cat, String[]... args) {
		List<String> temp = new ArrayList<String>();
		for(Links l:LinkList) {
			if(l.getCat() == cat) {
				if(args.length > 0) {
					List<String> tags = new ArrayList<String>();
					if(l.getArgs() != null) {
						if(l.getArgs().contains(",")) {
							tags = new ArrayList<String>(Arrays.asList(l.getArgs().toLowerCase().split(",")));
						} else {
							tags.add(l.getArgs().toLowerCase());
						}
					
						if(tags.contains(args[0][1].toLowerCase())) {
							temp.add(l.getLink());
						}
					}
				} else {
					temp.add(l.getLink());
				}
			}
		}
		return temp;
	}
	
	private Boolean hasPermission(String sender,Command.Flags Level) {
		Map<Command.Flags,Integer> levels = new HashMap<Command.Flags,Integer>();
		levels.put(Command.Flags.ADMIN, 3);
		levels.put(Command.Flags.MOD, 2);
		levels.put(Command.Flags.USER, 1);
		levels.put(Command.Flags.ALL, 0);
		
		Integer usrlvl = 0;
		Integer cmdlvl = levels.get(Level);
		if(UserLevels.containsKey(sender.toLowerCase())) {
			Command.Flags flag = UserLevels.get(sender);
			usrlvl = levels.get(flag);
		}
		
		if(usrlvl >= cmdlvl) {
			return true;
		}
		
		return false;
	}
	
	private Boolean DupeCheck(String link) {
		for(Links l : LinkList) {
			if(l.getLink().equals(link)) {
				return true;
			}
		}
		
		return false;
	}
	
	private Boolean SaveList(String lst) {
		try {
			if(lst == "links") {
				FileOutputStream fos = new FileOutputStream("links.txt");
				ObjectOutputStream outstream = new ObjectOutputStream(fos);
				outstream.writeObject(LinkList);
				outstream.close();
				fos.close();
				return true;
			} else if(lst == "users") {
				FileOutputStream fos = new FileOutputStream("users.txt");
				ObjectOutputStream outstream = new ObjectOutputStream(fos);
				outstream.writeObject(UserLevels);
				outstream.close();
				fos.close();
				return true;
			}
			return false;
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
	
	private String addCommand(String[] args,Boolean admin) {
		String msg = "WTF Just happened!... Ah shit there must have been an error.";
		switch(args[1]) {
			case "user":
				if((Boolean.parseBoolean(botSettings.getProperty("add_user_admin_only")) == true && admin == true) || Boolean.parseBoolean(botSettings.getProperty("add_user_admin_only")) == false) {
					if(args.length < 4) {
						msg = "you fucked something up bro. retry using .add user <username> <level>";
					} else {
						msg = addUser(args);
					}
				} else {
					msg = "You don't have proper permission to use this command.";
				}
				break;
				
			case "tits":
				msg = addLinks(Links.SourceCategory.NSFW,args);
				break;
				
			case "brolinx":
				msg = addLinks(Links.SourceCategory.YOUTUBE,args);
				break;
				
			case "lawlz":
				msg = addLinks(Links.SourceCategory.FUNNYS,args);
				break;
		}
		
		return msg;
	}
	
	private String addUser(String[] args) {
		try {
			String usrlvl = args[3];
			
			Command.Flags flag = null;
			if(usrlvl.equalsIgnoreCase("admin")) {
				flag = Command.Flags.ADMIN;
			} else if(usrlvl.equalsIgnoreCase("mod")) {
				flag = Command.Flags.MOD;
			} else if(usrlvl.equalsIgnoreCase("user")) {
				flag = Command.Flags.USER;
			}
			
			if(flag != null) {
				UserLevels.put(args[2].toLowerCase(), flag);
				if(SaveList("users")) {
					return "User Added!";
				} else {
					return "There was an unexpected error.";
				}
			} else {
				return "Invalid user level. use 'admin', 'mod', or 'user'";
			}
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String addLinks(Links.SourceCategory cat,String[] args) {
		Links l = new Links(cat,args[2]);
		if(args[3] != null) {
			l.setArgs(args[3]);
		}
		
		if(DupeCheck(args[2]) == false) {
			LinkList.add(l);
		
			if(SaveList("links") == true) {
				if(cat == Links.SourceCategory.NSFW) {
					return "Future fappage added!";
				} else if(cat == Links.SourceCategory.YOUTUBE) {
					return "Broin' Out...SUCCESSFUL!";
				} else {
					return "You made some high person extremely happy!";
				}
			}
		} else {
			return "DUPLICATE! Y U NO ADD UNIQUE LINK?";
		}
		
		return "There was an unexpected error.";
	}
	
	private String getLinkCount() {
		int n = 0;
		int y = 0;
		int l = 0;
		
		for(Links lnk : LinkList) {
			if(lnk.getCat() == Links.SourceCategory.NSFW) {
				n++;
			} else if(lnk.getCat() == Links.SourceCategory.YOUTUBE) {
				y++;
			} else {
				l++;
			}
		}
		
		return "There are " + Integer.toString(n) + " NSFW links, " + Integer.toString(y) + 
				" Youtube links, and " + Integer.toString(l) + " LOLOL links.";
	}
	
// ------------------------------------------------------------------------------------------------------------
	
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
	
}