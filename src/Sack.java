/*************************************************************************
 *  CS 510 Data Management in Cloud File : Sack.java
 *  
 *
 *  Sack file is implemented using a linked list.
 *  Sack class represents a sack of items.
 *
 *************************************************************************/

import java.util.Iterator;
import java.util.NoSuchElementException;


public class Sack<Item> implements Iterable<Item> {
	private int N; // number of elements in Sack
	private Node firstNode; // beginning of Sack
	private Node lastNode; // End of Sack


	private class Node {
		private Item item;
		private Node next;
	}

	/**
	 * Create an empty stack.
	 */
	public Sack() {
		firstNode = null;
		N = 0;
		assert check();
	}

	/**
	 * Add the item to the Sack.
	 */
	public void add(Item item) {
		Node oldfirst = firstNode;

		firstNode = new Node();
		firstNode.item = item;
		firstNode.next = oldfirst;

		N++;

		assert check();
	}

	/**
	 * Recently added item to the stack is deleted and returned.
	 */
	public Item pop() {
		if (isEmpty())
			throw new NoSuchElementException("Stack underflow");
		Item item = firstNode.item; // item to be returned is saved
		firstNode = firstNode.next; // FirstNode - deleted
		N--;
		assert check();
		return item; // returning the saved item
	}

	/**
	 * Add item to the queue.
	 */
	public void Enqueue(Item item) {
		Node oldlast = lastNode;
		lastNode = new Node();
		lastNode.item = item;
		lastNode.next = null;
		if (isEmpty())
			firstNode = lastNode;
		else
			oldlast.next = lastNode;
		N++;
		assert check();
	}

	/**
	 * Is stack empty?
	 */
	public boolean isEmpty() {
		return firstNode == null;
	}

	private boolean check() {
		if (N == 0) {
			if (firstNode != null)
				return false;
		} else if (N == 1) {
			System.out.println("<Sack> check() N:" + N);
			if (firstNode == null)
				return false;
			if (firstNode.next != null)
				return false;
		} else {
			if (firstNode.next == null)
				return false;
		}

		// check internal consistency of instance variable N
		int numberOfNodes = 0;
		for (Node n = firstNode; n != null; n = n.next) {
			numberOfNodes++;
		}
		if (numberOfNodes != N)
			return false;

		return true;
	}

	/**
	 * Return an iterator that iterates over the items in the Sack.
	 */
	public Iterator<Item> iterator() {
		return new ListIterator();
	}

	private class ListIterator implements Iterator<Item> {
		private Node currentNode = firstNode;

		public boolean hasNext() {
			return currentNode != null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public Item next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Item item = currentNode.item;
			currentNode = currentNode.next;
			return item;
		}
	}

}
