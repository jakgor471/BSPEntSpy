package bspentspy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Undo {
	private ArrayList<ArrayList<UndoEntry>> stack;
	private int currLevel;
	private boolean activeUndo;
	private boolean currEmpty;

	private static Undo instance = new Undo();
	private static ArrayList<ActionListener> onUndo = new ArrayList<ActionListener>();
	private static ArrayList<ActionListener> onRedo = new ArrayList<ActionListener>();
	private static ArrayList<ActionListener> onUpdate = new ArrayList<ActionListener>();

	private Undo() {
		stack = new ArrayList<ArrayList<UndoEntry>>();
		currLevel = -1;
		activeUndo = false;
		currEmpty = true;
	}

	public static Undo getInstance() {
		return instance;
	}

	public static boolean isActiveUndo() {
		return instance.activeUndo;
	}

	public static boolean canUndo() {
		return instance.currLevel > -1;
	}

	public static boolean canRedo() {
		return instance.currLevel + 1 < instance.stack.size();
	}

	public static void undo() {
		instance.currLevel = Math.min(instance.currLevel, instance.stack.size() - 1);
		ArrayList<UndoEntry> current = getCurrentUndo();
		if (instance.activeUndo || current == null)
			return;

		ListIterator<UndoEntry> it = current.listIterator(current.size());
		while (it.hasPrevious()) {
			it.previous().undo();
		}
		--instance.currLevel;

		for (ActionListener al : onUndo) {
			al.actionPerformed(new ActionEvent(instance, instance.currLevel + 1, "undo"));
		}
	}

	public static void redo() {
		instance.currLevel = Math.min(instance.currLevel + 1, instance.stack.size());
		ArrayList<UndoEntry> current = getCurrentUndo();
		if (instance.activeUndo || current == null)
			return;

		Iterator<UndoEntry> it = current.iterator();
		while (it.hasNext()) {
			it.next().redo();
		}

		for (ActionListener al : onRedo) {
			al.actionPerformed(new ActionEvent(instance, instance.currLevel, "redo"));
		}
	}

	public static void addUndoListener(ActionListener al) {
		onUndo.add(al);
	}

	public static void removeUndoListener(ActionListener al) {
		onUndo.remove(al);
	}

	public static void addRedoListener(ActionListener al) {
		onRedo.add(al);
	}

	public static void removeRedoListener(ActionListener al) {
		onRedo.remove(al);
	}

	public static void addUpdateListener(ActionListener al) {
		onUpdate.add(al);
	}

	public static void removeUpdateListener(ActionListener al) {
		onUpdate.remove(al);
	}

	public static void create() {
		if (instance.activeUndo)
			return;
		instance.activeUndo = true;
		instance.currEmpty = true;

		instance.currLevel = Math.min(instance.currLevel + 1, instance.stack.size());
		for (int i = instance.stack.size() - 1; i >= instance.currLevel; --i) {
			instance.stack.remove(i);
		}

		ArrayList<UndoEntry> current = new ArrayList<UndoEntry>();
		current.add(new UndoEntry());
		instance.stack.add(current);
	}

	public static void pause() {
		instance.activeUndo = false;
	}

	public static void resume() {
		instance.activeUndo = true;
	}

	public static void finish() {
		instance.activeUndo = false;
		ArrayList<UndoEntry> current = getCurrentUndo();
		instance.currEmpty = instance.currEmpty && current.get(current.size() - 1).isEmpty();

		if (instance.currEmpty) {
			instance.stack.remove(instance.currLevel);
			--instance.currLevel;
		}

		for (ActionListener al : onUpdate) {
			al.actionPerformed(new ActionEvent(instance, instance.currLevel, "update"));
		}
	}

	public static void setTarget(Object target) {
		ArrayList<UndoEntry> current = getCurrentUndo();
		if (!instance.activeUndo || current == null)
			return;

		if (current.get(current.size() - 1).target != target && current.get(current.size() - 1).target != null) {
			UndoEntry newue = new UndoEntry();
			newue.target = target;
			current.add(newue);

			instance.currEmpty = instance.currEmpty && current.get(current.size() - 1).empty;
		}

		current.get(current.size() - 1).target = target;
	}

	public static void addCommand(Command c) {
		ArrayList<UndoEntry> current = getCurrentUndo();
		if (!instance.activeUndo || current == null)
			return;

		current.get(current.size() - 1).addCommand(c);
	}

	public static boolean isEmpty() {
		return instance.stack.size() < 1;
	}

	public static String printStack() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < instance.stack.size(); ++i) {
			if (i == instance.currLevel)
				sb.append("(CURRENT) ");
			sb.append("LEVEL ").append(i).append("\nENTRIES:\n");
			ArrayList<UndoEntry> entries = instance.stack.get(i);

			for (UndoEntry ue : entries) {
				sb.append("\t").append(ue.toString("\t")).append("\n");
			}
		}

		System.out.println(sb.toString());
		return sb.toString();
	}

	public static String printStackHTML() {
		String str = printStack();

		final HashMap<String, String> repMap = new HashMap<String, String>();
		StringBuffer sb = new StringBuffer();
		repMap.put("\t", "&emsp;");
		repMap.put("\n", "<br>");

		Pattern p = Pattern.compile("\t|\n");
		Matcher match = p.matcher(str);

		while (match.find()) {
			match.appendReplacement(sb, repMap.getOrDefault(match.group(), ""));
		}

		match.appendTail(sb);
		return sb.toString();
	}

	private static ArrayList<UndoEntry> getCurrentUndo() {
		if (instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return null;
		return instance.stack.get(instance.currLevel);
	}

	public static interface Command {
		public Command join(Command previous);

		public void undo(Object target);

		public void redo(Object target);

		public String toString(String indent);

		public int size();
	}

	private static class UndoEntry {
		private ArrayDeque<Command> commands;
		private Object target;
		private boolean empty;

		public UndoEntry() {
			commands = new ArrayDeque<Command>();
			target = null;
			empty = true;
		}

		public int size() {
			int size = 0;
			for (Command c : commands) {
				size += c.size();
			}

			return size;
		}

		public void addCommand(Command toAdd) {
			empty = false;
			if (commands.size() > 0 && commands.peek().getClass() == toAdd.getClass()) {
				toAdd = toAdd.join(commands.peek());
			}

			if (toAdd != null)
				commands.add(toAdd);
		}

		public void undo() {
			Iterator<Command> it = commands.descendingIterator();

			while (it.hasNext()) {
				it.next().undo(target);
			}
		}

		public void redo() {
			Iterator<Command> it = commands.iterator();

			while (it.hasNext()) {
				it.next().redo(target);
			}
		}

		public boolean isEmpty() {
			return empty && target != null;
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();

			sb.append("TARGET: ").append(target).append("\n");

			Iterator<Command> it = commands.iterator();
			while (it.hasNext()) {
				sb.append(indent).append(it.next().toString(indent + "\t"));
			}

			return sb.toString();
		}
	}
}
