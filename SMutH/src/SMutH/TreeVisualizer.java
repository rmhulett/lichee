package SMutH;

import io.SNVDatabase;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.util.Animator;

public class TreeVisualizer {
	
	private VisualizationViewer<Integer, Integer> visServer;
	private TreeLayout<Integer, Integer> treeLayout;
	private RadialTreeLayout<Integer, Integer> radialLayout;
	private VisualizationServer.Paintable rings;
	
	/**
	 * Creates the actual graphics and sets up visualizing the tree
	 * 
	 * Uses a combination of JUNG and Java's Swing libraries to display
	 * tree. This might be made into it's own class if necessary.
	 * 
	 * @param g	A specific instance of a DirectedGraph built already
	 * @param hashMap 
	 */
	public TreeVisualizer(DirectedGraph<Integer, Integer> g, HashMap<Integer, String> nodeLabels, HashMap<Integer, String> edgeLabels, SNVDatabase db) {
		final HashMap<Integer, String> nodeLabelsFinal;
		final HashMap<Integer, String> edgeLabelsFinal;
		
		if (nodeLabels == null) nodeLabelsFinal = new HashMap<Integer, String>();
		else nodeLabelsFinal = new HashMap<Integer, String>(nodeLabels);
		
		nodeLabelsFinal.put(0, "germline");
		for(int i=0; i < db.getNumofSamples(); i++){
			nodeLabelsFinal.put(-i-1, db.getName(i));
		}
		
		
		if (edgeLabels == null) edgeLabelsFinal = new HashMap<Integer, String>();
		else edgeLabelsFinal = new HashMap<Integer, String>(edgeLabels);
		
		JFrame frame = new JFrame(TreeBuilder.testName+" Tree View");
		DelegateTree<Integer, Integer> tree = new DelegateTree<Integer, Integer>(g);
		tree.setRoot(0);
		treeLayout = new TreeLayout<Integer, Integer>((Forest<Integer, Integer>) tree,100,70);
		//treeLayout.setSize(new Dimension(600, 600));
		radialLayout = new RadialTreeLayout<Integer, Integer>(tree);
		radialLayout.setSize(new Dimension(700, 700));
//		BasicVisualizationServer<Integer, Integer> visServer =
//			new BasicVisualizationServer<Integer, Integer>(treeLayout);
		visServer = new VisualizationViewer<Integer, Integer>(treeLayout);
		visServer.setPreferredSize(new Dimension(600, 500));
		rings = new Rings(g);
		
		//Setting up transformers for JUNG
		
		Transformer<Integer, String> PhyVertexLabelTransformer = new Transformer<Integer, String>(){
			public String transform(Integer num) {
				if (nodeLabelsFinal != null && nodeLabelsFinal.containsKey(num)) return nodeLabelsFinal.get(num);
				/*if (num < 0)
					return db.getName(-1*num - 1);
				else if (num == 0)
					return "Root";*/
				//else return "N-" + num.toString();
				else return null;//"N-" + num.toString();
			}
		};
		
		Transformer<Integer, String> PhyEdgeLabelTransformer = new Transformer<Integer, String>(){
			public String transform(Integer num) {
				if (edgeLabelsFinal != null && edgeLabelsFinal.containsKey(num)) return edgeLabelsFinal.get(num); 
				else
					return null;
			}
		};
		
//		Transformer<Integer, EdgeShape<Integer, Integer>> PhyEdgeShapeTransformer = new Transformer<Integer, EdgeShape<Integer, Integer>>(){
//			public Line<Integer, Integer> transformer(Integer num){
//				if (num >= 0) return (new EdgeShape.Line<Integer, Integer>());
//				else return (new EdgeShape.BentLine<Integer,Integer>());
//			}
//
//			@Override
//			public EdgeShape<Integer, Integer> transform(Integer num) {
//				if (num >= 0) return (new EdgeShape.Line<Integer, Integer>());
//				else return (new EdgeShape.BentLine<Integer,Integer>());
//			}
//		}
		
		float dash[] = {5.0f};
		final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
		Transformer<Integer, Stroke> PhyEdgeStrokeTransformer =
			new Transformer<Integer, Stroke>() {
				public Stroke transform(Integer num) {
					if (num < 0) return edgeStroke;
					else return null;
				}
			};
			
		Transformer<Integer, Paint> PhyVertexPaintTransformer =
			new Transformer<Integer, Paint>() {
				public Paint transform(Integer num){
					if (nodeLabelsFinal != null && nodeLabelsFinal.containsKey(num)) return Color.BLUE;
					if (num < 0) return Color.GREEN;
					else return Color.RED;
				}
			};
		
		//visServer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
		//visServer.setBackground(Color.WHITE);
		visServer.getRenderContext().setVertexLabelTransformer(PhyVertexLabelTransformer);
		visServer.getRenderContext().setVertexFillPaintTransformer(PhyVertexPaintTransformer);
		visServer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Integer, Integer>());
		visServer.getRenderContext().setEdgeLabelTransformer(PhyEdgeLabelTransformer);
		visServer.getRenderContext().setEdgeStrokeTransformer(PhyEdgeStrokeTransformer);
		
