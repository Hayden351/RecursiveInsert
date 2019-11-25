/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package recursiveinsert;

import definition.Graph;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Hayden Fields
 */
public class RecursiveInsert
{
    public static class ForeignKeyReference
    {
        String tableName;
        String tableId;
        String foreignTableName;
        String foreignTableId  ;
        
        public ForeignKeyReference(String tableName, String tableId, String foreignTableName, String foreignTableId)
        {
            this.tableName = tableName;
            this.tableId = tableId;
            this.foreignTableName = foreignTableName;
            this.foreignTableId = foreignTableId;
        }

        @Override
        public String toString ()
        {
            return String.format("%s(%s) -> %s(%s)", tableName, tableId, foreignTableName, foreignTableId);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.tableName);
            hash = 11 * hash + Objects.hashCode(this.tableId);
            hash = 11 * hash + Objects.hashCode(this.foreignTableName);
            hash = 11 * hash + Objects.hashCode(this.foreignTableId);
            return hash;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ForeignKeyReference other = (ForeignKeyReference) obj;
            if (!Objects.equals(this.tableName, other.tableName))
                return false;
            if (!Objects.equals(this.tableId, other.tableId))
                return false;
            if (!Objects.equals(this.foreignTableName, other.foreignTableName))
                return false;
            if (!Objects.equals(this.foreignTableId, other.foreignTableId))
                return false;
            return true;
        }
    }
    
    /*
    SELECT tc.table_schema,  tc.constraint_name,  tc.table_name, 
    kcu.column_name, 
    ccu.table_schema AS foreign_table_schema, ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name 
    FROM  information_schema.table_constraints tc 
          JOIN information_schema.key_column_usage kcu
               ON (tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema)
          JOIN information_schema.constraint_column_usage ccu 
               ON (ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema)
    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name like '%';
    */
    
    
    /*
    
    TODO: Sort of interesting that it didn't occur to me until I started working
          in multiple databases that hardcoding the meta tables for columns and
          tables is very generic. Pretty obvious after the fact. Maybe one way
          of resolving this is having a some way to say I want tables, columns
          foreign relationships, primary keys of specific tables and leave the
          implementation in a segregated portion of the code. So we have a
          value which is the relational database system, we give it a connection
          and it does the necessary queries
    
    TODO: as of 11-24-2019 18:58 there are two choices in front of me for
          the traverse method I have two task that must be completed
          the only question is the order
    task 1: make the schema more complicated to verify what I have currently works
    task 2: allow the user to specify values that will cloned slightly modified
            then inserted
    
    For thorough test we want 1-1, 1=*, *-1 and *-* relationships
    
    */
    public static Scanner in = new Scanner(System.in);
    
    public static void main(String[] args) throws SQLException
    {
        String foreignKeysSql =
            "SELECT tc.table_schema,  tc.constraint_name,  tc.table_name, " +
            "    kcu.column_name, " +
            "    ccu.table_schema AS foreign_table_schema, ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name " +
            "    FROM  information_schema.table_constraints tc " +
            "          JOIN information_schema.key_column_usage kcu " +
            "               ON (tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema) " +
            "          JOIN information_schema.constraint_column_usage ccu " +
            "               ON (ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema) " +
            "    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name like '%'; ";
        
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");
        
        PreparedStatement foreignKeyReferencesQuery = connection.prepareStatement(foreignKeysSql);
        
        ResultSet rs = foreignKeyReferencesQuery.executeQuery();
        List<ForeignKeyReference> foreignKeyReferences = new ArrayList<>();
        while (rs.next())
        {
            String tableName = rs.getString("table_name");
            String tableId = rs.getString("column_name");
            String foreignTableName = rs.getString("foreign_table_name");
            String foreignTableId = rs.getString("foreign_column_name");
//            System.out.printf("%s %s %s %s\n", tableName,tableId, foreignTableName, foreignTableId);
            
            foreignKeyReferences.add(new ForeignKeyReference(tableName, tableId, foreignTableName, foreignTableId));
        }
        
        foreignKeyReferences.forEach(System.out::println);
        
        
        
        String initialTable = "source_table";
        
        
        System.out.printf("Enter an id of table %s: ", initialTable);
        int id = Integer.parseInt(in.next());
        
        // TODO: what if the id is actually a string or some other type?
        //       can technically have any value as the primary key
        traverse("source_table", "source_table_id", id, foreignKeyReferences, new HashSet<>());
        
        connection.close();
    }
    
    public static void traverse(String table, String tableId, int id, List<ForeignKeyReference> foreignKeyReferences, Set<String> visited) throws SQLException
    {
        // TODO: If we havent decided to move to the current element we shouldn't
        /*
        inserts need to happen with
        referenced first then the table
        then the referencers
        
        but we want to ask the user whether they want to clone or not before even going to the referenced tables
        
        */
        
        // we just visited the current table
        visited.add(table);
        System.out.printf("Do you want to clone the corresponding rows in table %s?\n", table);
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");)
        {
            String sql = String.format("select * from %s where %s = %s", table, tableId, id);
            
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            
            //TODO: what if there are multiples?
            //      put it into a list and if 1 then a one to many else a one to one
            //      what if is a one to many but there is only one? does it make a difference?
            while (rs.next())
            {
                System.out.println("    "+resultSetToMap(rs));
            }
        }
        
        
        
        // traverse the tables that the current table references first
        // TODO: would it be better if I abstracted the references and referencers into some common code then just match up the parameters as needed?
        for (ForeignKeyReference reference : foreignKeyReferences)
            // look for the table that this table references
            if (reference.tableName.equals(table))
                if (!visited.contains(reference.foreignTableName))
                {
                    // we need to get the foreign key out of the current table
                    try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");)
                    {
                        String sql = String.format("select %s from %s where %s = %s", reference.tableId, table, tableId, id);
                        
                        ResultSet rs = conn.prepareStatement(sql).executeQuery();
                        
                        // TODO: can there be multiples? If yes than wat do?
                        if (rs.next())
                        {
                            // TODO: currently assume that the primary key is an integer which is true in this case but not in general
                            //       should eventually not do that
                            int foreignTableId = rs.getInt(1);
                            traverse(reference.foreignTableName, reference.foreignTableId, foreignTableId, foreignKeyReferences, visited);
                        }
                    }
                }
        // evaluate the current table
        System.out.println(table);
        
        
        // evaluate tables that reference the current table
        for (ForeignKeyReference reference : foreignKeyReferences)
            // look for the tables that are referencing this table
            if (reference.foreignTableName.equals(table))
                if (!visited.contains(reference.tableName))
                {
                    try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:54321/postgres?user=DarkFight753");)
                    {
//                        // get the column that is a primary key on the referencing table
//                        String getPrimaryKeyOnReferencingTable =
//                            "SELECT kcu.column_name " +
//                            "    FROM information_schema.table_constraints tc " +
//                            "         join information_schema.key_column_usage kcu " +
//                            "         on (tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema) " +
//                            "    WHERE 'referencing_table' = tc.table_name " +
//                            "      and constraint_type = 'PRIMARY KEY' ";
//                        ResultSet primaryKeyResult = conn.prepareStatement(getPrimaryKeyOnReferencingTable).executeQuery();
//                        
//                        String primaryId;
//                        
//                        if (primaryKeyResult.next())
//                        {
//                             primaryId = primaryKeyResult.getString("column_name");
//                        }
//                        else throw new IllegalArgumentException("Either the query has some error or we have a table without a primary key. Which is totally possible but just a little bit weird.");
//                        
//                        
//                        // TODO: we need to find the primary key of the table to select
//                        //       but wait, what if there is more than one? Did I accidently resolve this todo?
//                        // TODO: what if there are multiple columns that define the primary key
//                        String sql = String.format("select %s as primaryId from %s where %s = %s", primaryId, reference.tableName, reference.tableId, id);
//                        System.out.println(sql);
//                        ResultSet rs = conn.prepareStatement(sql).executeQuery();
//                        
//                        // TODO: can there be multiples? If yes than wat do?
//                        if (rs.next())
//                        {
//                            // TODO: all that work to get this id while interesting turned out to be a waste of time
//                            //       I want to capture all the knowledge that went into getting this value but need to
//                            //       remove it from this code because this code is sufficiently complex as is
//                            int foreignTableId = rs.getInt(1);
//                        }
                        
                        traverse(reference.tableName, reference.tableId, id, foreignKeyReferences, visited);
                    }
                }
    }
    
    public static Map<String, String> resultSetToMap(ResultSet rs) throws SQLException
    {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
            result.put(rs.getMetaData().getColumnName(i), rs.getString(i));
        return result;
    }
}

