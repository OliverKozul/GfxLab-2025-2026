package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Material;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;

import static xyz.marsavic.utils.Numeric.*;


public interface SDF extends Solid {
	
	double dist(Vec3 p);
	

	int MAX_ITERS = 32;
	double DELTA = 1e-4;
	
	@Override
	default Hit firstHit(Ray ray, double afterTime) {
		Ray ray_ = ray.normalized_();
		double l = ray.d().length();
		double t = 0;
		int nIters = 0;
		
		while (true) {
			Vec3 p = ray_.at(t);
			double d = dist(p);
			t += d;
			if (d < DELTA) {
				return new HitSDF(t / l, this, p);
			}
			nIters++;
			if (nIters > MAX_ITERS) {
				return d < 1 ? new HitSDF(t / l, this, p) : Hit.AtInfinity.axisAligned(ray.d(), false);
			}
		}
	}
	
	
	class HitSDF implements Hit {
		private final double t;
		private final SDF sdf;
		private final Vec3 p;
		private Vec3 cachedN = null;
		
		HitSDF(double t, SDF sdf, Vec3 p) {
			this.t = t;
			this.sdf = sdf;
			this.p = p;
		}
		
		@Override
		public double t() {
			return t;
		}
		
		private static final double EPSILON = 1e-6;
		private static final Vec3 EPSILON_X = Vec3.EX.mul(EPSILON);
		private static final Vec3 EPSILON_Y = Vec3.EY.mul(EPSILON);
		private static final Vec3 EPSILON_Z = Vec3.EZ.mul(EPSILON);

		@Override
		public Vec3 n() {
			if (cachedN == null) {
				double dp = sdf.dist(p);
				cachedN = Vec3.xyz(
						sdf.dist(p.add(EPSILON_X)) - dp,
						sdf.dist(p.add(EPSILON_Y)) - dp,
						sdf.dist(p.add(EPSILON_Z)) - dp
				);
			}
			return cachedN;
		}
		
		static final Material MATERIAL = Material.matte(Color.hsb(0.6, 0.5, 1.0)).specular(Color.WHITE);
		@Override
		public Material material() {
			return MATERIAL;
		}
		
		@Override
		public Vector uv() {
			return Vector.ZERO;
		}

		@Override
		public Vec3 tangent() {
			Vec3 n = n_();
			Vec3 helper = Math.abs(n.x()) < 0.9 ? Vec3.EX : Vec3.EY;
			return helper.sub(n.mul(n.dot(helper)));
		}
	}
	
	
	static SDF ball(Vec3 c, double r) { return p -> p.sub(c).length() - r; }
	
	static SDF box(Vec3 r) {
		return p -> {
			Vec3 q = p.abs();
			return
				Vec3.max(q.sub(r), Vec3.ZERO).length() 
				- 
				Math.max(r.sub(q).min(), 0);
		};
	}
	
	static SDF union(SDF a, SDF b) {
		return p -> Math.min(a.dist(p), b.dist(p));
	}
	
	static SDF expand(SDF a, double r) {
		return p -> a.dist(p) - r;
	}
	
	static SDF smoothUnion(SDF a, SDF b, double r) {
		return p -> {
			double da = a.dist(p);
			double db = b.dist(p);
			double k = clamp((da - db) / r * 0.5 + 0.5);
			return (1 - k) * da + k * db - k * (1 - k) * r;
		};
	}
	
}
