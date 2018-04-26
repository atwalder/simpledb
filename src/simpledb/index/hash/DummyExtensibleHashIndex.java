package simpledb.index.hash;

import simpledb.index.Index;
import simpledb.query.Constant;

import java.util.ArrayList;

//CS Project 2: Dummy class for testing the logic of Extensible Hash Map. For schema, only field is int
//CS Project 2: In the event the actual SimpleDB ExtensibleHashIndex clas doesn't work... you can see the usage and logic here too
//CS Project 2: Additional note; rather than having pointers to buckets, this just has a list. So, 'pointers' that should go to another bucket do redirect, but also have their own buckets (that remain empty until used)
public class DummyExtensibleHashIndex{
    //CS Project 2: public static int MAX_SIZE = 3; Max size is assumed to be 3 in all functions/classes for manageable testing
    private String idxname;
    private int searchkey;
    ArrayList<DummyTable> tables = new ArrayList<DummyTable>(); //CS Project 2: fills the role of the table files, keeps track of tables
    private int currentTable; //CS Project 2: keeps track of the table currently being examined, fills the role of TableScan ts
    private int globaldepth;

    /**
     * Opens a hash index for the specified index.
     * @param idxname the name of the index
     */
    public DummyExtensibleHashIndex(String idxname) {
        this.idxname = idxname;
        this.globaldepth = 1;
        tables.add(0, new DummyTable(globaldepth)); //CS Project 2: creates the initial 2 tables
        tables.add(1, new DummyTable(globaldepth));
    }

    /**
     * Positions the index before the first index record
     * having the specified search key.
     * The method hashes the search key to determine the bucket,
     * and then opens a table scan on the file
     * corresponding to the bucket.
     * The table scan for the previous bucket (if any) is closed.
     * @see Index#beforeFirst(Constant)
     */
    //CS Project 2: Pretty much the same as in the actual Extensible Hash Class
    public void beforeFirst(int searchkey) {
        this.searchkey = searchkey;
        int bucket = searchkey % ((int)Math.pow(2, globaldepth));
        currentTable = bucket;
        //Redirects if the table it is targeting has less LD than the current GD
        if(tables.get(bucket).LocalDepth < globaldepth){
            System.out.print("Retargeted an add for " + bucket);
            bucket = searchkey % ((int)Math.pow(2, tables.get(bucket).LocalDepth));
            currentTable = bucket;
            System.out.println(" to " + bucket);
        }
    }

    /**
     * Moves to the next record having the search key.
     * The method loops through the table scan for the bucket,
     * looking for a matching record, and returning false
     * if there are no more such records.
     * @see Index#next()
     */
    //CS Project 2: Pretty much the same as in the actual Extensible Hash Class
    public boolean next() {
        while (tables.get(currentTable).next())
            if (tables.get(currentTable).getVal() == searchkey)
                return true;
        return false;
    }

    /**
     * Inserts a new record into the table scan for the bucket.
     */
    public void insert(int val) {

        //CS Project 2: Check to see if the bucket it would go in is full, if so, divide
        beforeFirst(val);
        System.out.println("attempting to insert " + val + " in bucket " + currentTable);
        //CS Project 2: Dummy tables use their own function to return if they're full or not
        if(tables.get(currentTable).fullCount()){ //CS Project 2: If the bucket we're trying to insert into is full...
            //CS Project 2: ...check its the local depth to see if it's less than the global depth and, if so, increase it.
            int curLD = tables.get(currentTable).LocalDepth;
            if(curLD < globaldepth) {
                System.out.println("Local depth of bucket " + currentTable + ": " + tables.get(currentTable).LocalDepth);
                //CS Project 2: For each table, check if that table points to the bucket we've decided to increase (includes that bucket itself)
                for(int i = 0; i < tables.size(); i++){
                    if(i % tables.get(i).LocalDepth == currentTable){
                        tables.get(i).LocalDepth++;
                    }
                }
                //fixLD(); WORKS WITHOUT THIS NOW. NICE. IT WAS SHIT ANYWAYS.
                System.out.println("Increased to :" + tables.get(currentTable).LocalDepth);
            }
            //CS Project 2: If the LD is already equal to the GD, then increase the GD.
            else if(tables.get(currentTable).LocalDepth == globaldepth){
                increaseGD();
            }
            //CS Project 2: then refresh - re-insert all of the existing records
            System.out.println("refreshing...");
            refresh();
            System.out.println("refreshed!");
            insert(val);
        }
        else{
            //CS Project 2: When there's room, finally do the actual insertion of values
            tables.get(currentTable).insert();
            tables.get(currentTable).setVal(val);
        }
    }
/*  //THIS CAN GOOOOO
    public void fixLD(){
        int half = tables.size() / 2;
        int LD;
        for(int i = 0; i < half; i++){
            LD = tables.get(i).LocalDepth;
            tables.get(i + half).LocalDepth = LD;
        }
    }
*/
    //fix this yo
    //current version is way different from the actual but for testing logic it's fine
    //CS Project 2: should re-insert everything, to reorganize records when buckets expand/double
    public void refresh(){
        int hold;
        int hold2;
        int hold3;
        DummyTable curr;
        for (int i = 0; i < tables.size(); i++){
            curr = tables.get(i);
            curr.beforeFirst();
            hold = curr.getVal();
            curr.setVal(0);
            curr.next();
            hold2 = curr.getVal();
            curr.setVal(0);
            curr.next();
            hold3 = curr.getVal();
            curr.setVal(0);

            ////CS Project 2: only bother re-inserting if not 0 (which represents empty here)
            if(hold != 0)
                insert(hold);
            if(hold2 != 0)
                insert(hold2);
            if(hold3 != 0)
                insert(hold3);
        }
    }

    //CS Project 2: doubles the buckets, assigning them the proper LD, then increases global depth
    public void increaseGD(){
        //CS Project 2: currenttable is the one that was too full... so only increase the LD to match GD for that and the one it s
        //CS Project 2: add the new tables to the 'global' table
        int numBuckets = tables.size();
        int LD;
        for(int i = numBuckets; i < numBuckets * 2; i++){
            System.out.println("Adding bucket: " + (i));
            //CS Project 2: The LocalDepth of the first and second half of the buckets are mirrors, so do this to confirm that it's all good.
            LD = tables.get(i - numBuckets).LocalDepth;
            System.out.println("Mirrored for bucket " + i + " = " + LD);
            tables.add(i, new DummyTable(LD));
        }
        //CS Project 2: Once the doubling has happened, increase the LD of the target bucket and its mirror to complete the split
        tables.get(currentTable).LocalDepth++;
        tables.get(currentTable + numBuckets).LocalDepth++;
        globaldepth++;
        System.out.println("increased");
    }

    /**
     * Deletes the specified record from the table scan for
     * the bucket.  The method starts at the beginning of the
     * scan, and loops through the records until the
     * specified record is found.
     */
    //CS Project 2: Pretty much the same as in the actual Extensible Hash Class
    public void delete(int val) {
        beforeFirst(val);
        while(tables.get(currentTable).next())
            if (tables.get(currentTable).getVal() == (val)) {
                tables.get(currentTable).setVal(0);
                return;
            }
    }

    //CS Project 2: Pretty much the same as in the actual Extensible Hash Class
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("\n----EXTENSIBLE HASH " + idxname + "----\n");
        for (int i = 0; i < (int)Math.pow(2, globaldepth); i++){
            result.append("Bucket: " + i + "--\n");
            //CS Project 2: Except, the dummy tables have their own toString, which makes things a bit easier
            result.append(tables.get(i).toString());
        }
        return result.toString();
    }
}
