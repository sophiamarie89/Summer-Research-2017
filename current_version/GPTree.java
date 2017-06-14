import java.util.ArrayList;


public class GPTree {
	
	// All the possible names of variables from Steve code to pick randomly
	public static String[] var_names = {"velocityScale", "maxSpeed", "normalSpeed", "neighborRadius",
										"separationWeight", "alignmentWeight", "cohesionWeight", "pacekeepingWeight",
										"randomMotionProbability", "numNeighborsOwnFlock", "numNeighborsAllFlocks"};
			
	// Values to create random constants within a range
	public static float min_const;
	public static float max_const;

	// The greatest number of nodes that can be in a sequence
	public static int max_seq;
	
	// Restriction for tree approximate tree depth
	// (because apparently this is an inverted tree)
	public static float max_depth;
	
	// Will be automatically set to null in constructor.
	// holds entire tree as insane linked list
	public GPNode root;
	
	//needed for breeding
	public static int fitness;
	
	public GPTree(float min_const, float max_const, float max_depth, int max_seq){
		fitness = 0;
		this.root = null;
		GPTree.min_const = min_const;
		GPTree.max_const = max_const;
		GPTree.max_depth = max_depth;
		GPTree.max_seq = max_seq;
	}
	

	//for generating new tree without an existing root
	public GPNode generateNewTree() {
		GPNode.NodeType type = GPNode.NodeType.IF;

		//determine number of kids
		int numKids = determineKidNum(type);

		// The root has a type, no parents, no children, no variable name, no const value
		root = new GPNode(null, GPNode.ReturnType.N);
		root.nodeType = type;

		// Currently writing only IF statements for root node
		// so children rt is B, N, and optional N (for IF-ELSE)
		GPNode.ReturnType t = GPNode.ReturnType.B;
		for(int i = 0; i < numKids; i++){
			//get required type
			if(i == 1){	t = GPNode.ReturnType.N; }
			
			float depth = 2;
			GPNode newChild = generateSubtree(depth, root, t);
			root.children.add(newChild); 
		}

		return root;
	}

	public static GPNode generateSubtree(float depth, GPNode parent, GPNode.ReturnType rt){
		
		// First need to determine probability that will be terminal
		float term_threshold = 100 * depth / max_depth;
		float randVal = GPNode.randomVal(0, 100);

		//values that will be needed for creation of subrootine, regardless of case and NodeType
		GPNode subrootine = new GPNode(parent, rt);
		GPNode.NodeType pick;
		boolean terminal = false; // <-- to be used later when making recursive call
		int rand_index;
		String var_name;

		//
		// TERMINAL CASE
		//
		if (randVal <= term_threshold) {
			terminal = true;
			//setting NodeType for all nodes, and var/ const for edge cases
			if(rt == GPNode.ReturnType.F){
				float flag = 1;
				pick = GPNode.terminal_type(flag);
			} else if (rt == GPNode.ReturnType.B){
				boolean flag = true;
				pick = GPNode.terminal_type(flag);
			} else if (rt == GPNode.ReturnType.N){
				pick = GPNode.terminal_type();
			} else {
				System.out.println("Tried to create new terminal node at undefined return type");
				return null;
			}
			subrootine.nodeType = pick;
		}
		//
		// END OF TERMINAL SPECIFIC COMMANDS
		//
		// BEGINNING OF ACTIVE COMMANDS
		//
		else{
			if(rt == GPNode.ReturnType.F){
				float flag = 1;
				pick = GPNode.active_type(flag);
			} else if (rt == GPNode.ReturnType.B){
				boolean flag = true;
				pick = GPNode.active_type(flag);
			} else if (rt == GPNode.ReturnType.N){
				pick = GPNode.active_type();
			} else {
				System.out.println("Tried to create new active node at undefined return type");
				return null;
			}
		}

		subrootine.nodeType = pick;
		
		// accommodate VAR and CONST need assignments
		if (pick == GPNode.NodeType.CONST){
			subrootine.constValue = GPNode.randomVal(min_const, max_const);
		} else if (pick == GPNode.NodeType.VAR){
			rand_index = GPNode.randomVal(0, var_names.length);
			subrootine.varName = var_names[rand_index];
		}

		//
		// ADDING CHILDREN
		//
		ArrayList<GPNode> kids = new ArrayList<GPNode>();
		if(terminal == false){
			kids = addChildren(depth+1, pick, subrootine);
		} else {
			kids = addChildren(max_depth, pick, subrootine);
		}
		subrootine.children = kids;


		return subrootine;
	}

