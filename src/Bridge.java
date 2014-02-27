/*************************************************************************
 * CS 510 Data Management in Cloud File : Bridge.java
 * 
 * 
 * Bridge.java - identifies the edges that are bridge and prints them out.
 * -identifies strongly connected component of the given domain data
 * 
 * 
 *************************************************************************/

public class Bridge {

	private int nbridges; // no. of bridges
	private int[] low; // This array helps to identify if there is a loop
	private int[] pre; // maintains the visited nodes; assigns -1 if its
						// unvisited
	private int nCount; // counter

	private boolean[] visited;
	private int[] sccID; // id of strong component containing v
	private int[] low1;
	private int pre1; // pre-order number counter
	private int nCount1; // no. of strongly-connected components
	private Sack<Integer> stack;

	private int nNodeSize;

	public void findBridge(SimpleDBSample S) {

		nNodeSize = S.getNodeSize();

		if (nNodeSize > 1) {
			low = new int[nNodeSize + 1];
			pre = new int[nNodeSize + 1];

			// System.out.println(" <getNodeSize>: " + nNodeSize);

			low[0] = 0;
			pre[0] = 0;

			for (int nCurrentNode = 1; nCurrentNode <= nNodeSize; nCurrentNode++) {
				low[nCurrentNode] = -1;
				pre[nCurrentNode] = -1;
			}

			for (int nCurrentNode = 1; nCurrentNode <= nNodeSize; nCurrentNode++) {

				if (pre[nCurrentNode] == -1) {
					DFS(S, nCurrentNode, nCurrentNode);
				}

			}
		} else {
			System.out
					.println("No sufficient Data in Domain to compute Bridges");
		}

	}

	private void DFS(SimpleDBSample S, int u, int nCurrentNode) {

		pre[nCurrentNode] = nCount++;
		low[nCurrentNode] = pre[nCurrentNode];

		for (int nAdjNode : S.getAdjNode(nCurrentNode)) {
			if (pre[nAdjNode] == -1) {
				DFS(S, nCurrentNode, nAdjNode);

				low[nCurrentNode] = Math.min(low[nCurrentNode], low[nAdjNode]);

				if (low[nAdjNode] == pre[nAdjNode]) {
					System.out.println(nCurrentNode + "-" + nAdjNode
							+ " is a BRIDGE");
					nbridges++;
				}
			}

			// update low number - ignore reverse of edge leading to v
			else if (nAdjNode != u) {
				low[nCurrentNode] = Math.min(low[nCurrentNode], pre[nAdjNode]);
			}
		}
	}

	/**
	 * Prints the string representation of the Domain.
	 */
	public void toString(SimpleDBSample S) {
		String NEWLINE = System.getProperty("line.separator");
		int nNodeSize = S.getNodeSize();
		System.out
				.println("<String Representation of the relationship between nodes>"
						+ NEWLINE);
		System.out.println(nNodeSize + " vertices " + NEWLINE);
		for (int v = 1; v <= nNodeSize; v++) {
			System.out.print(v + ": ");
			for (int w : S.getAdjNode(v)) {
				System.out.print(w + " ");
			}
			System.out.print(NEWLINE);
		}
		System.out.print(NEWLINE);
	}

	public int totalNoOfBridges() {
		return nbridges;
	}

	public void findSCC(SimpleDBSample S) {
		nNodeSize = S.getNodeSize();
		visited = new boolean[nNodeSize + 1];
		stack = new Sack<Integer>();
		sccID = new int[nNodeSize + 1];
		low1 = new int[nNodeSize + 1];

		low1[0] = 0;
		visited[0] = false;

		for (int nCurrentNode = 1; nCurrentNode <= nNodeSize; nCurrentNode++) {
			if (!visited[nCurrentNode])
				DFS_SCC(S, nCurrentNode);
		}

	}

	private void DFS_SCC(SimpleDBSample S, int nCurrentNode) {
		visited[nCurrentNode] = true;
		low1[nCurrentNode] = pre1++;
		int nDelItem;
		int min = low1[nCurrentNode];
		stack.add(nCurrentNode);
		for (int nAdjNode : S.getAdjNode(nCurrentNode)) {
			if (!visited[nAdjNode])
				DFS_SCC(S, nAdjNode);
			if (low1[nAdjNode] < min)
				min = low1[nAdjNode];
		}
		if (min < low1[nCurrentNode]) {
			low1[nCurrentNode] = min;
			return;
		}

		do {
			nDelItem = stack.pop();
			sccID[nDelItem] = nCount1;
			low1[nDelItem] = nNodeSize;
		} while (nDelItem != nCurrentNode);
		nCount1++;
	}

	/**
	 * Prints and Returns total No. of Strongly-connected components.
	 */
	@SuppressWarnings("unchecked")
	public int totalNoOfSCC() {

		System.out.println("Strongly Connected Components are:");

		Sack<Integer>[] components = (Sack<Integer>[]) new Sack[nCount1 + 1];
		components[0] = new Sack<Integer>();
		for (int i = 1; i <= nCount1; i++) {
			components[i] = new Sack<Integer>();
		}
		for (int v = 1; v <= nNodeSize; v++) {
			components[sccID(v)].Enqueue(v);
		}

		// printing SCC
		for (int i = 0; i < nCount1; i++) {
			for (int v : components[i]) {
				System.out.print(v + " ");
			}
			System.out.println();
		}

		return nCount1;
	}

	// in which strongly connected component is vertex v?
	public int sccID(int v) {
		return sccID[v];
	}

	public Bridge() {
		// System.out.println("<Bridge>	Bridge");
	}

}
