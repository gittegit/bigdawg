package istc.bigdawg.islands.relational.operators;

import java.util.List;
import java.util.Map;

import istc.bigdawg.islands.DataObject;
import istc.bigdawg.islands.OperatorVisitor;
import istc.bigdawg.islands.TheObjectThatResolvesAllDifferencesAmongTheIslands;
import istc.bigdawg.islands.operators.SeqScan;
import istc.bigdawg.islands.relational.SQLOutItemResolver;
import istc.bigdawg.islands.relational.SQLTableExpression;
import istc.bigdawg.islands.relational.utils.SQLAttribute;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class SQLIslandSeqScan extends SQLIslandScan implements SeqScan {

	// this is another difference from regular sql processing where the inclination is to keep the rows whole until otherwise needed
	public SQLIslandSeqScan (Map<String, String> parameters, List<String> output, SQLIslandOperator child, SQLTableExpression supplement) throws Exception  {
		super(parameters, output, child, supplement);
		
		// match output to base relation

		String schemaAndName = parameters.get("Schema");
		if (schemaAndName == null || schemaAndName.equals("public")) schemaAndName = super.getSrcTable();
		else schemaAndName = schemaAndName + "." + super.getSrcTable();
		
		this.dataObjects.add(schemaAndName);
		
		String createTableString = TheObjectThatResolvesAllDifferencesAmongTheIslands.getRelationalIslandCreateTableString(schemaAndName);
		
		CreateTable create = null;
		try {
		create = (CreateTable) CCJSqlParserUtil.parse(createTableString);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		
		DataObject baseTable = new DataObject(create); 
		
		for(int i = 0; i < output.size(); ++i) {
			
			String expr = output.get(i); // fully qualified name
			
			SQLOutItemResolver out = new SQLOutItemResolver(expr, baseTable.getAttributes(), supplement);
			
			SQLAttribute sa =  out.getAttribute();
			String alias = sa.getName();
			
			outSchema.put(alias, sa);
			
		}
		
		if (getFilterExpression() != null && (!getFilterExpression().equals("")))
			setOperatorName("filter");
		else if (children.size() != 0)
			setOperatorName("project");
		else 
			setOperatorName("scan");
		
	}
	
	public SQLIslandSeqScan(SQLIslandOperator o, boolean addChild) throws Exception {
		super(o, addChild);
		this.setOperatorName(((SQLIslandSeqScan)o).getOperatorName());
	}

	@Override
	public void accept(OperatorVisitor operatorVisitor) throws Exception {
		operatorVisitor.visit(this);
	}
	
	
	public String toString() {
		return "(SeqScan " + getSrcTable() + ", " + getFilterExpression()+")";
	}
	
	
	@Override
	public String getTreeRepresentation(boolean isRoot) throws Exception{
		
		if (isPruned() && (!isRoot)) {
			return "{PRUNED}";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		if (children.isEmpty() && getOperatorName().equals("scan")){
			// it is a scan
			sb.append(this.getSrcTable());
		} else if (children.isEmpty()) {
			sb.append(getOperatorName()).append('{').append(this.getSrcTable()).append('}');
		} else {
			// filter, project
			sb.append(getOperatorName()).append(children.get(0).getTreeRepresentation(false));
		}
		sb.append('}');
		
		return sb.toString();
	}

	@Override
	public String getFullyQualifiedName() {
		return getTable().getFullyQualifiedName();
	}
};