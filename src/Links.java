

public class Links implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum SourceCategory { NSFW,YOUTUBE,FUNNYS }
	private int LinkID;
	private String Link;
	private String args;
	private SourceCategory Cat;
	private String Submitter;
	
	public Links(SourceCategory _cat, String _link) {
		Cat = _cat;
		Link = _link;
	}
	public Links(SourceCategory _cat, String _link, int _id) {
		this(_cat,_link);
		LinkID = _id;
	}
	
	public String getLink() {
		 return Link;
	}
	public void setLink(String _link) {
		Link = _link;
	}
	
	public String getArgs() {
		return args;
	}
	public void setArgs(String _args) {
		args = _args;
	}
	
	public SourceCategory getCat() {
		return Cat;
	}
	public void setCat(SourceCategory _cat) {
		Cat = _cat;
	}
	
	public String getSubmitter() {
		return Submitter;
	}
	public void setSubmitter(String name) {
		Submitter = name;
	}
	
	public Integer getLinkID() {
		return LinkID;
	}
	
	public String returnCatString() {
		if(this.Cat == SourceCategory.FUNNYS) {
			return "funnies";
		} else if(this.Cat == SourceCategory.NSFW) {
			return "nsfw";
		} else {
			return "brolinx";
		}
	}
	
	public Boolean CheckTag(String[] tag) {	
		if(this.args != null) {
			String[] linkTags = this.args.split(",");
			for(String s:linkTags) {
				for(int i=0;i<tag.length;i++) {
					if(tag[i].equalsIgnoreCase(s)) {
						return true;
					}
				}
			}
			return false;
		}
		
		return false;
			
	}
}
