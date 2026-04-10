package xyz.marsavic.graph;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import xyz.marsavic.geometry.Box;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.ElementAnimationSink;
import xyz.marsavic.gfxlab.ElementNormalMapEditor;
import xyz.marsavic.javafx.UtilsFX;
import xyz.marsavic.reactions.elements.*;

import java.util.*;
import java.util.stream.Collectors;


public class Graph extends Pane {
	// ---
	private final Map<Element, Vertex> map_element_vertex_ = new HashMap<>();
	private final List<Element> elements_ = new ArrayList<>();
	private final List<Edge> edges_ = new ArrayList<>();
	private final Map<Element, List<Edge>> map_inputEdges_ = new HashMap<>(); 
	private final Map<Element.Input <?>, VertexInputJack > map_input_jack_  = new HashMap<>();
	private final Map<Element.Output<?>, VertexOutputJack> map_output_jack_ = new HashMap<>();
	
	private final List<Vertex> vertices_ = new ArrayList<>();
	private final List<Connection> connections_ = new ArrayList<>();

	// ---
	
	public final Map<Element, Vertex> map_element_vertex = Collections.unmodifiableMap(map_element_vertex_);
	public final List<Element> elements = Collections.unmodifiableList(elements_);
	public final List<Edge> edges = Collections.unmodifiableList(edges_);
	public final Map<Element, List<Edge>> map_inputEdges = Collections.unmodifiableMap(map_inputEdges_);
	public final Map<Element.Input <?>, VertexInputJack > map_input_jack  = Collections.unmodifiableMap(map_input_jack_ );
	public final Map<Element.Output<?>, VertexOutputJack> map_output_jack = Collections.unmodifiableMap(map_output_jack_);
	
	public final List<Vertex> vertices = Collections.unmodifiableList(vertices_);
	public final List<Connection> connections = Collections.unmodifiableList(connections_);
	
	// ---
	
	private final Pane paneConnections = new Pane();
	private final Pane paneVertices    = new Pane();

	private boolean panning;
	private Vector panPress, panStartTranslate;

	

	public Graph() {
		getChildren().addAll(paneConnections, paneVertices);
		paneConnections.setMouseTransparent(true);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		
		setPickOnBounds(true); // allows dragging even when clicking "empty" background inside the Pane bounds
		installBackgroundPanning();

		// Create a clipping rectangle
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(widthProperty());
		clip.heightProperty().bind(heightProperty());
		setClip(clip);
	}

	
	private Vertex createVertexByType(Element e) {
//		if (e instanceof ElementAnimationSink   e_) return new Vertex_AnimationSinkImage (e_);
		if (e instanceof ElementNormalMapEditor e_) return new Vertex_NormalMapEditor(e_);
		if (e instanceof ElementAnimationSink   e_) return new Vertex_AnimationSink      (e_);
		if (e instanceof ElementDouble          e_) return new Vertex_Double             (e_);
		if (e instanceof ElementInteger         e_) return new Vertex_Integer            (e_);
		if (e instanceof ElementLong            e_) return new Vertex_Long               (e_);
		if (e instanceof ElementBoolean         e_) return new Vertex_Boolean            (e_);
		if (e instanceof ElementSingleOutput<?> e_) return new Vertex_ElementSingleOutput(e_);
		if (e instanceof ElementA1<?>           e_) return new Vertex_ElementA1          (e_);
		if (e instanceof Element                e_) return new Vertex_Default            (e_);
		return null;
	}
	

	public Vertex createVertex(Element e) {
		Vertex existing = map_element_vertex.get(e);
		if (existing != null) {
			return existing;
		}
		
		Vertex vertex = createVertexByType(e);
		
		elements_.add(e);
		vertices_.add(vertex);
		map_element_vertex_.put(e, vertex);
		map_inputEdges_.put(e, new ArrayList<>());
		
		paneVertices.getChildren().add(vertex.region());

		for (VertexInputJack vertexInputJack : vertex.inputJacks()) {
			map_input_jack_.put(vertexInputJack.input, vertexInputJack);
		}

		for (VertexOutputJack vertexOutputJack : vertex.outputJacks()) {
			map_output_jack_.put(vertexOutputJack.output, vertexOutputJack);
		}
		
		enableNodeDragging(vertex.region());
		
		return vertex;
	}




