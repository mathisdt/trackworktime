# Track Work Time

Tato aplikace dokáže snadno sledovat váš pracovní čas! Sledování času můžete automatizovat pomocí funkcí geofencingu (viz níže). Můžete také **každý zaznamenaný interval** kategorizovat podle předdefinovaného klienta/úkolu a volného textu. Seznam klientů/úkolů lze samozřejmě upravovat podle vašich potřeb a aplikace má widget pro domovskou obrazovku.

Navíc, pokud si přejete, je postaráno o **váš flexibilní úvazek** : vždy vidíte, kolik jste odpracovali. 
Můžete také sledovat, kolik pracovní doby vám ještě zbývá pro dnešek nebo aktuální týden (pomocí **oznámení** které můžete povolit).

Aplikace vám umožňuje snadno upravovat plánovanou pracovní dobu – stačí klepnout na datum, které chcete upravit, v hlavní stůl.

Můžete zadat **zeměpisné souřadnice** nebo **název sítě Wi-Fi** vašeho pracoviště a aplikace to dokáže... **automaticky vás zaznamená**, když jste v práci.
To se děje **bez použití GPS**, takže se vaše baterie nevybíjí. (V práci nemusíte být připojeni k síti Wi-Fi, stačí, aby byla **viditelná**.)

Nechcete otevírat aplikaci kvůli zaznamenávání příchodů a odchodů? Žádný problém – existují alespoň tři způsoby, jak to udělat: 
přidejte **widget** na domovskou obrazovku, použijte **zkratky spouštěče** (dlouze stiskněte ikonu aplikace) nebo přidejte na panel 
novou **položku rychlého nastavení** klepnutím na tužku níže a přetažením položky „Sledovat pracovní dobu“, které pak 
mohou přepínat váš stav odpočtu.

