package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.gfxlab.Affine3;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;

public class Transformed implements Solid {

	private final Solid solid;
	private final Affine3 t, tInv, tInvT;

	public Transformed(Solid solid, Affine3 t) {
		this.solid = solid;
		this.t     = t;
		tInv  = t.inverse();
		tInvT = tInv.transpose();
	}

	@Override
	public Hit firstHit(Ray ray, double afterTime) {
		Ray rayO     = ray.transformed(tInv);
		Hit hitO     = solid.firstHit(rayO, afterTime);
		Vec3 n       = tInvT.at(hitO.n());
		Vec3 tangent = t.at(hitO.tangent());
		return hitO.withN(n, tangent);
	}
	
	@Override
	public boolean hitBetween(Ray ray, double afterTime, double beforeTime) {
		Ray rayO = ray.transformed(tInv);
		return solid.hitBetween(rayO, afterTime, beforeTime);
	}
	
}
