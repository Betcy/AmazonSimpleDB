//package simpleDBExample;

/**
 * CS510: Database Management in the Cloud
 * Spring 2013
 * Course Project - Working with SimpleDB
 * 
 * Brianna Shade
 * bshade@pdx.edu
 * 
 * Preliminary code structure provided by Amazon's sample SimpleDB program
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.util.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
//import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataResult;
/**
 * This sample demonstrates how SimpleDB can be used to store example graph data.
 */
public class SimpleDBSample{
	public static AmazonSimpleDB sdb;
	private static final int WEIGHTED = 1;
	private static final int DIRECTED = 0;
	
	private static int NodeSize; // Number of nodes in the domain
	private static Sack<Integer>[] adj;

	
	/**
     * This credentials provider implementation loads the AWS credentials
     * from a properties file at the root of the classpath.     * 
     */
	public static void init(){
		sdb = new AmazonSimpleDBClient(new ClasspathPropertiesFileCredentialsProvider());
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sdb.setRegion(usWest2);

        System.out.println("===========================================");
        System.out.println("CS510: Database Management in the Cloud Class Project");
        System.out.println("SimpleDB sample graph data usage");
        System.out.println("===========================================\n");
	}
	
	/**
	 * Prints all currently available domains
	 */
	public static void printDomains(){
		// List domains
        System.out.println("Listing all domains in your account:");
        for (String domainName : sdb.listDomains().getDomainNames()) {
            System.out.println("  " + domainName);
        }
        System.out.println();
	}
	
	/**
	 * This creates a standard data node with provided node ID, Name, and Gender
	 * @param id
	 * @param name
	 * @param gender
	 * @return = newly created node
	 */
	public static ReplaceableItem createNode(int id, String name, String gender){
		ReplaceableItem node = new ReplaceableItem(Integer.toString(id)).withAttributes(
                new ReplaceableAttribute("Name", name, true),
                new ReplaceableAttribute("Gender", gender, true));
		
		return node;
	}
	
	/**
	 * Overloaded method to create a standard data node with provided node ID, Name, Gender, and optional Occupation
	 * @param id
	 * @param name
	 * @param gender
	 * @param occ
	 * @return = the newly created node
	 */
	public static ReplaceableItem createNode(int id, String name, String gender, String occ){
		ReplaceableItem node = createNode(id, name, gender).withAttributes(
                new ReplaceableAttribute("Occupation", occ, true));
		
		return node;
	}
	
	/**
	 * Calculates the next available node ID.  Increments from last node, so will not reuse
	 * IDs previously deleted (unless IDs were deleted from the end of the list
	 * @param nodes = currently existing nodes
	 * @return = next available (unused) integer ID
	 */
	public static int nextAvailID(List<ReplaceableItem> nodes){
		if(nodes.size() == 0) return 1;
		
		int id = Integer.parseInt(nodes.get(nodes.size()-1).getName());
		id++;
		return id;
	}
	
