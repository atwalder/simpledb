package simpledb.index.hash;
import simpledb.record.*;
import simpledb.query.*;
import simpledb.index.Index;
import simpledb.tx.Transaction;

//CS Project 2: Added class for extensible hash index
public class ExtensibleHashIndex implements Index{
    //CS Project 2: Same variables as HashIndex, but remove NUM_BUCKETS and use global depth instead
    //public static int NUM_BUCKETS = 100;
    //CS Project 2: MAX_SIZE limits the maximum size of the bucket...
    public static int MAX_SIZE = 3; //should probably set it to something slightly larger...
    private String idxname;
    private Schema sch;
    private Transaction tx;
    private Constant searchkey = null;
    private TableScan ts = null;
    //CS Project 2: Variables specific to Extensible Hashing
    private int globaldepth;
    private Transaction globalTX;
    private TableInfo globalTI;
    private TableScan globalTS;

    private String currTblname;

    /**
     * Opens a hash index for the specified index.
     * @param idxname the name of the index
     * @param sch the schema of the index records
     * @param tx the calling transaction
     */
    public ExtensibleHashIndex(String idxname, Schema sch, Transaction tx) {
        this.idxname = idxname;
        this.sch = sch;
        this.tx = tx;
        Schema gsch = new Schema();
        gsch.addIntField("label"); //CS Project 2: keeps track of the table's spot/label
        gsch.addStringField("filename", 20); //CS Project 2: keeps track of the table's file name
        gsch.addIntField("LocalDepth"); //CS Project 2: keeps track of the table's local depth
        globalTI = new TableInfo("global", gsch);
        globalTX = new Transaction();
        globalTS = new TableScan(globalTI, globalTX);
        if(!globalTS.next()){
            //CS Project 2: If globalTS is empty,set it up and insert the global table entries for the initial two buckets
            globaldepth = 1;
            globalTS.insert();
            globalTS.setInt("label", 0);
            globalTS.setString("filename", (idxname + 0));
            globalTS.setInt("LocalDepth", globaldepth);

            globalTS.insert();
            globalTS.setInt("label", 1);
            globalTS.setString("filename", (idxname + 1));
            globalTS.setInt("LocalDepth", globaldepth);
        }
        else{
            int count = 0;
            globalTS.beforeFirst();
            while(globalTS.next()){
                count++;
            }
            //CS Project 2: calculate global depth based on how many buckets there are
            globaldepth = (int) (Math.log(count) / Math.log(2));
        }
    }

    /**
     * Positions the index before the first index record
     * having the specified search key.
     * The method hashes the search key to determine the bucket,
     * and then opens a table scan on the file
     * corresponding to the bucket.
     * The table scan for the previous bucket (if any) is closed.
     * @see simpledb.index.Index#beforeFirst(simpledb.query.Constant)
     */
    public void beforeFirst(Constant searchkey) {
        close();
        this.searchkey = searchkey;
        //CS Project 2: Changed the hashing method to be based off the global depth
        int bucket = searchkey.hashCode() % ((int)Math.pow(2, globaldepth));
        //int bucket = searchkey.hashCode() % NUM_BUCKETS;
        String tblname = idxname + bucket;
        //CS Project 2: Find the target bucket in global table and check its LD
        globalTS.beforeFirst();
        while(globalTS.next()){
            if(globalTS.getString("filename").equals(idxname + bucket)){
                //CS Project 2: If the LD of the target < GD, redirect based on LD
                if(globalTS.getInt("LocalDepth") < globaldepth){
                    bucket = searchkey.hashCode() % ((int)Math.pow(2, globalTS.getInt("LocalDepth")));
                    tblname = idxname + bucket;
                }
            }
        }
        //CS Project 2: Open up a TableScan of the target table once it's been decided
        TableInfo ti = new TableInfo(tblname, sch);
        ts = new TableScan(ti, tx);
        currTblname = tblname;
    }

    /**
     * Moves to the next record having the search key.
     * The method loops through the table scan for the bucket,
     * looking for a matching record, and returning false
     * if there are no more such records.
     * @see simpledb.index.Index#next()
     */
    public boolean next() {
        while (ts.next())
            if (ts.getVal("dataval").equals(searchkey))
                return true;
        return false;
    }

    /**
     * Retrieves the dataRID from the current record
     * in the table scan for the bucket.
     * @see simpledb.index.Index#getDataRid()
     */
    public RID getDataRid() {
        int blknum = ts.getInt("block");
        int id = ts.getInt("id");
        return new RID(blknum, id);
    }

