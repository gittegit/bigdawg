/**
 * 
 */
package istc.bigdawg.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.scidb.jdbc.IResultSetWrapper;

import istc.bigdawg.scidb.SciDBConnectionInfo;
import istc.bigdawg.scidb.SciDBHandler;

/**
 * @author Adam Dziedzic
 * 
 *         Feb 24, 2016 7:36:20 PM
 */
public class Utils {
	
	/**
	 * Show meta data about the target array.
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	private static void showMetaData(ResultSet resultSet) throws SQLException {
		ResultSetMetaData meta = resultSet.getMetaData();
		System.out.println("Source array name: " + meta.getTableName(0));
		System.out.println(meta.getColumnCount() + " columns:");

		IResultSetWrapper resWrapper = resultSet
				.unwrap(IResultSetWrapper.class);
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			System.out.println(meta.getColumnName(i) + " - "
					+ meta.getColumnTypeName(i) + " - is attribute:"
					+ resWrapper.isColumnAttribute(i));
		}
	}

	public static long getNumberOfCellsSciDB(SciDBConnectionInfo conTo, String array) throws SQLException {
		Connection con = SciDBHandler.getConnection(conTo);
		Statement query = con.createStatement();
		ResultSet resultSet = query.executeQuery("select * from " + array);

		showMetaData(resultSet);

		long numberOfCellsSciDB = 0;
		while (!resultSet.isAfterLast()) {
			// System.out.println(resultSet.getString(3));
			// System.out.println(resultSet.getInt(1));
			++numberOfCellsSciDB;
			resultSet.next();
		}
		resultSet.close();
		query.close();
		con.close();
		return numberOfCellsSciDB;
	}

}