"""Ordnet die Woerter des Johannesevangeliums Sachgebieten zu (Thematic Greek Lexicon und Teil 2 des Complete Greek Lexicon).

Jedes Wort steht in genau einem Gebiet. Zuerst zaehlen die Listen unten (nach Umschrift, wie in gen_lexicon.py), was
darin nicht vorkommt, ordnet sich nach der Wortart ein (Verben: "Everyday Actions", Adjektive: "Qualities", Namen: "Names
and Places", Funktionswoerter: "Little Words"). Reststuecke melden sich beim Aufruf von check().
"""

THEMES = [
    # (kurzname, Titel, Einleitung im Ton von John Koine, Woerter nach Umschrift)
    ("divine", "God and Heaven",
     "Let's start at the top. Greek texts usually do.",
     "theos patēr pneuma kurios Iēsous Christos Messias uhios logos doxa doxazō hagios hagiazō hagnizō ouranos epouranios "
     "angelos Satanas diabolos daimonion daimonizomai paraklētos theosebēs aiōn aiōnios amēn hōsanna monogenēs amnos arnion "
     "eucharisteō proskuneō proskunētēs eulogeō makarios sōtēr sōtēria sōzō charis latreia"),
    ("light", "Light, Darkness and Truth",
     "Writers love a contrast. Light and dark, true and false. Subtle they are not.",
     "phōs phōtizō skotos skotia alētheia alēthēs alēthinos alēthōs pseudos pseustēs phainō luchnos phanos lampas dolos kruptos "
     "kruptō phanerōs phaneroō apokaluptō phanos"),
    ("life", "Life, Death and Healing",
     "Birth, illness, death and, in this particular book, a surprising amount of getting up again afterwards.",
     "zaō zōē zōopoieō thanatos thnēskō apothnēskō teleutaō nekros apokteinō iaomai therapeuō hugiēs astheneia astheneō puretos "
     "tuphlos tuphloō chōlos xēros xērainō egeirō anistēmi anastasis diegeirō exupnizō koimaō koimēsis hupnos gennaō genetē "
     "tiktō mnēmeion entaphiazō entaphiasmos keiria othonion soudarion arōma smurna muron nardos aloē sunetheia"),
    ("mind", "Knowing, Seeing and Believing",
     "What goes on inside people's heads. Mostly confusion, if the disciples are anything to go by.",
     "ginōskō oida gnōrizō gnōstos noeō manthanō didaskō didaskalos didachē didaktos mimnēskō mnēmoneuō hupomimnēskō dokeō "
     "logizomai oiomai sumbouleuō bouleuō boulomai thelō thelēma epithumia zēlos zēteō zētēsis ereunaō punthanomai eperōtaō "
     "erōtaō aiteō prosaiteō homologeō arneomai pisteuō pistos apistos pistikos apeitheō elpizō agapaō agapē phileō philos "
     "miseō exetazō lagchanō kardia blepō horaō eidō theaomai theōreō anablepō emblepō optanomai"),
    ("feelings", "Feelings",
     "Joy, fear, grief and a fair bit of anger. Greek people had feelings too. Who knew.",
     "chairō chara phobeō phobos lupeō lupē thlipsis tarassō embrimaomai dakruō klaiō thrēneō orgē cholaō parrēsia tolmaō "
     "deiliaō aporeō thaumazō thaumastos tharseō agalliaō gonguzō gongusmos mainomai"),
    ("speech", "Speaking, Hearing and Writing",
     "Words about words. Yes, a lexicon has a section on speaking. It's called being self-aware.",
     "legō erō eipon laleō lalia phēmi apokrinō apokrisis boaō krazō kraugazō phōneō phōnē kaleō akouō akoē angellō anangellō "
     "apangellō exēgeomai mēnuō deiknuō sēmainō graphō gramma graphē grammateus biblion anaginōskō titlos rhēma paroimia "
     "hermēneuō methermēneuō Hebraisti Hellēnisti Rhōmaisti onoma loidoreō blasphēmeō blasphēmia prophēteuō prophētēs elegchō "
     "antilegō katēgoreō katēgoria martureō marturia nai ouchi akoē"),
    ("travel", "Going, Coming and Moving",
     "Nobody in an ancient text stays put for long. Some of them clock up more miles than a postal worker.",
     "erchomai eiserchomai exerchomai aperchomai anerchomai dierchomai proserchomai sunerchomai suneiserchomai katabainō "
     "anabainō embainō apobainō metabainō agō eisagō exagō paragō hupagō poreuō ekporeuō peripateō trechō protrechō pheugō hēkō "
     "paraginomai hupantaō hupantēsis anachōreō ekneuō hodos hodoiporia hodēgeō pempō apostellō apostolos pherō bastazō helkuō "
     "surō elaunō epairō hupsoō kuptō parakuptō anakuptō piptō hallomai ballō ekballō deuro deute prosphero prospherō "
     "paragō diatribō peran"),
    ("posture", "Staying, Sitting and Standing",
     "For when the walking stops. Which, admittedly, isn't often.",
     "menō epimenō kathēmai kathezomai kathizō anakeimai anapiptō katakeimai keimai histēmi paristēmi periistēmi skēnoō monē "
     "klinō epikeimai"),
    ("family", "Family and Everyday People",
     "Mothers, brothers, neighbours and one extremely unfortunate bridegroom with a wine problem.",
     "anthrōpos anēr gunē paidion paidarion pais teknon teknion mētēr adelphos adelphē thugatēr goneus orphanos pentheros "
     "sungenēs geitōn numphē numphios gamos laos ochlos plēthos geōrgos poimēn kleptēs lēstēs ptōchos prosaitēs gerōn "
     "anthrōpoktonos kēpouros hamartōlos thurōros"),
    ("rulers", "Rulers, Servants and Officials",
     "The people who give orders, and the people who regret receiving them.",
     "basileus basilikos basileia archōn archiereus hiereus Leuitēs Pharisaios presbuteros mathētēs summathētēs doulos "
     "douleuō diakonos diakoneō hupēretēs paidiskē misthōtos misthos stratiōtēs chiliarchos architriklinos kermatistēs "
     "kollubistēs exousia sunedrion archō apostolos Kaisar"),
    ("body", "The Body",
     "Head, shoulders, knees and, mercifully for the translator, only a few toes.",
     "sōma sarx kephalē ophthalmos ōtarion ōtion stoma cheir daktulos pous skelos pterna brachiōn stēthos kolpos koilia thrix "
     "osteon ahima opsis ptusma ptuō psuchē pleura"),
    ("nature", "Water, Earth and Weather",
     "Rivers, wind, stone and fire. The outdoors, for those who still go there.",
     "hudōr thalassa potamos pēgē phrear cheimarros rheō anemos pneō brontē gē chamai pēlos lithos lithinos lithazō oros "
     "spēlaion aigialos pur kaiō anthrakia thermainō psuchos cheimōn xērainō nomē"),
    ("food", "Food, Farming and Animals",
     "Bread, fish, wine and the odd wolf. Ancient texts do not skip meals.",
     "artos brōma brōsis trophē bibrōskō trōgō phagein katesthiō aristaō deipnon pinō posis potērion oinos oxos methuō hudria "
     "geuō peinaō dipsaō psōmion sitos kokkos speirō sperma therizō therismos karpos ampelos klēma sukē krithinos probaton "
     "poimnē poimainō boskō chortazō chortos ichthus opsarion prosphagion halieuō diktuon ploion ploiarion onos onarion pōlos "
     "bous lukos ophis peristera alektōr thremma auxanō phulassō kērpos kopos kopiaō"),
    ("places", "Houses, Cities and Places",
     "Where things happen, which is everywhere, as long as there's a courtyard nearby.",
     "oikos oikia oikodomeō thura aulē polis kōmē chōra chōrion topos patris sunagōgē hieros naos gazophulakion stoa "
     "praitōrion bēma phulakē kēpos agorazō emporion trapeza kolumbēthra lithostrōtos probatikos stadios"),
    ("law", "Law, Worship and Feasts",
     "Rules, rituals and the occasional feast. Rather more fun than the rules.",
     "nomos entolē heortē sabbaton pascha skēnopēgia egkainia paraskeuē thuō peritomē peritemnō baptizō baptō louō hamartia "
     "hamartanō adikia moicheia moicheuō porneia dikaios dikaiosunē krinō krisis krima katakrinō apōleia apolluō apoluō "
     "sēmeion teras ergon ergazomai timaō timē atimazō atheteō katharos kathairō katharismos miainō sphragizō diaspora "
     "aposunagōgos eparatos"),
    ("time", "Numbers and Time",
     "Counting things and waiting for them. Please note that Greek numbers do not get easier past ten.",
     "heis ehis duo treis tessares pente hex oktō dōdeka dekapente eikosi triakonta tessarakonta pentēkonta hekaton diakosioi "
     "triakosioi pentakischilioi hebdomos dekatos hektos tetartaios tritos deuteros tris tetramēnos hēmera hōra nux etos eniautos "
     "chronos kairos arti nun ēdē tote palin pantote opsia prōi prōia orthros epaurion chthes proteros prōtos eschatos archē "
     "katabolē hōra"),
    ("things", "Clothes, Tools and Money",
     "What people wear, carry, hit each other with and pay each other in.",
     "himation himatismos chitōn hupodēma zōnnumi diazōnnumi lention niptēr niptō machaira logchē hoplon stauros stauroō "
     "sustauroomai stephanos phragellion mastigoō kerma dēnarion litra skeuos hēlos himas schoinion krabbatos kophinos klasma "
     "antlēma antleō phoreō periballō huphantos arraphos porphureos ependutēs kolpos glōssokomon speira thēkē hussōpos spongos "
     "baion phoinix pēchus metron metrētēs arithmos tupos hupodeigma"),
    ("qualities", "Qualities and Descriptions",
     "Good, bad, big, small. The adjectives, that is. Not my moods.",
     ""),
    ("actions", "Everyday Actions",
     "Everything people do that didn't fit anywhere else. Which is quite a lot.",
     ""),
    ("names", "Names and Places",
     "Who and where. Spelling is a bit of a free-for-all, so don't take it personally.",
     ""),
    ("little", "Little Words",
     "Prepositions, conjunctions, particles and other tiny things that hold sentences together. You'll see them everywhere. Make friends.",
     ""),
]