Pokud dáváte přednost sledování pohybu jinými aplikacemi, jako je LlamaLab Automate nebo Tasker, nevadí – TWT dokáže být spouštěn z jiných 
aplikací a pouze vést evidenci vaší pracovní doby. V tomto případě si musíte vytvořit intent s akcí *org.zephyrsoft.trackworktime.ClockIn* 
nebo *org.zephyrsoft.trackworktime.ClockOut*.  Při použití ClockIn můžete také nastavit parametry task=... a text=... v části „extra“, 
aby vaše události byly smysluplnější. Zde je několik snímků obrazovky, které ukazují, jak to lze provést v Automate:
[Flow overview](https://zephyrsoft.org/images/automate-1.png),
["Broadcast send" block arguments](https://zephyrsoft.org/images/automate-2.png).
Pomocí akce *org.zephyrsoft.trackworktime.StatusRequest* můžete také získat aktuální stav TWT: Je uživatel přihlášen, a pokud ano, s 
jakým úkolem a kolik času zbývá na dnes? Zde je návod, jak to můžete použít v aplikaci Automate:
[Flow overview](https://zephyrsoft.org/images/automate-3.png),
["Send broadcast" settings top](https://zephyrsoft.org/images/automate-4.png),
["Send broadcast" settings bottom](https://zephyrsoft.org/images/automate-5.png),
["Receive broadcast" settings top](https://zephyrsoft.org/images/automate-6.png),
["Receive broadcast" settings bottom](https://zephyrsoft.org/images/automate-7.png),
["Dialog message" settings](https://zephyrsoft.org/images/automate-8.png),
[resulting message](https://zephyrsoft.org/images/automate-9.png).

Je to také možné i naopak: TWT generuje broadcast intenty při vytváření/aktualizaci/smazání události. 
Automatizační aplikace mohou naslouchat akcím *org.zephyrsoft.trackworktime.event.Created* 
*org.zephyrsoft.trackworktime.event.Updated* a *org.zephyrsoft.trackworktime.event.Deleted* 

K dispozici jsou následující doplňky:
id (číslo jednoznačně identifikující událost),
date (datum události ve formátu RRRR-MM-DD), 
time (čas události ve formátu HH:MM:SS), 
timezone_offset (posun ve standardním formátu, např. +02:00), 
timezone_offset_minutes (posun v minutách, např. 120), 
type_id (číslo jednoznačně identifikující typ události, 0=odhlášení / 1=příchod),
type (název typu události, CLOCK_IN nebo CLOCK_OUT), 
task_id (číslo jednoznačně identifikující úlohu události, není k dispozici u událostí odhlášení), 
task (název úkolu události, není k dispozici u událostí odchodu), 
comment (k dispozici pouze v případě, že jej uživatel poskytl), 
source (kde byla událost původně generována, možné hodnoty jsou 
MAIN_SCREEN_BUTTON, EVENT_LIST, QUICK_SETTINGS, LAUNCHER_SHORTCUT, MULTI_INSERT, AUTO_PAUSE, LOCATION, WIFI, RECEIVED_ITENT
[zahrnuje jak externě vytvořená vysílání, tak akce z vlastního widgetu a oznámení TWT]).

Několik snímků obrazovky, abyste si to mohli prohlédnout v akci v Automate:
[Flow overview](https://zephyrsoft.org/images/automate-receive-1.png),
["Receive Broadcast" settings top](https://zephyrsoft.org/images/automate-receive-2.png),
["Receive Broadcast" settings bottom](https://zephyrsoft.org/images/automate-receive-3.png),
["Dialog Message" settings](https://zephyrsoft.org/images/automate-receive-4.png),
[Result 1](https://zephyrsoft.org/images/automate-receive-5.png),
[Result 2](https://zephyrsoft.org/images/automate-receive-6.png).

Pokud máte chytré hodinky **Pebble** , můžete si pro ně povolit oznámení aplikace vás upozorní na 
události spojené s příchodem a odchodem, což je obzvlášť užitečné, pokud chcete být informováni o 
automatickém sledování času prostřednictvím polohy nebo WiFi.

Pro **ostatní chytré hodinky** si můžete povolit některé možnosti týkající se oznámení, které vám 
pomohou. Po zaškrtnutí volby **Oznámení povoleny*** se zobrazí oznámení na telefonu, ale pouze pokud 
jste přihlášeni (pracujete). Chcete-li mít oznámení na telefonu zobrazeno trvale i pokud nejste přihlášeni, 
zaškrtněte **Vždy zobrazovat oznámení** a budete mít přehled zapnutý neustále. 
Pokud chcete oznámení zobrazovat i na hodinkách, musíte zatrhnout volbu **Oznámení jako dočasná**, protože jinak je Android nebude na 
ostatní zařízení synchronizovat. Jedná se o určitý kompromis, protože s touto možností nebude oznámení 
stále připnuto na začátek seznamu oznámení a také se dá neúmyslně zavřít (ale po minutě se znovu zobrazí). Kromě 
toho můžete zaškrtnou **Všechna oznámení tichá** abys vás rozptylovaly (toto se projeví na hodinkách i v telefonu).

Aplikace vám konečně může generovat **reporty** . Report o nezpracovaných událostech je ideální, pokud chcete 
exportovat data někde jinde, zatímco roční/měsíční/týdenní přehledy jsou skvělé, pro sledování práce na jednotlivých úkolech.

Důležitá poznámka: **Tato aplikace rozhodně nepoužije vaše osobní údaje k ničemu, co nechcete!** Pokud aplikace spadne, nabídne vám odeslání 
informací o okolnostech pádu vývojáři. (a to pouze v případě, že s tím souhlasíte, budete pokaždé dotázáni). Aplikace NEOBSAHUJE 
sledované časy ani místa v hlášení o chybě, ale je připojen obecný soubor protokolu, který může potenciálně obsahovat osobní údaje - 
Pokud ano, bude to přísně důvěrné a použito pouze k identifikaci problému. 

[<img src="https://zephyrsoft.org/wp-content/uploads/get-it-on-fdroid.png"
     alt="Get it on F-Droid"
     height="54px">](https://f-droid.org/packages/org.zephyrsoft.trackworktime/)
[<img src="https://zephyrsoft.org/wp-content/uploads/get-it-on-google-play.png"
     alt="Get it on Google Play"
     height="54px">](https://play.google.com/store/apps/details?id=org.zephyrsoft.trackworktime)

Minulý vývoj můžete sledovat v [historii verzí](https://zephyrsoft.org/trackworktime/history).  
  
**Toto je open source projekt , takže pokud se vám něco nelíbí, můžete se ozvat a**
[nahlásit problém](https://codeberg.org/mathisdt/trackworktime/issues) nebo dokonce věci sami opravit a vytvořit pull request.
Prosím, nepokoušejte se se mnou komunikovat prostřednictvím recenzí, to nefunguje
oběma směry. Vždycky mi můžete [napsat email](https://zephyrsoft.org/contact-about-me) a já se podívám, co se dá dělat.




## License

Tento projekt je licencován pod GPL v3. Pokud odešlete nebo přispějete změnami, budou tyto automaticky licencovány. také pod licencí GPL v3. 
Pokud si to nepřejete, prosím, neodesílejte příspěvek (např. pull request)! 