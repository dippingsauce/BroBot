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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.jibble.pircbot.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bot extends PircBot {
	private List<Links> LinkList = new ArrayList<Links>();
	private List<Command> CmdList = new ArrayList<Command>();
	private Map<String,Command.Flags> UserLevels = new HashMap<String,Command.Flags>();
	private Map<String,Integer> BadURLs = new HashMap<String,Integer>();
	private List<String[]> TagAliases = new ArrayList<String[]>();
	public Properties botSettings = new Properties();
	private int TorrentID = -1;
	private dbFunctions dbf = null;
	
	public Bot() {
		//initialize all the data
		initData();

		// check to make sure bot_nick is set right in the settings
		// then set the name to it, if its not set correctly the name
		// will be "propertyError"
		if(botSettings.getProperty("bot_nick").toString() != "") {
			this.setName(botSettings.getProperty("bot_nick"));
		} else {
			this.setName("propertyError");
		}
		
		//auto delete 404'd images
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				List<Integer> DeadIDs = new ArrayList<Integer>();
				List<Links> TempList = getLinkList();
				for(Links l: TempList) {
					if(l.getCat() == Links.SourceCategory.NSFW) {
						try {
							if(check404(l.getLink())) {
								DeadIDs.add(l.getLinkID());
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				//After getting all dead links, we call on the database function to remove
				//dead links from the database.
				if(DeadIDs.size() > 0) {
					dbf.removeDeadLinks(DeadIDs);
				}
				
				//get the newest list from database
				LinkList = getLinkList();
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
	
	private void initData()  {
		// add all our commands to the CmdList (still need to think of the right place to put this)
		Command c = new Command();
		c.setCmdName("help");
		c.setDescription("This Command is to help users use the bot.");
		c.setUserFlags("all");
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("commands");
		c.setDescription("Gives you a list of all the valid commands");
		c.setUserFlags("all");
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("nsfw");
		c.setDescription("Sends a random fappage links. Optionally add a tag at the end to get more specific links (.nsfw <tag>) furthermore you can use ! to exclude the tag (.nsfw !<tag>)");
		c.setUserFlags("all");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("shorten");
		c.setDescription(".shorten <url> | Shortens specified URL using goo.gl");
		c.setUserFlags("all");
		c.setHidden(true);
		c.setEnabled(false);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("identify");
		c.setDescription("Identifies the bot.");
		c.setUserFlags("mod");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("add");
		c.setDescription("To add a user (.add user <nick> <level>) current levels: admin,mod,user; for nsfw (.add tits <link> <tags>(optional)); for brolinx (.add brolinx <link> <genre>(optional)) genre and tags use tag formatting. no spacing just comma seperated.");
		c.setUserFlags("mod");
		c.setHidden(true);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("brolinx");
		c.setDescription("Gives you some sick ass bro links. Optionally add a tag at the end to get more specific links (.brolinx <tag>) furthermore you can use ! to exclude the tag (.brolinx !<tag>)");
		c.setUserFlags("all");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("funnies");
		c.setDescription("Gives you some funny links. Optionally add a tag at the end to get more specific links (.funnies <tag>) furthermore you can use ! to exclude the tag (.funnies !<tag>)");
		c.setUserFlags("all");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("count");
		c.setDescription("Shows the count of all the links in the 'database'");
		c.setUserFlags("all");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("import");
		c.setDescription("For when you have a list of links and want to import them. Make sure to specify whether its brolinx, lolol, or nsfw after the command. Also make sure the format in the text file matches link then tags (i.e. .import nsfw nsfw.txt)");
		c.setUserFlags("admin");
		c.setHidden(false);
		c.setEnabled(false);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("export");
		c.setDescription("For when you want to export a category of links. Make sure to specify whether its youtube, lolol, or nsfw after the command. (i.e. .export nsfw nsfw.txt)");
		c.setUserFlags("admin");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("imdb");
		c.setDescription("Enter a movie title and get some info about the movie from IMDB. (i.e. .imdb Star Wars). optionally use the -y flag for the year (i.e. .imdb star wars -y 1977");
		c.setUserFlags("user");
		c.setHidden(false);
		c.setEnabled(true);
		CmdList.add(c);
		
		c = new Command();
		c.setCmdName("donate");
		c.setDescription("I've spent some time coding this and if you are feeling generous I can use your donations to upgrade my server that I host most of my projects on. If not enjoy it anyway!");
		c.setUserFlags("all");
		c.setHidden(true);
		c.setEnabled(false);
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
		
		//activate the database driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch(ClassNotFoundException cnfe) {
			System.out.print(cnfe.getMessage());
		}
		
		//create all the initial database structure
		CreateInitialDatabase();
		
		//make an instance of your dbfunctions class to use
		dbf = new dbFunctions();
		
		//we need to take all the current links in database and load them into our LinkList
		LinkList = getLinkList();
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
		} else {
			if(hasPermission(sender, "admin")) {
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
						sendMessage(sender,addCommand(args,true,sender,hostname));
					} else {
						sendMessage(sender,"You do not have proper permissions");
					}
					break;
					
				case "info":
					sendMessage(sender, getInfo(args[1]));
					break;
					
				case "edit":
					sendMessage(sender, editLink(args));
					break;
					
				case "editconfig":
					sendMessage(sender, editConfig(args));
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
			} else {
				sendMessage(sender,"Nope!");
			}
		}
	}
	
	public void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		if(Boolean.parseBoolean(this.botSettings.getProperty("invite_only")) && 
				channel.equals("#"+this.botSettings.getProperty("bot_chan"))) {
			this.setMode(channel, "+i");
		}
	}
	
	public void onMessage(String channel, String sender, String login, 
						String hostname, String message) {

		// This will confirm the incoming message is a command
		if(message.startsWith(getTrigger())) {
			//for whatever reason we cant have a loop in here so at this point
			//we know it SHOULD be a command and will call a helper function
			doCommand(message,channel,sender,hostname);
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
	private String getTrigger() {
		if(this.botSettings.getProperty("trigger_char") != null) {
			return this.botSettings.getProperty("trigger_char").toString();
		} else {
			return ".";
		}
	}
	
	private void doCommand(String cmd, String chan, String sender,String hostname) {
		String[] args = cmd.split(" ");
		
		for(Command c:CmdList) {
			if(c.getCmdName().equals(args[0].substring(1))) {
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
				
				switch(args[0].substring(1)) {
					case "help":
						showHelp(args,sender);
						break;
						
					case "commands":
						showCommands(sender);
						break;
						
					case "nsfw":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.NSFW,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.NSFW));
							break;
						}
						
					case "brolinx":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.YOUTUBE,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.YOUTUBE));
							break;
						}
						
					case "funnies":
						if(args.length > 1)
						{
							sendMessage(chan,getLinks(Links.SourceCategory.FUNNYS,args));
							break;
						} else {
							sendMessage(chan,getLinks(Links.SourceCategory.FUNNYS));
							break;
						}
						
					case "identify":
						this.identify("zombies");
						break;
						
					case "count":
						sendNotice(sender,dbf.getLinkCount());
						break;
						
					case "add":
						if (dbf.getUserLevel(sender) >= 3) {
							sendMessage(chan,addCommand(args,true,sender,chan));
						} else {
							sendMessage(chan,addCommand(args,false,sender,chan));
						}
						
						break;
						
					case "import":
						sendMessage(chan,importLinks(args));
						break;
						
					case "export":
						sendMessage(chan,exportLinks(args));
						break;
						
					case "imdb":
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
			if(dbf.getUserLevel(sender) >= 3) {
				if(args[1].equals("configs")) {
					Object[] keys = botSettings.keySet().toArray();
					for(int i=0;i<keys.length;i++) {
						if(i == 0) {
							msg = keys[i].toString() + ", ";
						} else if(i == keys.length - 1) {
							msg += keys[i].toString();
						} else {
							msg += keys[i].toString() + ", ";
						}
						
						
					}
				}
			}
			
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
				Integer randInt = rand.nextInt(temp.size());
				return temp.get(randInt).getLinkID().toString() + ". " + temp.get(randInt).getLink();
			}
			
			return "Unfortunately, I couldn't locate any links that were up your alley.";
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String getInfo(String lID) {
		for(Links l:LinkList) {
			if(lID.equals(l.getLinkID().toString())) {
				return "\u0002ID:\u0002 " + l.getLinkID().toString() + " \u0002Submitted By:\u0002 " + l.getSubmitter() + " \u0002Tags:\u0002 " + l.getArgs();
			}
		}
		
		return "Couldn't locate link in the database.";
	}
	
	private String editConfig(String[] args) {
		if(botSettings.containsKey(args[1])) {
			return args[1] + " changed from " + botSettings.setProperty(args[1], args[2]).toString() + " to " + args[2];
			
		} else {
			return "Invalid Setting, please try again";
		}
	}
	
	private String editLink(String[] args) {
		//edit <linkID> <what you are editing> <how you are editing it> <the edit> so for now edit llinkid tags <add|change> newinfo or edit linkid category <funnies|nsfw|brolinx>
		boolean edited = false;
		if(!args[1].isEmpty() || !args[2].isEmpty()) {
			for(Links l:LinkList) {
				if(args[1].equals(l.getLinkID().toString())) {
					edited = dbf.editLinks(l.getLinkID(), args, l.getArgs());
				}
			}
		}
		
		if(edited == false) {
			return "Couldn't find link or rare case that it didnt edit correctly";
		} else {
			LinkList = getLinkList();
			return "All is well.";
		}
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
							//if tag contains an exclude character ! grab all tags that don't contain the tag
							if(args[0][0].contains("!")) {
								if(!l.CheckTag(dbf.getTagReferences(args[0][0].replace("!", "").toLowerCase()))) {
									temp.add(l);
								}
							//if it doesn't contain an exclude character then grab all links that contain the tag
							} else {
								if(l.CheckTag(dbf.getTagReferences(args[0][0].toLowerCase()))) {
									temp.add(l);
								}
							}
						} else {
							//if tag contains an exclude character ! grab all tags that dont contain the tag
							if(args[0][1].contains("!")) {
								if(!l.CheckTag(dbf.getTagReferences(args[0][1].replace("!", "").toLowerCase()))) {
									temp.add(l);
								}
							//if it doesn't contain an exclude character then grab all links that contain the tag
							} else {
								if(l.CheckTag(dbf.getTagReferences(args[0][1].toLowerCase()))) {
									temp.add(l);
								}
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
	
	private Boolean hasPermission(String sender, String userlevelneeded) {
		int usrlvl = dbf.getUserLevel(sender);
		
		int cmdlvl = -1;
		if(userlevelneeded.equalsIgnoreCase("admin")) {
			cmdlvl = 3;
		} else if(userlevelneeded.equalsIgnoreCase("mod")) {
			cmdlvl = 2;
		} else if(userlevelneeded.equalsIgnoreCase("user")) {
			cmdlvl = 1;
		} else if(userlevelneeded.equalsIgnoreCase("all")) {
			cmdlvl = 0;
		} else {
			cmdlvl = -1;
		}

		
		if(usrlvl >= cmdlvl && cmdlvl != -1) {
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
			/*if(checking404 == true) {
				return "I am currently checking for dead images and deleting them. I am primitive " +
						"so please be patient and try again in a bit";
			}*/ // I don't think I'm primitive anymore. Marked for Deletion
			
			FileInputStream fstream = new FileInputStream(args[2]);
			DataInputStream dstream = new DataInputStream(fstream);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(dstream));
			String line;
			int counter = 0;

			List<Links> templist = new ArrayList<Links>();
			
			if(args[1].equalsIgnoreCase("nsfw")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(!DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.NSFW, attrib[0]);
							
						if(attrib.length > 1) {
							if(attrib[1].length() > 0) {
								l.setArgs(attrib[1]);
							}
						}
						templist.add(l);
						counter++;
					}
				}
			} else if(args[1].equalsIgnoreCase("brolinx")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(!DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.YOUTUBE, attrib[0]);
							
						if(attrib.length > 1) {
							if(attrib[1].length() > 0) {
								l.setArgs(attrib[1]);
							}
						}
						templist.add(l);
						counter++;
					}
				}		
			} else if(args[1].equalsIgnoreCase("lolol") || args[1].equalsIgnoreCase("funnies")) {
				while ((line = br.readLine()) != null) {
					String[] attrib = line.split(" ");
					if(!DupeCheck(attrib[0])) {
						Links l = new Links(Links.SourceCategory.FUNNYS, attrib[0]);
							
						if(attrib.length > 1) {
							if(attrib[1].length() > 0) {
								l.setArgs(attrib[1]);
							}
						}
						templist.add(l);
						counter++;
					}
				}		
			} else {
				br.close();
				return "You didn't specify what category of links you are importing. Please try 'nsfw','brolinx',or 'lolol'";
			}
			
			br.close();
			dstream.close();
			fstream.close();
			
			if(!LinkList.addAll(templist)) {
				return "Couldn't add new links to database.";
			}		
			return "Successfully Imported " + Integer.toString(counter) + " Links!";
