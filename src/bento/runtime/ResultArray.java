/* Bento
 *
 * $Id: ResultArray.java,v 1.5 2007/10/24 04:22:27 sthippo Exp $
 *
 * Copyright (c) 2007 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.*;

import java.util.*;
import java.sql.*;

/**
 * A ResultSet wrapper that implements the BentoArray interface.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class ResultArray extends AbstractList implements BentoArray {

    private ResultSet resultSet;
    private ResultSetMetaData resultMetaData;
    private List instantiatedResultSet = null;
    private int size;

    public ResultArray(ResultSet results) throws SQLException {
    	this(results, -1);
    }

    public ResultArray(ResultSet results, int size) throws SQLException {
        resultSet = results;
        resultMetaData = results.getMetaData();
        this.size = size;
    }

    public Object getArrayObject() {
        return this;
    }

    public Object instantiateArray(Context context) {
        instantiateResults(); 
        return instantiatedResultSet;
    }
       
    private void instantiateResults() {
        if (instantiatedResultSet == null) {
System.out.println("-}} instantiating result set");
            instantiatedResultSet = new ArrayList();
            Iterator it = new ResultIterator(resultSet);
            while (it.hasNext()) {
                instantiatedResultSet.add(it.next());
            }
System.out.println("-}} instantiated " + instantiatedResultSet.size() + " rows");
        }
    }

    public Object get(int n) {
        if (instantiatedResultSet == null) {
            instantiateResults(); 
        }            
        return instantiatedResultSet.get(n);
    }

    public int size() {
        return getSize();
    }
    
    public int getSize() {
    	if (size == -1) {
    	    instantiateResults();
            size = instantiatedResultSet.size();
    	}
    	return size;
    }

    public boolean isGrowable() {
        return false;
    }

    public boolean add(Object element) {
        return false;
    }

    public Object set(int n, Object element) {
        throw new UnsupportedOperationException("ResultArrays are immutable");
    }

    public Iterator iterator() {
        if (instantiatedResultSet == null) {
            instantiateResults();
        }
        return instantiatedResultSet.iterator();
    }

    public ResultSet getResultSet() {
        return resultSet;
    }
}


class ResultIterator implements Iterator {
    private ResultSet resultSet;
    private String[] columns; 
    private DatabaseRow nextRow = null;
    private boolean more = true;

    public ResultIterator(ResultSet resultSet) {
        this.resultSet = resultSet;
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int numCols = rsmd.getColumnCount();
            columns = new String[numCols];
            for (int i = 0; i < numCols; i++) {
                columns[i] = rsmd.getColumnName(i + 1);
            }
            if (resultSet.getType() != ResultSet.TYPE_FORWARD_ONLY) {
                resultSet.beforeFirst();
            }
        } catch (Exception e) {
            System.out.println("Exception constructing ResultIterator: " + e);
        }
    }

    public boolean hasNext() {
        if (nextRow == null) {
            nextRow();
        }
        return more;
    }

    public Object next() {
        if (nextRow == null) {
            nextRow();
            if (nextRow == null) {
                throw new NoSuchElementException();
            }
        }

        DatabaseRow r = nextRow;
        nextRow = null;
        return r;
    }

    public void remove() {
        throw new UnsupportedOperationException("ResultArrays are immutable");
    }
    private void nextRow() {
        try {
            more = resultSet.next();
            if (more) {
                nextRow = new DatabaseRow(resultSet, columns, resultSet.getRow());
            } else {
                nextRow = null;
                resultSet.close();
            }
        } catch (Exception e) {
            more = false;
            nextRow = null;
        }
    }
}