# Nachtraege fuer Woerter, die in keiner Liste oben stehen
EXTRA = {
    "names": "Ainōn Arimathaia Bēthania Bēthesda Bēthleem Bēthsaida gabbatha Galilaia Galilaios Golgotha elaiōn Ephraim "
             "Hierosoluma Hierosolumitēs Iordanēs Ioudaia Israēl Kana Kapernaoum kedros kranion Magdalēnē Nazareth Nazōraios "
             "Saleim Samareia Samareitēs Samareitis Silōam Siōn Suchar Tiberias Rhōmaios Hellēn ethnos",
    "rulers": "rhabbi rhabboni",
    "law": "aitia ethos schisma sunētheia",
    "nature": "akantha",
    "things": "dōrea eidos klēros meros migma osmē chreia",
    "divine": "eirēnē plērōma",
    "time": "hēlikia telos",
    "light": "kosmos",
    "food": "manna",
    "speech": "prophasis",
    "body": "rhapisma",
    "little": "heautou hosos tosoutos",
}

# Funktionswoerter und Ortsadverbien: nach Wortart
LITTLE_POS = {"prep.", "conj.", "part.", "pron.", "art.", "interj.", "cond.", "adv."}
PLACE_ADVERBS_TO_LITTLE = True


def assign(entries):
    """entries: Liste (schluessel, grundform, umschrift, wortart, bedeutung, anzahl). Gibt {kurzname: [entry, ...]} zurueck."""
    explicit = {}
    for short, _title, _intro, words in THEMES:
        for w in words.split():
            explicit.setdefault(w, short)
    for short, words in EXTRA.items():
        for w in words.split():
            explicit.setdefault(w, short)
    out = {t[0]: [] for t in THEMES}
    for e in entries:
        tr, pos = e[2], e[3]
        # mehrere Umschriften wie "gnōstos, gnōrimos": die erste zaehlt
        first = tr.split(",")[0].strip()
        theme = explicit.get(first) or explicit.get(tr)
        if theme is None:
            if pos == "name":
                theme = "names"
            elif pos in LITTLE_POS:
                theme = "little"
            elif pos == "adj.":
                theme = "qualities"
            elif pos == "v.":
                theme = "actions"
            else:
                theme = "leftover"
        out.setdefault(theme, []).append(e)
    return out


def check(entries):
    groups = assign(entries)
    for short, title, _i, _w in THEMES:
        print(f"{short:10} {title:36} {len(groups[short])}")
    left = groups.get("leftover", [])
    print("leftover:", len(left))
    for e in left:
        print("  ", e[2], e[3], e[4])
    return groups
