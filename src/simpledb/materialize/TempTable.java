package simpledb.materialize;

import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.*;

/**
 * A class that creates temporary tables.
 * A temporary table is not registered in the catalog.
 * The class therefore has a method getTableInfo to return the 
 * table's metadata. 
 * @author Edward Sciore
 */
//project 2: UPDARED TO KEEP TRACK OF TBLNAMES
public class TempTable {
   private static int nextTableNum = 0;
   private TableInfo ti;
   private Transaction tx;
   private String tblname; //project 2: added copy through ti 
   
   /**
    * Allocates a name for for a new temporary table
    * having the specified schema.
    * @param sch the new table's schema
    * @param tx the calling transaction
    */
   public TempTable(Schema sch, Transaction tx) {
      String tblname = nextTableName();
      ti = new TableInfo(tblname, sch);
      this.tblname = tblname; //project 2: set temptables tablename
      this.tx = tx;
   }
   
//project 2: added getter for temp table tablename
   public String getTablename(){ //added
	   return this.tblname;
   }
   
   /**
    * Opens a table scan for the temporary table.
    */
   public UpdateScan open() {
	  ti.setSorted(0); //project 2: added because a table is scanned by UpdateScan reset to false
      return new TableScan(ti, tx);
   }
   
   /**
    * Return the table's metadata.
    * @return the table's metadata
    */
   public TableInfo getTableInfo() {
      return ti;
   }
   
   private static synchronized String nextTableName() {
      nextTableNum++;
      return "temp" + nextTableNum;
   }
}