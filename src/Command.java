public class Command {
		
		public enum Flags { ALL, ADMIN, USER, MOD }
		@SuppressWarnings("unused")
		private static final long serialVersionUID = 1L;
		private String Name;
		private String Description;
		private Flags UserFlags;
		private Boolean Hidden;
		private Boolean Enabled;
		
		public Command(String _name, String _desc, Flags _uf, Boolean _hidden, Boolean _enabled) {
			Name = _name;
			Description = _desc;
			UserFlags = _uf;
			Hidden = _hidden;
			Enabled = _enabled;
		}
		
		public Command() {
			
		}
		
		public String getCmdName() {
			return Name;
		}
		public void setCmdName(String _name) {
			Name = _name;
		}
		
		public String getDescription() {
			return Description;
		}
		public void setDescription(String _desc) {
			Description = _desc;
		}
		
		public Flags getUserFlags() {
			return UserFlags;
		}
		public void setUserFlags(Flags _flags) {
			UserFlags = _flags;
		}
		
		public Boolean isHidden() {
			return Hidden;
		}
		public void setHidden(Boolean _hid) {
			Hidden = _hid;
		}
		
		public Boolean isEnabled() {
			return Enabled;
		}
		public void setEnabled(Boolean _enabled) {
			Enabled = _enabled;
		}
	
 }

