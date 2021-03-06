package bisbat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

public class RoomFinder extends Thread {
	
	private LinkedList<Room> foundRooms;
	public LinkedList<String> commandList;
	private boolean failure = false;
	private Bisbat bisbat = null;
	
	public RoomFinder(Bisbat bisbat) {
		this.bisbat = bisbat;
	}
	
	public void start() {
		foundRooms = new LinkedList<Room>();
		commandList = new LinkedList<String>();
	}
	
	public void add(Room room) {
		foundRooms.add(room);
		
	}
	public void add(String command) {
		commandList.add(command);
	}
	
	public boolean isEmpty() {
		return foundRooms.isEmpty();
	}
	public Room getLastRoom() {
		if(foundRooms.isEmpty()){
			Bisbat.debug("RoomFinder.foundRooms.isEmpty = true");
			return null;
		}
		return foundRooms.getLast();
	}
	
	public Room popFirstRoom() {
		commandList.remove(0);
		return foundRooms.remove(0);
	}
	

	/**
	 * Will return the next room we have seen, or null if there was a 
	 * failure (not enough move points, door closed, etc.).
	 * @return Room we just found.
	 */
	public Room pop() {
		//Bisbat.print("Waiting for a room."); // debugger
		int count = 0;
		while(foundRooms.size() <= 0) {
			try {
				if(failure) {
					commandList.remove(0);
					failure = false;
					//Bisbat.debug("We had a problem walking somewhere!");
					return null;
				}
				/*
				 * 140 is the average time for a room (lag), we check every 70 then.
				 */
				
				Thread.sleep(10); // Optimize with semaphores? !TODO
				count++;
			} catch(InterruptedException e) {
				Bisbat.debug("InterruptedException: ... not sure what's causing this one!");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(count > 0) {
			//Bisbat.debug("Waited " + count + " loops for a room.");
		}

		if(bisbat.currentRoom == null) {
			bisbat.currentRoom = foundRooms.get(0); // look at me !TODO replace with remove(0)?
		} else {
			Room temp = searchForMatchingRoom(bisbat.currentRoom, foundRooms.get(0), 
					commandList.get(0));
			if(temp != null) {
				// System.out.println("We found a room that matched!  Replacing " + foundRooms.firstElement().title + " with " + tmp.title); // debugger
				foundRooms.remove(0);
				foundRooms.add(temp);
			} else {
				//System.out.println("We didn't find a room that matched: " + foundRooms.firstElement().title); // debugger
			}
		}
		Room tmp = popFirstRoom(); // get first does not "pop" the list
		if(tmp == null) {
			throw(new NullPointerException());
		}
		return tmp;
	}
	
	/**
	 *  Breadth-First-Search from a room matching findMe
	 *  Room needs to pass 3 tests.  It has to look like a room we know,
	 *  it has to be at least 2 away, and its spacial relativity must be less than 2.
	 */
	static public Room searchForMatchingRoom(Room indexRoom, Room findMe, String command) {
		//Bisbat.print("searching for matching room"); // debugger
		
		Vector<Room> exploredRooms = new Vector<Room>();
		Vector<Room> searchQueue = new Vector<Room>();
		ArrayList<Exit> path;
		searchQueue.add(indexRoom);
		
		while(searchQueue.size() > 0) {
			Room currentRoom = searchQueue.remove(0);
			if(!exploredRooms.contains(currentRoom)) {
				exploredRooms.add(currentRoom);
				
				//!TODO - Check for orthogonol room matches (grid areas).
				/*************** DO THIS FOR EVERY ROOM IN THE SEARCH QUEUE ***************/
				
				if(currentRoom.matchesRoom(findMe) ) {
					//System.out.println("We matched " + findMe.title + " and " + currentRoom.title + " -- BUT THIS IS WRONG!"); // debugger
					path = searchForPathBetweenRooms(indexRoom, currentRoom);
					path.add(0,new Exit(Exit.getOpposite(command)));
					if(path.size() > 2) {
						//Bisbat.debug("Using spatial relativity calculation");
						if(Exit.spatialRelativityCalculation(path) < .99) {
							//Bisbat.print("Found a room that matched."); // debugger
							return currentRoom;
						} else {
							//System.out.println("Probably was too far away."); // debugger
						}
					} else {
						//System.out.println("It wasn't far enough away to be the same."); // debugger
					}
				} else {
					//System.out.println("Didn't match because it didn't look the same."); // debugger
				}
				
				for(Exit e : currentRoom.exits) {
					if(e.nextRoom != null) {
						//System.out.println("We found another room to search");
						searchQueue.add(e.nextRoom);
					}
				}
				
				/*************** STOP DOING THIS FOR EVERY ROOM IN THE SEARCH QUEUE ***************/
			}
		}
		//Bisbat.print("Didn't find a room that matched."); // debugger
		return null;
	}
	
	
	/**
	 * Finds every room in the knowledge base that matches this given room
	 * @param indexRoom: room to start search from (knowledge base)
	 * @param findMe: room that we are searching for matches of
	 * @return: all rooms that might actually be the given room findMe
	 */
	static public Vector<Room> searchForAllMatchingRooms(Room indexRoom, Room findMe) {
		Vector<Room> result = new Vector<Room>();
		Vector<Room> exploredRooms = new Vector<Room>();
		Vector<Room> searchQueue = new Vector<Room>();
		searchQueue.add(indexRoom);
		
		while(searchQueue.size() > 0) {
			Room currentRoom = searchQueue.remove(0);
			if(!exploredRooms.contains(currentRoom)) {
				exploredRooms.add(currentRoom);
				if(currentRoom.matchesRoom(findMe) ) {
					result.add(currentRoom);
				}
				for(Exit e : currentRoom.exits) {
					if(e.nextRoom != null) {
						searchQueue.add(e.nextRoom);
					}
				}
			}
		}
		//Bisbat.debug("matching rooms: " + result.size() + " Number of considered rooms: " + exploredRooms.size());
		return result;
	}
	
	/**
	 * Finds a path to an unconfirmed room if there is an unconfirmed room.
	 * @param start: room to start search room
	 * @return: path to an unconfirmed room if one exists
	 */
	static public ArrayList<Exit> searchForPathToUnconfirmedRoom(Room start) {
		return searchForPathToUnconfirmedRoom(start, new ArrayList<Exit>(), new ArrayList<Room>());
	}
	
	/**
	 * Finds a path to an unconfirmed room if there is an unconfirmed room.
	 * @param start: Room to start looking for the path
	 * @param path: Path to start room
	 * @param explored: rooms that have been consider (eleminate repeat searches)
	 * @return: path to an unconfirmed room if one exists
	 */
	@SuppressWarnings("unchecked")
	static private ArrayList<Exit> searchForPathToUnconfirmedRoom(Room start, ArrayList<Exit> path, ArrayList<Room> explored) {
		explored.add(start);
		if(!start.isConfirmed()) {
			return path;
		} else {
			for(Exit e : start.exits) {
				if(e.nextRoom != null && !explored.contains(e.nextRoom)) {
					ArrayList<Exit> nPath = (ArrayList<Exit>)path.clone();
					nPath.add(e);
					ArrayList<Exit> temp = searchForPathToUnconfirmedRoom(e.nextRoom, nPath, explored);
					if (temp != null) {
						return temp;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Finds a path between two rooms
	 * @param start: intial room in search for destination room
	 * @param destination: destination room
	 */
	static public ArrayList<Exit> searchForPathBetweenRooms(Room start, Room destination) {
		return searchForPathBetweenRooms(start, destination, new ArrayList<Exit>(), new ArrayList<Room>());
	}
	
	/**
	 * recursive method that returns path between two rooms
	 * @param start: current room in search for destination room
	 * @param destination: destination room
	 * @param path: path from initial room to current room
	 * @param explored: list of already exlpored rooms (avoid duplicate checks)
	 */
	@SuppressWarnings("unchecked")
	static private ArrayList<Exit> searchForPathBetweenRooms(Room start, Room destination, ArrayList<Exit> path, ArrayList<Room> explored) {
		explored.add(start);
		if(start == destination) {
			return path;
		} else {
			
			for(Exit e : start.exits) {
				if(e.nextRoom != null && !explored.contains(e.nextRoom)) {
					ArrayList<Exit> nPath = (ArrayList<Exit>)path.clone();
					nPath.add(e);
					ArrayList<Exit> temp = searchForPathBetweenRooms(e.nextRoom, destination, nPath, explored);
					if (temp != null) {
						return temp;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Finds directions that have been confirmed from every room in 'rooms'.
	 * @param rooms: set of rooms we want to find common confirmed directions from
	 * @return: a list of the common directions.
	 */
	static public Vector<String> commonConfirmedDirection(Vector<Room> rooms) {
		//Bisbat.debug("entering commonConfirmedDirection");
		if (rooms == null || rooms.isEmpty()) {
			//Bisbat.debug("exiting commonConfirmedDirection at exit 1");
			return null;
		} else if (rooms.size() == 1) {
			Vector<String> result = new Vector<String>();
			//Bisbat.debug("commonConfirmedDirection line 1");
			ArrayList<Exit> commonExits = rooms.firstElement().getConfirmedExits();
			//Bisbat.debug("commonConfirmedDirection line 2 " + commonExits.size());
			for(Exit exit : commonExits) {
				result.add(exit.getDirection());
			}
			//Bisbat.debug("commonConfirmedDirection line 2 " + result.size());
			//Bisbat.debug("exiting commonConfirmedDirection at exit 2");
			return result;
		}
		Vector<String> result = new Vector<String>();
		ArrayList<Exit> exits = rooms.firstElement().getConfirmedExits();
		for(Exit exit : exits) {
			String dir = exit.getDirection();
			for(Room room : rooms) {
				Exit ex = room.getExit(dir);
				if (ex == null || !ex.isConfirmed()) {
					continue;
				}
			}
			result.add(dir);
		}
		//Bisbat.debug("exiting commonConfirmedDirection at exit 3");
		return result;
	}
	
	/**
	 * Recusively finds the longest confirmed path (or a path of length 10) that 'rooms' share.
	 * @param rooms: rooms that we want to find a confirmed path from.
	 * @return: longest confirmed path (or path of length 10) from 'rooms'.
	 */
	static public Vector<String> commonConfirmedPath (Vector<Room> rooms){
		return commonConfirmedPath(rooms, 0, new Vector<String>());
	}
	
	/**
	 * Recusively finds the longest confirmed path (or a path of length 10) that 'rooms' share.
	 * @param rooms: rooms that we want to find a confirmed path from.
	 * @param depth: how far resursively we have traveled
	 * @param path: path to this point
	 * @return: longest confirmed path (or path of length 10) from 'rooms'.
	 */
	static private Vector<String> commonConfirmedPath(Vector<Room> rooms, int depth, Vector<String> path){
		if (depth >= 10) {
			return path;
		}
		Vector<String> commonDirections = commonConfirmedDirection(rooms);
		//Bisbat.debug("Number of matching rooms: " + rooms.size());
		if (commonDirections == null || commonDirections.isEmpty()) {
			return path;
		}
		//Bisbat.debug("Number of common confirmed directions: " + commonDirections.size() + " at depth: " + depth);
		Vector<Vector<String>> allPaths = new Vector<Vector<String>>();
		for (String dir : commonDirections) {
			Vector<Room> nextRooms = new Vector<Room>();
			for (Room room : rooms) {
				if (room != null && room.getExit(dir) != null) {
					Room temp = room.getExit(dir).nextRoom;
					if (temp != null) {
						nextRooms.add(temp);
					}
				}	
			}
			Vector<String> newPath = new Vector<String>(path);
			newPath.add(dir);
			allPaths.add(commonConfirmedPath(nextRooms, depth + 1, newPath));
		}
		Vector<String> longest = new Vector<String>();
		double spatialMax = 0.0d;
		for (Vector<String> list : allPaths) {
			if (list.size() > longest.size()) {
				longest = list;
			} else if (list.size() ==  longest.size()) {
				double temp = Exit.spatialRelativityCalculation(list);
				if (temp >= spatialMax) {
					if (Math.random() >= .2) {
						//Bisbat.debug("using a higher spatial relativity path");
						longest = list;
						spatialMax = temp;
					}
				}
			}
		}
		//Bisbat.debug("Longest confirming path <= 10 is this long: " + longest.size());\
		if (longest.isEmpty()) {
			Bisbat.debug("Turns out that these rooms have no Common Confirmed Path.");
		}
		return longest;
	}
	
	public void failure() {
		failure = true;	
	}
	
}