		//Creating graph mouse
		DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		visServer.setGraphMouse(graphMouse);
		
		Container content = frame.getContentPane();
		//final GraphZoomScrollPane panel = new GraphZoomScrollPane(visServer);
		content.add(visServer);
		
		JToggleButton radial = new JToggleButton("Radial");
		radial.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					LayoutTransition<Integer, Integer> lt = 
						new LayoutTransition<Integer, Integer>(visServer, treeLayout, radialLayout);
					Animator a = new Animator(lt);
					a.start();
					visServer.getRenderContext().getMultiLayerTransformer().setToIdentity();
					visServer.addPreRenderPaintable(rings);
				} else {
					LayoutTransition<Integer, Integer> lt =
						new LayoutTransition<Integer, Integer>(visServer, radialLayout, treeLayout);
					Animator a = new Animator(lt);
					a.start();
					visServer.getRenderContext().getMultiLayerTransformer().setToIdentity();
					visServer.removePreRenderPaintable(rings);
				}
				visServer.repaint();
			}
		});
		
		JPanel controls = new JPanel();
		//controls.setBackground(Color.WHITE);
		controls.add(radial);
		content.add(controls, BorderLayout.SOUTH);
		
		//JFrame frame = new JFrame("Simple Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.getContentPane().add(content);
		frame.pack();
		frame.setVisible(true);
		
		Dimension size = frame.getSize();
	      //BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
	      BufferedImage image = (BufferedImage)frame.createImage(size.width, size.height);
	      Graphics gr = image.getGraphics();
	      frame.paint(gr);
	      gr.dispose();
	      
	      try
	      {
	        ImageIO.write(image, "jpg", new File(TreeBuilder.path+TreeBuilder.testName+".tree.jpg"));
	      }
	      catch (IOException e)
	      {
	        e.printStackTrace();
	      }
	}
	
	public TreeVisualizer(DirectedGraph<Integer, Integer> g, HashMap<Integer, String> nodeLabels, HashMap<Integer, String> edgeLabels) {
		
		final HashMap<Integer, String> nodeLabelsFinal = new HashMap<Integer, String>(nodeLabels);
		
		JFrame frame = new JFrame(TreeBuilder.testName + " Network View");
		DAGLayout<Integer, Integer> dagLayout = new DAGLayout<Integer, Integer>(g);
		visServer = new VisualizationViewer<Integer, Integer>(dagLayout);
		visServer.setPreferredSize(new Dimension(600, 500));
		
		//Creating graph mouse
		DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		visServer.setGraphMouse(graphMouse);
		
		Transformer<Integer, String> PhyVertexLabelTransformer = new Transformer<Integer, String>(){
			public String transform(Integer num) {
				if (nodeLabelsFinal != null && nodeLabelsFinal.containsKey(num)) {
					return nodeLabelsFinal.get(num);
				}
				else return null;
			}
		};
		
		visServer.getRenderContext().setVertexLabelTransformer(PhyVertexLabelTransformer);
		
		Container content = frame.getContentPane();
		content.add(visServer);
		
		JPanel controls = new JPanel();
		content.add(controls, BorderLayout.SOUTH);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		Dimension size = frame.getSize();
	    BufferedImage image = (BufferedImage)frame.createImage(size.width, size.height);
	    Graphics gr = image.getGraphics();
	    frame.paint(gr);
	    gr.dispose();
      
	    try {
	    	ImageIO.write(image, "jpg", new File(TreeBuilder.path+TreeBuilder.testName+".tree.jpg"));
	    }
	    catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
	public TreeVisualizer(DirectedGraph<Integer, Integer> g, HashMap<Integer, String> nodeLabels) {
		
		final HashMap<Integer, String> nodeLabelsFinal = new HashMap<Integer, String>(nodeLabels);
		
		JFrame frame = new JFrame(TreeBuilder.testName + " Tree View");
		DelegateTree<Integer, Integer> tree = new DelegateTree<Integer, Integer>(g);
		tree.setRoot(0);
		treeLayout = new TreeLayout<Integer, Integer>((Forest<Integer, Integer>) tree,100,70);
		visServer = new VisualizationViewer<Integer, Integer>(treeLayout);
		visServer.setPreferredSize(new Dimension(600, 500));
		
		//Creating graph mouse
		DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		visServer.setGraphMouse(graphMouse);
		
		Transformer<Integer, String> PhyVertexLabelTransformer = new Transformer<Integer, String>(){
			public String transform(Integer num) {
				if (nodeLabelsFinal != null && nodeLabelsFinal.containsKey(num)) {
					return nodeLabelsFinal.get(num);
				}
				else return null;
			}
		};
		
		visServer.getRenderContext().setVertexLabelTransformer(PhyVertexLabelTransformer);
		
		Container content = frame.getContentPane();
		content.add(visServer);
		
		JPanel controls = new JPanel();
		content.add(controls, BorderLayout.SOUTH);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		Dimension size = frame.getSize();
	    BufferedImage image = (BufferedImage)frame.createImage(size.width, size.height);
	    Graphics gr = image.getGraphics();
	    frame.paint(gr);
	    gr.dispose();
      
	    try {
	    	ImageIO.write(image, "jpg", new File(TreeBuilder.path+TreeBuilder.testName+".tree.jpg"));
	    }
	    catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
	/**
	 * Function: Rings
	 * Constructor: Rings r = new Rings()
	 * ----
	 * A simple set of shapes taken from the JUNG library
	 * to draw the graph in a radial views.
	 *
	 */
	class Rings implements VisualizationServer.Paintable {
		
		Collection<Double> depths;
		
		public Rings(DirectedGraph<Integer, Integer> graph) {
			depths = getDepths(graph);
		}
		
		private Collection<Double> getDepths(DirectedGraph<Integer, Integer> graph) {
			Set<Double> depths = new HashSet<Double>();
			Map<Integer,PolarPoint> polarLocations = radialLayout.getPolarLocations();
			for(Integer v : graph.getVertices()) {
				PolarPoint pp = polarLocations.get(v);
				depths.add(pp.getRadius());
			}
			return depths;
		}

		public void paint(Graphics g) {
			g.setColor(Color.lightGray);
		
			Graphics2D g2d = (Graphics2D)g;
			Point2D center = radialLayout.getCenter();

			Ellipse2D ellipse = new Ellipse2D.Double();
			for(double d : depths) {
				ellipse.setFrameFromDiagonal(center.getX()-d, center.getY()-d, 
						center.getX()+d, center.getY()+d);
				Shape shape = visServer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).transform(ellipse);
				g2d.draw(shape);
			}
		}

		public boolean useTransform() {
			return true;
		}
	}

}
