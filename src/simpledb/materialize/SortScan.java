package simpledb.materialize;

import simpledb.record.RID;
import simpledb.record.TableInfo;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.*;
import java.util.*;

/**
 * The Scan class for the <i>sort</i> operator.
 * @author Edward Sciore
 */
/**
 * @author sciore
 *
 */
//project 2: UPDATED
public class SortScan implements Scan {
   private UpdateScan s1, s2=null, currentscan=null;
   private RecordComparator comp;
   private boolean hasmore1, hasmore2=false;
   private List<RID> savedposition;
   private Transaction tx;
   private String tblname; //project 2: added to carry through for table info
   
   /**
    * Creates a sort scan, given a list of 1 or 2 runs.
    * If there is only 1 run, then s2 will be null and
    * hasmore2 will be false.
    * @param runs the list of runs
    * @param comp the record comparator
    */
   public SortScan(String tblname, Transaction tx, List<TempTable> runs, RecordComparator comp) { //could pass in orignal record file
      this.tblname = tblname;
	  this.tx = tx;
	  this.comp = comp;
      runs.get(0).getTableInfo().setSorted(0); //project 2: when scanned by update scan, sorted flag reset to false
      s1 = (UpdateScan) runs.get(0).open();
      hasmore1 = s1.next();
      if (runs.size() > 1) {
    	 runs.get(1).getTableInfo().setSorted(0); //project 2: when scanned by update scan, sorted flag reset to false
    	 s2 = (UpdateScan) runs.get(1).open();
         hasmore2 = s2.next();
      }
      
      TableInfo orig = SimpleDB.mdMgr().getTableInfo(this.tblname, this.tx); //project 2: get TI of original table
      TableScan ts = new TableScan(orig, this.tx); //project 2: open new table scan on original table
      ts.beforeFirst(); //project 2: point before first record
      runs.get(0).getTableInfo().setSorted(1); //project 2: runs is now sorted so set flag as true!
      TableScan sorted = new TableScan(runs.get(0).getTableInfo(), this.tx); //project 2: hope creating tablescan from sorted temptable
      sorted.beforeFirst(); //project 2: point before first record
      
      //project 2: replace each record in original table with the temp table sorted records
      while (sorted.next() && ts.next())
    	  ts.setVal("dataval", sorted.getVal("dataval"));
      	  ts.setVal("block", sorted.getVal("block"));
          ts.setVal("id", sorted.getVal("id"));
     orig.setSorted(1); //project 2: the original table is now sorted, set flag
   }
   
   /**
    * Positions the scan before the first record in sorted order.
    * Internally, it moves to the first record of each underlying scan.
    * The variable currentscan is set to null, indicating that there is
    * no current scan.
    * @see simpledb.query.Scan#beforeFirst()
    */
   public void beforeFirst() {
      currentscan = null;
      s1.beforeFirst();
      hasmore1 = s1.next();
      if (s2 != null) {
         s2.beforeFirst();
         hasmore2 = s2.next();
      }
   }
   
   /**
    * Moves to the next record in sorted order.
    * First, the current scan is moved to the next record.
    * Then the lowest record of the two scans is found, and that
    * scan is chosen to be the new current scan.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() { //where logic is?
      if (currentscan != null) {
         if (currentscan == s1) {
            hasmore1 = s1.next();
         }
         else if (currentscan == s2)
            hasmore2 = s2.next();
      }
      
      if (!hasmore1 && !hasmore2)
         return false;
      else if (hasmore1 && hasmore2) {
         if (comp.compare(s1, s2) < 0) {
            currentscan = s1;
         }
         else
            currentscan = s2;
      }
      else if (hasmore1) {
         currentscan = s1;
      }
      else if (hasmore2) {
         currentscan = s2;
      }
         
      return true;
   }
   
   /**
    * Closes the two underlying scans.
    * @see simpledb.query.Scan#close()
    */
   public void close() {
      s1.close();
      if (s2 != null)
         s2.close();
   }
   
   /**
    * Gets the Constant value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getVal(java.lang.String)
    */
   public Constant getVal(String fldname) {
      return currentscan.getVal(fldname);
   }
   
   /**
    * Gets the integer value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getInt(java.lang.String)
    */
   public int getInt(String fldname) {
      return currentscan.getInt(fldname);
   }
   
   /**
    * Gets the string value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getString(java.lang.String)
    */
   public String getString(String fldname) {
      return currentscan.getString(fldname);
   }
   
   /**
    * Returns true if the specified field is in the current scan.
    * @see simpledb.query.Scan#hasField(java.lang.String)
    */
   public boolean hasField(String fldname) {
      return currentscan.hasField(fldname);
   }
   
   /**
    * Saves the position of the current record,
    * so that it can be restored at a later time.
    */
   public void savePosition() {
      RID rid1 = s1.getRid();
      RID rid2 = (s2 == null) ? null : s2.getRid();
      savedposition = Arrays.asList(rid1,rid2);
   }
   
   /**
    * Moves the scan to its previously-saved position.
    */
   public void restorePosition() {
      RID rid1 = savedposition.get(0);
      RID rid2 = savedposition.get(1);
      s1.moveToRid(rid1);
      if (rid2 != null)
         s2.moveToRid(rid2);
   }
}