	public static ArrayList<GPNode> addChildren(float depth, GPNode.NodeType nt, GPNode parent){

		//what will be returned (the kids with full subtrees)
		ArrayList<GPNode> kids = new ArrayList<GPNode>();

		//Parameters needed for recursive call
		int numKids;
		GPNode.ReturnType childRT = GPNode.ReturnType.N;
		int rand_index;
		String var_name;
		GPNode child;

		// making recursive call for every kind but VAR and CONST (those have no children)
		switch(nt) {
			case VAR:
			case CONST:
				break;
	
			case SEQUENCE:
				numKids = GPNode.randomVal(2, max_seq+1);

				for(int i = 0; i < numKids; i++){
					child = generateSubtree(depth, parent, childRT);
					kids.add(child);
				}
				break;
	
			case IF:
				numKids = GPNode.randomVal(2, 4); //for IF-ELSE, randomVal is exclusive for upper limit

				// first child must be a boolean value
				child = generateSubtree(depth, parent, GPNode.ReturnType.B);
				kids.add(child);

				for(int i = 1; i < numKids; i++){
					child = generateSubtree(depth, parent, childRT);
					kids.add(child);
				}
				break;
	
			case ASSIGN:
			case INC:
			case DEC:
				rand_index = GPNode.randomVal(0, var_names.length);
				var_name = var_names[rand_index];

				//first child must be variable value
				child = new GPNode(parent, GPNode.ReturnType.F);
				child.varName = var_name;
				child.nodeType = GPNode.NodeType.VAR;
				kids.add(child);

				if(nt == GPNode.NodeType.ASSIGN){
					child = generateSubtree(depth, parent, GPNode.ReturnType.F);
					kids.add(child);
				}
				break;

			case ADD:
			case SUB:
			case MULT:
			case DIV:
			case EXP:
				child = generateSubtree(depth, parent, GPNode.ReturnType.F);
				kids.add(child);
				child = generateSubtree(depth, parent, GPNode.ReturnType.F);
				kids.add(child);
				break;

			case EQ:
			case LT:
			case GT:
			case LEQ:
			case GEQ:
				child = generateSubtree(depth, parent, GPNode.ReturnType.F);
				kids.add(child);

				if (child.nodeType == GPNode.NodeType.VAR){
					child = generateSubtree(depth, parent, GPNode.ReturnType.F);
					kids.add(child);
				} else {
					rand_index = GPNode.randomVal(0, var_names.length);
					var_name = var_names[rand_index];

					child = new GPNode(parent, GPNode.ReturnType.F);
					child.varName = var_name;
					child.nodeType = GPNode.NodeType.VAR;
					kids.add(child);
				}
				break;

			case OR:
			case AND:
				child = generateSubtree(depth, parent, GPNode.ReturnType.B);
				kids.add(child);
				child = generateSubtree(depth, parent, GPNode.ReturnType.B);
				kids.add(child);
				break;

			case NEG:
				child = generateSubtree(depth, parent, GPNode.ReturnType.B);
				kids.add(child);
				break;

			default:
				System.out.println("error: undefined NodeType in child construction " + nt);
				System.exit(0);
		}
		return kids;
	}

	public int determineKidNum(GPNode.NodeType type){
		int numKids = 0;
		if(type == GPNode.NodeType.IF){
			numKids = GPNode.randomVal(2,3);
		}
		else if (type == GPNode.NodeType.SEQUENCE){
			numKids = GPNode.randomVal(2, max_seq+1);
			// if computer is stupid, switch 2 to 1
		}
		else{
			System.out.println("tried to make random number of kids for node of type: " + type);
		}
		return numKids;
	}

}