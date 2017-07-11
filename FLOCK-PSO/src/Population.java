import java.util.ArrayList;

public class Population{
	public static ArrayList<GPTree> pop; //actual population of trees
	int pm = 30; 	// percent probability of mutation
	int pc = 72; 	// percent probability of crossover
	int NUM_GEN = 37; 	//number of generations of evolution
	double k_as_frac_of_N = 0.2; 	// portion of population to use in tournament selection
	int TRIES_MAX = 37; 	//max number of times to try to find compatible nodes for crossover


	// for creating a tree. adjustable for user
	double min_const = 0;
	double max_const = 1000;
	double max_depth = 10;
	int max_seq = 5;

	public Population(int numTrees){
		this.pop = new ArrayList<GPTree>();
		//generate the specified number of random trees
		GPTree new_tree;
		for (int i = 0; i < numTrees; i++){
			new_tree = new GPTree( min_const, max_const, max_depth, max_seq);
			new_tree.generateNewTree();
			pop.add(new_tree);
		}

		int randFit;
		for (int i = 0; i < numTrees; i++){
//			randFit = GPNode.randomVal(0, numTrees);
			pop.get(i).fitness = calc_fit(pop.get(i));
		}

		// for (int i = 0; i < numTrees; i++){
		// 	System.out.print(i + ": ");
		// 	pop.get(i).printStats();
		// }
		GPTree t1 = pop.get(0);
		// GPTree t2 = pop.get(1);

		// System.out.println("t1: ");
		// t1.printTree();
		// int fit = calc_fit(t1);
		// System.out.println("fitness: " + fit);
		// System.out.println("\n\nt2: ");
		// t2.printTree();

		// single_crossover(t1, t2);

		// System.out.println("\n\n\nnew t1: ");
		// t1.printTree();
		// System.out.println("\n\nnew t2: ");
		// t2.printTree();
		single_gen();

		System.out.println("\n\nt0 after:");
		pop.get(0).printTree();
	}

	public void run(){
		for(int i = 0; i < NUM_GEN; i++){
			single_gen();
		}
	}



	/*
	* Runs one generation of crossover and mutation
	* Input: void
	* Output: void
	*/
	public void single_gen(){
		ArrayList<GPTree> new_kids = new ArrayList<GPTree>();
		int N = pop.size();
		GPTree child1, child2, parent1, parent2;
		int rand;
		System.out.println("t0 before:");
		pop.get(0).printTree();
		while (new_kids.size() != N){
			parent1 = tournament_selection();
			child1 = new GPTree(parent1);
			parent2 = tournament_selection();
			child2 = new GPTree(parent2);

			rand = GPNode.randomVal(0,100);
			if(rand <= pc){ single_crossover(child1, child2); }
			if(rand <= pm){ mutate(child1, child2); }

			child1.fitness = calc_fit(child1);
			child2.fitness = calc_fit(child2);
			
			new_kids.add(child1);
			new_kids.add(child2);
		}
		pop = new_kids;
	}



	/*
	 * Garbage calculating fitness
	 */
//	public int calc_fit(GPTree tree) {
//		int num_add = 0;
//		ArrayList<GPNode> all_nodes = tree.toArrayList();
//		for (int i = 0; i < all_nodes.size(); i++) {
//			if (all_nodes.get(i).nodeType == GPNode.NodeType.VAR) {
//				num_add += 1;
//			}
//		}
//		return num_add;
//	}

	public double calc_fit(GPTree tree) {
		
		PSO pso = new PSO(tree);
		return pso.evalGPTree();
		
	}

	/*
	 * Given two trees, mutate them at random points based
	 *	on an already established mutation probability: pm
	 * Input: two trees, t1 and t2
	 * Output: mutated trees
	*/
	public void mutate(GPTree t1, GPTree t2){
		ArrayList<GPNode> nodes1 = t1.toArrayList();
		ArrayList<GPNode> nodes2 = t2.toArrayList();

		int numnodes1 = nodes1.size();
		int numnodes2 = nodes2.size();

		// walk through each node on each tree with some probability of mutating
		int rand = 0;
		GPNode node;
		for(int i = 0; i<numnodes1;i++){
			node = nodes1.get(i);
			rand = GPNode.randomVal(0, 100);
			if(rand <= pm){	single_mut(node);
			}
		}
		for(int i = 0; i<numnodes2;i++){
			node = nodes2.get(i);
			rand = GPNode.randomVal(0, 100);
			if(rand <= pm){ single_mut(node); }
		}

	}

