package istc.bigdawg.islands.SciDB.operators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import istc.bigdawg.islands.DataObjectAttribute;
import istc.bigdawg.islands.OperatorVisitor;
import istc.bigdawg.islands.SciDB.SciDBArray;
import istc.bigdawg.islands.operators.Operator;
import istc.bigdawg.islands.operators.Sort;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class SciDBIslandSort extends SciDBIslandOperator implements Sort {

	
	private List<String> sortKeys;
	
	private SortOrder sortOrder;
	
	private List<OrderByElement> orderByElements;
	
	private boolean isWinAgg = false; // is it part of a windowed aggregate or an ORDER BY clause?
	
	// for AFL
	public SciDBIslandSort(Map<String, String> parameters, SciDBArray output,  List<String> keys, Operator child) throws Exception  {
		super(parameters, output, child);

		isBlocking = true;
		blockerCount++;
		this.blockerID = blockerCount;

		// two order bys might exist in a supplement:
		// 1) within an OVER () clause for windowed aggregate
		// 2) as an ORDER BY clause
		// instantiate iterator to get the right one
		// iterate from first OVER --> ORDER BY

		setSortKeys(keys);
		
		outSchema = new LinkedHashMap<String, DataObjectAttribute>(((SciDBIslandOperator)child).outSchema);
		
	}
	
	public SciDBIslandSort(SciDBIslandOperator o, boolean addChild) throws Exception {
		super(o, addChild);
		SciDBIslandSort s = (SciDBIslandSort) o;
		
		this.blockerID = s.blockerID;

		this.setSortKeys(new ArrayList<>());
		this.setWinAgg(s.isWinAgg());
		for (String str : s.getSortKeys()) {
			this.getSortKeys().add(new String(str));
		}
		this.sortOrder = s.sortOrder; 
		this.setOrderByElements(new ArrayList<>());
		for (OrderByElement ob : s.getOrderByElements()) {
			this.getOrderByElements().add(ob);
		}
	}
	
	
	public String toString() {
		return "Sort operator on columns " + getSortKeys().toString() + " with ordering " + sortOrder;
	}
	
	/**
	 * PRESERVED
	 * @return
	 * @throws Exception
	 */
	public List<OrderByElement> updateOrderByElements() throws Exception {
		
		List<OrderByElement> ret = new ArrayList<>();
		
		List<Operator> treeWalker;
		for (OrderByElement obe : getOrderByElements()) {
			
			treeWalker = children;
			boolean found = false;
			
			while (treeWalker.size() > 0 && (!found)) {
				List<Operator> nextGeneration = new ArrayList<>();
				
				for (Operator o : treeWalker) {
					if (o.isPruned()) {
						
						Column c = (Column)obe.getExpression();
						
						if (((SciDBIslandOperator)o).getOutSchema().containsKey(c.getFullyQualifiedName())) {
							OrderByElement newobe = new OrderByElement();
							newobe.setExpression(new Column(new Table(o.getPruneToken()), c.getColumnName()));
							ret.add(newobe);
							found = true;
							break;
						}
					} else {
						nextGeneration.addAll(o.getChildren());
					}
				}
				
				treeWalker = nextGeneration;
			}
			
			
		}
		
		return ret;
	}
	
	@Override
	public void accept(OperatorVisitor operatorVisitor) throws Exception {
		operatorVisitor.visit(this);
	}
	
	@Override
	public String getTreeRepresentation(boolean isRoot) throws Exception{
		return "{sort"+children.get(0).getTreeRepresentation(false)+"}";
	}

	public boolean isWinAgg() {
		return isWinAgg;
	}

	public void setWinAgg(boolean isWinAgg) {
		this.isWinAgg = isWinAgg;
	}

	public List<String> getSortKeys() {
		return sortKeys;
	}

	public void setSortKeys(List<String> sortKeys) {
		this.sortKeys = sortKeys;
	}

	public List<OrderByElement> getOrderByElements() {
		return orderByElements;
	}

	public void setOrderByElements(List<OrderByElement> orderByElements) {
		this.orderByElements = orderByElements;
	}
	
};