package xyz.marsavic.gfxlab.graphics3d;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.textures.NormalMapTexture;


public record Material (
		Color diffuse,
		Color specular,
		double shininess,
		Color reflective,
		Color refractive,
		double refractiveIndex,

		Color emittance,
		BSDF bsdf,

		Vec3 normalMap
) implements F1<Material, Vector> {

	public Material diffuse        (Color  diffuse        ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material specular       (Color  specular       ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material shininess      (double shininess      ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material reflective     (Color  reflective     ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material refractive     (Color  refractive     ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material refractiveIndex(double refractiveIndex) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material emittance      (Color  emittance      ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material normalMap	   (Vec3 normalMap		  ) 	{ return new Material(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance, normalMap); }
	public Material specularCopyDiffuse() 						{ return this.specular(diffuse()); }


	public Material(Color diffuse, Color specular, double shininess, Color reflective, Color refractive, double refractiveIndex, Color emittance, Vec3 normalMap) {
		this(diffuse, specular, shininess, reflective, refractive, refractiveIndex, emittance,
				BSDF.importanceAverage(
						new BSDF[] {
								BSDF.diffuse   (diffuse),                       // TODO add specular
								BSDF.reflective(reflective),
								BSDF.refractive(refractive, refractiveIndex),
						},
						new double[] {
								diffuse   .luminance(),           // TODO Test importance sampling: How noisy is the result for different choices of weights.
								reflective.luminance(),
								refractive.luminance(),
						}
				),
				normalMap
		);
	}


	public Material(BSDF bsdf) {
		this(Color.BLACK, Color.BLACK, 32.0, Color.BLACK, Color.BLACK, 1.4, Color.BLACK, bsdf, null);
	}


	@Override
	public Material at(Vector uv) {
		return this;
	}


	// --- Utility constants and factory methods ---

	public static final Material BLACK   = new Material(Color.BLACK, Color.BLACK, 32, Color.BLACK, Color.BLACK, 1.5, Color.BLACK, null);

	public static Material matte (Color  c) { return BLACK.diffuse(c); }
	public static Material matte (double k) { return matte(Color.gray(k)); }
	public static Material matte (        ) { return matte(1.0); }
	public static final Material MATTE = matte();

	public static Material mirror(Color  c) { return BLACK.reflective(c); }
	public static Material mirror(double k) { return mirror(Color.gray(k)); }
	public static Material mirror(        ) { return mirror(1.0); }
	public static final Material MIRROR = mirror();

	public static Material glass (Color  c) { return BLACK.refractive(c).refractiveIndex(1.5); }
	public static Material glass (double k) { return glass(Color.gray(k)); }
	public static Material glass (        ) { return glass(1.0); }
	public static final Material GLASS = glass();

	public static Material light (Color  c) { return BLACK.emittance(c); }
	public static Material light (double k) { return light(Color.gray(k)); }
	public static Material light (        ) { return light(1.0); }
	public static final Material LIGHT = light();


	public static final Material DEFAULT = MATTE;


	public Material mul(double k) {
		return new Material(
				diffuse        .mul(k),
				specular       .mul(k),
				shininess         * k ,
				reflective     .mul(k),
				refractive     .mul(k),
				refractiveIndex   * k ,
				emittance      .mul(k),
				bsdf           .mul(k),
				normalMap
		);
	}

	public Material add(Material o) {
		return new Material(
				diffuse        .add(o.diffuse        ),
				specular       .add(o.specular       ),
				shininess         + o.shininess       ,
				reflective     .add(o.reflective     ),
				refractive     .add(o.refractive     ),
				refractiveIndex   + o.refractiveIndex ,
				emittance      .add(o.emittance      ),
				BSDF.importanceAverage(new BSDF[] {this.bsdf, o.bsdf}, new double[] {1, 1}),
				normalMap != null && o.normalMap != null ? normalMap.add(o.normalMap).normalized_() :
						normalMap != null ? normalMap : o.normalMap
		);
	}

	public static Material lerp(Material a, Material b, double k) {
		return a.mul(1-k).add(b.mul(k));
	}

	public static F1<Material, Vector> withNormalMap(Material base, NormalMapTexture... normalMaps) {
		return uv -> {
			Vec3 blended = null;
			for (NormalMapTexture nm : normalMaps) {
				Vec3 sampled = nm.sample(uv);
				blended = blended == null ? sampled : blended.add(sampled).normalized_();
			}
			return base.normalMap(blended);
		};
	}
}