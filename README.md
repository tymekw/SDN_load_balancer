# SDN load balancer

Projekt składa się z load balancera zaimplementowanego w floodlight. 

## topologia sieci
[topologia.pdf](https://github.com/tymekw/SDN_load_balancer/files/10527294/topologia.pdf)

## kod
 - Skrypt `main.py` tworzący topologię oraz uruchamiający mininet. Dodawane są statyczne adresy MAC i IP hostów oraz serwerów oraz uzupełniane są tablice ARP.
 - Klasy tworzące load balancer w floodlight

## uruchomienie i testowanie
- `sudo python main.py`
- uruchomić przełącznik floodlight
- z dowolnego hosta wydać polecenie `wget 10.0.2.5:8000/<PLIK_DO_POBRNIA>`

## implementacja
Celem było zaimplementowanie Load balancera wykorzytujacego algorytm Weighted response time. Jednak ze wzdględu na trudność z otrzymaniem odpowiedzi między serwerami a kontrolerem ta część została zasymulowana za pomocą losowych czasów odpowiedzi. 
Hosty wysyłają zapytania na adres `10.0.2.5`, które są przekazywane do wybranego serwera (z najkrótszym ostatnim czasem odpowiedzi lub jeśli są takie same to najkrotszym średnim czasem 10 ostatnich odpowiedzi), w ten sposób ruch jest rozkładany pomiędzy serwery. Z punktu widzenia klientów istnieje tylko jednen serwer z danymi.


## Przykład działania
Wysyłane jest zapytanie na ten sam adres z 3 różnych hostów. Odpowiadają 3 różne najlepsze serwery.

![3xh1](https://user-images.githubusercontent.com/44200232/215272255-e5bea893-5dfa-4c92-be30-8589e277946c.png)
