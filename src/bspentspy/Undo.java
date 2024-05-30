package bspentspy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

public class Undo {
	private ArrayList<ArrayList<UndoEntry>> stack;
	private int currLevel;
	private boolean activeUndo;
	private boolean currEmpty;
	
	private static Undo instance = new Undo();
	
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
	
	public static void undo() {
		instance.currLevel = Math.min(instance.currLevel, instance.stack.size() - 1);
		ArrayList<UndoEntry> current = getCurrentUndo();
		if(instance.activeUndo || current == null)
			return;
		
		ListIterator<UndoEntry> it = current.listIterator(current.size());
		while(it.hasPrevious()) {
			it.previous().undo();
		}
		--instance.currLevel;
	}
	
	public static void redo() {
		instance.currLevel = Math.min(instance.currLevel + 1, instance.stack.size());
		ArrayList<UndoEntry> current = getCurrentUndo();
		if(instance.activeUndo || current == null)
			return;
		
		Iterator<UndoEntry> it = current.iterator();
		while(it.hasNext()) {
			it.next().redo();
		}
	}
	
	public static void create() {
		if(instance.activeUndo)
			return;
		instance.activeUndo = true;
		instance.currEmpty = true;
		
		instance.currLevel = Math.min(instance.currLevel + 1, instance.stack.size());
		for(int i = instance.stack.size() - 1; i >= instance.currLevel; --i) {
			instance.stack.remove(i);
		}
		
		ArrayList<UndoEntry> current = new ArrayList<UndoEntry>();
		current.add(new UndoEntry());
		instance.stack.add(current);
	}
	
	public static void finish() {
		instance.activeUndo = false;
		ArrayList<UndoEntry> current = getCurrentUndo();
		instance.currEmpty = instance.currEmpty && current.get(current.size() - 1).empty;
		
		if(instance.currEmpty) {
			instance.stack.remove(instance.currLevel);
			--instance.currLevel;
		}
	}
	
	public static void setTarget(Object target) {
		ArrayList<UndoEntry> current = getCurrentUndo();
		if(!instance.activeUndo || current == null)
			return;
		
		if(current.get(current.size() - 1).target != target) {
			UndoEntry newue = new UndoEntry();
			newue.target = target;
			current.add(newue);
			
			instance.currEmpty = instance.currEmpty && current.get(current.size() - 1).empty;
		}
	}
	
	public static void addCommand(Command c) {
		ArrayList<UndoEntry> current = getCurrentUndo();
		if(!instance.activeUndo || current == null)
			return;
		
		current.get(current.size() - 1).addCommand(c);
	}
	
	private static ArrayList<UndoEntry> getCurrentUndo(){
		if(instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return null;
		return instance.stack.get(instance.currLevel);
	}
	
	public static interface Command{
		public Command join(Command previous);
		public void undo(Object target);
		public void redo(Object target);
	}
	
	public static class DummyCommand implements Command{
		ArrayDeque<String> things = new ArrayDeque<String>();
		
		public Command join(Command other) {
			DummyCommand otherdc = (DummyCommand)other;
			otherdc.things.addAll(things);
			
			return null;
		}
		
		public void undo(Object target) {
			Iterator it = things.descendingIterator();
			
			System.out.println("==== UNDO ====");
			while(it.hasNext()) {
				System.out.println("UNDO: " + it.next());
			}
		}
		
		public void redo(Object target) {
			Iterator it = things.iterator();
			
			System.out.println("==== REDO ====");
			while(it.hasNext()) {
				System.out.println("REDO: " + it.next());
			}
		}
	}
	
	private static class UndoEntry{
		private ArrayDeque<Command> commands;
		private Object target;
		public boolean empty;
		
		public UndoEntry() {
			commands = new ArrayDeque<Command>();
			target = null;
			empty = true;
		}
		
		public void addCommand(Command toAdd) {
			empty = false;
			if(commands.size() > 0 && commands.peek().getClass() == toAdd.getClass()) {
				toAdd = toAdd.join(commands.peek());
			}
			
			if(toAdd != null)
				commands.add(toAdd);
		}
		
		public void undo() {
			Iterator<Command> it = commands.descendingIterator();
			
			while(it.hasNext()) {
				it.next().undo(target);
			}
		}
		
		public void redo() {
			Iterator<Command> it = commands.iterator();
			
			while(it.hasNext()) {
				it.next().redo(target);
			}
		}
	}
}
