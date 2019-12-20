/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/

package agg.xt_basis.colim;
//-------------------------------------------------------------------
//                 dynamic object array          
//-------------------------------------------------------------------


import java.util.Enumeration;
import java.util.Vector;

 
public class COLIM_VECTOR {

	final Vector<Object> v;
	  
    public COLIM_VECTOR() {
    	v = new Vector<Object>();
    }

    public COLIM_VECTOR(int size) { 
    	v = new Vector<Object>(size);
    }

    public COLIM_VECTOR(COLIM_VECTOR buf) {     
    	v = new Vector<Object>(buf.v);
    }

    public void push_back(Object obj) { 
    	v.add(obj);
    }

    public Object item(int index) { 
    	return v.get(index);
    }

    public void put(Object obj, int index) { 
    	v.add(index, obj);
    } 
    
    public int indexOf(Object obj) {
    	return v.indexOf(obj);
    }
    
    @SuppressWarnings("rawtypes")
	public Enumeration elements() {
    	return v.elements();
    }
    
    public int size() {
    	return v.size();
    }
    
    public void setSize(int size) {
  	  	v.setSize(size);
    }

    public void ensureCapacity(int size) {
    	v.ensureCapacity(size);
    }
    
    public void clear() {
    	v.clear();
    }
    
    public void trimToSize() {
    	v.trimToSize();
    }
    
    public String toString() {
    	return v.toString();
    }
}




