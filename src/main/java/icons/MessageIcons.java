package icons;

public enum MessageIcons {
	Information("OptionPane.informationIcon"),
	Warning("OptionPane.warningIcon"),
	Error("OptionPane.errorIcon"),
	Question("OptionPane.questionIcon");

	String value;
	MessageIcons(String value) {
		this.value = value;
	}

	public String getName() {
		return value;
	}
}
