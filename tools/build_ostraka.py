"""Erzeugt tools/data/ostraka-jokes.json: die Witze auf den Ostraka, griechisch mit englischer Loesung.

Aufruf: python tools/build_ostraka.py
Eingabe: tools/data/src/philogelos-greek.json (griechischer Text des Philogelos, Eulogikon, Public Domain Mark)
Ausgabe: tools/data/ostraka-jokes.json (wird beim Bauen nach assets/scribery/data/ kopiert)

Die Auswahl (Nummern nach Thierfelder) und die englischen Fassungen stehen hier. Die englischen Texte sind eigene, kurze
Uebersetzungen fuer das Spiel (keine Uebernahme einer modernen Uebersetzung). Ausgewaehlt sind kurze, harmlose Witze.
"""
import json
import os
import random
import re
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import build_manuscripts as BM  # noqa: E402  (falsche Antworten wie bei den Johannes-Manuskripten)
import ostraka_glosses as GL  # noqa: E402
SRC = os.path.join(ROOT, "tools", "data", "src", "philogelos-greek.json")
OUT = os.path.join(ROOT, "tools", "data", "ostraka-jokes.json")

EN = {
    1: "A scholar ordered a lamp from a silversmith. When asked how big it should be, he said: \"Big enough for eight people.\"",
    2: "A scholar nearly drowned while swimming. He swore not to go into the water until he had first learned to swim properly.",
    3: "A man said to a scholar's doctor: \"Doctor, when I get up from sleep I'm dazed for half an hour, and then I'm fine.\" The doctor said: \"Then get up half an hour later.\"",
    4: "A scholar was selling a horse. Someone asked if it was a first-time thrower. \"No, a second-time thrower.\" \"How do you know?\" \"It threw me once and my father once.\"",
    5: "Someone met a scholar and said: \"Sir, I saw you in my dream.\" \"I swear, I was busy and didn't notice you.\"",
    8: "A scholar wanted to catch a mouse that kept eating his books. So he took a bite of meat and sat in the dark.",
    9: "A scholar wanted to teach his donkey not to eat, so he gave it no food. When the donkey starved to death, he said: \"What a loss! Just when it had learned not to eat, it died.\"",
    10: "A scholar selling a horse was asked whether it was timid. \"By my father's life, no! It stands alone in the stable.\"",
    11: "A scholar wanted to see whether he looked good asleep, so he shut his eyes and looked in the mirror.",
    12: "A friend asked a scholar who was going abroad to buy him two boys, each fifteen years old. The scholar said: \"If I can't find such boys, I'll buy you one who is thirty.\"",
    14: "A scholar bought a house, then leaned out of the window and asked the passers-by whether it suited him.",
    17: "A friend abroad wrote to a scholar, asking him to buy books. The scholar didn't bother. When his friend came back, he said: \"I never received the letter you sent about the books.\"",
    18: "Someone told a scholar: \"The slave you sold me has died.\" \"By the gods,\" he said, \"he never did anything like that when he was with me.\"",
    19: "A scholar saw many sparrows in a tree. He spread out his coat and shook the tree, hoping to catch them.",
    21: "A scholar who wanted to sleep had no pillow, so he told his slave to put a jar under his head. The slave said it was hard, so he ordered it filled with feathers.",
    22: "A scholar met a friend and said: \"I heard that you died.\" \"But you see me alive.\" \"Yes, but the man who told me is far more trustworthy than you.\"",
    24: "A scholar quarrelled with his father: \"You wretched slave, can't you see what you've cost me? If you hadn't been born, I would have inherited from my grandfather.\"",
    25: "A scholar was sailing in a fierce storm while his slaves wept. \"Don't cry,\" he said, \"I've freed you all in my will.\"",
    28: "A dog bit a scholar's thumb. He said: \"If it had caught my cloak, it would be torn.\"",
    29: "One of two twin brothers died. A scholar met the living one and asked: \"Are you the one who died, or your brother?\"",
    31: "A scholar wanted to cross a river, so he boarded the boat riding his horse. Asked why he didn't get off, he said: \"I'm in a hurry.\"",
    32: "A scholar invited to dinner did not eat. When someone asked why, he said: \"So that I won't seem to have come only for the food.\"",
    41: "A scholar who was selling his house carried a stone from it around as a sample.",
    61: "A scholar teaching small children suddenly shouted at the corner: \"Dionysius is misbehaving over there!\" Someone said that he wasn't even here yet. \"Then when he comes.\"",
    62: "At the games held in Rome once in a thousand years, a scholar comforted a weeping athlete who had lost: \"Don't be sad. You'll win the next one.\"",
    66: "A scholar saw a barge on the river, heavily loaded with grain. \"If the river rises a little,\" he said, \"it will sink.\"",
    74: "Someone told a scholar with a skinny horse: \"Your horse is looking toward Hades.\" The scholar said: \"I can see that too.\"",
    75: "A scholar who was ill and hungry was told that it wasn't yet the fourth hour. Not believing it, he ordered the clock to be brought to him.",
    77: "A scholar carried out his son's body and met the boy's teacher. \"Forgive me,\" he said, \"that my son did not come to school. He has died.\"",
    88: "A scholar was walking home and climbing a steep hill. \"When I came this way before,\" he wondered, \"it was downhill. How has it changed so fast into uphill?\"",
    93: "A scholar learned that a ladder has twenty steps going up, and asked whether it has as many going down.",
    101: "A scholar saw twin brothers and heard people marvelling at how alike they were. \"This one isn't as much like that one,\" he said, \"as that one is like this one.\"",
    104: "A miser writing his will named himself as his heir.",
    122: "An Abderite was selling a basin that had no handles (\"ears\"). When someone asked why he had taken them off, he said: \"So that it won't hear it is being sold and run away.\"",
    125: "An Abderite's sparrow died. Some time later he saw an ostrich and said: \"If my sparrow had lived, it would be that big by now.\"",
    148: "A chatty barber asked a witty man: \"How shall I cut your hair?\" \"In silence,\" he said.",
    173: "A man from Kyme was selling honey. A customer tasted it and said it was excellent. \"If a mouse hadn't fallen into it,\" the seller said, \"I wouldn't be selling it.\"",
    174: "A doctor had given up on a sick man. When the man recovered, he walked a big circle around the doctor. Asked why, he said: \"You told me I was dying, and I am ashamed to be alive.\"",
    183: "A man went to a grumpy doctor: \"Doctor, I can't lie down, I can't stand, and I can't sit.\" The doctor said: \"All you're missing is hanging.\"",
    223: "A starving man saw a loaf of bread lying on a lintel. He said: \"God, either lift me up, or bring that down.\"",
    228: "A drunk was scolded for drinking too much and not thinking straight. Hardly able to see for the wine, he answered: \"Am I the drunk, or are you, with your two faces?\"",
    253: "A scholar met a doctor and said: \"Forgive me, and don't blame me, that I haven't been ill.\"",
    255: "A scholar learned that a raven lives for more than two hundred years, so he bought one to try it out.",
    257: "A scholar buried his son and met the boy's teacher. \"Did the boy come?\" \"No.\" \"Then, teacher, he is dead.\"",
    264: "A witty man was in court before a governor, who dozed off. He shouted: \"I appeal!\" \"To whom?\" \"To you, awake.\"",
    265: "Asked how many measures an amphora holds, a scholar answered: \"Of wine or of water?\"",
}


