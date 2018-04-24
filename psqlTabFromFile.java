import java.sql.*;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Scanner;

/*
 *  program pobiera dane z pliku .csv, na ich podstawie tworzy w bazie danych tabele,
 *  dodaje rekordy oraz opcjonalnie wypisuje je w konsoli lub do pliku .html
 * 
 *  kompilacja:		javac nazwa.java
 *  uruchomienie:	java -cp .:postgresql-42.2.2.jar nazwa plik.csv
 *
 *
 *  SPIS TRESCI:
 *  	POBRANIE DANYCH DO LOGOWANIA I POLACZENIE Z BAZA DANYCH
 *  	POBRANIE NAZWY TABELI 
 *  	UTWORZENIE TABELI Z PLIKU
 *  	DODANIE REKORDOW Z PLIKU
 *  	AKTUALIZACJA ROZMIARU KOLUMNY
 *  	DRUKOWANIE TABELI
 *  	TWORZENIE PLIKU HTML
 *  	WYPISANIE DANYCH Z TABELI DO PLIKU HTML
 *  	OPCJE PROGRAMU
 *  	MAIN
 *
 */

public class psqlTabFromFile {

	Connection c = null;
	String csvPath = null;
	String tableName = null;
	String [] columnNames = null;
	int tableSize;

	psqlTabFromFile (String csvPath){
		connectWithDatabase();
		this.csvPath = csvPath;
		this.tableName = getTableName();
		createTableFromFile();
		programOptions();
	}

	
// --------------------------- POBRANIE DANYCH DO LOGOWANIA I POLACZENIE Z BAZA DANYCH ----------------------------

	void connectWithDatabase () {
		try { 
			Scanner scanner = new Scanner(System.in);
	   		System.out.print("Podaj login: ");
	   		String login = scanner.nextLine();
	   		System.out.print("Podaj nazwe bazy: ");
	   		String database = scanner.nextLine();		

			Console console;
 			char[] passwd;
			 if ((console = System.console()) != null &&
		  	   	(passwd = console.readPassword("[%s]", "Podaj haslo:")) != null) {
		  	   	String haslo = new String(passwd);
		     
			Class.forName("org.postgresql.Driver");
			this.c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + database, login, haslo);
			}
			
			c.setAutoCommit(false);   // potem  c.commit();
			System.out.println("\n** Poprawnie polaczono sie z baza danych **\n");
			
	   	//	scanner.close();
	  	} catch (Exception e) {
		    	e.printStackTrace();
		     	System.err.println(e.getClass().getName()+": "+e.getMessage());
		     	System.exit(0); }
	}


// -------------------------------------------- POBRANIE NAZWY TABELI --------------------------------------------------------
   
	String getTableName () {	
		
		if (new File(this.csvPath).exists() != true)  {
			System.out.println("\n[ Podany plik z danymi nie istnieje ]\n");
			closeDatabase();
		}
		if (this.csvPath.endsWith(".csv") != true) {
			System.out.println("\n[ Niepoprawne rozszerzenie pliku z danymi  \t\t\t\t]"
								+ "\n[ Poprawne uruchomienie: java -cp .:postgresql-42.2.2.jar psql plik.csv ]\n");
			closeDatabase();
		}
		int dlugosc = this.csvPath.length();
		
		if (this.csvPath.lastIndexOf('/')== -1)
			return this.csvPath.substring(0, dlugosc-4);
		else
			return this.csvPath.substring(this.csvPath.lastIndexOf('/')+1, dlugosc-4);
	}

// ----------------------------------------- UTWORZENIE TABELI Z PLIKU -----------------------------------------------------

 	 void createTableFromFile (){
	    try { 
	 	 try{
	 	 	Statement st = this.c.createStatement();
	 	 	
			Scanner scanner = new Scanner(new File(this.csvPath));
			if (scanner.hasNext() == false) {
				System.out.println("[ Plik jest pusty ]");
				closeDatabase();
			}
			this.columnNames = scanner.nextLine().split(";");
			this.tableSize = this.columnNames.length;
			String query = null;
			
			st.executeUpdate ("DROP TABLE IF EXISTS " + this.tableName + ";");
			
			query = "CREATE TABLE " +this.tableName+" ( id SERIAL PRIMARY KEY" ;
			for (int i = 0; i<this.tableSize; i++) {
				query += 	", " + this.columnNames[i] + " VARCHAR(20) " ;
			}
			query += ");" ;
		
			st.executeUpdate (query);				
			System.out.println("** Utworzono tabele **\n");

			insertRowsFromFile (scanner, st);
			
			scanner.close();
			st.close();
		}catch (FileNotFoundException f) {System.out.println("Nie znaleziono pliku");}
	  } catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);  }
   }

