import java.util.List;
import java.util.Random;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;



public class dbFunctions {
	Connection conn = null;
	
	public dbFunctions() {
		//activate the database driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch(ClassNotFoundException cnfe) {
			System.out.print(cnfe.getMessage());
		}
		
		//initiate the connection string
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:brobot.db");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getLinkCount() {
		int nsfw = 0;
		int brolinx = 0;
		int funnies = 0;
		
		try {			
			Statement stmnt = conn.createStatement();
			
			ResultSet rs = stmnt.executeQuery("SELECT COUNT(*) FROM tblLinks WHERE LinkCat = 'nsfw'");
			nsfw = rs.getInt(1);
			
			rs = stmnt.executeQuery("SELECT COUNT(*) FROM tblLinks WHERE LinkCat = 'brolinx'");
			brolinx = rs.getInt(1);
			
			rs = stmnt.executeQuery("SELECT COUNT(*) FROM tblLinks WHERE LinkCat = 'funnies'");
			funnies = rs.getInt(1);
			
			stmnt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return "There are " + Integer.toString(nsfw) + " NSFW links, " + Integer.toString(brolinx) + 
				" Brolinx links, and " + Integer.toString(funnies) + " Funnies links.";
	}
	
	public ResultSet getLinksFromDB() {
		try {
			Statement stmnt = conn.createStatement();
			
			ResultSet rs = stmnt.executeQuery("SELECT * FROM tblLinks");
			return rs;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int getUserLevel(String user) {
		try {
			PreparedStatement pstmnt = conn.prepareStatement("SELECT UserFlag FROM tblUsers WHERE UserNick = ? COLLATE NOCASE");
			pstmnt.setString(1, user);
			ResultSet rs = pstmnt.executeQuery();
			
			if(rs.getString(1).equalsIgnoreCase("owner")) {
				return 4;
			} else if(rs.getString(1).equalsIgnoreCase("admin")) {
				return 3;
			} else if(rs.getString(1).equalsIgnoreCase("mod")) {
				return 2;
			} else if(rs.getString(1).equalsIgnoreCase("user")) {
				return 1;
			} else {
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public String[] getTagReferences(String tag) {
		try {
			PreparedStatement pstmnt = conn.prepareStatement("SELECT Tags FROM tblTags WHERE Tags LIKE ? COLLATE NOCASE");
			pstmnt.setString(1,"%"+tag+"%");
			ResultSet rs = pstmnt.executeQuery();
			
			while(rs.next()) {
				return rs.getString(1).split(",");
			}
			
			return new String[] { tag };
		} catch(SQLException se) {
			se.printStackTrace();
		}
		
		return null;
	}
	
	public String addUserToDB(String name,String level) {
		try {
			PreparedStatement pstmnt = conn.prepareStatement("INSERT OR IGNORE INTO tblUsers(UserNick,UserFlag) VALUES (?,?);");
			pstmnt.setString(1, name);
			pstmnt.setString(2, level);
			
			pstmnt.executeUpdate();
			return "User added.";
			
		} catch (SQLException se) {
			se.printStackTrace();
		}
		
		return "There was an error adding user to the database.";
	}
	public String addLinkToDB(String[] args) {
		try {
			PreparedStatement pstmnt;
			if(args.length > 3) {
				pstmnt = conn.prepareStatement("INSERT OR IGNORE INTO tblLinks(LinkURL,LinkCat,LinkTags,LinkUploader) VALUES (?,?,?,?);");
				pstmnt.setString(1, args[0]);
				pstmnt.setString(2, args[1]);
				pstmnt.setString(3, args[2]);
				pstmnt.setString(4, args[3]);
			} else {
				pstmnt = conn.prepareStatement("INSERT OR IGNORE INTO tblLinks(LinkURL,LinkCat,LinkUploader) VALUES (?,?,?);");
				pstmnt.setString(1, args[0]);
				pstmnt.setString(2, args[1]);
				pstmnt.setString(3, args[2]);
			}
			
			pstmnt.executeUpdate();
			if(args[1].equals("nsfw")) {
				return "Future fappage added!";
			} else if(args[2].equals("brolinx")) {
				return "Broin' Out...SUCCESSFUL!";
			} else {
				return "You made some high person extremely happy!";
			}
			
		} catch(SQLException se) {
			se.printStackTrace();
		}
		
		return "This is quite embarrassing. We have programming issues. Damn you Dippingsauce!";
	}
	
	public void removeDeadLinks(List<Integer> dIDs) {
		try {
			PreparedStatement pstmnt = conn.prepareStatement("DELETE FROM tblLinks WHERE linkID = ?");
			for(int i:dIDs) {					
				pstmnt.setInt(1, i);
				pstmnt.executeUpdate();	
			}
		} catch(SQLException se) {
			se.printStackTrace();
		}
	}
	
	public Boolean editLinks(int lID, String[] args, String ltags) {
		try {
			switch(args[2].toString()) {
				case "tags":
					if(args[3].equals("add")) {
						//need to bring over current tags in a variable to concat with new tag(s)
						PreparedStatement pstmnt = conn.prepareStatement("UPDATE tblLinks SET LinkTags = ? WHERE linkID = ?");					
						pstmnt.setString(1, ltags+","+args[4]);
						pstmnt.setInt(2,lID);
						pstmnt.executeUpdate();
					} else if(args[3].equals("change")) {
						PreparedStatement pstmnt = conn.prepareStatement("UPDATE tblLinks SET LinkTags = ? WHERE linkID = ?");					
						pstmnt.setString(1, args[4]);
						pstmnt.setInt(2,lID);
						pstmnt.executeUpdate();
					}
					break;
					
				case "category":
					if(args[3].equalsIgnoreCase("funnies") || args[3].equalsIgnoreCase("nsfw") || args[3].equalsIgnoreCase("brolinx")) {
						PreparedStatement pstmnt = conn.prepareStatement("UPDATE tblLinks SET LinkCat = ? WHERE linkID = ?");					
						pstmnt.setString(1, args[3]);
						pstmnt.setInt(2,lID);
						pstmnt.executeUpdate();
					} 
					break;
			}
			
			return true;
		} catch(SQLException se) {
			se.printStackTrace();
		}
		return false;
	}
	
	/*
	 *  Decided to leave this up to the list that is preloaded on startup
	 * 
	 * public String getRandLink(String cat,String tags) {
		try {		
			PreparedStatement pstmnt = conn.prepareStatement("SELECT LinkURL,LinkTags FROM tblLinks WHERE LinkCat = ?");
			pstmnt.setString(1, cat);
			ResultSet rs = pstmnt.executeQuery();
			
			ArrayList<String> temp = new ArrayList<String>();
			
			while(rs.next()) {
				//output of this should look like 'LINK TAGS,TAGS,TAGS' so we will split the link from the tags
				//using split(" ")
				temp.add(rs.getString(1) + " " + rs.getString(2));
			}
			
			//check if user specified tags or not
			if(tags != null) {
				//yes they did
				//now check if the tag is an including or excluding tag
				if(tags.startsWith("!")) {
					//get all links that DON'T contain this tag
				} else {
					//get all links containing this tag
				}
			} else {
				//no they didn't
				Random rand = new Random();
				if(!temp.isEmpty()) {
					return temp.get(rand.nextInt(temp.size())).toString().split(" ")[0];
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		
		return "Unfortunately, I couldn't locate any links that were up your alley.";
	}*/
}