# Diese Witze haben Wort-Glossen und lassen sich am Uebersetzungstisch uebersetzen (alle echt, aus dem Philogelos).
KEEP = [1, 2, 3, 5, 8, 11, 14, 18, 19, 21, 25, 28, 29, 31, 41, 66, 74, 75, 77, 93, 101, 104, 125, 148, 174, 223, 253, 255, 264, 265]


def tokens(greek):
    out = []
    for t in greek.split():
        t = re.sub(r"^[—]+|[·;.,?!]+$", "", t)
        if t:
            out.append(t)
    return out


def word_pool():
    """Glossen der Johannes-Woerter je Wortart, als Auswahl fuer falsche Antworten."""
    pool = defaultdict(list)
    for name in ("manuscripts.john1.json", "manuscripts.john-rest.json"):
        for f in json.load(open(os.path.join(ROOT, "tools", "data", name), encoding="utf-8"))["fragments"]:
            for w in f["words"]:
                g = w["gloss"]["ok"][0]
                cls = BM.pcls(w["morph"])
                if g not in pool[cls]:
                    pool[cls].append(g)
    return pool


MORPH = {"V": "V-AAI-3S", "N": "N-NSM", "NP": "N-NPM", "A": "A-NSM", "T": "T-NSM", "PREP": "PREP", "CONJ": "CONJ",
         "PRT": "PRT", "ADV": "ADV", "P": "P-NSM"}


