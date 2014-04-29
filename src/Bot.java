import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bot extends PircBot {
	private List<Links> LinkList = new ArrayList<Links>();
	private List<Command> CmdList = new ArrayList<Command>();
	private Map<String,Command.Flags> UserLevels = new HashMap<String,Command.Flags>();
	private Map<String,Integer> BadURLs = new HashMap<String,Integer>();
	public Properties botSettings = new Properties();
	private int TorrentID = -1;
	
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
		
		//auto delete 404'd images
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				List<Links> remove = new ArrayList<Links>();
				for(Links l: LinkList) {
					if(l.getCat() == Links.SourceCategory.NSFW) {
						try {
							if(check404(l.getLink())) {
								remove.add(l);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				LinkList.removeAll(remove);
				
				SaveList("links");
			}
		}, 1000*60*2, 12*60*60*1000);
		
		//torrent announce 1000*60 is 1 min (1000 is 1 second so if you want to change the time keep that in mind)
		if(Boolean.parseBoolean(botSettings.getProperty("torrent_announce").toString())) {
			Timer t2 = new Timer();
			t2.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					String msg = "";
					if((msg = checkAnnounce(TorrentID)) != "Error") {
						sendMessage("#"+botSettings.getProperty("bot_chan"),msg);
					}
				}
	
			}, 1000*60,1000*60);
		}
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
		
		c = new Command();
		c.setCmdName(".import");
		c.setDescription("For when you have a list of links and want to import them. Make sure to specify whether its brolinx, lolol, or nsfw after the command. Also make sure the format in the text file matches link then tags (i.e. .import nsfw nsfw.txt)");
		c.setUserFlags(Command.Flags.ADMIN);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".export");
		c.setDescription("For when you want to export a category of links. Make sure to specify whether its youtube, lolol, or nsfw after the command. (i.e. .export nsfw nsfw.txt)");
		c.setUserFlags(Command.Flags.ADMIN);
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName(".imdb");
		c.setDescription("Enter a movie title and get some info about the movie from IMDB. (i.e. .imdb Star Wars). optionally use the -y flag for the year (i.e. .imdb star wars -y 1977");
		c.setUserFlags(Command.Flags.USER);
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
	
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		
		if(message.equalsIgnoreCase(this.botSettings.getProperty("invite_phrase"))) {
			this.sendInvite(sender, "#"+this.botSettings.getProperty("bot_chan"));
		} else if(UserLevels.containsKey(sender.toLowerCase())) {
			String[] args = message.split(" ");
			switch(args[0]) {
				case "join":
					this.joinChannel(args[1]);
					break;
				
				case "part":
					if(args.length > 2) {
						this.partChannel(args[1], args[2]);
					} else	{
						this.partChannel(args[1]);
					}
					break;
					
				case "add":
					if (UserLevels.get(sender.toLowerCase()) == Command.Flags.ADMIN) {
						sendMessage(sender,addCommand(args,true,sender));
					} else {
						sendMessage(sender,"You do not have proper permissions");
					}
					break;
					
				case "info":
					sendMessage(sender, getInfo(args[1]));
					break;
				
				//this is for the /me command. so for this case you would pm your bot and type me does this and that
				//and itll send botname does this and that into the chat specified in settings
				case "me":
					this.sendAction("#" + this.botSettings.getProperty("bot_chan"), message.substring(3));
					break;
					
				case "say":
					this.sendMessage("#" + this.botSettings.getProperty("bot_chan"), message.substring(4));
					break;
			}
		}
	}
	
	public void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		if(Boolean.parseBoolean(this.botSettings.getProperty("invite_only")) && 
				channel == "#"+this.botSettings.getProperty("bot_chan")) {
			this.setMode(channel, "+i");
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
		
		// Testing a little bit of "AI"
		if(message.startsWith(this.botSettings.getProperty("bot_nick").toString())) {
			AI chica = new AI();
			String[] args = message.split(" ");
			if(args.length == 1) {
				if(sender.equalsIgnoreCase(this.botSettings.getProperty("owner_nick"))) {
					sendMessage(channel,"Sup bro, what can I do for you?");
				} else {
					sendMessage(channel,chica.grabGreeting());
				}
			} else if(chica.isReceiveString(Arrays.asList(Arrays.copyOfRange(args, 1, args.length / 2)))) {
				int nickindx = chica.getReceiveIndex(Arrays.asList(args));
				String strnick = args[nickindx + 1];
				if (strnick.equalsIgnoreCase("me")) {
					strnick = "";
				}
				if(Arrays.asList(Arrays.copyOfRange(args, args.length / 2,args.length)).contains("nsfw")) {
					if(chica.isAdditionString(Arrays.asList(Arrays.copyOfRange(args, Arrays.asList(args).indexOf("nsfw"), args.length)))) {
						String[] tags = args[args.length -1].split(",");
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.NSFW,tags));						
					} else {
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.NSFW));
					}
				} else if(Arrays.asList(Arrays.copyOfRange(args, args.length / 2,args.length)).contains("funny")) {
					if(chica.isAdditionString(Arrays.asList(Arrays.copyOfRange(args, Arrays.asList(args).indexOf("funny"), args.length)))) {
						String[] tags = args[args.length -1].split(",");
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.FUNNYS,tags));						
					} else {
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.FUNNYS));
					}
				} else if(Arrays.asList(Arrays.copyOfRange(args, args.length / 2,args.length)).contains("brolinx")) {
					if(chica.isAdditionString(Arrays.asList(Arrays.copyOfRange(args, Arrays.asList(args).indexOf("brolinx"), args.length)))) {
						String[] tags = args[args.length -1].split(",");
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.YOUTUBE,tags));						
					} else {
						sendMessage(channel,strnick+ " " + chica.grabResponse() + ". " + getLinks(Links.SourceCategory.YOUTUBE));
					}
				}
				
			}
		}
		
		
		if(Boolean.parseBoolean(botSettings.getProperty("youtube_announce"))) {
			
			//String pattern = "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9-_]{11}(&(.+))?";
			//Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
			//if(message.trim().matches(compiledPattern.toString())) {
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
					if(title.contains("YouTube")) {
						sendMessage(channel, title);
					}
					
					br.close();
					isr.close();
					is.close();
				}
				catch (Exception ex) {
					System.out.print(ex.getCause() + "|| " + ex.getMessage());
				}
			//}
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
						showHelp(args,sender);
						break;
						
					case ".commands":
						showCommands(sender);
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
							sendMessage(chan,addCommand(args,true,sender));
						} else {
							sendMessage(chan,addCommand(args,false,sender));
						}
						
						break;
						
					case ".import":
						sendMessage(chan,importLinks(args));
						break;
						
					case ".export":
						sendMessage(chan,exportLinks(args));
						break;
						
					case ".imdb":
						sendMessage(chan,getIMDBdata(args));
						break;
				}
			}
		}
	}
	
