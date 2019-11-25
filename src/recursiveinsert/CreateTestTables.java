package recursiveinsert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Hayden Fields
 */
public class CreateTestTables
{
    // we want a source table
    // a referencer and a referenced table
    
    /*
    create
        table referenced_table
        (
            referenced_table_id serial primary key,
            data varchar(32)
        );
    create
        table source_table
        (
            source_table_id serial primary key,
            referenced_table_id integer references referenced_table(referenced_table_id),
            data varchar(32) 
        );
    create
        table referencing_table
        (
            referencing_table_id serial primary key,
            source_table_id integer references source_table(source_table_id),
            data varchar(32) 
        );
    
    INSERT INTO REFERENCED_TABLE (data) VALUES ('data1');
    INSERT INTO REFERENCED_TABLE (data) VALUES ('data2');
    INSERT INTO REFERENCED_TABLE (data) VALUES ('data3');
    INSERT INTO REFERENCED_TABLE (data) VALUES ('data4');

    INSERT INTO SOURCE_TABLE (referenced_table_id,data) VALUES (4,'asdf');
    INSERT INTO SOURCE_TABLE (referenced_table_id,data) VALUES (2,'sdfg');
    INSERT INTO SOURCE_TABLE (referenced_table_id,data) VALUES (4,'sdfg');

    insert into referencing_table(data, source_table_id) values('data1', 3);
    insert into referencing_table(data, source_table_id) values('data2', 1);
    */
    public static void main (String[] args) throws IOException, SQLException
    {
        executeScript("src/recursiveinsert/input");
    }
    // lazy script to execute an .sql file terminated by ;
    // fail at the very least in the case when there is a ; in a string
    public static void executeScript(String filePath) throws IOException, SQLException
    {
        try (BufferedReader in = new BufferedReader(new FileReader(filePath));
            Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");)
        {
            StringBuilder result = new StringBuilder();
            int ch;
            while ((ch = in.read()) != -1)
            {
                if (';' == ch) 
                { System.out.printf("{####{%s}####}\n", result.toString().trim()); connection.prepareStatement(result.toString()).execute(); result = new StringBuilder(); }
                else result.append((char)ch);
            }
        }
    }
}
