package bspentspy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Undo {
	private ArrayList<UndoEntry> stack;
	private int currLevel;
	
	private static Undo instance = new Undo();
	
	private Undo() {
		stack = new ArrayList<UndoEntry>();
		currLevel = -1;
	}
	
	public static Undo getInstance() {
		return instance;
	}
	
	public static void undo() {
		if(instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return;
		
		instance.stack.get(instance.currLevel--).undo();
	}
	
	public static void redo() {
		if(instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return;
		
		instance.stack.get(instance.currLevel++).redo();
	}
	
	public static void create() {
		++instance.currLevel;
		
		for(int i = instance.stack.size() - 1; i >= instance.currLevel; --i) {
			instance.stack.remove(i);
		}
		instance.stack.add(new UndoEntry());
	}
	
	public static void setTarget(Object target) {
		if(instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return;
		
		instance.stack.get(instance.currLevel).target = target;
	}
	
	public static void addCommand(Command c) {
		if(instance.currLevel < 0 || instance.currLevel >= instance.stack.size())
			return;
		
		instance.stack.get(instance.currLevel).addCommand(c);
	}
	
	public static interface Command{
		public Command join(Command other);
		public void undo(Object target);
		public void redo(Object target);
	}
	
	private static class UndoEntry{
		private ArrayDeque<Command> commands;
		private Object target;
		
		public UndoEntry() {
			commands = new ArrayDeque<Command>();
		}
		
		public void addCommand(Command toAdd) {
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
			Iterator<Command> it = commands.descendingIterator();
			
			while(it.hasNext()) {
				it.next().redo(target);
			}
		}
	}
}
