package routing;

import core.Settings;

public abstract class DTNRouter extends ActiveRouter {

	public DTNRouter(Settings s) {
		super(s);
	}
	
	public DTNRouter(DTNRouter r) {
		super(r);
	}
	

	
	public String hello(){return "DTNRouter";}
	

}
