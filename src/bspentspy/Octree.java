package bspentspy;

import java.util.ArrayList;

class Octree<T extends Octree.IOriginThing> {
	public static final int MAX_ENTRIES = 4;
	public static final int MAX_DEPTH = 8;

	private float minx, miny, minz, size, halfsize;
	private int depth;
	private Octree<T> children[];
	private ArrayList<T> entries;

	private static class PointDist {
		double dist;
		IOriginThing point;

		public PointDist(IOriginThing p, double d) {
			dist = d;
			point = p;
		}

		public static PointDist furthest() {
			return new PointDist(null, Double.MAX_VALUE);
		}
	}

	private Octree(float minx, float miny, float minz, float size, int depth) {
		this.minx = minx;
		this.miny = miny;
		this.minz = minz;
		this.size = size;

		this.depth = depth;

		halfsize = size * 0.5f;
		children = null;
		entries = new ArrayList<T>();
	}

	public Octree(float minx, float miny, float minz, float size) {
		this(minx, miny, minz, size, 0);
	}

	public void insert(T thing) {
		if (entries != null) {
			entries.add(thing);

			if (entries.size() > MAX_ENTRIES)
				subdivide();

			return;
		}

		Vector vec = thing.getOrigin();

		int index = findIndex(vec.x, vec.y, vec.z);

		children[index].insert(thing);
	}
	
	public void remove(T thing) {
		Vector vec = thing.getOrigin();
		remove(thing, vec.x, vec.y, vec.z);
	}
	
	private boolean remove(T thing, float x, float y, float z) {
		if (children == null) {
			return entries.remove(thing);
		}
		
		int index = findIndex(x, y, z);
		
		//if removal happened check if children are not empty
		//if all children are empty this tree becomes a leaf
		if(children[index].remove(thing, x, y, z)) {
			boolean dead = true;
			
			for(int i = 0; i < 8 && dead; ++i) {
				dead = dead && (children == null || children[i].entries.size() < 1);
			}
			
			if(dead) {
				children = null;
				return true;
			}
		}
		
		return false;
	}
	
	private int findIndex(float x, float y, float z) {
		float x1 = x - minx;
		float y1 = y - miny;
		float z1 = z - minz;

		int index = 0;
		index |= (x1 >= halfsize ? 1 : 0) << 2;
		index |= (y1 >= halfsize ? 1 : 0) << 1;
		index |= (z1 >= halfsize ? 1 : 0);
		
		return index;
	}

	private Octree<T> findRegion(float x, float y, float z) {
		if (children == null)
			return this;

		return children[findIndex(x, y, z)].findRegion(x, y, z);
	}

	private Octree<T> findClosestNonEmptyLeaf(float x, float y, float z) {
		if (children == null)
			return this; // can't go any deeper

		int index = findIndex(x, y, z);

		// best guess turns out to have children, try there
		if (children[index].children != null)
			return children[index].findClosestNonEmptyLeaf(x, y, z);

		Octree<T> closest = children[index]; // for safety to avoid nullPointerExceptionHandlerIncident
		double closestDist = Double.MAX_VALUE;

		// best bet failed, it has no children
		// find the children closest to a given point, non empty leaf or a node
		for (int i = (index + 1) & 0x07; i != index; i = (++i) & 0x07) {
			Octree<T> current = children[i];
			double dist = current.getDistance(x, y, z);
			if (dist < closestDist
					&& (current.children != null || (current.entries != null && current.entries.size() > 0))) {
				closestDist = dist;
				closest = current;
			}
		}

		return closest.findClosestNonEmptyLeaf(x, y, z);
	}

	private PointDist findClosestPoint(float x, float y, float z) {
		PointDist closest = PointDist.furthest();

		if (entries == null)
			return closest;

		for (IOriginThing bc : entries) {
			Vector vec = bc.getOrigin();
			float x1 = vec.x - x;
			float y1 = vec.y - y;
			float z1 = vec.z - z;
			double dist = x1 * x1 + y1 * y1 + z1 * z1;

			if (dist < closest.dist) {
				closest.point = bc;
				closest.dist = dist;
			}
		}

		return closest;
	}

