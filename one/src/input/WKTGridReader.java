package input;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import core.Coord;

import movement.grid.GridCell;
import movement.grid.GridMap;
import movement.map.MapNode;

public class WKTGridReader extends WKTReader{
	
	private List<GridCell> cells;
	
	
	/**
	 * Constructor. Creates a new WKT reader ready for addPaths() calls.
	 * @param bidi If true, all read paths are set bidirectional (i.e. if node A 
	 * is a neighbor of node B, node B is also a neighbor of node A).
	 */
	public WKTGridReader() {
		this.cells = new ArrayList<GridCell>();
	}
	
	/**
	 * Returns new a GridMap that is based on the read map
	 * @return new a GridMap that is based on the read map
	 */
	public GridMap getMap() {
		return new GridMap(this.cells);
	}
	
	public void addCells(File file) throws IOException {
		addCells(new FileReader(file));
	}

	public void addCells(Reader input) throws IOException {
		String type;
		String contents;
		
		init(input);
		
		while((type = nextType()) != null) {
			if (type.equals(POLYGON)) {
				contents = readNestedContents();
				updateMap(parsePolygon(contents));
			}
			/*else if (type.equals(MULTILINESTRING)) {
				for (List<Coord> list : parseMultilinestring()) {
					updateMap(list);
				}
			}*/
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}
	}
	
	
	private void updateMap(List<Coord> coords) {
		try{
			GridCell cell = new GridCell(coords);
			this.cells.add(cell);
		}
		catch (IllegalArgumentException e){
			System.out.println("Illegal argument in GridCell creation: "+e.getMessage());
		}
	}
}
