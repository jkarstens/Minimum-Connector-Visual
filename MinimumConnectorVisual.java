import java.util.ArrayList;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MinimumConnectorVisual extends Applet implements ItemListener, ActionListener, KeyListener, MouseListener, MouseMotionListener, Runnable{

	private Graphics dbGraphics;
	private Image dbImage;

	private Thread thread;

	private Font instructionFont, subtitleFont, pointFont;

	private Checkbox drawCheckbox, exampleCheckbox;
	private CheckboxGroup cbg;
	private boolean drawFlag;

	private Choice startChoice, endChoice, calculateChoice;
	private int routeLength;

	private Button calculateButton, finishButton, clearButton, helpButton;

	private MinimumConnector exampleConnector, drawConnector; //example=default, draw=user path (can be overwritten)

	//for drawConnector
	private ArrayList<NamedPoint> xyMousePresses; //pressx, pressy
	private int mousePressedCounter;
	private ArrayList<NamedLine> drawLines;
	private char drawPointName;
	private int dragX, dragY; //to draw continuous line
	private boolean dragging, editing;
	private NamedLine editingLine;
	private String editingString;

	private String helpString;

	public void init(){

		setSize(1000, 700);
		setBackground(new Color(222, 222, 222));

		instructionFont = new Font("Calibri", Font.BOLD, 18);
		subtitleFont = new Font("Garamond", Font.BOLD, 18);
		pointFont = new Font("Century Gothic", Font.BOLD, 19);

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setFocusable(true); //for keylistener

		cbg = new CheckboxGroup();
		exampleCheckbox = new Checkbox("Work with the example", cbg, true);
		exampleCheckbox.addItemListener(this);
		drawCheckbox = new Checkbox("Draw your own", cbg, false);
		drawCheckbox.addItemListener(this);
		exampleCheckbox.setFocusable(false);
		drawCheckbox.setFocusable(false);
		add(exampleCheckbox);
		add(drawCheckbox);

		startChoice = new Choice();
		startChoice.setBackground(new Color(233, 150, 122));
		endChoice = new Choice();
		endChoice.setBackground(startChoice.getBackground());
		calculateChoice = new Choice();
		calculateChoice.setBackground(new Color(238, 130, 238));
		calculateChoice.add("Minimum Route");
		calculateChoice.add("Maximum Route");
		calculateChoice.add("Random Route");
		startChoice.setFocusable(false);
		endChoice.setFocusable(false);
		calculateChoice.setFocusable(false);
		add(startChoice);
		add(endChoice);
		add(calculateChoice);

		routeLength = -1;

		calculateButton = new Button("CALCULATE!");
		calculateButton.addActionListener(this);
		calculateButton.setBackground(new Color(147, 112, 219));
		finishButton = new Button("FINISH");
		finishButton.addActionListener(this);
		finishButton.setBackground(new Color(208, 202, 140));
		finishButton.setEnabled(false);
		finishButton.setVisible(false);
		clearButton = new Button("CLEAR");
		clearButton.addActionListener(this);
		clearButton.setBackground(finishButton.getBackground());
		clearButton.setVisible(false);
		helpButton = new Button("DRAW HELP");
		helpButton.addActionListener(this);
		helpButton.setBackground(new Color(250, 240, 230));
		helpButton.setVisible(false);
		calculateButton.setFocusable(false);
		finishButton.setFocusable(false);
		clearButton.setFocusable(false);
		helpButton.setFocusable(false);
		add(calculateButton);
		add(finishButton);
		add(clearButton);
		add(helpButton);

		helpString = "Click to create a point and start drawing a route. Move the mouse and clock again to terminate that route with another point.\nClick again on any point and move the mouse to draw another route from that point.\nRoute values are calculated automatically. To change a route value, right click on a route and enter in a numerical value. Then press ENTER to continue drawing.\nPoints are named automatically.\nTo finish a route, press FINISH. Routes may be edited after they are finished, however, componenets cannot be deleted one by one.\nTo perform calculations on a route, use the selection boxes and buttons on the right side of the window. All calculations are made without revisiting nodes.\nCalculations can only be performed on a FINISHED route. To redraw a different route, press CLEAR.";

		exampleConnector = new MinimumConnector('A');
		exampleConnector.addRoute('A', 'B', 4);
		exampleConnector.addRoute('B', 'C', 6);
		exampleConnector.addRoute('C', 'D', 13);
		exampleConnector.addRoute('D', 'E', 5);
		exampleConnector.addRoute('E', 'F', 6);
		exampleConnector.addRoute('F', 'G', 4);
		exampleConnector.addRoute('G', 'A', 8);
		exampleConnector.addRoute('G', 'B', 2);
		exampleConnector.addRoute('G', 'C', 8);
		exampleConnector.addRoute('G', 'D', 14);
		exampleConnector.addRoute('G', 'E', 11);
		exampleConnector.addRoute('F', 'A', 11);

		for(char c='A'; c<'H'; c++){

			startChoice.add(c + "");
			endChoice.add(c + "");
		}

		startChoice.select(0);
		endChoice.select(3);

		xyMousePresses = new ArrayList<NamedPoint>();
		mousePressedCounter = 0;
		drawPointName = 'A';
		drawLines = new ArrayList<NamedLine>();
		editingString = "";
	}

	public void paint(Graphics g){

		setAWTComponentBounds(g);
		drawStrings(g);

		g.setColor(new Color(60, 139, 60)); //dividing line
		g.fillRect(800, 0, 10, 700);

		if(!drawFlag) drawExampleConnector(g);

		if(drawFlag){

			Graphics2D g2 = (Graphics2D)(g);
			g2.setStroke( new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND) );
			g2.setColor(Color.BLACK);

			for(NamedPoint p : xyMousePresses){

				g2.fillOval(p.x-5, p.y-5, 11, 11);
				g2.drawString(p.name + "", p.x-10, p.y-5);
			}

			for(NamedLine line : drawLines){

				g2.setColor(new Color(96, 80, 195));
				g2.draw(line);
				g2.setColor(new Color(80, 80, 80));
				g2.drawString(line.length + "", (int)((line.getX1()+line.getX2())/2) -5, (int)((line.getY1()+line.getY2())/2) -10);
			}

			if(dragging){

				g2.setColor(new Color(96, 80, 195));
				g2.drawLine(xyMousePresses.get( xyMousePresses.size()-1 ).x, xyMousePresses.get( xyMousePresses.size()-1 ).y, dragX, dragY);
			}
		}
	}

	public void drawExampleConnector(Graphics g){

		Graphics2D g2 = (Graphics2D)(g);
		g2.setStroke( new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND) );
		g2.setColor(Color.BLACK);

		g2.setFont(pointFont);

		//draw points
		char pointCounter = 'A';

		g2.fillOval(50-5, 350-5, 11, 11);
		g2.drawString(pointCounter++ + "", 40, 345); //-10, -5
		g2.fillOval(200-5, 200-5, 11, 11);
		g2.drawString(pointCounter++ + "", 190, 195);
		g2.fillOval(450-5, 200-5, 11, 11);
		g2.drawString(pointCounter++ + "", 440, 195);
		g2.fillOval(600-5, 350-5, 11, 11);
		g2.drawString(pointCounter++ + "", 590, 345-5);
		g2.fillOval(450-5, 500-5, 11, 11);
		g2.drawString(pointCounter++ + "", 440, 495-5);
		g2.fillOval(200-5, 500-5, 11, 11);
		g2.drawString(pointCounter++ + "", 190, 495-5);
		g2.fillOval(325-5, 350-5, 11, 11);
		g2.drawString(pointCounter++ + "", 320, 345-5);

		//draw lines
		g2.setColor(new Color(96, 80, 195));
		g2.drawLine(50, 350, 200, 200);
		g2.drawLine(200, 200, 450, 200);
		g2.drawLine(450, 200, 600, 350);
		g2.drawLine(600, 350, 450, 500);
		g2.drawLine(450, 500, 200, 500);
		g2.drawLine(200, 500, 325, 350);
		g2.drawLine(325, 350, 50, 350);
		g2.drawLine(200, 500, 50, 350);
		g2.drawLine(50, 350, 325, 350);
		g2.drawLine(200, 200, 325, 350);
		g2.drawLine(450, 200, 325, 350);
		g2.drawLine(600, 350, 325, 350);
		g2.drawLine(450, 500, 325, 350);
		g2.drawLine(200, 500, 325, 350);
		g2.setColor(new Color(80, 80, 80));
		g2.drawString("4", 125-6, 275-6);
		g2.drawString("6", 325-6, 200-6);
		g2.drawString("13", 525-6, 275-6);
		g2.drawString("5", 525-6, 425-6);
		g2.drawString("6", 325-6, 500-6);
		g2.drawString("4", 274+3, 425-6);
		g2.drawString("11", 125-6, 425-6);
		g2.drawString("8", 178-6, 345);
		g2.drawString("2", 262+3, 275);
		g2.drawString("8", 382-6, 275);
		g2.drawString("14", 472-6, 345);
		g2.drawString("11", 382, 425-6);
	}

	public void setAWTComponentBounds(Graphics g){

		exampleCheckbox.setBounds(840, 50, 140, 14);
		drawCheckbox.setBounds(860, 80, 100, 14);

		startChoice.setBounds(842, 168, 50, 20);
		endChoice.setBounds(916, 168, 50, 20);
		calculateChoice.setBounds(850, 270, 115, 20);

		calculateButton.setBounds(855, 330, 100, 30);
		finishButton.setBounds(860, 540, 90, 25);
		clearButton.setBounds(860, 570, 90, 25);
		helpButton.setBounds(860, 615, 90, 25);
	}

	public void drawStrings(Graphics g){

		g.setColor(Color.BLACK);
		g.setFont(subtitleFont);
		g.drawString("Start", 847, 160);
		g.drawString("End", 922, 160);
		g.drawString("Calculate", 868, 260);
		g.drawString("Calculated Route:", 837, 420);
		if(routeLength > -1 && routeLength < 10000000) g.drawString(routeLength + "", 880, 455);

		if(drawFlag){

			g.setColor(Color.BLACK);
			g.setFont(instructionFont);
			g.drawString("Click and move the mouse to start drawing a route. Click again to terminate that route with a point.", 30, 30);
			g.drawString("Clicking on an existing point will not create a new point, but will still allow for a new route to be drawn.", 20, 50);
			if(!editing) g.drawString("Right-click on route lengths to change them.", 270, 70);
			else{

				g.setColor(new Color(200, 20, 20));
				g.drawString("Enter in a route length, then press ENTER: " + editingString, 260, 70);
			}

			if(calculateButton.isEnabled()){

				if(drawConnector == null){

					startChoice.setEnabled(false);
					endChoice.setEnabled(false);
					calculateChoice.setEnabled(false);
					calculateButton.setEnabled(false);
				}
			}
		}
	}

	public void update(Graphics g){

		if(dbImage == null){

			dbImage = createImage(getSize().width, getSize().height);
			dbGraphics = dbImage.getGraphics();
		}

		dbGraphics.setColor(getBackground());
		dbGraphics.fillRect(0, 0, getSize().width, getSize().height);
		dbGraphics.setColor(getForeground());
		paint(dbGraphics);

		g.drawImage(dbImage, 0, 0, this);
	}

	public void itemStateChanged(ItemEvent e){

		if(drawCheckbox.getState()){

			drawFlag = true;
			finishButton.setVisible(true);
			clearButton.setVisible(true);
			helpButton.setVisible(true);
		}

		else{

			drawFlag = false;
			finishButton.setVisible(false);
			clearButton.setVisible(false);
			helpButton.setVisible(false);
		}
	}

	public void actionPerformed(ActionEvent e){

		Object source = e.getSource();

		if(source == helpButton){

			JOptionPane.showMessageDialog(this, helpString, "Help", JOptionPane.INFORMATION_MESSAGE);
		}

		if(source == calculateButton){

			char start = startChoice.getSelectedItem().charAt(0);
			char end = endChoice.getSelectedItem().charAt(0);

			if(!drawFlag) routeLength = exampleConnector.getRouteLength(start, end, calculateChoice.getSelectedIndex());
			else routeLength = drawConnector.getRouteLength(start, end, calculateChoice.getSelectedIndex());

			System.out.println(routeLength);
		}

		if(source == clearButton){

			drawConnector = null;
			xyMousePresses.clear();
			drawPointName = 'A';
			drawLines.clear();
			mousePressedCounter = 0;
			dragging = false;
			finishButton.setEnabled(false);
		}

		if(source == finishButton){

			drawConnector = new MinimumConnector('A');
			//add data from drawLines
			for(NamedLine l : drawLines){

				//find the two points
				NamedPoint p1 = null, p2 = null;

				for(int i=0; i<xyMousePresses.size(); i++){

					NamedPoint p = xyMousePresses.get(i);

					if( p1 == null && ( p.equals( l.getP1() ) || p.equals( l.getP2() ) ) ){

						p1 = p;
						i=-1; //restart loop - necessary?
					}

					else if( !p.equals(p1) && ( p.equals( l.getP1() ) || p.equals( l.getP2() ) ) ){

						p2 = p;
						break;
					}
				}

				drawConnector.addRoute(p1.name, p2.name, l.length);
				System.out.println("route added: " + p1.name + " -> " + p2.name + ", d = " + l.length);//this is all right!
			}

			startChoice.removeAll();
			endChoice.removeAll();

			for(char c='A'; c<drawPointName; c++){

				startChoice.add(c + "");
				endChoice.add(c + "");
			}

			endChoice.select( endChoice.getItemCount()-1 );

			startChoice.setEnabled(true);
			endChoice.setEnabled(true);
			calculateChoice.setEnabled(true);
			calculateButton.setEnabled(true);
		}
	}

	public void keyPressed(KeyEvent e){

		if(editing){

			if(editingString.length() < 9) for(int i=48; i<58; i++) if(e.getKeyCode() == i) editingString += (i-48) + "";

			if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){

				editingString = editingString.substring(0, editingString.length()-1);
			}

			if(e.getKeyCode() == KeyEvent.VK_ENTER){

				int l = Integer.parseInt(editingString);
				editingLine.length = l;
				editingString = "";
				editing = false;
				editingLine = null;
			}
		}
	}

	public void keyReleased(KeyEvent e){
	}

	public void keyTyped(KeyEvent e){
	}

	public void mouseEntered(MouseEvent e){
	}

	public void mouseExited(MouseEvent e){
	}

	public void mousePressed(MouseEvent e){

		if(drawFlag && e.getX() < 800 && e.getY() > 40){ //valid press

			if(e.getButton() == MouseEvent.BUTTON1){ //left click (normal)

				mousePressedCounter++;
				//check if user pressed on an existing point
				boolean existent = false;
				for(NamedPoint p : xyMousePresses){

					if(p.x-15 <= e.getX() && p.x+15 >= e.getX() && p.y-15 <= e.getY() && p.y+15 >= e.getY()){

						xyMousePresses.add( new NamedPoint( p.x, p.y, p.name ) );
						existent = true;
						break;
					}
				}

				if(!existent) xyMousePresses.add( new NamedPoint( e.getX(), e.getY(), drawPointName++ ) );
				dragging = false;

				if(mousePressedCounter % 2 == 0){

					drawLines.add( new NamedLine( xyMousePresses.get( xyMousePresses.size()-2 ).x, xyMousePresses.get( xyMousePresses.size()-2 ).y, xyMousePresses.get( xyMousePresses.size()-1 ).x, xyMousePresses.get( xyMousePresses.size()-1 ).y ) );
					finishButton.setEnabled(true);
				}

				else finishButton.setEnabled(false);
			}

			else if(e.getButton() == MouseEvent.BUTTON3){ //right click

				for(NamedLine line : drawLines){ //find distance to edit

					int posX = (int)((line.getX1()+line.getX2())/2);
					int posY = (int)((line.getY1()+line.getY2())/2) -10;

					if(e.getX() > posX-10 && e.getX() < posX+10 && e.getY() > posY-10 && e.getY() < posY+10){

						editing = true;
						editingLine = line;
						break;
					}
				}
			}
		}
	}

	public void mouseReleased(MouseEvent e){
	}

	public void mouseClicked(MouseEvent e){
	}

	public void mouseMoved(MouseEvent e){

		if(drawFlag && e.getX() < 800 & e.getY() > 40 && mousePressedCounter % 2 != 0 && !xyMousePresses.isEmpty()){ //valid drag

			dragX = e.getX();
			dragY = e.getY();
			dragging = true;
		}
	}

	public void mouseDragged(MouseEvent e){
	}

	public void start(){

		if(thread == null){

			thread = new Thread(this);
			thread.start();
		}
	}

	public void run(){

		while(thread != null){

			repaint();

			try{
				Thread.sleep(20);
			}
			catch(InterruptedException e){
			}
		}
	}

	public void stop(){

		thread = null;
	}

	public static class NamedPoint extends Point{

		public char name;

		public NamedPoint(int x, int y, char c){

			super(x, y);
			name = c;
		}

		public NamedPoint(int x, int y){ //this point is already here, but keep it for pressing records

			this(x, y, '\u0000');
		}
	}

	public static class NamedLine extends Line2D.Double{

		public int length;

		public NamedLine(int x1, int y1, int x2, int y2){

			super(x1, y1, x2, y2);
			length = (int) Math.sqrt( Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2) )/10;
		}

		public Point getP1(){ //overriden for ints

			return new Point( (int)x1, (int)y1 );
		}

		public Point getP2(){

			return new Point( (int)x2, (int)y2 );
		}
	}

	public static class MinimumConnector{

		private Node start;
		public static int MIN_ROUTE = 0;
		public static int MAX_ROUTE = 1;
		public static int RAND_ROUTE = 2;

		public MinimumConnector(char p){ //starting Node

			ArrayList<Node> all = new ArrayList<Node>();
			start = new Node(p, all);
		}

		public void addRoute(char p, char q, int d){ //from p (probably already existing) to q (new Node)

			//find p (denoted as n)
			Node n = null;

			for(int i=0; i<start.allNodes.size(); i++){

				if(start.allNodes.get(i).name == p){

					n = start.allNodes.get(i);
					break;
				}
			}

			if(n == null) n = new Node(p, start.allNodes); //p is a new point

			n.addConnection(q, d);
		}

		public int getRouteLength(char p, char q, int routeType){ //from p to q, min, max, or random route

			//find p and q (denoted as r and s)
			Node r = null, s = null;

			for(int i=0; i<start.allNodes.size(); i++){

				if(start.allNodes.get(i).name == p){

					r = start.allNodes.get(i);
					break;
				}
			}

			for(int i=0; i<start.allNodes.size(); i++){

				if(start.allNodes.get(i).name == q){

					s = start.allNodes.get(i);
					break;
				}
			}

			return getRouteLength(r, new DistancedNode(s, -1), new ArrayList<Node>(), routeType);
		}

		private int getRouteLength(Node current, DistancedNode finish, ArrayList<Node> visited, int routeType){

			visited.add(current);

			//System.out.println("@ " + current.name + ", target: " + finish.node.name + ", " + visited.size() + " visited so far");

			if(current.equals(finish)){

				String tagString = "";

				for(int i=0; i<visited.size(); i++) tagString += visited.get(i).name + ",";
				System.out.println("Valid Route Found: " + tagString);
				return 0;
			}

			//dont check Nodes that have already been visited
			ArrayList<DistancedNode> newConnections = new ArrayList<DistancedNode>();

			for(int i=0; i<current.connections.size(); i++){

				boolean repeat = false;

				for(int j=0; j<visited.size(); j++){

					if(current.connections.get(i).equals(visited.get(j))){

						repeat = true;
						break;
					}
				}

				if(!repeat) newConnections.add(current.connections.get(i));
			}

			ArrayList<Integer> distances = new ArrayList<Integer>();
			for(int i=0; i<newConnections.size(); i++) distances.add( newConnections.get(i).distance + getRouteLength(newConnections.get(i).node, finish, new ArrayList<Node>(visited), routeType) );

			if(newConnections.size() != 0){

				switch(routeType){

					case 0: return min(distances);
					case 1: return max(distances);
					case 2: return rand(distances);
				}
			}

			return 10000000; //not a valid path - no places to go that havent been visited - return a flag that says dont count this one
		}

		private int min(ArrayList<Integer> nums){

			int minimum = 0; //index
			for(int i=1; i<nums.size(); i++) if(nums.get(i) < nums.get(minimum)) minimum = i;
			return nums.get(minimum);
		}

		private int max(ArrayList<Integer> nums){

			int maximum = 0; //index

			try{

				while(nums.get(maximum) > 10000000) maximum++;
			}
			catch(IndexOutOfBoundsException e){

				return 0; //no valid paths
			}

			for(int i=1; i<nums.size(); i++) if(nums.get(i) > nums.get(maximum) && nums.get(i) < 10000000) maximum = i;
			return nums.get(maximum);
		}

		private int rand(ArrayList<Integer> nums){

			int rand = (int)(nums.size()*Math.random()); //index

			try{

				while(nums.get(rand) > 10000000) rand = (int)(nums.size()*Math.random());
			}
			catch(ArrayIndexOutOfBoundsException e){

				return 0; //no valid paths
			}

			return nums.get(rand);
		}

		public static class Node{ //its a node with connections, tree-like, with added feature of distances between branches

			public char name; //name of point
			public ArrayList<DistancedNode> connections; //DistancedNode consists of a connecting Node and its distance away
			public ArrayList<Node> allNodes; //copy of all nodes in the maze so to not make duplicates

			public Node(char p, DistancedNode connection, ArrayList<Node> all){

				name = p;
				connections = new ArrayList<DistancedNode>();
				if(connection != null) connections.add(connection);
				allNodes = all;
				allNodes.add(this);
			}

			public Node(char p, ArrayList<Node> all){

				this(p, null, all);
			}

			public void addConnection(char q, int d){ //new Node

				Node connectingNode = null;

				for(int i=0; i<allNodes.size(); i++){

					if(allNodes.get(i).name == q) connectingNode = allNodes.get(i); //check for previously made nodes
				}

				if(connectingNode == null) connectingNode = new Node(q, allNodes);

				connectingNode.connections.add( new DistancedNode(this, d) );
				DistancedNode connectingDistancedNode = new DistancedNode(connectingNode, d);
				connections.add(connectingDistancedNode);
			}

			public Node next(){ //returns next alphabetical connection

				for(int i=0; i<connections.size(); i++){

					if(connections.get(i).node.name == name + 1){

						return connections.get(i).node;
					}
				}

				return null;
			}

			public boolean equals(Node n){

				return n.name == name;
			}

			public boolean equals(DistancedNode n){

				return n.node.name == name;
			}
		}

		public static class DistancedNode{

			public Node node;
			public int distance;

			public DistancedNode(Node n, int d){

				node = n;
				distance = d;
			}

			public boolean equals(DistancedNode dn){

				return dn.node.equals(node);
			}

			public boolean equals(Node n){

				return n.equals(node);
			}
		}
	}

	public static void main(String[] args){

		Applet thisApplet = new MinimumConnectorVisual();
		thisApplet.init();
		thisApplet.start();

		JFrame frame = new JFrame("Minimum Connector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(thisApplet.getSize());
		//frame.setUndecorated(true);
		frame.setLayout(new BorderLayout());
		frame.getContentPane().add(thisApplet, BorderLayout.CENTER);
		frame.setVisible(true);
	}

}
