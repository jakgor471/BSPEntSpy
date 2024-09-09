package bspentspy;

public class EntityCubemap extends Entity {
	int[] cubemapOrigin;
	
	public EntityCubemap() {
		cubemapOrigin = new int[] {0, 0, 0};
		setKeyVal("classname", "env_cubemap");
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
