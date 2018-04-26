package simpledb.index.hash;

//CS Project 2: Dummy class replacing the tablescans for testing the logic of Extensible Hash Map (through the dummy version of that class)
//CS Project 2: Implements its own version of all relevant functions from table scan, using an array to store contents of bucket instead of files
//CS Project 2: Assumes bucket size of 3 and only containing ints
public class DummyTable {
    int[] contents = new int[3];
    int index;
    int LocalDepth;

    public DummyTable(int LD){
        LocalDepth = LD;
    }

    public void beforeFirst(){index = 0;}

    public boolean next(){
        if(index < 2){
            index++;
            return true;
        }
        else{
            return false;
        }
    }

    public void close(){
        //nothing here, yeah?
    }

    public int getVal(){
        //0 if empty
        return contents[index];
    }

    public void insert(){
        index = 0;
        while(contents[index] != 0){
            if(index == 2){
                return;
            }
            else {
                index++;
            }
        }
    }

    public void setVal(int val){
        contents[index] = val;
    }

    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("LocalDepth: " + LocalDepth + "\n");
        result.append("DataVal: " + contents[0] + "\n");
        result.append("DataVal: " + contents[1] + "\n");
        result.append("DataVal: " + contents[2] + "\n");
        return result.toString();
    }

    public boolean fullCount(){
        int count = 0;
        for(int i = 0; i < 3; i++){
            if(contents[i] != 0){
                count++;
            }
        }
        if(count == 3){
            return true;
        }
        else{
            return false;
        }
    }
}
