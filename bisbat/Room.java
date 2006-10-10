package bisbat;

import java.util.ArrayList;
import java.util.LinkedList;

public class Room {
	
	public Room(String rTitle, String rDescription, String rExits, String beingsAndObjects) {
		title = rTitle;
		description = rDescription;
		String[] exitList = rExits.split(", ");
		for(String s : exitList) {
			exits.add(new Exit(s));
		}
			
		
	}
	public ArrayList<Exit> exits = new ArrayList<Exit>();
	public String description = new String();
	public String title = new String();
	public ArrayList<Being> beings = new ArrayList<Being>();
	public ArrayList<Item> items = new ArrayList<Item>();
	
	
	/**
	 * Returns true if this room is indistinguishable from given room.
	 */
	public boolean matchesRoom(Room room) {
		/*System.out.println("Findy room:");
		room.print();
		System.out.println("This room:");
		print();
		*/
		if (!title.equals(room.title)) {	
			return false;
		}
		if (!description.equals(room.description)) {
			return false;
		}
		if (exits.size() != room.exits.size()) {
			return false;
		}
		Exit temp;
		for (Exit e : exits) {
			temp = room.getExit(e.direction);
			if (temp == null) {
				return false;
			}
			if (e.isDoor != temp.isDoor) {
				return false;
			}
		}
		
		
		
		/*
		 Should check adjacent rooms for difference too
		 Possibly a depth limited breadth first search
		 Without this, Bisbat will probably fail when presented with identical rooms
		*/
		// can't find a difference
		return true; 
	}
	
	/** 
	 * updates information about the current state of this room 
	 * using the given room.
	 * 
	 * @param room A room that has the update information about doors, items, and beings.
	 */
	public boolean update(Room room) {
		if (!matchesRoom(room)) return false; //rooms don't match
		beings = room.beings;
		items = room.items;
		Exit temp;
		for (Exit e : exits) {
			temp = room.getExit(e.direction);
			if (e.isDoor) {
				e.isDoorOpen = temp.isDoorOpen;
			}
		}
		return true; 
	}
	
	
	/**
	 * prints a string representation to system.out 
	 * !TODO this is a debugging method
	 */
	public void print() {
		System.out.println(toString());
		System.out.print("\n");		
	}
	public String toString() {
		String result = "TITLE:" + title + "\nDESCRIPTION:" + description + "\n";
		for(Exit e : exits) {
			result += e.getCommand() + "\t";
		}
		return result;
	}
	
	Exit getRandomUnexploredExit() {
		ArrayList<Exit> une = getUnexploredExits();

		
		if(une.size() == 0) {
			// Now we have finished this room, we need to go to another room that has unexplored exits.
			System.out.println("Finished exploring this room.");
			return null;
		}
		
		return une.get((int)Math.round(Math.random() * (une.size() -1)));
		//return getExit((int)Math.round(Math.random() * (exits.size() - 1)));
		
	}
	Exit getExit(int i) {
		return exits.get(i);
		
	}
	Exit getExit(String s) {
		for(Exit e : exits) {
			if(e.direction.equals(s)) {
				return e;
			}
		}
		return null;
	}
	void printTree(String tabs, ArrayList<Room> exploredRooms) {
		if(!exploredRooms.contains(this)) {
			exploredRooms.add(this);
			System.out.println(tabs + this.title + "(" + getUnexploredExits().size() + ")");
			for(Exit e: exits) {
				if(e.nextRoom != null) {
					e.nextRoom.printTree(tabs + "  ", exploredRooms);
				}
			}
			
		}
	}
	void printTree() {	
		ArrayList<Room> explored = new ArrayList<Room>();
		printTree("", explored);
	}

	public ArrayList<Exit> getUnexploredExits() {
		ArrayList<Exit> une = new ArrayList<Exit>();
		for(Exit e : exits) {
			if(e.nextRoom == null) {
				une.add(e);
			}
		}
		return une;
	}
	

	
}
