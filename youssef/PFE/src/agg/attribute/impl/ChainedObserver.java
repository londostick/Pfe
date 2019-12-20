/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.impl;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Vector;

import agg.attribute.AttrEvent;
import agg.attribute.AttrObserver;
import agg.attribute.AttrTuple;

/**
 * Handling of attribute observers.
 * 
 * @see agg.attribute.AttrObserver
 * @author $Author: olga $
 * @version $Id: ChainedObserver.java,v 1.12 2010/08/23 07:30:49 olga Exp $
 */
@SuppressWarnings("serial")
public abstract class ChainedObserver extends ManagedObject implements
		AttrObserver, AttrTuple {

	//
	// Static variables and constants.

	protected static final int MAX_SIZE_OF_EVENT_STACK = 100;

	protected static int sizeOfEventStack = 0;

	//
	// Protected instance variables.

	/**
	 * Container with observers of this instance, all of which implement the
	 * AttrObserver interface.
	 * 
	 * @see agg.attribute.AttrObserver
	 */
	protected transient final Vector<WeakReference<AttrObserver>> observers;


	public ChainedObserver(AttrTupleManager m) {
		super(m);
		
		this.observers = new Vector<WeakReference<AttrObserver>>();
	}

	/**
	 * removes all observer which are null
	 */
	private synchronized void removeNullObserver() {
		for (int i = 0; i < this.observers.size(); i++) {
			if (this.observers.get(i).get() == null) {
				this.observers.remove(i);
				i--;
			}
		}
	}

	//
	// Handling of this.observers. Part of the implementation of
	// agg.attribute.AttrTuple
	//

	public synchronized Enumeration<AttrObserver> getObservers() {
		final Vector<AttrObserver> tmp = new Vector<AttrObserver>();
//		System.out.println("ChangedObserver.getObservers   "+this.observers.size());
		for (int i = 0; i < this.observers.size(); i++) {
			WeakReference<AttrObserver> wr = this.observers.get(i);
			if (wr.get() != null) {
				tmp.addElement(wr.get());
			} else {
//				System.out.println("ChangedObserver.getObservers:  remove null at  "+i);
				this.observers.remove(i);
				i--;
			}
		}
		return tmp.elements();
	}

	public void addObserver(AttrObserver attrObs) {
		if (attrObs != null) {
//			removeNullObserver();
			boolean found = false;
			for (int i = 0; i < this.observers.size() && !found; i++) {
				try {
					found = attrObs == this.observers.get(i).get();
				}
				catch (ArrayIndexOutOfBoundsException ex) {
					return;
				}
			}
			if (!found) {
//				System.out.println(this.observers.size()+"  ChangedObserver.addObserver:  "+attrObs+"   "+this.observers.hashCode());
				this.observers.addElement(new WeakReference<AttrObserver>(attrObs));
			}
		} 
	}

	public void addObserverAtPos(AttrObserver attrObs, int pos) {
		if (attrObs != null) {
//			removeNullObserver();
			boolean found = false;
			for (int i = 0; i < this.observers.size() && !found; i++) {
				found = attrObs == this.observers.elementAt(i).get();
			}
			if (!found) {
//				System.out.println(this.observers.size()+"  ChangedObserver.addObserverAtPos:  "+attrObs+"   "+this.observers.hashCode());
				this.observers.insertElementAt(new WeakReference<AttrObserver>(attrObs), pos);
			}
		}
	}
	
	public boolean contains(AttrObserver attrObs) {
		if ((attrObs == null) || (this.observers.isEmpty()))
			return false;
		
		for (int i = 0; i < this.observers.size(); i++) {
			WeakReference<AttrObserver> wr = this.observers.elementAt(i);
			if ((wr.get() != null) && wr.get() == attrObs)
				return true;
		}
		
		return false;
	}

	

	@SuppressWarnings("unused")
	private int findObserver(AttrObserver obj) {
		if ((obj == null) || (this.observers.isEmpty()))
			return -1;
		removeNullObserver();
		for (int i = 0; i < this.observers.size(); i++) {
			WeakReference<AttrObserver> wr = this.observers.elementAt(i);
			if ((wr.get() != null) && wr.get() == obj)
				return i;
		}
		return -1;
	}

	public synchronized void removeObserver(AttrObserver attrObs) {
		if (attrObs == null || this.observers.isEmpty())
			return;
		
		for (int i = 0; i < this.observers.size(); i++) {
			WeakReference<AttrObserver> wr = this.observers.elementAt(i);
			if (wr.get() == attrObs) {
				this.observers.remove(i);
				break;
			}
		}
	}

	protected void fireAttrChanged(int id, int index) {
		fireAttrChanged(id, index, index);
	}

	protected void fireAttrChanged(int id, int index0, int index1) {
		fireAttrChanged(new TupleEvent(this, id, index0, index1));
	}

	protected void fireAttrChanged(TupleEvent evt) {
		if (evt == null)
			return;

		for (Enumeration<WeakReference<AttrObserver>> en = this.observers.elements(); en.hasMoreElements();) {
			WeakReference<AttrObserver> wr = en.nextElement();
			if (wr.get() != null) {
				TupleEvent childEvt = filterEvent(wr.get(), evt);
				if (childEvt != null)
					wr.get().attributeChanged(childEvt);
			}
		}
	}

	/** Propagates the event to the this.observers, pretending to be the source. */
	protected abstract void propagateEvent(TupleEvent e);

	/**
	 * This method should be overridden by classes that wish to customize or
	 * filter the actual event depending on the respective observer and/or its
	 * own framework (index transformation, id change). If [null] is returned,
	 * the specified observer will not get any notification this time.
	 */
	protected TupleEvent filterEvent(AttrObserver obs, TupleEvent e) {
		return e;
	}

	//
	// Being an observer oneself. Implementation of
	// agg.attribute.AttrObserver.

	/**
	 * Per default, always save observer dependencies within the attribute
	 * component.
	 */
	public boolean isPersistentFor(AttrTuple at) {
		return true;
	}

	/**
	 * Checks if an endless event recursion took place. If so, a runtime
	 * exception with a warning text is thrown, as this indicates an error in
	 * the implementation. Otherwise, it calls the one of the respective
	 * updating methods which can be overridden by subclasses .
	 */

	public void attributeChanged(AttrEvent evt) {
		if (++sizeOfEventStack > MAX_SIZE_OF_EVENT_STACK) {
			throw new RuntimeException("Infinite event recursion occured.");
		}
		TupleEvent event = (TupleEvent) evt;
		int msg = event.getID();
		if (msg == AttrEvent.GENERAL_CHANGE)
			updateGeneralChange(event);
		else if (msg == AttrEvent.MEMBER_ADDED)
			updateMemberAdded(event);
		else if (msg == AttrEvent.MEMBER_DELETED)
			updateMemberDeleted(event);
		else if (msg == AttrEvent.MEMBER_MODIFIED)
			updateMemberModified(event);
		else if (msg == AttrEvent.MEMBER_RENAMED)
			updateMemberRenamed(event);
		else if (msg == AttrEvent.MEMBER_RETYPED)
			updateMemberRetyped(event);
		else if (msg == AttrEvent.MEMBER_VALUE_MODIFIED)
			updateValueModified(event);
		else if (msg == AttrEvent.MEMBER_VALUE_CORRECTNESS)
			updateValueCorrectness(event);
		else
			updateUnknownChange(event);

		sizeOfEventStack--;
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateGeneralChange(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateMemberAdded(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateMemberDeleted(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateMemberModified(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateMemberRenamed(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateMemberRetyped(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateValueModified(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateValueCorrectness(TupleEvent event) {
		propagateEvent(event);
	}

	/**
	 * Fires the same event. Subclasses should override this method for
	 * customized behaviour.
	 */
	protected void updateUnknownChange(TupleEvent event) {
		propagateEvent(event);
	}

	/*
	 * sets a vector of AttrObserver. This is only needed for reading objects.
	 * The old this.observers will be replaced.
	 */
	/*
	private void setObservers(Vector<AttrObserver> observer) {
		// System.out.println("ChainedObserver.setObservers ");
		// System.out.println("observer : "+observer);
		this.observers = new Vector<WeakReference<AttrObserver>>();

		if ((observer == null) || (observer.size() == 0))
			return;

		Enumeration<AttrObserver> en = observer.elements();
		while (en.hasMoreElements()) {
			AttrObserver e = en.nextElement();
			// System.out.println(e);
			if (e != null)
				this.observers.addElement(new WeakReference<AttrObserver>(e));
		}
		// System.out.println("ChainedObserver.setObservers END ");
	}
*/
}
/*
 * $Log: ChainedObserver.java,v $
 * Revision 1.12  2010/08/23 07:30:49  olga
 * tuning
 *
 * Revision 1.11  2010/03/18 18:14:34  olga
 * tuning
 *
 * Revision 1.10  2010/03/17 21:37:37  olga
 * tuning
 *
 * Revision 1.9  2010/03/16 15:53:00  olga
 * optimized
 *
 * Revision 1.8  2010/03/08 15:37:22  olga
 * code optimizing
 *
 * Revision 1.7  2008/09/25 08:02:50  olga
 * improved graphics update during graph transformation
 *
 * Revision 1.6  2008/06/26 14:18:46  olga
 * Graph visualization tuning
 *
 * Revision 1.5  2007/11/01 09:58:13  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.4  2007/09/10 13:05:18  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.3 2007/03/28 10:00:30 olga -
 * extensive changes of Node/Edge Type Editor, - first Undo implementation for
 * graphs and Node/edge Type editing and transformation, - new / reimplemented
 * options for layered transformation, for graph layouter - enable / disable for
 * NACs, attr conditions, formula - GUI tuning
 * 
 * Revision 1.2 2006/08/02 09:00:57 olga Preliminary version 1.5.0 with -
 * multiple node type inheritance, - new implemented evolutionary graph layouter
 * for graph transformation sequences
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.7 2004/12/20 14:53:47 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.6 2004/06/09 11:32:53 olga Attribute-Eingebe/Bedingungen : NAC
 * kann jetzt eigene Variablen und Bedingungen haben. CP Berechnung korregiert.
 * 
 * Revision 1.5 2003/03/05 18:24:21 komm sorted/optimized import statements
 * 
 * Revision 1.4 2002/11/25 14:56:27 olga Der Fehler unter Windows 2000 im
 * AttributEditor ist endlich behoben. Es laeuft aber mit Java1.3.0 laeuft
 * endgueltig nicht. Also nicht Java1.3.0 benutzen!
 * 
 * Revision 1.3 2002/10/04 16:36:38 olga Es gibt noch Fehler unter Window
 * 
 * Revision 1.2 2002/09/23 12:23:55 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:01 olga Imported sources
 * 
 * Revision 1.7 2001/03/08 10:38:39 olga Testausgaben raus.
 * 
 * Revision 1.6 2000/04/05 12:09:02 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.5 1999/09/13 14:05:35 shultzke fireAttrChanged an WeakReferences
 * angepasst
 * 
 * Revision 1.4 1999/09/06 13:38:55 shultzke ChainedObserver auf WeakReferences
 * umgestellt, samt serialUID
 */
