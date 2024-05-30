package bspentspy;

import java.util.ArrayList;
import java.util.Stack;

public class Undo {
	private Stack<ArrayList<Command>> stack;
	private static Undo instance = new Undo();
	
	private Undo() {
		stack = new Stack<ArrayList<Command>>();
	}
	
	public static Undo getInstance() {
		return instance;
	}
	
	public static abstract class Command{
		
	}
}
