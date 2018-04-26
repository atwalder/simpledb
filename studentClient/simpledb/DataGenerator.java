import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import simpledb.opt.ExploitSortQueryPlanner;
import simpledb.parse.QueryData;
import simpledb.query.Predicate;
import simpledb.remote.SimpleDriver;

public class DataGenerator {
	//Use the following java code to create data.
	/******************************************************************/
	 final static int maxSize=100;
	 
	 /**
	  * @param args
	  */
	 public static void main(String[] args) {
	  // TODO Auto-generated method stub
	  Collection<String> tables = new ArrayList<String>();
	  Collection<String> fields = new ArrayList<String>();
	  Predicate p = new Predicate(); //corresponding to true
	  Connection conn=null;
	  Driver d = new SimpleDriver();
	  String host = "localhost"; //you may change it if your SimpleDB server is running on a different machine
	  String url = "jdbc:simpledb://" + host;
	  String qry="Create table test1" +
	  "( a1 int," +
	  "  a2 int"+
	  ")";
	  Random rand=null;
	  Statement s=null;
	  try {
	   conn = d.connect(url, null);
	   s=conn.createStatement();
	   s.executeUpdate("Create table test1" +
	     "( a1 int," +
	     "  a2 int"+
	   ")");
	   s.executeUpdate("Create table test2" +
	     "( a1 int," +
	     "  a2 int"+
	   ")");
	   s.executeUpdate("Create table test3" +
	     "( a1 int," +
	     "  a2 int"+
	   ")");
	   s.executeUpdate("Create table test4" +
	     "( a1 int," +
	     "  a2 int"+
	   ")");
	   s.executeUpdate("Create table test5" +
	     "( a1 int," +
	     "  a2 int"+
	   ")");

	   s.executeUpdate("create sh index idx1 on test1 (a1)");
	   s.executeUpdate("create ex index idx2 on test2 (a1)");
	   s.executeUpdate("create bt index idx3 on test3 (a1)");
	   for(int i=1;i<6;i++)
	   {
	    if(i!=5)
	    {
	     rand=new Random(1);// ensure every table gets the same data
	     for(int j=0;j<maxSize;j++)
	     {
	      s.executeUpdate("insert into test"+i+" (a1,a2) values("+rand.nextInt(1000)+","+rand.nextInt(1000)+ ")");
	     }
	    }
	    else//case where i=5
	    {
	     for(int j=0;j<maxSize/2;j++)// insert 10000 records into test5
	     {
	      s.executeUpdate("insert into test"+i+" (a1,a2) values("+j+","+j+ ")");
	     }
	    }
	   }
	   tables.add("test4");
	   tables.add("test5");
	   fields.add("a1");
	   fields.add("a2");
	   //QueryData data = new QueryData(fields, tables, p);
	   //ExploitSortQueryPlanner e = new ExploitSortQueryPlanner();
	   //e.createPlan(data, tx);
	   long startTime1 = System.nanoTime();
	   s.executeQuery("select a1, a2 from test4, test5 where a1 = a2");
	   System.out.println("done!!!!!");
	   long endTime1 = System.nanoTime();
	   long totalTime1 = endTime1 - startTime1;
	   long startTime2 = System.nanoTime();
	   s.executeQuery("select a1, a2 from test4, test5 where test4.a1 = test5.a2");
	   System.out.println("done!!!!! 2");
	   long endTime2 = System.nanoTime();	
	   long totalTime2 = endTime2 - startTime2;
	   System.out.println(totalTime1);
	   System.out.println(totalTime2);
	   conn.close();

	  } catch (SQLException e) {
	   // TODO Auto-generated catch block
	   e.printStackTrace();
	  }finally
	  {
	   try {
	    conn.close();
	   } catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	   }
	  }
	 }
	



}
