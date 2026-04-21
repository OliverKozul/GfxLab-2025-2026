package xyz.marsavic.gfxlab.graphics3d.scenes;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.*;
import xyz.marsavic.gfxlab.aggregation.AggregatorFrameLast;
import xyz.marsavic.gfxlab.aggregation.EAggregator;
import xyz.marsavic.gfxlab.graphics3d.*;
import xyz.marsavic.gfxlab.graphics3d.cameras.Perspective;
import xyz.marsavic.gfxlab.graphics3d.cameras.TransformedCamera;
import xyz.marsavic.gfxlab.graphics3d.raytracers.RayTracerSimple;
import xyz.marsavic.gfxlab.graphics3d.solids.Ball;
import xyz.marsavic.gfxlab.graphics3d.textures.NormalMapTexture;
import xyz.marsavic.gfxlab.tonemapping.ToneMapping2;
import xyz.marsavic.gfxlab.tonemapping.ToneMapping3;
import xyz.marsavic.gfxlab.tonemapping.matrixcolor_to_colortransforms.AutoSoft;
import xyz.marsavic.reactions.elements.ElementF;
import xyz.marsavic.utils.Hash;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static xyz.marsavic.gfxlab.Vec3.xyz;
import static xyz.marsavic.reactions.elements.Elements.e;


public record NormalMapTest(
		double phiX,
		double phiY,
		double lightPhiX,
		double lightPhiY,
		double deepOceanHeight,
		double shallowOceanHeight,
		double coastHeight,
		double shoreHeight,
		double plainsHeight,
		double forestHeight,
		double mountainHeight,
		Camera camera
) implements FFSceneT {

	private Color heightToColor(float height) {
		if (height < deepOceanHeight) return Color.rgb(0.02, 0.05, 0.3);   	// deep ocean
		if (height < shallowOceanHeight) return Color.rgb(0.05, 0.15, 0.5);    // shallow ocean
		if (height < coastHeight) return Color.rgb(0.1,  0.4,  0.6);   		// coast
		if (height < shoreHeight) return Color.rgb(0.85, 0.80, 0.55);  		// shore/beach
		if (height < plainsHeight) return Color.rgb(0.2,  0.55, 0.15);  		// lowland
		if (height < forestHeight) return Color.rgb(0.15, 0.40, 0.1);   		// forest
		if (height < mountainHeight) return Color.rgb(0.55, 0.50, 0.45);  		// mountain
		return Color.rgb(0.95, 0.95, 0.95);                      				// snow peak
	}

	@Override
	public Solid solid() {
		try {
			var normalMapEarth = NormalMapTexture.fromFile("resources/xyz/marsavic/gfxlab/resources/normal_maps/Earth.png");
			var heightMapEarth = NormalMapTexture.fromFile("resources/xyz/marsavic/gfxlab/resources/height_maps/EarthHeightMap2.png");

			F1<Material, Vector> earthMaterial = uv -> {
				Vector remappedUv = Vector.xy(
						uv.x(),
						0.5 - uv.y() / 2.0
				);
				Vec3 normal = normalMapEarth.sample(remappedUv);
				float height = heightMapEarth.sampleHeight(remappedUv);
				Color color = heightToColor(height);
				return Material.matte(color).normalMap(normal);
			};

			return Ball.cr(Vec3.xyz(0, 0, 0), 1.0)
					.material(earthMaterial)
					.transformed(Affine3.chain(
							Affine3.rotationAboutY(phiY),
							Affine3.rotationAboutX(phiX)
			));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<Light> lights() {
		Vec3 lightPos = Affine3.chain(
				Affine3.rotationAboutY(lightPhiY),
				Affine3.rotationAboutX(lightPhiX)
		).at(Vec3.xyz(0, 0, -4));

		return List.of(
				Light.pc(lightPos, Color.WHITE.mul(3.0)),
				Light.pc(Vec3.xyz(-3, 0, 3), Color.WHITE.mul(0.3))
		);
	}

	public static ElementF<Animation> setup() {
		return
				e(ToneMapping3.class,
						new EAggregator(
								e(AggregatorFrameLast::new),
								e(RayTracerSimple.class,
										e(NormalMapTest.class
												, e(0.3)		// phiX
												, e(0.0)		// phiY
												, e(0.0)    	// lightPhiX
												, e(0.0)    	// lightPhiY
												, e(0.65)		// deep ocean
												, e(0.70)		// shallow ocean
												, e(0.72)		// coast
												, e(0.76)		// shore
												, e(0.79)		// plains
												, e(0.90)		// forest
												, e(0.92)		// mountain
												, e(TransformedCamera.class
														, e(Perspective.class, e(1/3.0))
														, e(Affine3::isometry, e(0.0), e(0.0), e(0.0), e(0.0), e(0.0), e(-3.5))
												)
										),
										e(16)
								),
								e(TransformationFromSize.ToGeometricT0_.class),
								e(xyz(1, 640, 640)),
								e(true),
								e(false),
								e(Hash.class, e(0x8EE6B0C4E02CA7B2L))
						),
						e(ToneMapping2.class,
								e(AutoSoft.class, e(0x1p-4), e(1.0))
						)
				);
	}
}