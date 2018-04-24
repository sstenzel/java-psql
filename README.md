# JAVA + PSQL

[ Projekt na zajęcia, 03/2018 ]

- Linux, terminal

Treść zadania:
1. Program na odczytać wiersz nagłówkowy z pliku CSV i na jego podstawie utworzyć tabelę w bazie danych (PostgreSQL):
a) nazwa tabeli ma być taka sama jak nazwa pliku CSV (bez rozszerzenia), jeśli tabela istnieje, to powinna zostać usunięta i na nowo utworzona,
b) tabela powinna posiadać klucz główny o nazwie "id" z autoinkrementacją,
c) nazwy pozostałych kolumn tabeli powinny być odczytane z nagłówka pliku CSV, kolumn w tabeli (nie licząc klucza głównego) powinno być tyle ile jest pól w nagłówku pliku CSV, przyjmijmy, że dodawane kolumny będą typu VARCHAR(20),

2. Program powinien dane odczytane z pliku CSV wpisać do utworzonej tabeli:
a) jeżeli dane się nie mieszczą w kolumnie, to program powinien wykonać polecenie alter table ... i zmienić typ kolumny np. na  VARCHAR(wieksza_dlugosc_danych_ktore_chcemy_wpisac),
b) jeżeli danych (pól w wierszu pliku CSV) jest za dużo w stosunku do nagłówka, to dane nadmiarowe powinny zostać zignorowane,
c) jeżeli danych (pól w wierszu pliku CSV) jest za mało w stosunku do nagłówka, to jako wartości brakujących kolumn powinno się przyjąć NULL, 

3. Program ma reagować na wywołanie: ./program nazwa.csv

4. Na życzenia użytkownika program powinien stworzyć plik w standardzie HTML5 z zawartością utworzonej tabeli (dane mają być pobrane z bazy danych nie z pliku!).

