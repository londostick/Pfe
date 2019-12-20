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
//                 Colimit Basic Data Type COPROD_OBJECT                     
//-------------------------------------------------------------------


public class COPROD_OBJECT {
  
  public COPROD_OBJECT(int low, int upp) 
  {
    lower = low;
    upper = upp; 
  }

  public int lower;
  public int upper;

  public String toString() 
  {
    StringBuffer Result = new StringBuffer("{");
    Result.append(lower);
    Result.append(",");
    Result.append(upper);
    Result.append("}");
    return new String(Result);
  }
}
