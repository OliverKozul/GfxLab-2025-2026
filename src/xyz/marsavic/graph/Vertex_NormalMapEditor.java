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
	private final int imageW;
	private final int imageH;
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
		normalMap = element.in0.get();
		imageW = normalMap.image().getWidth();
		imageH = normalMap.image().getHeight();

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

		int px = (int)(e.getX() * imageW  / SIZE);
		int py = (int)(e.getY() * imageH / SIZE);
		paintBrush(px, py, radius, indent);
		element.out.fireInvalidated();
		redraw();
		e.consume();
	}

	private void paintBrush(int cx, int cy, double radius, boolean indent) {
		BufferedImage image = normalMap.image();
		int r = (int) Math.ceil(radius);

		float targetR = 0.5f;
		float targetG = 0.5f;
		float targetB = indent ? 0.0f : 1.0f;

		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist > radius) continue;

				int x = Math.floorMod(cx + dx, imageW);
				int y = Math.floorMod(cy + dy, imageH);

				double ratio = 1.0 - (dist / radius);
				float strength = (float)(ratio * ratio * 0.2);

				int rgb = image.getRGB(x, y);
				float r_ = ((rgb >> 16) & 0xFF) / 255.0f;
				float g_ = ((rgb >>  8) & 0xFF) / 255.0f;
				float b_ = ( rgb        & 0xFF) / 255.0f;

				r_ = r_ + (targetR - r_) * strength;
				g_ = g_ + (targetG - g_) * strength;
				b_ = b_ + (targetB - b_) * strength;

				int newRgb = ((int)(r_ * 255) << 16) | ((int)(g_ * 255) << 8) | (int)(b_ * 255);
				image.setRGB(x, y, newRgb);
			}
		}
	}

	private void redraw() {
		BufferedImage image = normalMap.image();
		BufferedImage res = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_RGB);

		float lx = -0.5f, ly = -0.5f, lz = 1.0f;
		float lLen = (float) Math.sqrt(lx*lx + ly*ly + lz*lz);
		lx /= lLen; ly /= lLen; lz /= lLen;

		for (int y = 0; y < imageH; y++) {
			for (int x = 0; x < imageW; x++) {
				int rgb = image.getRGB(x, y);
				float r = ((rgb >> 16) & 0xFF) / 255.0f;
				float g = ((rgb >>  8) & 0xFF) / 255.0f;
				float b = ( rgb        & 0xFF) / 255.0f;

				float nx = r * 2.0f - 1.0f;
				float ny = g * 2.0f - 1.0f;
				float nz = b * 2.0f - 1.0f;

				float diff = Math.max(0, nx * lx + ny * ly + nz * lz);
				int shade = (int)(diff * 255);

				res.setRGB(x, y, (shade << 16) | (shade << 8) | shade);
			}
		}

		WritableImage fxImg = SwingFXUtils.toFXImage(res, null);
		canvas.getGraphicsContext2D().drawImage(fxImg, 0, 0, SIZE, SIZE);
	}

	@Override public ElementNormalMapEditor element()                  { return element;     }
	@Override public Collection<VertexInputJack>  inputJacks()        { return inputJacks;  }
	@Override public Collection<VertexOutputJack> outputJacks()       { return outputJacks; }
	@Override public Region region()                                   { return this;        }
}