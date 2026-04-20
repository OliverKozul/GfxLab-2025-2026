package xyz.marsavic.gfxlab.graphics3d.raytracers;

import xyz.marsavic.functions.F0;
import xyz.marsavic.functions.F1;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.*;


public class RayTracerSimple extends RayTracer {
	
	private static final double EPSILON = 1e-9;
	private final int maxDepth;
	
	
	public RayTracerSimple(F0<F1<Scene, Double>> ffSceneT, int maxDepth) {
		super(ffSceneT);
		this.maxDepth = maxDepth;
	}
	
	@Override
	protected Color sample(Scene scene, Ray ray) {
		return sample(scene, ray, maxDepth);
	}
	
	protected Color sample(Scene scene, Ray ray, int depthRemaining) {
		if (depthRemaining == 0) {
			return Color.BLACK;
		}
		
		Hit hit = scene.solid().firstHit(ray, EPSILON);  // We start from epsilon to avoid floating-point errors
		if (hit.t() == Double.POSITIVE_INFINITY) {
			return scene.colorBackground();
		}

		Material material = hit.material();
		Vec3 p = ray.at(hit.t());                        // The hit point
		Vec3 n_ = hit.n_();                              // Normalized normal to the body surface at the hit point

		if (material.normalMap() != null) {				 // Calculate new normal using normal map (if it exists)
			Vec3 tangent = hit.tangent();
			if (tangent != null && tangent.lengthSquared() > EPSILON) {
				Vec3 bitangent  = n_.cross(tangent);
				Vec3 texNormal  = material.normalMap();
				n_ = tangent   		.mul(texNormal.x())
					.add(bitangent  .mul(texNormal.y()))
					.add(n_         .mul(texNormal.z()))
					.normalized_();
			}
		}

		Vec3 i_ = ray.d().inverse().normalized_();       // Incoming direction
		Vec3 r_ = GeometryUtils.reflectedN(n_, i_);      // Reflected ray (i_ reflected over n_)
		
		Color lightDiffuse  = Color.BLACK;               // The sum of diffuse contributions from all the lights
		Color lightSpecular = Color.BLACK;               // The sum of specular contributions from all the lights
		
		for (Light light : scene.lights()) {
			Vec3 l = light.p().sub(p);                   // Vector from p to the light
			
//			Ray rayToLight = Ray.pd(p, l);
//			if (scene.solid().hitBetween(rayToLight, EPSILON, 1)) continue; // If in shadow
			
			double lLSqr = l.lengthSquared();            // Distance from p to the light squared
			double lL = Math.sqrt(lLSqr);                // Distance from p to the light
			double cosLN = n_.dot(l);                    // Cosine of the angle between l and n_ (multiplied by lL)
			
			if (cosLN > 0) {                             // If the light is above the surface
				cosLN /= lL;                        
				Color irradiance = light.c().mul(cosLN / lLSqr);
				// The irradiance represents how much light is received by a unit area of the surface. It is
				// proportional to the cosine of the incoming angle and inversely proportional to the distance squared
				// (inverse-square law).
				lightDiffuse = lightDiffuse.add(irradiance);
				
				double cosLR = l.dot(r_);
				if (cosLR > 0) {                         // If the angle between l and r is acute
					cosLR /= lL;
					lightSpecular = lightSpecular.add(irradiance.mul(Math.pow(cosLR, material.shininess())));
				}
			}
		}
		
		// When material has reflective properties, we recursively find the color visible along the ray (p, r).
		Color lightReflective = material.reflective().zero() ? 
				Color.BLACK :
				sample(scene, Ray.pd(p, r_), depthRemaining - 1);
		
		Color lightRefractive = material.refractive().zero() ? 
				Color.BLACK :
				sample(scene, Ray.pd(p, GeometryUtils.refractedNN(n_, i_, material.refractiveIndex())), depthRemaining - 1);
		
		Color result = Color.BLACK;
		result = result.add(lightDiffuse   .mul(material.diffuse   ()));
		result = result.add(lightSpecular  .mul(material.specular  ()));
		result = result.add(lightReflective.mul(material.reflective()));
		result = result.add(lightRefractive.mul(material.refractive()));
		
		return result;
	}
	
}