def build_fragments(jokes):
    """Die Joke-Texte als Fragmente im Format der Manuskripte (Worte mit richtigen und falschen Antworten)."""
    pool = word_pool()
    rng = random.Random(20261119)
    # Eigene richtige Antworten kommen auch in den Pool, damit die falschen Antworten zum Thema der Witze passen
    for g in GL.G.values():
        cls = BM.pcls(MORPH[g[0]])
        if g[1][0] not in pool[cls]:
            pool[cls].append(g[1][0])
    frags, problems = [], []
    for j in jokes:
        words = []
        for i, t in enumerate(tokens(j["greek"])):
            kind, ok = GL.G[t]
            morph = MORPH[kind]
            wrong = BM.wrong_answers({"morph": morph}, ok, pool, rng, ())
            if len(wrong) < 3:
                problems.append(f"{j['id']} {t}: nur {len(wrong)} falsche Antworten")
            words.append({"i": i, "greek": t, "lemma": "", "morph": morph, "gloss": {"ok": ok, "wrong": wrong, "status": "draft"}})
        frags.append({"id": j["id"], "ref": f"Philogelos {j['philogelos']}", "chapter": 0, "order": j["philogelos"], "tier": 1,
                      "reference_en": j["en"], "words": words})
    assert not problems, problems
    return frags


def main():
    src = json.load(open(SRC, encoding="utf-8"))["jokes"]
    jokes = []
    for n in KEEP:
        greek = src[str(n)].split(" Σχολαστικὸς ὑπαντήσας")[0]       # Nr. 22 hat in der Quelle eine lueckenhafte Variante angehaengt
        greek = greek.replace("ʼ", "᾽")      # Apostroph: ᾽ wie in den Manuskripten (die Schrift hat kein ʼ)
        jokes.append({"id": f"p{n}", "philogelos": n, "greek": greek, "en": EN[n]})
    doc = {
        "schema": "scribery.ostraka-jokes/2",
        "source": "Philogelos (Hierokles und Philagrios, vermutlich 4./5. Jh. n. Chr.), Zaehlung nach Thierfelder 1968. Griechischer Text: "
                  "tools/data/src/philogelos-greek.json (Eulogikon, Public Domain Mark). Die englischen Texte sind eigene Uebersetzungen.",
        "jokes": jokes,
    }
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(doc, f, ensure_ascii=False, indent=2)
        f.write("\n")
    frags = build_fragments(jokes)
    with open(os.path.join(ROOT, "tools", "data", "ostraka.json"), "w", encoding="utf-8", newline=chr(10)) as f:
        json.dump({"schema": 1, "placeholder": False, "source": doc["source"], "fragments": frags}, f, ensure_ascii=False, indent=1)
        f.write(chr(10))
    print(len(jokes), "Witze,", sum(len(x["words"]) for x in frags), "Woerter")


if __name__ == "__main__":
    main()