// ------------------- Functions for commands ------------------------------------	
	private void showHelp(String[] args, String sender) {
		String msg = "Help requested! If you need help with a specific command; Try .help <command name> or .commands to see a list of available commands.";
		
		if(args.length > 1) {
			for(Command c:CmdList) {
				if(c.getCmdName().endsWith(args[1])) {
					msg = c.getDescription();
				}
			}
		}
		
		sendNotice(sender,msg);
	}
	
	private void showCommands(String sender) {
		String lst = "";
		
		for(Command c:CmdList) {
			if(c.isHidden() == false) {
				lst += c.getCmdName() + ", ";
			}
		}
		
		lst = lst.substring(0,lst.length() - 2);
		sendNotice(sender,lst);
	}
	
	private String getLinks(Links.SourceCategory cat,String[]... args) {
		try {
			List<Links> temp = null;
			
			if(args.length > 0) {
				temp = ReturnList(cat, args);
			} else {
				temp = ReturnList(cat);
			}
			
			Random rand = new Random();
			if(!temp.isEmpty()) {
				return temp.get(rand.nextInt(temp.size())).getLink();
			}
			
			return "Unfortunately, I couldn't locate any links that were up your alley.";
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String getInfo(String link) {
		for(Links l:LinkList) {
			if(link.equals(l.getLink())) {
				return "\u0002ID:\u0002 " + LinkList.indexOf(l) + " \u0002Submitted By:\u0002 " + l.getSubmitter() + " \u0002Tags:\u0002 " + l.getArgs();
			}
		}
		
		return "Couldn't locate link in the database.";
	}
	
	private List<Links> ReturnList(Links.SourceCategory cat, String[]... args) {
		List<Links> temp = new ArrayList<Links>();
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
						
						
						if(args[0].length == 1) {
							if(tags.contains(args[0][0].toLowerCase())) {
								temp.add(l);
							} 
						} else {
							if(tags.contains(args[0][1].toLowerCase())) {
								temp.add(l);
							}
						}
					}
				} else {
					temp.add(l);
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
			Command.Flags flag = UserLevels.get(sender.toLowerCase());
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
	
	private String importLinks(String[] args) {
		try{
			FileInputStream fstream = new FileInputStream(args[2]);
			DataInputStream dstream = new DataInputStream(fstream);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(dstream));
			String line;
			int counter = 0;

			if(args[1].equalsIgnoreCase("nsfw")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.NSFW, attrib[0]);
						if(attrib[1].length() > 0) {
							l.setArgs(attrib[1]);
						}
						LinkList.add(l);
						counter++;
					}
				}
			} else if(args[1].equalsIgnoreCase("brolinx")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.YOUTUBE, attrib[0]);
						if(attrib[1].length() > 0) {
							l.setArgs(attrib[1]);
						}
						LinkList.add(l);
						counter++;
					}
				}		
			} else if(args[1].equalsIgnoreCase("lolol")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.FUNNYS, attrib[0]);
						if(attrib[1].length() > 0) {
							l.setArgs(attrib[1]);
						}
						LinkList.add(l);
						counter++;
					}
				}		
			} else {
				return "You didn't specify what category of links you are importing. Please try 'nsfw','brolinx',or 'lolol'";
			}
			
			dstream.close();
			fstream.close();
								
			if(SaveList("links")) {
				return Integer.toString(counter) + " Links imported successfully.";
			} else {
				return "There was a problem saving the links to the list.";
			}
			
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String exportLinks(String[] args) {
		try {
			Integer counter = 0;
			FileWriter pw = new FileWriter(args[2],true);
			BufferedWriter bw = new BufferedWriter(pw);

			switch(args[1].toLowerCase()) {
				case "nsfw":
					for(Links l: LinkList){
						if(l.getCat() == Links.SourceCategory.NSFW) {
							bw.write(l.getLink() + " " + l.getArgs());
							bw.newLine();
							counter++;
						}
					}
					break;
					
				case "brolinx":
					for(Links l: LinkList){
						if(l.getCat() == Links.SourceCategory.YOUTUBE) {
							bw.write(l.getLink() + " " + l.getArgs());
							bw.newLine();
							counter++;
						}
					}
					break;
					
				case "lolol":
					for(Links l: LinkList){
						if(l.getCat() == Links.SourceCategory.YOUTUBE) {
							bw.write(l.getLink() + " " + l.getArgs());
							bw.newLine();
							counter++;
						}
					}
					break;
					
				case "all":
					for(Links l: LinkList){
						bw.write(l.getLink() + " " + l.getArgs());
						bw.newLine();
						counter++;
					}
					break;
			}
			
			bw.close();
			pw.close();
			
			return "All " + counter.toString() + " messages have been exported successfully!";
		} catch(Exception ex) {
			return ex.getMessage();
		}
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
	
	private String addCommand(String[] args,Boolean admin,String sender) {
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
				
			case "nsfw":
			case "tits":
				msg = addLinks(Links.SourceCategory.NSFW,args,sender);
				break;
				
			case "brolinx":
				msg = addLinks(Links.SourceCategory.YOUTUBE,args,sender);
				break;
				
			case "lulz":
			case "lolol":
			case "lawlz":
				msg = addLinks(Links.SourceCategory.FUNNYS,args,sender);
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
	
	private String addLinks(Links.SourceCategory cat,String[] args,String sender) {
		Links l = new Links(cat,args[2]);
		l.setSubmitter(sender);
		if(args.length > 3) {
			l.setArgs(args[3]);
		}
		
		if(DupeCheck(args[2]) == false || check404(args[2]) == false) {
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
			return "Duplicate or dead link. Get better material!";
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
				" Brolinx links, and " + Integer.toString(l) + " Funnies links.";
	}
	
	private Boolean check404(String strURL) {
		HttpURLConnection conn;
		HttpURLConnection.setFollowRedirects(false);
		try {
			conn = (HttpURLConnection)new URL(strURL).openConnection();
			if(conn.getResponseCode() == 404){
				return true;
			} else if(strURL.contains("imgur") && conn.getResponseCode() == 302){
				return true;
			} else {
				return false;
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(UnknownHostException uhe) {
			if(BadURLs.containsKey(strURL)) {
				if(BadURLs.get(strURL) < 5) {
					return true;
				} else {
					BadURLs.put(strURL, BadURLs.get(strURL) + 1);
					return false;
				}
			} else {
				BadURLs.put(strURL, 1);
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;		
	}
	 
	private String getIMDBdata(String[] args) {
		try {
			String apiURL = "http://www.omdbapi.com/?s=";
			String moviename = "";
			Boolean hasYear = false;
			String year = "";
			
			for(int i = 1; i < args.length; i++) {
				if(args[i].startsWith("-")) {
					if(args[i].toLowerCase().endsWith("y")) {
						hasYear = true;
						year = args[i+1];
						args = Arrays.copyOfRange(args,0, i);
					}
				}
			}
			
			if(args.length > 2) {
				for(int i = 1; i < args.length; i++) {
					if(i < args.length - 1) {
						moviename = args[i] + "%20";
					} else {
						moviename += args[i];
					}
				}
			}else{
				moviename = args[1];
			}
			moviename = moviename.trim();
			
			if(hasYear == false) {
				apiURL += moviename + "&r=JSON";
			} else {
				apiURL += moviename + "&r=JSON&y=" + year;
			}
			
			if(moviename != "") {
				HttpURLConnection conn = (HttpURLConnection)new URL(apiURL).openConnection();
				conn.setRequestMethod("GET");
				
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputdata;
				StringBuffer response = new StringBuffer();
				
				while ((inputdata = in.readLine()) != null) {
					response.append(inputdata);
				}
				
				in.close();
				
				JSONObject data = new JSONObject(response.toString());
				if(data.has("Error")) {
					return "\u0002Error:\u0002 " + data.get("Error").toString();
				} 
				
				JSONArray datalist = data.getJSONArray("Search");

				JSONObject movie = new JSONObject();
				for(int i = 0; i < datalist.length();i++) {
					if(!datalist.getJSONObject(i).getString("Type").contains("episode")) {
						movie = datalist.getJSONObject(i);
						break;
					}	
				}
				
				if(movie.has("Error")) {
					return "\u0002Error:\u0002 " + movie.get("Error").toString();
				}
				
				apiURL = "http://www.omdbapi.com/?i=" + movie.get("imdbID").toString();
				conn = (HttpURLConnection) new URL(apiURL).openConnection();
				conn.setRequestMethod("GET");
					
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				response = new StringBuffer();
					
				while ((inputdata = in.readLine()) != null) {
					response.append(inputdata);
				}
				in.close();
					
				JSONObject moviedata = new JSONObject(response.toString());
				
				if(moviedata.has("Error")) {
					return "\u0002Error:\u0002 " + moviedata.get("Error").toString();
				}
					
				String msg = "";
				msg += "\u0002Title:\u0002 " + moviedata.get("Title") + " ";
				msg += "\u0002Year:\u0002 " + moviedata.get("Year") + " ";
				msg += "\u0002IMDB Rating:\u0002 " + moviedata.get("imdbRating") + " ";
				msg += "\u0002URL:\u0002 " + "http://www.imdb.com/title/" + moviedata.get("imdbID");
					
				return msg;
				
			} else {
				return "Don't fuck with me...Enter a movie name";
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return e.getMessage();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			return e.getMessage();
		}
	}
	
	private String checkAnnounce(int torrentID) {
		try{
			String TorURL = this.botSettings.getProperty("announce_url").toString() + torrentID;
			HttpURLConnection conn = (HttpURLConnection)new URL(TorURL).openConnection();
			conn.setRequestMethod("GET");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputdata;
			StringBuffer response = new StringBuffer();
			
			while ((inputdata = in.readLine()) != null) {
				response.append(inputdata);
			}
			
			in.close();
			
			
			JSONArray data = new JSONArray(response.toString());
			
			String msg = "";
			msg += Colors.PURPLE + Colors.UNDERLINE + "Title:" + Colors.NORMAL + " " + data.getJSONObject(0).get("torrent") + " ";
			msg += Colors.PURPLE + Colors.UNDERLINE + "URL:" + Colors.NORMAL + " " + botSettings.getProperty("torrent_url_prefix").toString() + data.getJSONObject(0).get("uri") + " ";
			
			String[] tags = data.getJSONObject(0).get("tags").toString().split(" ");
			String strtags = "";
			for(int i = 0; i < tags.length; i++)
			{
				if(i < 5) {
					strtags += tags[i] + " ";
				}
			}
			msg += Colors.PURPLE + Colors.UNDERLINE + "Tags:" + Colors.NORMAL + " " + strtags;
			
			if(torrentID != Integer.parseInt(data.getJSONObject(0).get("id").toString())) {
				TorrentID = Integer.parseInt(data.getJSONObject(0).get("id").toString()); 
			
				return msg;
			} else {
				return "";
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return "Error";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "Error";
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			return "Error";
		}
	}

// -----------------------------------------------------------------------------------------
	/*private String ShortenURL(String LongURL) {
		//url aint working look into it
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
			//return "SHIT FUCKIN SUCKS ITLL WORK WHEN I GET TO IT";
		/*} catch (MalformedURLException mue) {
			return mue.getMessage();
		} catch (IOException ioe) {
			return ioe.getMessage();
		} /*catch (JSONException je) {
			return je.getMessage();
		}
		
		//return shortURL;
	} */
}