//			if(SaveList("links")) {
//				return Integer.toString(counter) + " Links imported successfully.";
//			} else {
//				return "There was a problem saving the links to the list.";
//			}
			
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
	
	/*private Boolean SaveList(String lst) {
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
	} */ // Shouldn't need this anymore we aren't saving any lists. Marked for deletion.
	
	private String addCommand(String[] args,Boolean admin,String sender,String chan) {
		String msg = "this shouldn't be a thing in command adding.";
		switch(args[1]) {
			case "user":
				if((Boolean.parseBoolean(botSettings.getProperty("add_user_admin_only")) == true && admin == true) || Boolean.parseBoolean(botSettings.getProperty("add_user_admin_only")) == false) {
					if(args.length < 4) {
						msg = "you fucked something up bro. retry using .add user <username> <level>";
					} else {
						msg = addUser(args,chan);
					}
				} else {
					msg = "You don't have proper permission to use this command.";
				}
				break;
				
			case "nsfw":
			case "tits":
				msg = addLinks("nsfw",args,sender);
				LinkList = getLinkList();
				break;
				
			case "brolinx":
				msg = addLinks("brolinx",args,sender);
				LinkList = getLinkList();
				break;
				
			case "lulz":
			case "lolol":
			case "lawlz":
				msg = addLinks("lulz",args,sender);
				LinkList = getLinkList();
				break;
				
			default:
				msg = "That is not a proper category to add to. Please re-read the help command.";
				break;
		}
		
		return msg;
	}
	
	private String addUser(String[] args,String chan) {
		try {
			String usrlvl = args[3];
			if(usrlvl.equalsIgnoreCase("admin") || usrlvl.equalsIgnoreCase("mod") || usrlvl.equalsIgnoreCase("user")) {
				//call the function from dbf to add the user to the database
				return dbf.addUserToDB(args[2],usrlvl);
			} else {
				return "Invalid user level. use 'admin', 'mod', or 'user'";
			}
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	private String addLinks(String cat,String[] args,String sender) {
		String[] tempargs;
		if(args.length > 3) {
			tempargs = new String[]{args[2],cat,args[3],sender};
		} else {
			tempargs = new String[]{args[2],cat,sender};
		}
		
		if(check404(tempargs[0]) == false) {
			return dbf.addLinkToDB(tempargs);
		} else {
			return "This link is dead numb nuts..come on...";
		}
		
	}
	
	/*private String getLinkCount() {
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
	}*/ //using this straight from the db. Marked for deletion.
	
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
				if(BadURLs.get(strURL) >= 5) {
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
		
		return false;		
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
	
	/*private String[] getTagsFromAlias(String tag) {
		for(int i=0;i<TagAliases.size();i++) {
			for(int j=0;j<TagAliases.get(i).length;j++) {
				if(TagAliases.get(i)[j].equalsIgnoreCase(tag)) {
					return TagAliases.get(i);
				}
			}
		}
		
		return new String[] { tag };
	} */ // think i did this correctly in the database functions. Marked for deletion.

	@SuppressWarnings("unchecked")
	private void CreateInitialDatabase() {
		//create connection & statement variable for later use
		Connection conn = null;
		Statement stmnt = null;
		
		try {
			//connect or create database, if this is the first time running the code itll create the database
			conn = DriverManager.getConnection("jdbc:sqlite:brobot.db");
			
			stmnt = conn.createStatement();
			stmnt.setQueryTimeout(30);
			
			//start creating the tables *assuming first time running the code *add if not exists for all other times
			//create a table to keep track of the users
			String sql = "CREATE TABLE IF NOT EXISTS tblUsers " +
					"(adminID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
					"UserNick TEXT NOT NULL, " +
					"UserHost TEXT, " +
					"UserFlag TEXT NOT NULL, " +
					"UNIQUE(UserNick))";
			stmnt.executeUpdate(sql);
			
			// of course create a table to store the links
			sql = "CREATE TABLE IF NOT EXISTS tblLinks " +
				"(linkID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				"LinkURL TEXT NOT NULL, " +
				"LinkCat CHAR(10) NOT NULL, " +
				"LinkTags TEXT, " +
				"LinkUploader TEXT, " +
				"UNIQUE(LinkURL))";			
			stmnt.executeUpdate(sql);
			
			//this table will be to store tags "aliases"
			sql = "CREATE TABLE IF NOT EXISTS tblTags " +
					"(TagsID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
					"TagNickname TEXT NOT NULL, " +
					"Tags TEXT NOT NULL, " +
					"UNIQUE(TagNickname))";
			stmnt.executeUpdate(sql);
					
			//add master user to the user table
			sql = "INSERT OR IGNORE INTO tblUsers (UserNick,UserFlag) VALUES (?,?);";
			PreparedStatement pstmnt = conn.prepareStatement(sql);
			pstmnt.setString(1, botSettings.getProperty("owner_nick").toString());
			pstmnt.setString(2, "owner");
			pstmnt.execute();
			
			//add all the tag aliases to the database
			sql = "INSERT OR IGNORE INTO tblTags (TagNickname, Tags) VALUES (?,?);";
			pstmnt = conn.prepareStatement(sql);
			pstmnt.setString(1, "tits");
			pstmnt.setString(2, "tits,boobs,boobies,titties,jugs,knockers,rack");
			pstmnt.execute();
			
			pstmnt.setString(1, "vagina");
			pstmnt.setString(2, "vagina,vag,pussy,twat");
			pstmnt.execute();
			
			pstmnt.setString(1, "big boobs");
			pstmnt.setString(2, "big.boobs,big.tits,big.titties,big.boobies");
			pstmnt.execute();
			
			pstmnt.setString(1, "asshole");
			pstmnt.setString(2, "asshole,butthole,brown.star,brownstar");
			pstmnt.execute();
			
			pstmnt.setString(1, "small boobs");
			pstmnt.setString(2, "small.boobs,small.tits,small.titties,small.boobies");
			pstmnt.execute();
			
			stmnt.close();
			pstmnt.close();
			conn.close();
			
		} catch(SQLException se){
			System.out.println(se.getMessage());
		}
		
		//imports old links file into the database for anybody upgrading from a new version
		File f = new File("links.txt");
		if(f.exists() && f.length() != 0) {
			try{
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream instream = new ObjectInputStream(fis);
				LinkList = (ArrayList<Links>) instream.readObject();
				instream.close();
				fis.close();
				
				conn = DriverManager.getConnection("jdbc:sqlite:brobot.db");
				
				PreparedStatement pstmnt = conn.prepareStatement("INSERT INTO " +
						"tblLinks(LinkURL,LinkCat,LinkTags,LinkUploader) VALUES(?,?,?,?);");
				for(Links l:LinkList) {
					pstmnt.setString(1, l.getLink());
					pstmnt.setString(2, l.returnCatString());
					pstmnt.setString(3, l.getArgs());
					pstmnt.setString(4, l.getSubmitter());
					pstmnt.execute();
				}
				
				
				pstmnt.close();
				conn.close();
				
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException se) {
				se.printStackTrace();
			} finally {
				f.delete();
			}
					
		} 
		
		// Load users and levels from Users.txt to the database for the new version
		// then delete the file to clean up the folder
		f = new File("users.txt");
		if(f.exists() && f.length() != 0) {
			try{
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream instream = new ObjectInputStream(fis);
				UserLevels = (Map<String,Command.Flags>) instream.readObject();
				instream.close();
				fis.close();
				
				conn = DriverManager.getConnection("jdbc:sqlite:brobot.db");
				
				PreparedStatement pstmnt = conn.prepareStatement("INSERT INTO " +
						"tblUsers(UserNick,UserFlag) VALUES(?,?);");
				for(Map.Entry<String,Command.Flags> e : UserLevels.entrySet()) {
					//grab keys then grab values from map and then add to args
					pstmnt.setString(1, e.getKey().toString());
					pstmnt.setString(2, e.getValue().toString().toLowerCase());
					pstmnt.execute();
				}
				
				
				pstmnt.close();
				conn.close();
						
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException se) {
				se.printStackTrace();
			} finally {
				f.delete();
			}
					
		}
	}
	
	private List<Links> getLinkList() {
		List<Links> TempList = new ArrayList<Links>();
		try {
			ResultSet rs = dbf.getLinksFromDB();
			while(rs.next()) {
				Links l;
				if(rs.getString(3).equalsIgnoreCase("nsfw")) {
					l = new Links(Links.SourceCategory.NSFW,rs.getString(2),rs.getInt(1));
					if(rs.getString(4) != null) {
						l.setArgs(rs.getString(4));
					}
					l.setSubmitter(rs.getString(5));
					TempList.add(l);
				} else if(rs.getString(3).equalsIgnoreCase("brolinx")) {
					l = new Links(Links.SourceCategory.YOUTUBE,rs.getString(2),rs.getInt(1));
					if(rs.getString(4) != null) {
						l.setArgs(rs.getString(4));
					}
					l.setSubmitter(rs.getString(5));
					TempList.add(l);
				} else if(rs.getString(3).equalsIgnoreCase("funnies")) {
					l = new Links(Links.SourceCategory.FUNNYS,rs.getString(2),rs.getInt(1));
					if(rs.getString(4) != null) {
						l.setArgs(rs.getString(4));
					}
					l.setSubmitter(rs.getString(5));
					TempList.add(l);
				} else {
					System.out.print("There is an error in getLinkList() with the control of flow");
				}
			}
			
			return TempList;
		} catch(SQLException se) {
			System.out.print(se.getMessage());
		}
		
		return null;
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