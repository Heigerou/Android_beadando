# Company Status – Android alkalmazás

Ez az alkalmazás egy beadandó projekthez készült, két fő közös munkájával.  
A célunk az volt, hogy egy olyan appot készítsünk, ahol egy cég vagy csapat tagjai meg tudják nézni egymás státuszát, és frissíteni tudják, hogy éppen elérhetők-e vagy elfoglaltak.

Az app a MockAPI-t használja háttérként, így a szobák, felhasználók és státuszok online töltődnek be.

---

## 1. Mit tud az alkalmazás?

- Regisztráció és bejelentkezés  
- Szobák listázása  
- Szoba részleteinek megtekintése  
- Tagok aktuális státuszának mutatása  
- Saját státusz frissítése  
- „Utolsó aktivitás” megjelenítése  
- Kedvencek tárolása (Room DB)  
- Profil megtekintés és név módosítása  
- Beállítások: sötét mód, alap státusz, lista nézet, kijelentkezés  

---

## 2. Felhasznált technológiák (röviden, diákosan)

- Java és Android Studio  
- Navigation Component a képernyők közti váltáshoz  
- ViewModel + LiveData az adatokhoz  
- Retrofit az API hívásokhoz  
- Room Database a kedvencekhez és gyorsításhoz  
- Material Design a kinézethez  
- MockAPI mint online adatforrás  

---

## 3. Röviden, hogyan működik az app?

1. Induláskor az app megnézi, hogy a felhasználó be van-e jelentkezve.  
2. Ha igen, jön a főoldal, ahol a szobák láthatók.  
3. Szoba megnyitásakor betöltődnek a tagok és a státuszuk.  
4. A felhasználó tudja módosítani a saját státuszát.  
5. A kedvencek offline is megmaradnak Room DB-ben.  
6. A profilnál lehet változtatni a megjelenített nevet.  
7. A beállításokban több dolgot lehet állítani, például a sötét módot.  

---

## 4. Projekt felépítése

```
app/
 └── java/com.szokolaipecsy.companystatus/
     ├── auth/       # Login, Register, AuthGate
     ├── data/       # Room DB, DAO-k, Repository-k
     ├── network/    # Retrofit API-k és DTO-k
     ├── ui/         # Képernyők, Adapterek, ViewModel-ek
     └── util/       # Segédosztályok
```

---

## 5. A fejlesztők és a munkamegosztás

A projektet ketten készítettük, és több részen **együtt** dolgoztunk, különösen amikor a logikát és a felületet kellett összehangolni.

A munka elosztását így foglaljuk össze:

### **Szokolai János Dániel – Logika és működés**

Főbb feladatok:
- Bejelentkezés és regisztráció működtetése  
- API hívások, Retrofit beállítása  
- Főoldal és Room listázás működése  
- Kedvencek megoldása Room Database-szel  
- Room Detail képernyő logikája  
- Státusz frissítése és „last active” kezelés  
- Settings képernyő háttérlogikája  
- ViewModel és Repository réteg kialakítása  

### **Pécsy Szabolcs – Felhasználói felület és UX**

Főbb feladatok:
- XML layoutok elkészítése  
- Navigation Graph felépítése  
- Profil képernyő UI és név módosítás kezelése  
- Beállítások UI  
- Sötét mód és lista nézet váltása  
- RecyclerView adapterek a szobákhoz és tagokhoz  
- AuthGate képernyő vizuális elkészítése  

### **Közösen készítettük:**
- A teljes projekt struktúrát  
- A képernyők összehangolását (UI + logika összekötése)  
- A MockAPI szerkezetének kialakítását  
- Hibakeresést, tesztelést  
- A végleges működés kialakítását  

Ezért a commitok nem különítik el a kódot fejlesztőnként, viszont a fenti lista pontosan mutatja, ki melyik területért felelt.

---

## 6. Telepítés és futtatás

1. Klónozd a repót:
   ```
   git clone https://github.com/Heigerou/Android_beadando.git
   ```
2. Nyisd meg Android Studio-ban  
3. Várd meg a Gradle szinkront  
4. Futtatás emulátoron vagy telefonon  

Külön API kulcs nem kell.

---

## 7. Miért készült a projekt?

A beadandó célja volt gyakorolni:
- Android fejlesztési alapokat  
- REST API-val való kommunikációt  
- MVVM felépítést  
- Room adatbázis használatát  
- Többképernyős alkalmazás felépítését  

A projekt oktatási célra készült.