	private PointDist findClosestInRadius(float x, float y, float z, double radius) {
		if (children == null) {
			return findClosestPoint(x, y, z);
		}
		PointDist closest = new PointDist(null, radius);

		int index = findIndex(x, y, z);

		for (int i = index;;) {
			Octree<T> child = children[i];
			if (child.getDistance(x, y, z) <= closest.dist) {
				PointDist newClosest = child.findClosestInRadius(x, y, z, closest.dist);

				if (newClosest.point != null && newClosest.dist <= closest.dist) {
					closest = newClosest;
				}
			}

			i = (++i) & 7;
			if (i == index)
				break;
		}

		return closest;
	}

	public T findClosest(float x, float y, float z) {
		if (children == null && entries == null)
			return null;
		PointDist closest = findClosestInRadius(x, y, z, size * size);

		return (T)closest.point;
	}

	private double getDistance(float x, float y, float z) {
		float dx = Math.max(minx - x, Math.max(0, x - minx - size));
		float dy = Math.max(miny - y, Math.max(0, y - miny - size));
		float dz = Math.max(minz - z, Math.max(0, z - minz - size));

		return dx * dx + dy * dy + dz * dz;
	}

	private void addFromList(ArrayList<T> list) {
		if (list.size() < 1)
			return;

		int listsize = list.size();
		for (int i = 0; i < listsize; ++i) {
			T thing = list.get(i);
			Vector vec = thing.getOrigin();
			float x = vec.x - minx;
			float y = vec.y - miny;
			float z = vec.z - minz;

			if (x >= 0 && y >= 0 && z >= 0 && x < size && y < size && z < size) {
				entries.add(thing);

				T b2 = list.get(listsize - 1);
				list.set(i, b2);
				list.remove(listsize - 1);
				--listsize;
				--i;
			}
		}

		if (entries.size() > MAX_ENTRIES) // test test > was >=
			subdivide();
	}

	public void subdivide() {
		if (children != null || depth >= MAX_DEPTH)
			return;

		int newdepth = depth + 1;
		children = new Octree[8];
		children[0] = new Octree<T>(minx, miny, minz, halfsize, newdepth);
		children[0].addFromList(entries);

		children[4] = new Octree<T>(minx + halfsize, miny, minz, halfsize, newdepth);
		children[4].addFromList(entries);

		children[2] = new Octree<T>(minx, miny + halfsize, minz, halfsize, newdepth);
		children[2].addFromList(entries);

		children[6] = new Octree<T>(minx + halfsize, miny + halfsize, minz, halfsize, newdepth);
		children[6].addFromList(entries);

		children[1] = new Octree<T>(minx, miny, minz + halfsize, halfsize, newdepth);
		children[1].addFromList(entries);

		children[5] = new Octree<T>(minx + halfsize, miny, minz + halfsize, halfsize, newdepth);
		children[5].addFromList(entries);

		children[3] = new Octree<T>(minx, miny + halfsize, minz + halfsize, halfsize, newdepth);
		children[3].addFromList(entries);

		children[7] = new Octree<T>(minx + halfsize, miny + halfsize, minz + halfsize, halfsize, newdepth);
		children[7].addFromList(entries);

		entries.clear();
		entries = null;
	}

	public int count() {
		if (entries != null)
			return entries.size();

		int size = 0;
		for (int i = 0; i < 8; ++i) {
			size += children[i].count();
		}

		return size;
	}

	public boolean contains(T b) {
		if (entries != null)
			return entries.contains(b);

		boolean c = false;
		for (int i = 0; i < 8 && !c; ++i) {
			c = c || children[i].contains(b);
		}
		return c;
	}
	
	public static interface IOriginThing {
		public Vector getOrigin();
	}
	
	public static class Vector{
		public float x;
		public float y;
		public float z;
	}

	/*
	 * public String toString() { StringBuilder sb = new StringBuilder();
	 * 
	 * char indent[] = new char[depth * 2]; for(int i = 0; i < depth * 2; ++i)
	 * indent[i] = ' '; sb.append(indent);
	 * 
	 * if(children == null) {
	 * sb.append("Leaf, elements: ").append(entries.size()).append('\n');
	 * for(BlockColor b : entries)
	 * sb.append(indent).append(indent).append(b.toString()).append('\n'); } else {
	 * sb.append("Node: min ").append(minx).append(" ").append(miny).append(" ").
	 * append(minz).append(" size: ").append(size).append('\n');
	 * 
	 * for(int i = 0; i < 8; ++i) sb.append(children[i].toString()).append('\n'); }
	 * 
	 * return sb.toString(); }
	 */
}