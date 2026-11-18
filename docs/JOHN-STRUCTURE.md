# Gliederung des Johannesevangeliums für das Manuscript Archive

Erzeugt von `tools/structure_john.py` (nicht von Hand ändern; die Geschichten-Grenzen stehen in `tools/data/john-stories.tsv`). Maschinenlesbar: `tools/data/john-structure.json`.

**Ebenen:** Geschichte → Seite → Vers → Manuskript. Eine Seite liegt immer in genau einer Geschichte, ein Manuskript immer auf genau einer Seite. Ein Vers kann aus mehreren Manuskripten bestehen, ein Manuskript kann mehrere Verse enthalten.

- **64 Geschichten**, **143 Seiten** (je etwa 110 Wörter, 74–160), **878 Verse**, **1540 Manuskripte**, 15633 Wörter.
- 583 Verse bestehen aus mehr als einem Manuskript; 98 Manuskripte reichen über eine Versgrenze.
- Vers 5,4 fehlt (steht in NA28 nicht im Text, siehe `docs/CREDITS.md`).
- Fundorte: Weil Schwierigkeit und Biom quer zum Text liegen, liegen die Manuskripte einer Seite in durchschnittlich **2.7 verschiedenen Biomen** (Minimum 1, Maximum 5).

## Geschichten