// -------------------------------------------- DODANIE REKORDOW Z PLIKU ----------------------------------------------------

	void insertRowsFromFile (Scanner scanner, Statement st) {
		try{
			String query = null;
			String [] row = null;
			while(scanner.hasNext()){
				row = scanner.nextLine().split(";");
				query = " INSERT INTO " +this.tableName+" (" ;
				for (int i = 0; i<this.tableSize; i++) {
					if( i != this.tableSize-1)
						query += 	this.columnNames[i]  + ", ";
					else
						query += 	this.columnNames[i]  + ") VALUES ( ";
				}

				int i = 0;
				for (; ( i<row.length && i<this.tableSize-1) ; i++) {
					row[i] = checkSpelling(row[i]);
					if( row[i].length() >20 )
						columnSizeUpdate(this.columnNames[i], row[i].length() ); 
					query += 	"'" + row[i]  + "', ";
				}
				if (row.length >= this.tableSize ) {
					row[i] = checkSpelling(row[i]);
					if( row[i].length() >20 )
						columnSizeUpdate(this.columnNames[i], row[i].length() ); 
					query += 	"'" + row[i]  + "') ;";
				}
				else {
					for	(;  i<this.tableSize-1; i++) 
						query += 	"null , ";
					query +=  "null ) ; ";
				}

				st.executeUpdate(query);
			}
			if (row != null)
				System.out.println("** Dodano rekordy **");
			else
				System.out.println("** Brak rekordow do dodania **");
			
		} catch ( Exception e ) {
		System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		System.exit(0);  }
	}
   
// ----------------------------------------- SPRAWDZENIE POPRAWNOSCI ------------------------------------------------
   
   	String checkSpelling(String string) {
   		string = string.trim();
   		string = string.replace("\'", "\'\'");
   		return string;
   	}
   
// ----------------------------------------- AKTUALIZACJA ROZMIARU KOLUMNY -----------------------------------------
	
	void columnSizeUpdate(String columnName, int neededSize){
		try{
			Statement st = this.c.createStatement();
			ResultSet rs = st.executeQuery("select character_maximum_length from information_schema.columns "
						+" where table_name = '" +this.tableName+ "' AND column_name = '" +columnName+ "';");
			if (rs.next()) {
				if (rs.getInt(1) < neededSize ) {
					st.executeUpdate("ALTER TABLE " +this.tableName+ " ALTER COLUMN " +columnName+ 
									" TYPE VARCHAR ( "+neededSize+" );");
					System.out.println("** Zaktualizowano kolumne: " +columnName+ 
										", nowy rozmiar: " 	+neededSize+"   **\n"); 
				}
			}
			rs.close();
			st.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);  }
	}
	

// --------------------------------------------- ( TESTY ZAPYTAN ) ---------------------------------------------------------
	
	void executeQuery(Scanner scanner) {
		try {
			Statement st = this.c.createStatement();
			String query = scanner.nextLine();
			ResultSet rs =  st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			
			while ( rs.next() ) {
				for (int i = 1; i<=columnsNumber; i++) 
					System.out.print(rs.getString(i)  + "; ");
				System.out.println();
			}
			
			rs.close();
			st.close();
		 } catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		//	System.exit(0);  }
		}
   }
	
// ------------------------------------------------ DRUKOWANIE TABELI ---------------------------------------------------

	void printTable(){
		try {
			System.out.print("\nid;");
			for (int i = 1; i<this.tableSize; i++)
					System.out.print(this.columnNames[i] + ";");
			System.out.println();
			
			Statement st = this.c.createStatement();
			ResultSet rs = st.executeQuery( "SELECT * FROM " + this.tableName  + ";" );
			while ( rs.next() ) {
				for (int i = 1; i<=this.tableSize+1; i++)
					System.out.print(rs.getString(i) + ";");
				System.out.println();
			}
			rs.close();
			st.close();
		} catch ( Exception e ) {
		    System.err.println( e.getClass().getName() + ": " + e.getMessage() ); }
	}

