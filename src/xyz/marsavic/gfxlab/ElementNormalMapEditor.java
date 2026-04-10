package xyz.marsavic.gfxlab;

import xyz.marsavic.gfxlab.graphics3d.textures.NormalMapTexture;
import xyz.marsavic.reactions.elements.Element;
import xyz.marsavic.reactions.elements.HasOutput;


public class ElementNormalMapEditor extends Element implements HasOutput<NormalMapTexture> {

	public Input<NormalMapTexture> in0;
	public final Input<Double> inBrushSize;

	public final Output<NormalMapTexture> out = new Output<>("out", () -> in0.get());

	public ElementNormalMapEditor(HasOutput<NormalMapTexture> p0, HasOutput<Double> pBrushSize) {
		super("Normal Map Editor");
		in0 = new Input<>(p0);
		inBrushSize = new Input<>("Brush Size", Double.class, pBrushSize);
	}

	@Override
	public Output<NormalMapTexture> out() {
		return out;
	}

	public NormalMapTexture get() {
		return out.get();
	}
}