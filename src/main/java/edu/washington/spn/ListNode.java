package edu.washington.spn;

import java.io.Serializable;


public class ListNode<E>  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ListNode<E> next, prev;
	public E item;
	public double pos;
	public ListNode(E e) {
		this.item = e;
	}
}
