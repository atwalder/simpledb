package simpledb.index.hash;

public class ExtensibleHashTesting {

    //CS Project 2: Testing the logic of the Extensible Hash Map using a dummy class
    public static void main(String[] args){
        //CS Project 2: Simple example, check to see if it can properly do a split from GD 1 to 2
        DummyExtensibleHashIndex test = new DummyExtensibleHashIndex("test");
        test.insert(0);
        test.insert(1);
        test.insert(2);
        test.insert(3);
        test.insert(5);
        System.out.println(test.toString());
        test.insert(7);
        System.out.println(test.toString());

        //CS Project 2: Real test, solving HW3 Problem 1.
        DummyExtensibleHashIndex testHW = new DummyExtensibleHashIndex("testHW");
        //CS Project 2: Setup
        testHW.insert(4);
        testHW.insert(10);
        testHW.insert(5);
        testHW.insert(9);
        System.out.println(testHW.toString());

        //CS Project 2: Add 20, no split
        testHW.insert(20);
        System.out.println(testHW.toString());

        //CS Project 2: Add 33, no split
        testHW.insert(33);
        System.out.println(testHW.toString());

        //CS Project 2: Add 13, double split
        testHW.insert(13);
        System.out.println(testHW.toString());

        //CS Project 2: Add 14, one split
        testHW.insert(14);
        System.out.println(testHW.toString());

        //at least for dummy, next definitely skips the 'first' thing
        //the 'next' thing def doesn't work rn, but with new toString it exists
        //need to make sure that the
        //needed to tweak the way that it increases tables, due to how at GD 1 it has 2 tables, so can't use GD^2 exclusively
        //make sure that refresh grabs all the values right. For dummy, it grabs all 3 but the real one is different
        //when splitting: only update the LD of the one that comes from the split?
        //

    }
}
