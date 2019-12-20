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

import agg.util.colim.IntBuffer;


//-------------------------------------------------------------------
//                 dynamic integer array          
//-------------------------------------------------------------------


public class INT_VECTOR extends IntBuffer {
	
  public INT_VECTOR() { 
    clear();	  
  }

  public INT_VECTOR(int size) {
    clear();
    ensureCapacity(size);    
  }

  public INT_VECTOR(IntBuffer buf) {     
    super(buf);
  }

  public void push_back(int i) { 
    super.add(i); 
  }

  public int item(int index) { 
    return super.intAt(index); 
  }

  public void put(int i, int index) { 
    super.put(index, i); 
  }

}






