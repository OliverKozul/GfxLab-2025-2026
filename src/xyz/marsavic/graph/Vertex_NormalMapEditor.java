package xyz.marsavic.graph;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import xyz.marsavic.functions.A1;
import xyz.marsavic.gfxlab.ElementNormalMapEditor;
import xyz.marsavic.gfxlab.graphics3d.textures.NormalMapTexture;
import xyz.marsavic.reactions.values.EventInvalidated;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class Vertex_NormalMapEditor extends VBox implements Vertex {

	private static final int SIZE = 256;

	public final ElementNormalMapEditor element;
	public final VertexInputJack jackBrush;
	public final VertexInputJack jackTexture;
	public final VertexOutputJack jackOut;
	public final List<VertexInputJack> inputJacks;
	public final List<VertexOutputJack> outputJacks;

	private NormalMapTexture normalMap;
	private final Canvas canvas = new Canvas(SIZE, SIZE);

	final A1<EventInvalidated> onInvalidated = this::invalidated;

	public Vertex_NormalMapEditor(ElementNormalMapEditor element) {
		this.element = element;

		getStyleClass().add("vertex");

		VertexHeader vertexHeader = new VertexHeader(element);
		jackTexture = new VertexInputJack(element.in0);
		jackBrush = new VertexInputJack(element.inBrushSize);
		jackOut = new VertexOutputJack(element.out);

		HBox.setHgrow(vertexHeader, Priority.ALWAYS);
		HBox hBox = new HBox(jackTexture, jackBrush, vertexHeader, jackOut);
		hBox.setAlignment(Pos.CENTER_LEFT);

		canvas.setOnMousePressed(this::paint);
		canvas.setOnMouseDragged(this::paint);

		getChildren().addAll(hBox, canvas);
		inputJacks = List.of(jackTexture, jackBrush);
		outputJacks = List.of(jackOut);

		element.in0.output().onInvalidated().add(onInvalidated);
		invalidated(null);
	}

	private void invalidated(EventInvalidated e) {
		normalMap = element.in0.get();
		redraw();
	}

	private void paint(MouseEvent e) {
		if (normalMap == null) return;
		boolean indent = e.getButton() == MouseButton.PRIMARY;
		double radius = element.inBrushSize.get();

		int px = (int)(e.getX() * normalMap.image().getWidth()  / SIZE);
		int py = (int)(e.getY() * normalMap.image().getHeight() / SIZE);
		paintBrush(px, py, radius, indent);
		element.out.fireInvalidated();
		redraw();
		e.consume();
	}

	private void paintBrush(int cx, int cy, double radius, boolean indent) {
		BufferedImage image = normalMap.image();
		int r = (int) Math.ceil(radius);

		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist > radius) continue;

				int x = Math.floorMod(cx + dx, image.getWidth());
				int y = Math.floorMod(cy + dy, image.getHeight());

				double ratio = 1.0 - (dist / radius);
				double strength = ratio * ratio * 0.025f;

				int rgb = image.getRGB(x, y);
				int red   = (rgb >> 16) & 0xFF;
				int green = (rgb >>  8) & 0xFF;
				int blue  =  rgb        & 0xFF;

				float b = blue / 255.0f;

				if (indent) {
					b -= (float)(strength);
				} else {
					b += (float)(strength);
				}

				int newB = (int) (Math.clamp(b, 0.0f, 1.0f) * 255);
				int newRgb = (red << 16) | (green << 8) | newB;
				image.setRGB(x, y, newRgb);
			}
		}
	}

	private void redraw() {
		WritableImage fxImg = SwingFXUtils.toFXImage(normalMap.image(), null);
		canvas.getGraphicsContext2D().drawImage(fxImg, 0, 0, SIZE, SIZE);
	}

	@Override public ElementNormalMapEditor element()                  { return element;     }
	@Override public Collection<VertexInputJack>  inputJacks()        { return inputJacks;  }
	@Override public Collection<VertexOutputJack> outputJacks()       { return outputJacks; }
	@Override public Region region()                                   { return this;        }
}