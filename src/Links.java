public class Links implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum SourceCategory { NSFW,YOUTUBE,FUNNYS }
	private String Link;
	private String args;
	private SourceCategory Cat;
	private String Submitter;
	
	public Links(SourceCategory _cat, String _link) {
		Cat = _cat;
		Link = _link;
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
}
