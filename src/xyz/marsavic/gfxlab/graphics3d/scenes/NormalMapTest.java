package xyz.marsavic.gfxlab.graphics3d.scenes;

import xyz.marsavic.gfxlab.*;
import xyz.marsavic.gfxlab.aggregation.AggregatorFrameLast;
import xyz.marsavic.gfxlab.aggregation.EAggregator;
import xyz.marsavic.gfxlab.graphics3d.*;
import xyz.marsavic.gfxlab.graphics3d.cameras.Perspective;
import xyz.marsavic.gfxlab.graphics3d.cameras.TransformedCamera;
import xyz.marsavic.gfxlab.graphics3d.raytracers.RayTracerSimple;
import xyz.marsavic.gfxlab.graphics3d.solids.Ball;
import xyz.marsavic.gfxlab.graphics3d.solids.Box;
import xyz.marsavic.gfxlab.graphics3d.solids.Group;
import xyz.marsavic.gfxlab.graphics3d.solids.HalfSpace;
import xyz.marsavic.gfxlab.graphics3d.textures.Grid;
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
		double k,
		double phiX,
		double phiY,
		Camera camera
) implements FFSceneT {

	@Override
	public Solid solid() {
		var materialUVWalls = Grid.standardQuarter(Color.WHITE);
		var materialUVWallsL = Grid.standardQuarter(Color.hsb(0.00, 0.5, 1.0));
		var materialUVWallsR = Grid.standardQuarter(Color.hsb(0.33, 0.5, 1.0));

        try {
			var normalMaterial1 = Material.matte(Color.gray(0.8))
					.normalMap(NormalMapTexture.fromFile("resources/xyz/marsavic/gfxlab/resources/normal_maps/NormalMap.png"));
			var normalMaterial2 = Material.matte(Color.gray(0.8))
					.normalMap(NormalMapTexture.fromFile("resources/xyz/marsavic/gfxlab/resources/normal_maps/NormalMap2.png"));
			var normalMaterial3 = normalMaterial1.add(normalMaterial2);
			var normalMaterialEarth = Material.matte(Color.rgb(0.2, 0.4, 0.5))
					.normalMap(NormalMapTexture.fromFile("resources/xyz/marsavic/gfxlab/resources/normal_maps/Earth.png"));
            return Group.of(
                    HalfSpace.pn(Vec3.xyz(-1, 0, 0), Vec3.xyz(2, 0, 0), materialUVWallsL),
                    HalfSpace.pn(Vec3.xyz(1, 0, 0), Vec3.xyz(-2, 0, 0), materialUVWallsR),
                    HalfSpace.pn(Vec3.xyz(0, -1, 0), Vec3.xyz(0, 2, 0), materialUVWalls),
                    HalfSpace.pn(Vec3.xyz(0, 1, 0), Vec3.xyz(0, -2, 0), materialUVWalls),
                    HalfSpace.pn(Vec3.xyz(0, 0, 1), Vec3.xyz(0, 0, -2), materialUVWalls),

                    Box.$.pd(Vec3.xyz(0.5, 0, 0), Vec3.xyz(1, 1, 1)).material(normalMaterial3)

							.transformed(Affine3.chain(
                            Affine3.scaling(Vec3.xyz(k, 1.0, 1.0)),
                            Affine3.rotationAboutY(phiY),
                            Affine3.rotationAboutX(phiX)
                    )),
					Ball.cr(Vec3.xyz(-0.5, 0, 0), 0.5).material(normalMaterialEarth)

							.transformed(Affine3.chain(
									Affine3.scaling(Vec3.xyz(k, 1.0, 1.0)),
									Affine3.rotationAboutY(phiY),
									Affine3.rotationAboutX(phiX)
							))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public Collection<Light> lights() {
		return List.of(
				Light.pc(Vec3.xyz(-0.7, 0.7, -0.7), Color.WHITE),
				Light.pc(Vec3.xyz(-0.7, 0.7,  0.7), Color.WHITE),
				Light.pc(Vec3.xyz( 0.7, 0.7, -0.7), Color.WHITE),
				Light.pc(Vec3.xyz( 0.7, 0.7,  0.7), Color.WHITE)
		);
	}


	// ================================================================================================================


	public static ElementF<Animation> setup() {
		return
				e(ToneMapping3.class,
						new EAggregator(
								e(AggregatorFrameLast::new),
//								e(AggregatorOnDemand::new), // faster but not antialiased
								e(RayTracerSimple.class,
									e(NormalMapTest.class
											, e(0.5)
											, e(0.0)
											, e(0.0)
											, e(TransformedCamera.class
												, e(Perspective.class, e(1/3.0))
												, e(Affine3::isometry, e(0.0), e(0.0), e(0.0), e(0.0), e(0.0), e(-4.0))
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