	/*
	 * In this function, given a node, mutate  it based on relevant 
	 * Input:	node - an existing node that you want to do the thing to
	 * Output:	void
	 */
	public void single_mut(GPNode node){

		GPNode.NodeType selectType = node.nodeType;
		
		int newInd;
		// making recursive call for every kind but VAR and CONST (those have no children)
		switch(selectType) {
			// null cases
			case SEQUENCE:
			case IF:
			case ASSIGN:
			case NEG:
			case CONST:
			case VAR:
				break;

			// all math operations
			case ADD:
			case SUB:
			case MULT:
			case DIV:
			case EXP:
				newInd = GPNode.randomVal(0, GPNode.allMathOp.length);
				node.nodeType = GPNode.allMathOp[newInd];
				break;
			
			// all relational operations
			case EQ:
			case LT:
			case GT:
			case LEQ:
			case GEQ:
				newInd = GPNode.randomVal(0, GPNode.allCompareOp.length);
				node.nodeType = GPNode.allCompareOp[newInd];
				break;
			
			// ++ --
			case INC:
				node.nodeType = GPNode.NodeType.DEC;
				break;
			case DEC:
				node.nodeType = GPNode.NodeType.INC;
				break;				
			
			case AND:
				node.nodeType = GPNode.NodeType.OR;
				break;
			case OR:
				node.nodeType = GPNode.NodeType.AND;
				break;
	
			default:
				System.out.println("error: undefined NodeType in single mutation");
				System.out.println(selectType);
				System.exit(0);
		}
	}

	/*
	 * Go through k of N of the population, select the most fit
	 * Input: void
	 * Output: most fit of k 
	*/
	public GPTree tournament_selection(){
		GPTree best = null;
		int n = pop.size();
		double numTreesExact = k_as_frac_of_N * (double)n;
		int k = (int)Math.round( numTreesExact );
//		System.out.println("Given n = " + n + " trees, want " + k_as_frac_of_N + " of them, looking at: " + k + " trees.");
		
		GPTree cur;
		for(int i = 0; i < k; i++){
			int rand_ind = GPNode.randomVal(0, n);
			// System.out.println("number looked at: " + rand_ind);
			cur = pop.get( rand_ind );
			if( (best == null) || cur.fitness > best.fitness){
				best = cur;
			}
		}

		return best;
	}

	/*
	* Pick a point on each tree, swap all nodes at and below it (subtree)
	* Input: the two trees to be swappped
	* Output: void
	*/

	public void single_crossover(GPTree t1, GPTree t2){

		ArrayList<GPNode> nodes1 = t1.toArrayList();
		ArrayList<GPNode> nodes2 = t2.toArrayList();

		int numnodes1 = nodes1.size();
		int numnodes2 = nodes2.size();

		int num_valid_nodes = 0;
		GPNode node1 = nodes1.get(0); // needed to initialize
		int index = 0;
		int tries = 0;
		//for collecting applicable nodes in t2
		ArrayList<Integer> applicable_node_indexes = new ArrayList<Integer>();
		//find a node in t1 that is swappable with other nodes in t2
		while(num_valid_nodes == 0 && tries < TRIES_MAX){
			applicable_node_indexes.clear();
 
			//randomly select first node (but not the root)
			index = GPNode.randomVal(1, numnodes1);
			node1 = nodes1.get(index);
			GPNode.ReturnType retType = node1.rt;
					

			//adding the relevant nodes, just not root
			//nodes are ordered most->least depth
			for(int i = 1; i < numnodes2; i++){
				if(nodes2.get(i).rt == retType){
					applicable_node_indexes.add(0, i);
				}
			}

			num_valid_nodes = applicable_node_indexes.size();
			tries++;
		} 

		// System.out.println("Selected node " + index + " in t1.");

		if(num_valid_nodes > 0){
			ArrayList<Integer> weights = new ArrayList<Integer>();
			//weight for each node candidate based on their depth
			int wait;
			for(int i = 0; i < num_valid_nodes; i++){
				GPNode curNode = nodes2.get(applicable_node_indexes.get(i));
				wait = curNode.get_depth();
				weights.add(wait);
			}

			int sec_index = selectIndexWeighted(weights);
			int actual_sec_index = applicable_node_indexes.get(sec_index);
			// System.out.println("Selected node " + actual_sec_index + " in t2.");
			GPNode node2 = nodes2.get(actual_sec_index);


			// save all info of first node as temp
			GPNode.NodeType temp_nodeType = node1.nodeType;
			ArrayList<GPNode> temp_children = node1.children;
			String temp_varName = node1.varName;
			double temp_constValue = node1.constValue;

			// set node1's info as node2's
			node1.nodeType = node2.nodeType;;
			node1.children = node2.children;
			node1.varName = node2.varName;
			node1.constValue = node2.constValue;
			//update new children's parent pointers
			int num_kids = node1.children.size();
			for (int i = 0; i < num_kids; i++){
				node1.children.get(i).parent = node1;
			}

			// set node2's info as node1's
			node2.nodeType = temp_nodeType;
			// node2.parent = temp_parent;
			node2.children = temp_children;
			node2.varName = temp_varName;
			node2.constValue = temp_constValue;
			//update new children's parent pointers
			num_kids = node2.children.size();
			for (int i = 0; i < num_kids; i++){
				node2.children.get(i).parent = node2;
			}
		}


	}


	/* Given an array of raw weights, NOT relative, randomly select
	 * 	an index and return it.
	 * Useful for crossover, single mutation, subtree mutation
	 */
	public int selectIndexWeighted(ArrayList<Integer> weights){

		int sum = 0;
		for ( int i = 0; i < weights.size(); i++){ sum += weights.get(i); }
		
		int threshold = GPNode.randomVal(0,sum);
		int return_ind = 0;
		int cur_sum = 0;
		while(cur_sum < threshold){
			cur_sum += weights.get(return_ind);
			return_ind += 1;
		}
		return  Math.max(0, return_ind-1);
	}






















}