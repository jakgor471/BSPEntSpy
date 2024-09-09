package bspentspy;

public class EntityStaticProp extends Entity {
	short[] leaves;
	byte flags;
	
	public EntityStaticProp() {
		setKeyVal("classname", "prop_static");
	}
	
	public void delKeyVal(String k) {
		return;
	}

	public void delKeyValById(int uniqueId) {
		return;
	}

	public void delKeyVal(int i) {
		return;
	}
	
	public void changeKey(String from, String to) {
		return;
	}

	public void changeKey(Integer uniqueId, String to) {
		return;
	}
	
	public boolean canBeRemoved() {
		return false;
	}
	
	public boolean shouldSave() {
		return false;
	}
}
