import java.util.ArrayList;
import java.util.List;
import java.util.Random;



final class AI {
	List<String> ReceiveStrings = new ArrayList<String>();
	List<String> ResponseStrings = new ArrayList<String>();
	List<String> GreetingStrings = new ArrayList<String>();
	List<String> AdditionalStrings = new ArrayList<String>();
	
	public AI() {
		ReceiveStrings.add("get");
		ReceiveStrings.add("grab");
		ReceiveStrings.add("fetch");
		ReceiveStrings.add("hook");
		ReceiveStrings.add("send");
		ReceiveStrings.add("give");
		
		ResponseStrings.add("here you go");
		ResponseStrings.add("check this out");
		ResponseStrings.add("enjoy");
		ResponseStrings.add("peep this shit");
		ResponseStrings.add("hopefully this is good enough");
		
		GreetingStrings.add("What's up?");
		GreetingStrings.add("Sup?");
		GreetingStrings.add("How may I help you?");
		GreetingStrings.add("What do you want?");
		GreetingStrings.add("What's Shakin'?");
		
		AdditionalStrings.add("with");
		AdditionalStrings.add("containing");
		AdditionalStrings.add("consisting");
		AdditionalStrings.add("has");
	}
	
	public String grabResponse() {
		return ResponseStrings.get(new Random().nextInt(ResponseStrings.size()));
	}
	
	public String grabGreeting() {
		return GreetingStrings.get(new Random().nextInt(GreetingStrings.size()));
	}
	
	public Boolean isReceiveString(List<String> strTest) {
		Boolean bTest = false;
		for(String s: ReceiveStrings) {
			if(strTest.contains(s)) {
				bTest = true;
			}
		}
		return bTest;
	}
	
	public Integer getReceiveIndex(List<String> strTest) {
		Integer intTemp = -1; 
		for(String s: ReceiveStrings) {
			if(strTest.contains(s)) {
				intTemp = strTest.indexOf(s);
			}
		}
		return intTemp;
	}

	public Boolean isAdditionString(List<String> strTest) {
		Boolean bTest = false;
		if(strTest.isEmpty()) {
			return bTest;
		}
		for(String s: AdditionalStrings) {
			if(strTest.contains(s)) {
				bTest = true;
			}
		}

		return bTest;
	}
}