// ---------------------------------------------- TWORZENIE PLIKU HTML -------------------------------------------------------

	String createHtmlFile(Scanner scanner){
		String htmlPath;
		File htmlFile;
		char ready = 'y';
	   	do{
			System.out.print("\n[ Podaj nazwe pliku w ktorym chcesz zapisac dane ]: ");
			htmlPath = scanner.nextLine();

			if (htmlPath.endsWith(".html") != true){
				htmlPath += ".html";
			}
			htmlFile = new File(htmlPath);

			if (htmlFile .exists() == true){
				System.out.println("\n[ Podany plik html juz istnieje. Czy nadpisac? ]");
				System.out.print("[ y/n ]: ");
				ready = scanner.nextLine().charAt(0);
			} 
		} while(ready != 'y'); 
		return htmlPath;
	}

// ---------------------------------- WYPISANIE DANYCH Z TABELI DO PLIKU HTML --------------------------------------------

	void printTableToHTML(Scanner scanner){
		String htmlPath = createHtmlFile(scanner);
		String s;

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(htmlPath))) {

		s = "<!DOCTYPE html>\n<html lang=\"pl\">\n<head>\n "+
			" <title>"+ tableName +"</title>\n "+
			"<meta charset=\"UTF-8\">\n" +
			"<style> \n"+
 			" body {color: #333333; background-color: #99ccff;}  \n" +
			" table, th, td { border: 1px solid black;  border-radius: 5px; " +
			" padding-left: 5px; padding-right: 5px;} \n"+
			" table tr:nth-child(even) { background-color: #f2f2f2;} \n" +
			" table tr:nth-child(odd) {background-color: #fff;}\n" +
			"</style>\n</head>\n<body>\n "+
			"<table style=\"width:100%\"><tr><th>id</th>\n " ;
		writer.write(s, 0, s.length());

		for(int i = 0; i<this.tableSize; i++) { 
			s = " <th>" + this.columnNames[i] + "</th>\n";
			writer.write(s, 0, s.length());
		}
		writer.write("</tr>\n", 0, 6);
		try {
			Statement st = this.c.createStatement();
			ResultSet rs = st.executeQuery( "SELECT * FROM " + this.tableName  + ";" );

			while ( rs.next() ) {		
				writer.write("<tr>", 0, 4);
				for (int i = 1; i<=this.tableSize+1; i++) {
					s = " <td>" +rs.getString(i) + "</td>\n";
					writer.write(s, 0, s.length());
				}
				writer.write("</tr>\n", 0, 6);
			}
			s = "</table></body>\n</html>\n";
			writer.write(s, 0, s.length());
			
			System.out.println("\n** Utworzono plik: " +htmlPath+"  **");
			
			rs.close();
			st.close();
		} catch ( Exception e ) {
		    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		    System.exit(0); }
		} catch (IOException x) { 
		  System.err.format("IOException: %s%n", x);}
	}
	
	
// ------------------------------------------------- OPCJE PROGRAMU -----------------------------------------------------------

	void programOptions() {
	   	int exit = 0;
	   	Scanner scanner = new Scanner(System.in);
	   	do{
		   	System.out.println("\nWpisz: \n\tprint\t- wypisanie zawartosci tabeli w konsoli" 
		   							+ "\n\thtml\t- utworzenie pliku HTML z zawartoscia tabeli"
		   							+"\n\texit\t- wyjscie");
   			switch (scanner.nextLine()) {
   				case "print" : printTable(); break;
   				case "html" : printTableToHTML(scanner); break;
   				case "query" : executeQuery(scanner); break; 	// niewidoczne, do testow
   				case "exit" : exit = 1; break;
   			}
		} while (exit == 0);
		scanner.close();
		closeDatabase();
		System.exit(0);
	}
	
	void closeDatabase () {
		try{
			this.c.close();
			System.out.println("\n** Poprawnie zamknieto baze danych **\n");
			System.exit(0);
	  	} catch (Exception e) { e.printStackTrace();
	     	System.err.println(e.getClass().getName()+": "+e.getMessage());
	     	System.exit(0); }
	}

// ----------------------------------------------------------- MAIN ----------------------------------------------------------------------

	public static void main(String args[]) {
   		String csvPath = null;
   		if ( args[0] != null) {
   			csvPath = args[0];
   		}
   		new psqlTabFromFile(csvPath);	
   	}
  }