    /**
     * Inserts a new record into the table scan for the bucket.
     * @see simpledb.index.Index#insert(simpledb.query.Constant, simpledb.record.RID)
     */
    //CS Project 2: Changed significantly to detect and organize splits on insertion
    //starting with the shitty implementation
    public void insert(Constant val, RID rid) {
        //CS Project 2: Check to see if the bucket it would go in is full, if so, divide
        beforeFirst(val);
        int fullCount = 0;
        while(ts.next()){
            fullCount++;
        }
        //CS Project 2: If we try to insert and determine that the target bucket is full...
        if(fullCount >= MAX_SIZE){
            //CS Project 2: Find the target's LD and see if it's less than GD
            globalTS.beforeFirst();
            while(globalTS.next()){
                if(globalTS.getString("filename").equals(currTblname)){
                    //CS Project 2: when you find the target, check its LocalDepth
                    int LD = globalTS.getInt("LocalDepth");
                    int targetLabel = globalTS.getInt("label");
                    //CS Project 2: if it's less than the global depth...
                    if(LD < globaldepth){
                        //CS Project 2: scan the global table for every bucket that points to this with its current LD and increment its LD (this includes our original target).
                        globalTS.beforeFirst();
                        while(globalTS.next()){
                            if(globalTS.getInt("label") % LD == targetLabel){
                                globalTS.setInt("LocalDepth", globalTS.getInt("LocalDepth") + 1);
                            }
                        }
                    }
                    //CS Project 2: otherwise, if LD is equal to the GD, increase GD
                    else if(LD == globaldepth){
                        increaseGD(targetLabel);
                    }
                }
            }
            //CS Project 2: Then, once whatever happened is complete, re-insert the existing values
            refresh();
            //CS Project 2: then try inserting the new value again
            insert(val, rid);
        }
        else{
            //CS Project 2: if there's room in the target bucket, (finally) actually insert the record and its info
            ts.insert();
            ts.setInt("block", rid.blockNumber());
            ts.setInt("id", rid.id());
            ts.setVal("dataval", val);
        }
    }

    //CS Project 2: should re-insert everything to reorganize when buckets expand/double
    public void refresh(){
        Constant holdVal;
        RID holdRID;
        //CS Project 2: for each bucket...
        for (int i = 0; i < (int)Math.pow(2, globaldepth); i++){
            //CS Project 2: ...open up the table scan of that bucket...
            String tblname = idxname + i;
            TableInfo ti = new TableInfo(tblname, sch);
            ts = new TableScan(ti, tx);
            //CS Project 2: ...iterate through the records...
            while(ts.next()){
                //CS Project 2: ...then for each, grab the relevant information, delete the record, reinsert it!
                holdVal = ts.getVal("dataval");
                holdRID = new RID(ts.getInt("block"), ts.getInt("id"));
                ts.delete();
                insert(holdVal, holdRID);
            }
        }
    }

    //CS Project 2: doubles the buckets, then increases global depth
    public void increaseGD(int expandedLabel){
        //CS Project 2: count the number of buckets
        int numbuckets = 0;
        globalTS.beforeFirst();
        while(globalTS.next()){
            numbuckets++;
        }
        for(int i = numbuckets; i < numbuckets * 2; i++){
            globalTS.insert();
            globalTS.setInt("label", i);
            //any shenanigans with making it point to the table itself? Maybe too complicated to do rn
            globalTS.setString("filename", (idxname + i));
            globalTS.setInt("LocalDepth", globaldepth + 1);
        }
        //CS Project 2: increase the LD of the target and it's mirror
        int LD;
        globalTS.beforeFirst();
        while(globalTS.next()){
            //CS Project 2: If the label is either the one that was changed or its "mirror", increase the LD by 1 to match new GD
            if((globalTS.getInt("label") == expandedLabel) || (globalTS.getInt("label") == (expandedLabel+numbuckets))){
                LD = globalTS.getInt("LocalDepth");
                globalTS.setInt("LocalDepth", (LD + 1));
            }
        }
        globaldepth++;
    }

    /**
     * Deletes the specified record from the table scan for
     * the bucket.  The method starts at the beginning of the
     * scan, and loops through the records until the
     * specified record is found.
     * @see simpledb.index.Index#delete(simpledb.query.Constant, simpledb.record.RID)
     */
    public void delete(Constant val, RID rid) {
        beforeFirst(val);
        while(next())
            if (getDataRid().equals(rid)) {
                ts.delete();
                return;
            }
    }

    /**
     * Closes the index by closing the current table scan.
     * @see simpledb.index.Index#close()
     */
    public void close() {
        if (ts != null)
            ts.close();
    }

    /**
     * Returns the cost of searching an index file having the
     * specified number of blocks.
     * The method assumes that all buckets are about the
     * same size, and so the cost is simply the size of
     * the bucket.
     * @param numblocks the number of blocks of index records
     * @param rpb the number of records per block (not used here)
     * @return the cost of traversing the index
     */
    //Not static anymore, since globaldepth changes...
    public int searchCost(int numblocks, int rpb){
        return numblocks / (int)Math.pow(2, globaldepth);
    }
    /*
    public static int searchCost(int numblocks, int rpb){
        return numblocks / ExtensibleHashIndex.NUM_BUCKETS;
    }
    */

    //CS Project 2: toString
    public String toString(){
        StringBuilder result = new StringBuilder();
        //for each bucket...
        result.append("\n----EXTENSIBLE HASH " + idxname + "----\n");
        for (int i = 0; i < (int)Math.pow(2, globaldepth); i++){
            //...open up the table scan of that bucket...
            String tblname = idxname + i;
            result.append("Bucket: " + i + ". Tablename: " + tblname + "--\n");
            TableInfo ti = new TableInfo(tblname, sch);
            ts = new TableScan(ti, tx);
            //...iterate through the records...
            while(ts.next()){
                //...and put their info in the result!
                result.append("Block: " + ts.getInt("block") + "  ID: " + ts.getInt("id") + "  DataVal: " + ts.getVal("dataval") + "\n");
            }
        }
        return result.toString();
    }
}
