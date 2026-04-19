package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Material;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;
import xyz.marsavic.utils.Numeric;


public class Cylinder implements Solid {
	
	private final Vec3 p, d;
	private final double r;
	private final F1<Material, Vector> mapMaterial;
	
	// transient
	private final double rSqr;

	
	
	private Cylinder(Vec3 p, Vec3 d, double r, F1<Material, Vector> mapMaterial) {
		this.p = p;
		this.d = d;
		this.r = r;
		rSqr = r * r;
		this.mapMaterial = mapMaterial;
	}
	
	public static Cylinder pdr(Vec3 p, Vec3 d, double r, F1<Material, Vector> mapMaterial) {
		return new Cylinder(p, d, r, mapMaterial);
	}
	
	public static Cylinder pdr(Vec3 p, Vec3 d, double r) {
		return new Cylinder(p, d, r, Material.DEFAULT);
	}
	
	public Cylinder material(F1<Material, Vector> mapMaterial) {
		return new Cylinder(p, d, r, mapMaterial);
	}
	
	
	public Vec3 p() {
		return p;
	}
	
	
	public Vec3 d() {
		return d;
	}
	
	
	public double r() {
		return r;
	}
	
	
	
	@Override
	public Hit firstHit(Ray ray, double afterTime) {
		Vec3 e = p.sub(ray.p());                                // Vector from the ray origin to the cylinder center
		Vec3 eP = e.rejection(d);
		Vec3 dP = ray.d().rejection(d);
		double dSqr = dP.lengthSquared();
		double l = eP.dot(dP) / dSqr;
		double mSqr = l * l - (eP.lengthSquared() - rSqr) / dSqr;
		
		if (mSqr > 0) {
			double m = Math.sqrt(mSqr);
			if (l - m > afterTime) return new Cylinder.HitBall(ray, l - m);
			if (l + m > afterTime) return new Cylinder.HitBall(ray, l + m);
		}
		return Hit.AtInfinity.axisAligned(ray.d(), r < 0);
	}
	
	
	class HitBall extends Hit.RayT {
		
		protected HitBall(Ray ray, double t) {
			super(ray, t);
		}
		
		@Override
		public Vec3 n() {
			return ray().at(t()).sub(p()).rejection(d);
		}
		
		@Override
		public Material material() {
			return Cylinder.this.mapMaterial.at(uv());
		}
		
		@Override
		public Vector uv() {
			Vec3 n = n();
			return Vector.xy(
					Numeric.atan2T(n.z(), n.x()),
					-2 * Numeric.asinT(n.y() / r) + 0.5
			);
		}

		@Override
		public Vec3 tangent() {
			Vec3 n = n();
			return Vec3.xyz(-n.z(), 0, n.x());
		}
		
		@Override
		public Vec3 n_() {
			return n().div(r);
		}
		
	}
		
}