	public void createConnection(Element.Output<?> output, Element.Input<?> input) {
		Edge e = new Edge(input, output);
		edges_.add(e);
		map_inputEdges.get(input.element()).add(e);
		
		VertexInputJack vertexInputJack = map_input_jack.get(input);
		VertexOutputJack vertexOutputJack = map_output_jack.get(output);

		Connection c = new Connection(paneConnections, vertexInputJack, vertexOutputJack);
		connections_.add(c);
		paneConnections.getChildren().add(c);
	}
	
	
	
	private static Map<Element, Box> getVertexBoxes(Map<Element, Vertex> map_element_vertex) {
		return
				map_element_vertex.entrySet().stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> UtilsFX.box(entry.getValue().region())
				));
	}
	
	
	private static void setVertexPositions(Map<Element, Vertex> map_element_vertex, Map<Element, Box> positions) {
		positions.forEach((e, b) ->
				UtilsFX.setLayoutP(map_element_vertex.get(e).region(), b.p())
		);			
	}
	
	
/*
	// This is how I call Task. Not really needed here, as 'new LayoutDFS(...)' is fast.
	
	public void layItOutFX() {
		Map<Element, Box> boxesBefore = getVertexBoxesFX(map_element_vertex);
		Task<Map<Element, Box>> task = new Task<>() {
			@Override
			protected Map<Element, Box> call() throws Exception {
				return new LayoutDFS(boxesBefore, map_inputEdges).result;
			}
		};
		task.setOnSucceeded(e -> {
			setVertexPositionsFX(map_element_vertex, task.getValue());
			centerContentFX();
		});
		
		ForkJoinPool.commonPool().submit(task);
	}
*/

	
	private static final Vector margins = Vector.xy(30, 10);
	
	public static final Layout layout1 = Layout.combine(
			Layout.margins(margins),
			LayoutDFS.INSTANCE,
			LayoutForces.INSTANCE,
			Layout.margins(margins.inverse()),
			Layout.CENTER,
			Layout.QUANTIZE			
	);

	public static final Layout layout2 = Layout.combine(
			Layout.margins(margins),
			LayoutDFS.INSTANCE,
			Layout.margins(margins.inverse()),
			Layout.CENTER,
			Layout.QUANTIZE			
	);

	public static final Layout layout3 = Layout.combine(
			Layout.margins(margins),
			LayoutForces.INSTANCE,
			Layout.margins(margins.inverse()),
			Layout.CENTER,
			Layout.QUANTIZE			
	);

	
	public void makeLayout(Layout layout) {
		Map<Element, Box> boxes = getVertexBoxes(map_element_vertex);
		
		UtilsFX.submitTask(
				() -> layout.at(boxes, map_inputEdges),
				positions -> setVertexPositions(map_element_vertex, positions)
		);
	}
	
	
	public void centerOnZero() {
		setTranslate(UtilsFX.box(this).d().div(2));
	} 

	
	private void setTranslate(Vector t) {
		t = t.round();
		UtilsFX.setTranslate(paneVertices, t);
		UtilsFX.setTranslate(paneConnections, t);
	}
	
	
	private void installBackgroundPanning() {
		addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			if (e.getButton() != MouseButton.PRIMARY) return;

			if (isInsideVertex(e.getTarget())) return;

			panning = true;
			panPress = Vector.xy(e.getSceneX(), e.getSceneY());
			panStartTranslate = Vector.xy(paneVertices.getTranslateX(), paneVertices.getTranslateY());
			setCursor(Cursor.MOVE);
			e.consume();
		});

		addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			if (!panning) return;

			Vector sceneP = Vector.xy(e.getSceneX(), e.getSceneY());
			Vector d = sceneP.sub(panPress);
			setTranslate(panStartTranslate.add(d));

			e.consume();
		});

		addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (!panning) return;
			panning = false;
			setCursor(Cursor.DEFAULT);
			e.consume();
		});
	}

	
	private static boolean isInsideVertex(Object eventTarget) {
		if (!(eventTarget instanceof Node n)) return false;

		Parent p = n instanceof Parent ? (Parent) n : n.getParent();
		while (p != null) {
			if (p instanceof Vertex) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}


	static void enableNodeDragging(Node node) {
		final double[] start = new double[2];
	
		node.setOnMousePressed(e -> {
			node.toFront();
			start[0] = e.getSceneX() - node.getLayoutX();
			start[1] = e.getSceneY() - node.getLayoutY();
		});
	
		node.setOnMouseDragged(e -> {
			node.setLayoutX(e.getSceneX() - start[0]);
			node.setLayoutY(e.getSceneY() - start[1]);
		});
	}	
	
	
	
}
