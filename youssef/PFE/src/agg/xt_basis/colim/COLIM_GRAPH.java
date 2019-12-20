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
//                Colimit Basic Data Type COLIM_GRAPH            
//-------------------------------------------------------------------


import java.util.Enumeration;


public class COLIM_GRAPH implements COLIM_DEFS {
  
  public COLIM_GRAPH() 
  {
    f_node = new COLIM_VECTOR();
    f_edge = new COLIM_VECTOR();
    f_source = new INT_VECTOR();
    f_target = new INT_VECTOR();
  }

  public COLIM_GRAPH(COLIM_VECTOR nodes, COLIM_VECTOR edges, 
                     INT_VECTOR source, INT_VECTOR target) 
  {    
    f_node = new COLIM_VECTOR (nodes);
    f_edge = new COLIM_VECTOR (edges);
    f_source = new INT_VECTOR (source);
    f_target = new INT_VECTOR (target);
  }
     
  //---------------- construction --------------------

  public int insert_node(Object node_attr) 
  {
    int n = f_node.size();
    f_node.push_back(node_attr);
    return n;
  }

  public int insert_edge(Object edge_attr, int s, int t)
  {
    int e = f_edge.size();
    f_edge.push_back(edge_attr);
    f_source.push_back(s);
    f_target.push_back(t);
    return e;
  }

  public Object node_attr(int node) { return f_node.item(node); }

  public Object edge_attr(int edge) { return f_edge.item(edge); }

  public int source(int edge) { return f_source.item(edge); }

  public int target(int edge) { return f_target.item(edge); }

  //---------------- test output  ---------------------
  
  @SuppressWarnings("rawtypes")
public String toString() 
  {
    StringBuffer Result = new StringBuffer("\nnodes: ");
    for (Enumeration en = f_node.elements(); en.hasMoreElements(); ) {
      Object n = en.nextElement();
      if (n != null) {
        Result.append(n.toString());
        if (en.hasMoreElements()) Result.append(",");
      }
    }
    Result.append("\nedges: ");
    int index = 0;
    for (Enumeration en = f_edge.elements(); 
                     en.hasMoreElements(); index++) {
      Object e = en.nextElement(); 
      if (e != null) {
        Result.append(f_node.item(f_source.item(index)).toString());
        Result.append("--");
        Result.append(e.toString());
        Result.append("->");
        Result.append(f_node.item(f_target.item(index)).toString());
        if (en.hasMoreElements()) Result.append(", ");
      }
    } 
    Result.append("\n");
    return new String(Result);
  }

  protected COLIM_VECTOR f_node;
  protected COLIM_VECTOR f_edge;
  protected INT_VECTOR f_source;
  protected INT_VECTOR f_target;

}






