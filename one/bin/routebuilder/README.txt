On 19.8.2010 20:15, PJ Dillon wrote:
> Hi,
> 
> I created a simple tool for building routes and points-of-interest files 
> for and based on ONE. It gives you a graphical display of the vertices 
> and edges in your map. For building a route, you  can then click a 
> series of vertices to define the stops along the route. If you make a 
> mistake, you can click on the mistaken vertex again to unselect it (only 
> the final vertex can be unselected so that you can include a vertex in 
> your route multiple times). You can save and load your progress to a 
> file. The same functionality works for building a points-of-interest 
> file, except that you can unselect any point at any time.
> 
> The code uses the ONE's map reader, and, therefore, needs to compile 
> against the one. It's defined in its own package, so you should be able 
> to drop the tar ball into the ONE project (I personally have it defined 
> as a separate project in Eclipse).
> 
> To run the route builder:
> 
> java routebuilder.RouteBuilder <mapfile>
> 
> To run the POI builder:
> 
> java routebuilder.POIBuilder <mapfile>
> 
> where <mapfile> is the path to the map through which you'd like to build 
> a route.
> 
> PJ