| ID | Titel | Verse | Wörter | Seiten | Manuskripte |
|---|---|---|---|---|---|
| st01 | Prologue: the Word became flesh | 1:1–1:18 | 253 | pg001–pg002 | 21 |
| st02 | John testifies to the priests | 1:19–1:28 | 157 | pg003 | 16 |
| st03 | The Lamb of God | 1:29–1:34 | 115 | pg004 | 10 |
| st04 | The first disciples | 1:35–1:42 | 136 | pg005 | 17 |
| st05 | Philip and Nathanael | 1:43–1:51 | 169 | pg006–pg007 | 17 |
| st06 | The wedding at Cana | 2:1–2:12 | 209 | pg008–pg009 | 23 |
| st07 | Jesus cleanses the temple | 2:13–2:25 | 222 | pg010–pg011 | 20 |
| st08 | Nicodemus | 3:1–3:21 | 395 | pg012–pg015 | 37 |
| st09 | John's last testimony | 3:22–3:36 | 263 | pg016–pg017 | 27 |
| st10 | The woman at the well | 4:1–4:26 | 449 | pg018–pg021 | 47 |
| st11 | The Samaritans believe | 4:27–4:42 | 264 | pg022–pg023 | 27 |
| st12 | The official's son | 4:43–4:54 | 224 | pg024–pg025 | 21 |
| st13 | The pool of Bethesda | 5:1–5:15 | 223 | pg026–pg027 | 21 |
| st14 | The Son gives life | 5:16–5:30 | 308 | pg028–pg030 | 26 |
| st15 | The witnesses to Jesus | 5:31–5:47 | 261 | pg031–pg032 | 26 |
| st16 | Feeding the five thousand | 6:1–6:15 | 236 | pg033–pg034 | 22 |
| st17 | Walking on the sea | 6:16–6:21 | 87 | pg035 | 9 |
| st18 | The bread from heaven | 6:22–6:40 | 389 | pg036–pg039 | 34 |
| st19 | The bread of life | 6:41–6:59 | 334 | pg040–pg042 | 36 |
| st20 | Many disciples leave | 6:60–6:71 | 195 | pg043–pg044 | 19 |
| st21 | Jesus and his brothers | 7:1–7:13 | 214 | pg045–pg046 | 22 |
| st22 | Teaching at the feast | 7:14–7:36 | 397 | pg047–pg050 | 44 |
| st23 | Rivers of living water | 7:37–7:52 | 242 | pg051–pg052 | 20 |
| st24 | The woman caught in adultery | 7:53–8:11 | 169 | pg053–pg054 | 18 |
| st25 | The light of the world | 8:12–8:30 | 367 | pg055–pg057 | 38 |
| st26 | Abraham's children | 8:31–8:59 | 541 | pg058–pg062 | 59 |
| st27 | A blind man healed | 9:1–9:12 | 191 | pg063–pg064 | 21 |
| st28 | The Pharisees question the man | 9:13–9:34 | 391 | pg065–pg068 | 40 |
| st29 | Seeing and blindness | 9:35–9:41 | 110 | pg069 | 11 |
| st30 | The good shepherd | 10:1–10:21 | 367 | pg070–pg072 | 39 |
| st31 | At the feast of Dedication | 10:22–10:42 | 327 | pg073–pg075 | 30 |
| st32 | Lazarus is sick | 11:1–11:16 | 248 | pg076–pg077 | 25 |
| st33 | Martha and Mary | 11:17–11:37 | 330 | pg078–pg080 | 33 |
| st34 | Lazarus raised | 11:38–11:44 | 126 | pg081 | 14 |
| st35 | The plot against Jesus | 11:45–11:57 | 241 | pg082–pg083 | 21 |
| st36 | The anointing at Bethany | 12:1–12:11 | 192 | pg084–pg085 | 16 |
| st37 | The entry into Jerusalem | 12:12–12:19 | 140 | pg086 | 14 |
| st38 | The hour has come | 12:20–12:36 | 319 | pg087–pg089 | 36 |
| st39 | Unbelief and the last words | 12:37–12:50 | 241 | pg090–pg091 | 23 |
| st40 | Jesus washes the feet | 13:1–13:20 | 357 | pg092–pg094 | 35 |
| st41 | The betrayer | 13:21–13:30 | 160 | pg095 | 16 |
| st42 | The new commandment | 13:31–13:38 | 146 | pg096 | 15 |
| st43 | The way, the truth, the life | 14:1–14:14 | 257 | pg097–pg098 | 28 |
| st44 | The Helper is promised | 14:15–14:31 | 322 | pg099–pg101 | 33 |
| st45 | The vine and the branches | 15:1–15:17 | 312 | pg102–pg104 | 30 |
| st46 | The hatred of the world | 15:18–15:27 | 188 | pg105–pg106 | 18 |
| st47 | The work of the Helper | 16:1–16:15 | 224 | pg107–pg108 | 21 |
| st48 | Sorrow becomes joy | 16:16–16:33 | 357 | pg109–pg111 | 37 |
| st49 | The prayer: the hour | 17:1–17:5 | 91 | pg112 | 7 |
| st50 | The prayer for the disciples | 17:6–17:19 | 257 | pg113–pg114 | 21 |
| st51 | The prayer for all believers | 17:20–17:26 | 150 | pg115 | 14 |
| st52 | The arrest in the garden | 18:1–18:11 | 200 | pg116–pg117 | 18 |
| st53 | Annas and Peter's denial | 18:12–18:27 | 301 | pg118–pg120 | 28 |
| st54 | Jesus before Pilate | 18:28–18:40 | 288 | pg121–pg123 | 30 |
| st55 | The sentence | 19:1–19:16 | 304 | pg124–pg126 | 31 |
| st56 | The crucifixion | 19:17–19:30 | 282 | pg127–pg129 | 28 |
| st57 | The pierced side | 19:31–19:37 | 122 | pg130 | 11 |
| st58 | The burial | 19:38–19:42 | 111 | pg131 | 8 |
| st59 | The empty tomb | 20:1–20:10 | 168 | pg132–pg133 | 12 |
| st60 | Mary Magdalene | 20:11–20:18 | 166 | pg134–pg135 | 16 |
| st61 | Jesus among the disciples | 20:19–20:23 | 89 | pg136 | 9 |
| st62 | Thomas and the purpose of the book | 20:24–20:31 | 192 | pg137–pg138 | 17 |
| st63 | The catch of fish | 21:1–21:14 | 278 | pg139–pg141 | 28 |
| st64 | Peter restored and the ending | 21:15–21:25 | 269 | pg142–pg143 | 29 |

## Hinweise

- Die **Geschichten-Grenzen sind ein Entwurf** aus gängigen Gliederungen, die Titel sind eigene. Bitte durchsehen und in `john-stories.tsv` ändern, danach alles neu erzeugen (`build_manuscripts.py --rest`, `group_manuscripts.py`, `structure_john.py`).
- Die **Seiten** sind rein rechnerisch: etwa gleich lang, geschnitten an Versgrenzen. Für Prolog-Seiten (P1–P21, eingefroren) war das möglich, weil kein Pilot-Manuskript eine Versgrenze überschreitet.
- **Für das Archiv:** Ein Vers ist vollständig, wenn alle seine Manuskripte vorliegen (`verses.<kap:vers>.fragments`). Eine Seite ist vollständig, wenn alle ihre Verse vollständig sind, eine Geschichte, wenn alle ihre Seiten vollständig sind.
