package edu.washington.spn;

import java.util.Iterator;

class LinkedSetIterator<E> implements Iterator<E> {
	LinkedSet<E> set;
	ListNode<E> pointerToNext;
	
	public LinkedSetIterator(LinkedSet<E> set) {
		this.set = set;
		this.pointerToNext = set.head;
	}
	
	public LinkedSetIterator(LinkedSet<E> set, ListNode<E> start) {
		this.set = set;
		this.pointerToNext = start;
	}
	
	@Override
	public boolean hasNext() {
		return pointerToNext != null;
	}

	@Override
	public E next() {
		E toret = pointerToNext.item;
		pointerToNext = pointerToNext.next;
		return toret;
	}

	@Override
	public void remove() {
		ListNode<E> previouslyReturned = pointerToNext.prev;
		
		set.index.remove(previouslyReturned.item);
		
		if(pointerToNext != null)
			pointerToNext.prev = previouslyReturned.prev;
		if(previouslyReturned.prev != null)
			previouslyReturned.prev.next = pointerToNext;
		if(set.head.equals(previouslyReturned))
			set.head = pointerToNext;
		if(set.tail.equals(previouslyReturned))
			set.tail = previouslyReturned.prev;
		previouslyReturned = null;
	}
	
}