	/**
	 * Appends new weighted relationship data to existing nodes
	 * @param node = node to be updated
	 * @param ids = list of comma-delimited IDs to be appended
	 * @param relations = list of associated relations to be appended
	 * @param weights = list of associated weights to be added
	 * @return = updated node
	 */
	public static ReplaceableItem addWeightedRelationships(ReplaceableItem node, String ids, String relations, String weights){	
		if((StringUtils.countOccurrencesOf(ids, ",") != StringUtils.countOccurrencesOf(relations, ","))
				|| (StringUtils.countOccurrencesOf(relations, ",") != StringUtils.countOccurrencesOf(weights, ",")))
			throw new IllegalArgumentException("ids, relations, and weights must all contain " +
					"the same number of comma-delimited values:\nids: " + ids + "\nrelations: " + relations + "\nweights: " + weights);
		String relNodes = "";
		String rels = "";
		String wghts = "";
		
		List<ReplaceableAttribute> repAtts = new ArrayList<ReplaceableAttribute>();
		
		for(ReplaceableAttribute ra : node.getAttributes()){
			if(ra.getName() == "RelatedNodes"){
				relNodes = ra.getValue();
				if(!relNodes.equals("") && !ids.equals("")) relNodes += ",";
			}
			else if(ra.getName() == "Relationships"){
				rels = ra.getValue();
				if(!rels.equals("") && !relations.equals("")) rels += ", ";
			}
			else if(ra.getName() == "Weights"){
				wghts = ra.getValue();
				if(!wghts.equals("") && !weights.equals("")) wghts += ", ";
			}
			else repAtts.add(ra);
		}

		relNodes += ids;
		rels += relations;
		wghts += weights;		
		
		repAtts.add(new ReplaceableAttribute("RelatedNodes", relNodes, true));
		repAtts.add(new ReplaceableAttribute("Relationships", rels, true));
		repAtts.add(new ReplaceableAttribute("Weights", wghts, true));
        
		node.setAttributes(repAtts);
		
		return node;
	}
	
	/**
	 * Appends new directed relationship data to existing nodes
	 * @param node = node to be updated
	 * @param toIds = list of comma-delimited to-IDs to be appended
	 * @param toRels = list of associated to-relations to be appended
	 * @param fromIds = list of comma-delimited from-IDs to be appended
	 * @param fromRels = list of associated from-relations to be appended
	 * @return = updated node
	 */
	public static ReplaceableItem addDirectedRelationships(ReplaceableItem node, String toIds, String toRels, String fromIds, String fromRels){		
		if((StringUtils.countOccurrencesOf(toIds, ",") != StringUtils.countOccurrencesOf(toRels, ","))
				|| (StringUtils.countOccurrencesOf(fromIds, ",") != StringUtils.countOccurrencesOf(fromRels, ",")))
			throw new IllegalArgumentException("IDs and relations must contain the same number of comma-delimited values:" +
					"\ntoIds: " + toIds + "\ntoRels: " + toRels + "\nfromIds: " + fromIds + "\nfromRels: " + fromRels);
		
		String toRelNodes = "";
		String toRelns = "";
		String fromRelNodes = "";
		String fromRelns = "";
		
		List<ReplaceableAttribute> repAtts = new ArrayList<ReplaceableAttribute>();
		
		for(ReplaceableAttribute ra : node.getAttributes()){
			if(ra.getName() == "ToNodes"){
				toRelNodes = ra.getValue();
				if(!toRelNodes.equals("") && !toIds.equals("")) toRelNodes += ",";
			}
			else if(ra.getName() == "ToRelations"){
				toRelns = ra.getValue();
				if(!toRelns.equals("") && !toRels.equals("")) toRelns += ", ";
			}
			else if(ra.getName() == "FromNodes"){
				fromRelNodes = ra.getValue();
				if(!fromRelNodes.equals("") && !fromIds.equals("")) fromRelNodes += ",";
			}
			else if(ra.getName() == "FromRelations"){
				fromRelns = ra.getValue();
				if(!fromRelns.equals("") && !fromRels.equals("")) fromRelns += ", ";
			}
			else repAtts.add(ra);
		}

		toRelNodes += toIds;
		toRelns += toRels;
		fromRelNodes += fromIds;
		fromRelns += fromRels;		
		
		repAtts.add(new ReplaceableAttribute("ToNodes", toRelNodes, true));
		repAtts.add(new ReplaceableAttribute("ToRelations", toRelns, true));
		repAtts.add(new ReplaceableAttribute("FromNodes", fromRelNodes, true));
		repAtts.add(new ReplaceableAttribute("FromRelations", fromRelns, true));
        
		node.setAttributes(repAtts);
		
		return node;
	}
	
