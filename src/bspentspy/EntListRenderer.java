package bspentspy;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;

public class EntListRenderer extends DefaultListCellRenderer {
	static ImageIcon pointIcon;
	static ImageIcon modelIcon;
	static ImageIcon brushIcon;
	static ImageIcon triggerIcon;
	static ImageIcon lightIcon;
	static ImageIcon nodeIcon;
	static ImageIcon soundIcon;
	static ImageIcon itemIcon;
	static ImageIcon decalIcon;
	static ImageIcon logicIcon;
	static ImageIcon npcIcon;
	static ImageIcon weaponIcon;
	static ImageIcon axisIcon;
	static ImageIcon scrseqIcon;
	static HashMap<String, ImageIcon> iconMap;

	static {
		iconMap = new HashMap();

		pointIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/point.png"));
		modelIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/model.png"));
		brushIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/brush.png"));
		triggerIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/trigger.png"));
		lightIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/light.png"));
		nodeIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/node.png"));
		soundIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/sound.png"));
		itemIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/item.png"));
		decalIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/decal.png"));
		logicIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/logic.png"));
		npcIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/npc.png"));
		weaponIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/weapon.png"));
		axisIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/axis.png"));
		scrseqIcon = new ImageIcon(EntListRenderer.class.getResource("/images/newicons/scriptedsequence.png"));

		iconMap.put("light", lightIcon);
		iconMap.put("light_dynamic", lightIcon);
		iconMap.put("light_spot", lightIcon);
		iconMap.put("point_spotlight", lightIcon);
		iconMap.put("light_environment", lightIcon);
		iconMap.put("ambient_generic", soundIcon);
		iconMap.put("env_soundscape", soundIcon);
		iconMap.put("env_soundscape_triggerable", soundIcon);
		iconMap.put("env_soundscape_proxy", soundIcon);
		iconMap.put("infodecal", decalIcon);
		iconMap.put("info_overlay", decalIcon);
		iconMap.put("keyframe_rope", axisIcon);
		iconMap.put("move_rope", axisIcon);
		iconMap.put("scripted_sequence", scrseqIcon);
	}

	public EntListRenderer() {
	}

	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean hasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
		this.setIcon(nodetype((Entity) value));
		return this;
	}

	protected ImageIcon nodetype(Entity value) {
		String cls = value.getKeyValue("classname");
		if (EntListRenderer.iconMap.containsKey(cls)) {
			return iconMap.get(cls);
		}

		if (value.getKeyValue("model").startsWith("*")) {
			if (cls.indexOf("trigger") > -1)
				return triggerIcon;

			return brushIcon;
		}

		if (cls.indexOf("node") > -1)
			return nodeIcon;

		if (cls.indexOf("item_") > -1)
			return itemIcon;

		if (cls.indexOf("logic_") > -1)
			return logicIcon;

		if (cls.indexOf("weapon_") > -1)
			return weaponIcon;

		if (cls.indexOf("npc_") > -1 && cls.indexOf("_make") < 0)
			return npcIcon;

		if (!value.getKeyValue("model").equals("")) {
			return modelIcon;
		}

		return pointIcon;
	}
}
