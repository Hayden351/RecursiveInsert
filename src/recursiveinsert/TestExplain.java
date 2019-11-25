package recursiveinsert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Hayden Fields
 */
public class TestExplain
{
    /*
    Is this a rdbms specific implementation? It works through jdbc but is
    it apart of the sql standard?
    
    */
    public static void main (String[] args) throws SQLException
    {
        String sql = "explain analyze verbose SELECT *\n" +
        "    FROM referencing_table rt";
        
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");
        
        ResultSet rs = connection.prepareStatement(sql).executeQuery();
        
        while(rs.next())
        {
            System.out.println(RecursiveInsert.resultSetToMap(rs));
        }
        
        connection.close();
    }
}
