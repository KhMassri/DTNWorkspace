
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;


public class M {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	String id = "m8";
	String id2 = "m2&m8";

	String[] ids = id.split("&");
	String[] ids2 = id2.split("&");
	
	//System.out.print(ids.length);
	
	List<Set<String>> s= new ArrayList<Set<String>>();
	Set<String> el =new HashSet<String>();
	Set<String> el2 =new HashSet<String>();
	List<String> list = new ArrayList<String>();
	list.add("b");
	list.add("a");
	list.add("a");
	list.add("a");
	list.add("b");
	
	//ListIterator<String> iterator = list.listIterator();
	int i=0;
	while (i < list.size()) {
	    String str = list.get(i);
	    if(str.equals("a"))
	    {
	    	list.remove(i);
	    	continue;
	    }
	    	
	    i++;
	    
	} 
	
	
	
	System.out.print(list);
	
	


	Collections.addAll(el2, ids2);

	String idd = null;
	
	idd = (el2.toArray()[0].equals(id))?((String) el2.toArray()[1]):((String) el2.toArray()[0]);

			
	System.out.println(id2.contains("&"));

	
	/*if(ids.length>1)
		System.out.print(new StringBuffer(id).reverse().toString());
	else
		System.out.print(id);*/



}		




}
