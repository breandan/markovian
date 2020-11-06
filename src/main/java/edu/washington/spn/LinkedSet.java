package edu.washington.spn;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class LinkedSet<E> implements Collection<E>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public HashMap<E, ListNode<E>> index = new HashMap<E, ListNode<E>>();
	public ListNode<E> head=null;
	public ListNode<E> tail=null;
	int listSize = 0;
	boolean haveToRecount = true;

	public void replace(E oldval, E newval){
		ListNode<E> node = index.get(oldval);
		node.item = newval;
	}

	public void addAfter(E before, E after){
		if(contains(after))
			return;

		ListNode<E> beforeNode = index.get(before);
		ListNode<E> wasAfter = beforeNode.next;

		ListNode<E> newNode = new ListNode<E>(after);
		index.put(after, newNode);

		newNode.prev = beforeNode;
		newNode.next = wasAfter;
		if(wasAfter == null)
			newNode.pos = beforeNode.pos + 1;
		else
			newNode.pos = (beforeNode.pos + wasAfter.pos)/2.0;
		if(newNode.pos == beforeNode.pos || (wasAfter != null && newNode.pos == wasAfter.pos))
			System.err.println("addAfter: Run out of space to squeeze pos "+newNode.pos);


		beforeNode.next = newNode;
		if(wasAfter != null)
			wasAfter.prev = newNode;

		if(tail.equals(beforeNode))
			tail = newNode;

		listSize++;
	}

	public void addAllAfter(E before, Collection<? extends E> allAfter){
		for(E e : allAfter)
			addAfter(before, e);
	}

	public void addBefore(E before, E after){
		if(contains(before))
			return;

		ListNode<E> afterNode = index.get(after);
		ListNode<E> wasBefore = afterNode.prev;

		ListNode<E> newNode = new ListNode<E>(before);
		index.put(before, newNode);

		newNode.prev = wasBefore;
		newNode.next = afterNode;

		if(wasBefore == null)
			newNode.pos = afterNode.pos - 1;
		else
			newNode.pos = (afterNode.pos + wasBefore.pos)/2.0;


		if(wasBefore != null)
			wasBefore.next = newNode;
		afterNode.prev = newNode;

		if(head.equals(afterNode))
			head = newNode;

		if(newNode.pos == afterNode.pos || (wasBefore != null && newNode.pos == wasBefore.pos)){
			//			System.err.println("addBefore: Run out of space to squeeze pos"+newNode.pos+"  recounting...");
			//			recount();
			haveToRecount = true;
		}

		listSize++;
	}

	public void addAllBefore(Collection<? extends E> allBefore, E after){
		//		double offset = allBefore.size();
		//		for(ListNode<E> ln = index.get(after); ln != null; ln = ln.next){
		//			ln.pos += offset;
		//		}
		for(E e : allBefore)
			addBefore(e, after);
	}

	@Override
	public boolean add(E arg0) {
		ListNode<E> a = new ListNode<E>(arg0);
		index.put(arg0, a);
		listSize++;
		if(tail == null){
			a.next = null;
			a.prev = null;
			head = a;
			tail = a;
			return true;
		}
		ListNode<E> prevTail = tail;
		prevTail.next = a;
		a.prev = prevTail;
		a.next = null;
		a.pos = (1.0 + (tail != null ? tail.pos : 0.0));
		if(tail != null && a.pos == tail.pos)
			System.err.println("Add: Run out of space to squeeze pos"+a.pos);

		tail = a;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		for(E o : arg0)
			add(o);
		return true;
	}

	@Override
	public void clear() {
		head = null;
		tail = null;
		listSize = 0;
		index.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return index.containsKey(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		for(Object e : arg0)
			if(!index.containsKey(e))
				return false;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return index.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return new LinkedSetIterator<E>(this);
	}

	public Iterator<E> iteratorAt(Object after) {
		return new LinkedSetIterator<E>(this, index.get(after));
	}

	@Override
	public boolean remove(Object arg0) {
		ListNode<E> n = index.get(arg0);

		if(n == null)
			return false;

		ListNode<E> pointerToNext = n.next;

		index.remove(arg0);

		if(pointerToNext != null)
			pointerToNext.prev = n.prev;
		if(n.prev != null)
			n.prev.next = pointerToNext;
		if(head.equals(n))
			head = pointerToNext;
		if(tail.equals(n))
			tail = n.prev;
		n = null;

		listSize--;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		for(Object o : arg0)
			remove(o);
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
		//		return false;
	}

	@Override
	public int size() {
		return listSize;
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(E e : this)
			sb.append(e.toString()+",");
		return sb.toString();
	}

	public E removeFirst() {
		E toret = head.item;
		remove(toret);
		return toret;
	}

	public E getFirst() {
		E toret = head.item;
		return toret;
	}

	public boolean ensureBefore(E before, E after) {
		if(haveToRecount) recount();
		if(index.get(before).pos > index.get(after).pos){
			remove(before);
			addBefore(before, after);
			return false;
		}
		return true;
	}

	public void recount(){
		int count = 0;
		for(ListNode<E> ln = head; ln != null; ln = ln.next){
			ln.pos = count++;
		}
		haveToRecount = false;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeInt(listSize);
		for(E item : this)
			oos.writeObject(item);
	}

	// assumes "static java.util.Date aDate;" declared
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		
		index = new HashMap<E, ListNode<E>>();
		head=null;
		tail=null;
		listSize = 0;
		int count = ois.readInt();
		haveToRecount = true;
		
		for(int i=0; i<count; i++){
			Object o = ois.readObject();
			add((E)o);
		}
	}

}