	/**
     * Creates an array of SimpleDB ReplaceableItems populated with sample weighted data
     * @return = an array of sample item data
     */
	public static List<ReplaceableItem> createWeightedData() {
        List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();

        ReplaceableItem node = createNode(nextAvailID(sampleData), "George Bluth, Sr.", "Male", "Fugitive");
        node = addWeightedRelationships(node, "2", "Brother", "0.15");										//example of single relationship
        node = addWeightedRelationships(node, "4,6", "Father-in-Law, Wife", "0.72, 0.48");					//example of adding multiple relationships
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "Oscar Bluth", "Male");
        node = addWeightedRelationships(node, "1,3", "Brother, Uncle", "0.15, 0.5");
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "George Oscar \"GOB\" Bluth", "Male", "Magician");
        node = addWeightedRelationships(node, "2,8", "Nephew, Brother", "0.5, 0.9");
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "Tobias Funke", "Male", "Unemployed Actor");
        node = addWeightedRelationships(node, "1,5", "Son-in-Law, Husband", "0.72, 0.03");
        sampleData.add(node);

        return sampleData;
    }
	
	/**
     * Creates an array of SimpleDB ReplaceableItems populated with sample directed data
     * @return = an array of sample item data
     */
    public static List<ReplaceableItem> createDirectedData() {
        List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();

        ReplaceableItem node = createNode(nextAvailID(sampleData), "George Bluth, Sr.", "Male", "Fugitive");
        node = addDirectedRelationships(node, "2,4", "Brother, Father-in-Law", "", "");	//to-nodes only
        node = addDirectedRelationships(node, "", "", "6", "Husband");						//from-nodes only
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "Oscar Bluth", "Male");
        node = addDirectedRelationships(node, "3", "Uncle", "1", "Brother");
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "George Oscar \"GOB\" Bluth", "Male", "Magician");
        node = addDirectedRelationships(node, "8", "Brother", "2", "Uncle");
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "Tobias Funke", "Male", "Unemployed Actor");
        node = addDirectedRelationships(node, "5", "Husband", "1,5", "Father-in-Law, Wife");
        sampleData.add(node);
        
        node = createNode(nextAvailID(sampleData), "Lindsay Funke", "Female", "Activist");
        node = addDirectedRelationships(node, "4", "Wife", "4,8", "Husband, Sister");
        sampleData.add(node);

        return sampleData;
    }
    
    /**
     * Creates an array of SimpleDB ReplaceableItems populated from read-in sample directed data file
     * @return = an array of sample item data
     */
    public static List<ReplaceableItem> readInGraphData(String input, int graphType) {
        List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
        
        FileReader fr;
		BufferedReader buff;
		
		try{
			fr = new FileReader(input);
			buff = new BufferedReader(fr);
			
			String line;
			while((line = buff.readLine()) != null){
				//test line
				if(line == "") continue;
				String[] values = line.split("\\|");					//expects a regex - backslash must also be escaped
				
				int expLen;
				switch(graphType){
				case WEIGHTED: expLen = 6; break;
				case DIRECTED: expLen = 7; break;
				default: throw new IllegalArgumentException("Invalid graphType provided: " + graphType);
				}
				
				if(values.length != expLen)
					System.err.println("Line must contain " + expLen + " pipe-delimited fields, even if blank " +
							"(contains " + values.length + ") - " + line);
				else{
					ReplaceableItem node = createNode(nextAvailID(sampleData), values[0], values[1], values[2]);
					if(graphType == WEIGHTED) node = addWeightedRelationships(node, values[3], values[4], values[5]);
					else node = addDirectedRelationships(node, values[3], values[4], values[5], values[6]);
					sampleData.add(node);
				}
			}
		}
		catch(IOException ex){
			System.err.println("Oh no! An error occurred!\n Error: " + ex);
			System.exit(0);
		}
        
        return sampleData;
    }
    
    /**
     * Sample select behavior
     * @param dom = domain from which to select the data
     */
    public static void selectData(String dom){
    	// Select data from a domain
        // Notice the use of backticks around the domain name in our select expression.
        String selectExpression = "select * from `" + dom + "`";
        System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        for (Item item : sdb.select(selectRequest).getItems()) {
            System.out.println("  Item");
            System.out.println("    Name: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
                System.out.println("      Attribute");
                System.out.println("        Name:  " + attribute.getName());
                System.out.println("        Value: " + attribute.getValue());
            }
        }
        System.out.println();
    }
    
    /**
     * Sample delete attributes behavior - NOT CURRENTLY IN USE
     * @param dom = domain from which to delete
     */
    public static void deleteAtts(String dom){
    	// Delete values from an attribute
        System.out.println("Deleting Fugitive attributes in node 1.\n");
        Attribute deleteValueAttribute = new Attribute("Occupation", "Fugitive");
        sdb.deleteAttributes(new DeleteAttributesRequest(dom, "1")
                .withAttributes(deleteValueAttribute));

        // Delete an attribute and all of its values
        System.out.println("Deleting attribute Occupation in node 1.\n");
        sdb.deleteAttributes(new DeleteAttributesRequest(dom, "1")
                .withAttributes(new Attribute().withName("Occupation")));
    }
    
    /**
     * Sample replace attributes behavior - NOT CURRENTLY IN USE
     * @param dom = domain upon which to replace
     */
    public static void replAtts(String dom){
    	// Replace an attribute
        System.out.println("Replacing Occupation of node 3 with Writer.\n");
        List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
        replaceableAttributes.add(new ReplaceableAttribute("Occupation", "Writer", true));
        sdb.putAttributes(new PutAttributesRequest(dom, "3", replaceableAttributes));
    }
    
    /**
     * Sample deleting an item - NOT CURRENTLY IN USE
     * @param dom = domain from which to delete
     */
    public static void deleteItem(String dom){
    	// Delete an item and all of its attributes
        System.out.println("Deleting node ID 4.\n");
        sdb.deleteAttributes(new DeleteAttributesRequest(dom, "4"));
    }
    
    /**
     * Deletes domain
     * @param dom = domain to delete
     */
    public static void deleteDom(String dom){
    	// Delete a domain
    	System.out.println("Deleting " + dom + " domain.\n");
    	sdb.deleteDomain(new DeleteDomainRequest(dom));
    }
    
    /**
     * Prints the FOF 
     */
	public static void retrieveFOF() {
		String NEWLINE = System.getProperty("line.separator");
		for (int i = 1; i <= NodeSize; i++) {
			System.out.print(i + ": ");
			for (int j : getAdjNode(i)) {
				for (int k : getAdjNode(j)) {
					if (k != i) {
						System.out.print(k + " ");
					}

				}
			}
			System.out.print(NEWLINE);
		}
		}

    /**
     * Compute graph transitive closure
     * @param dom = domain from which to select the data
     * @param graphType = Type of graph which can be "directed" or "weighted"
     */
    public static void TransitiveClosure(String dom, String graphType){

    	System.out.println("----Computing Transitive Closure-----");
    	System.out.println();
    	String selectExpression = "select Count(*) from `" + dom + "`";
        
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        
        Item item2= sdb.select(selectRequest).getItems().get(0);
        Attribute attr= item2.getAttributes().get(0);
        Integer NodeCount = Integer.parseInt(attr.getValue());
        System.out.println("Number of Nodes: " + NodeCount);
        if(NodeCount <= 1){
        	System.out.println("Sorry, number of nodes should be greather than 1 to compute transitive closure");
        	return;
        }
        
        System.out.println();
        System.out.println("Creating adjecancy matrix");
        System.out.println();
        Boolean[][] matrix;
        matrix = new Boolean[NodeCount][NodeCount]; // boolean is smaller than integer
        for(Integer i=0;i<NodeCount;i++)
        	for(Integer j=0;j<NodeCount;j++)
        		matrix[i][j]=false; // Initializing matrix
        
        // building a hash map that translates node id to node ordinal position in the matrix
        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        
        String otherNodesField;
        if(graphType=="directed") otherNodesField="ToNodes"; 
        else if(graphType=="weighted") otherNodesField = "RelatedNodes";
        else{
    		System.out.println("Error: unexpected graph type " + graphType);
    		return;
    	}
        
        selectExpression = "select " + otherNodesField + " from `" + dom + "`";
        selectRequest = new SelectRequest(selectExpression);
        
        System.out.println("Building the mapping");
        // Integer NodeID=0;
        for (Item item : sdb.select(selectRequest).getItems()) {
            //hm.put(item.getName(), NodeID); // add a mapping between nodeID and node ordinal position in matrix
        	hm.put(item.getName(), Integer.parseInt(item.getName()) -1); // map 1 to 1, 2 to 2 , .. etc
                                            // e,g we map nodes A,F,K  to  0,1,2
                                            // so for matrix(A,K) we can use matrix(0,2)
           // NodeID++;
        }

        String gg;

        for (Item item : sdb.select(selectRequest).getItems()) { // For each Node in the graph
        	if(item.getAttributes().get(0).getValue().isEmpty()) continue;
        	
        	String [] str= item.getAttributes().get(0).getValue().split(","); // we get ToNodes(or RealtedNodes)
        	for (Integer i=0;i<str.length;i++)
        	{
        		Integer x=(Integer) hm.get(item.getName()); // map NodeID to ordinal position
        		gg = str[i].trim();
        		Integer y= hm.get(gg);  // map the other node id 
        		matrix[x][y]=true;  // set matrix cell to true indicating an edge between the two nodes
        	}
        
        }

        System.out.println("Printing adjacency matrix");
        System.out.println("     1  2  3  4  5  6  7  8  9  10 11 12 13 14");
        System.out.println("---------------------------------------------");
        for (int i = 0; i < matrix.length; i++) 
        {
        	if(i+1<10)
        	   System.out.print(i+1 + " |");
        	else
        		System.out.print(i+1 + "|");	 
            for (int j = 0; j < matrix.length; j++)
            {
                int t = (matrix[i][j] == true)? 1 : 0;
                System.out.print("  " + t);
            }//inner for
            System.out.println();
        }//outer for
        
    	matrix = closure(matrix); // get the full transitive closure
    	
    	System.out.println();
    	System.out.println();
    	
    	System.out.println("Printing transitive closure matrix");
        System.out.println("     1  2  3  4  5  6  7  8  9  10 11 12 13 14");
        System.out.println("---------------------------------------------");
        for (int i = 0; i < matrix.length; i++) 
        {
        	if(i+1<10)
        	   System.out.print(i+1 + " |");
        	else
        		System.out.print(i+1 + "|");	 
            for (int j = 0; j < matrix.length; j++)
            {
                int t = (matrix[i][j] == true)? 1 : 0;
                System.out.print("  " + t);
            }//inner for
            System.out.println();
        }//outer for
        System.out.println();
        
        System.out.println("Done with transitive closure");
    }


	@SuppressWarnings("finally")
	public static Boolean[][] closure(Boolean[][] Matrix) 
    {
        
        int len = Matrix.length ;
        Boolean[][] adj = new Boolean[len][len];
        try{
            //Copy matrix into adj : deep copy
            for (int i = 0; i < len; i++) 
                System.arraycopy(Matrix[i], 0, adj[i], 0, len);

            for (int k = 0; k < len; k++) {
                for (int i = 0; i < len; i++) {
                    for (int j = 0; j < len; j++) {
                        adj[i][j] = (adj[i][j] || (adj[i][k] && adj[k][j]));
                    }//innermost for
                }//inner for
            }//outer for            
        }//try
        catch(ArrayIndexOutOfBoundsException e)
        {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Adjacency Matrix for Graph2 has Missing Values, Mapping Might not be correct..!!{0}", e);
        }//catch
        catch(Exception e)
        {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unexpected Error Occurred, Mapping Might not be correct..!!{0}", e);
        }//catch
        finally{
            return adj;
        }//Finally

    }//closure

    
    
    /**
     * Returns the number of nodes in the domain.
     */
    public int getNodeSize() { 
    	return NodeSize; 
    }
    
	/**
	 * Return the value of the attribute "RelatedNodes" from the domain and separate String to array and add each edge.
	 */
	@SuppressWarnings("unchecked")
	public static void getMultipleValuesofAttribute(String domain, String graphType) {
	
		//Retrieving the number of nodes in the domain
		DomainMetadataResult item =  sdb.domainMetadata(new DomainMetadataRequest(domain));
		NodeSize = item.getItemCount();
		
	
		if(NodeSize <=1){
			System.out.println("No sufficient Data in Domain to compute Bridges, number of nodes must be more than 1 to compute BRIDGES");
			return;
		}
          
		if (NodeSize <= 0)
			throw new IllegalArgumentException(
					"Number of nodes must be Non-Negative");
		adj = (Sack<Integer>[]) new Sack[NodeSize + 1];
		adj[0] = new Sack<Integer>();
		for (int v = 1; v <= NodeSize; v++) {
			adj[v] = new Sack<Integer>();
		}
		// To retrieve values of an attribute "ToNodes" or "RelatedNodes" of the domain and
				// separate the list to array

		String EdgeNodeField;
		if (graphType == "directed") {
			EdgeNodeField = "ToNodes";
		} else if (graphType == "weighted") {
			EdgeNodeField = "RelatedNodes";
		} else {
			System.out.println("Error: unexpected graph type " + graphType);
			return;
		}

		String[] arr1 = {};
		String selectExpression2 = "select " + EdgeNodeField + " from `" + domain + "`";


		SelectRequest selectRequest2 = new SelectRequest(selectExpression2);

		for (Item item2 : sdb.select(selectRequest2).getItems()) {

			for (Attribute attribute : item2.getAttributes()) {
				if (attribute.getName().equals(EdgeNodeField)) {
					arr1 = StringUtils
							.commaDelimitedListToStringArray(attribute
									.getValue());
					for (int i = 0; i < arr1.length; i++) {
						addAdjacentNodeData(Integer.parseInt(item2.getName()),
								Integer.parseInt(arr1[i]));
					}
				}

			}
		}

	}
    
	
	/**
	 * addAdjacentNodeData() - Adds the undirected edge between current node and its adjacent node
	 * @param nCurrentNodeV
	 * @param nAdjacentNodeofV
	 */
	
	public static void addAdjacentNodeData(int nCurrentNodeV, int nAdjacentNodeofV) {
	
		if (nCurrentNodeV <= 0 || nCurrentNodeV > NodeSize)
			throw new IndexOutOfBoundsException();
		
		if (nAdjacentNodeofV <= 0 || nAdjacentNodeofV > NodeSize)
			throw new IndexOutOfBoundsException();
		
		if (nCurrentNodeV == nAdjacentNodeofV )
			throw new IllegalArgumentException("Current Node and Adjacent nodes cannot be same!");

		adj[nCurrentNodeV].add(nAdjacentNodeofV);
	}



	/**
	 * getAdjNode() - Retrieves the neighbor list of the current node
	 * @param nCurrentNodeV
	 */
	public static Iterable<Integer> getAdjNode(int nCurrentNodeV) {
		if (nCurrentNodeV <= 0 || nCurrentNodeV > NodeSize)
			throw new IndexOutOfBoundsException();
		return adj[nCurrentNodeV];
	}
    
    /**
     * Copy constructor.
     */
    public SimpleDBSample() {
    	System.out.println("<SimpleDBSample>	Copy Constructor");
    }

    public static void domainCreation() throws Exception {
    	init();        

        try {
        	//Weighted graph example
            String wDomain = "WeightedNodes";

            deleteDom(wDomain);

        	// Create weighted graph domain
            System.out.println("Creating domain called " + wDomain + ".\n");
            sdb.createDomain(new CreateDomainRequest(wDomain));
            
            printDomains();

            // Put weighted data into a domain
            System.out.println("Putting data into " + wDomain + " domain.\n");
            sdb.batchPutAttributes(new BatchPutAttributesRequest(wDomain, readInGraphData("src/graph_data-weighted.txt", WEIGHTED)));
            
            //Directed graph example
            String dDomain = "DirectedNodes";
            
            deleteDom(dDomain);

            //Create directed graph domain
            System.out.println("Creating domain called " + dDomain + ".\n");
            sdb.createDomain(new CreateDomainRequest(dDomain));

            //printDomains();

            // Put weighted data into a domain
            System.out.println("Putting data into " + dDomain + " domain.\n");
            sdb.batchPutAttributes(new BatchPutAttributesRequest(dDomain, readInGraphData("src/graph_data-directed.txt", DIRECTED)));
            
            //delay execution one second, as directed data is not immediately available
           Thread.currentThread();
			Thread.sleep(1000);
            
            //select sample data from each domain
          //selectData(wDomain);
            selectData(dDomain);
            
        }
        catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon SimpleDB, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        }
        catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with SimpleDB, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    
    /**
     * Main method - driver for testing
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

		SimpleDBSample S = new SimpleDBSample();
		domainCreation();

		if(args.length == 0){
	        System.out.println(" Arguments Not provided ");
	        System.exit(0);
		}
		 
		System.out.println("Query Selected: " + args[0]);
		System.out.println("Graph Type: " + args[1]);
		System.out.println("<============================  OUTPUT ============================ >");
		System.out.println("");

		switch(Integer.parseInt(args[0])){
		case 0:
			System.out.println("FRIENDS OF FRIEND");
			//Call Friends of Friend Functions
			switch(Character.toUpperCase(args[1].charAt(0))){
			case 'D':
				getMultipleValuesofAttribute("DirectedNodes", "directed");
				retrieveFOF();
				break;
			case 'W':
				getMultipleValuesofAttribute("WeightedNodes", "weighted");
				retrieveFOF();
				break;
			default:
				System.err.println("Invalid Graph Input: " + args[1]);
				System.exit(0);
			}
			break;

		case 1:
			//Calling Transitive Closure
			System.out.println("TRANSITIVE CLOSURE");
			switch(Character.toUpperCase(args[1].charAt(0))){
			case 'D':
				TransitiveClosure("DirectedNodes", "directed");
				break;
			case 'W':
				TransitiveClosure("WeightedNodes", "weighted");
				break;
			default:
				System.err.println("Invalid Graph Input: " + args[1]);
				System.exit(0);
			}
			break;
		case 2:
			//Calling Bridges
			System.out.println("BRIDGES");
			switch(Character.toUpperCase(args[1].charAt(0))){
			case 'D':
				getMultipleValuesofAttribute("DirectedNodes", "directed");
				if (NodeSize > 1) {
					Bridge bridge = new Bridge();
					bridge.toString(S);
					bridge.findSCC(S);
					System.out.println("	Total Number of SCC in the provided graph = "
							+ bridge.totalNoOfSCC());
				}
				break;
			case 'W':
				getMultipleValuesofAttribute("WeightedNodes", "weighted");
				if (NodeSize > 1) {
					Bridge bridge = new Bridge();
					bridge.toString(S);
					bridge.findBridge(S);
					System.out.println("	Total Number of Bridges in the provided graph = "
						+ bridge.totalNoOfBridges());
				}
				break;
			default:
				System.err.println("Invalid Graph Input: " + args[1]);
				System.exit(0);
			}
			/*if (NodeSize > 1) {
				Bridge bridge = new Bridge();
				bridge.toString(S);
				bridge.findBridge(S);
				System.out.println("	Total Number of Bridges in the provided graph = "
					+ bridge.totalNoOfBridges());
			}*/
			break;
		default:
			System.err.println("Invalid Query Input: " + args[0]);
	        System.exit(0);
		}

    }
    
}
