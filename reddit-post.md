# r/RLCraft launch post — Rich Text Editor version

## Title

**Recommended:**

> keepInventory made death meaningless and vanilla RLCraft made it brutal, so I made a mod that sits in between - dying costs you hearts instead of your inventory

Alternatives, same hook:

> I could never decide between losing everything on death and turning on keepInventory, so I made a mod where dying costs you max health instead

> Made a mod where death takes a heart off your max health instead of your inventory, and your death pile never despawns

**On "Stop turning on Keep Inventory, use this instead":** great hook, risky title. It is an
imperative, which reads as an ad; it names neither the mod nor what it does; and
"stop doing X, use Y instead" is a recognised clickbait shape that attracts downvotes on
modding subs. It also opens by telling everyone who uses keepInventory that they are
playing wrong, and a fair number of the sub does. The recommended title keeps the same
tension and puts it in first person, which is what worked for the enchantment mod post.

Keep the hook for the **first line of the body**, where it costs nothing.

---

## Using the Rich Text Editor

**The editor does not interpret pasted markdown.** Paste `**bold**` and you get literal
asterisks on the page. So every block below is plain text with nothing to strip out, and
the formatting is applied afterwards.

Two things make this easier than last time:

- **Images go where the cursor is.** This is the reason to use the rich text editor at all.
  The old build-in-order dance for the enchantment post existed because the markdown
  composer dumped every upload at the top of the body. Here you just put the cursor where
  the image belongs and use the image button.
- **Typing markdown still works, pasting it does not.** Typing `- ` at the start of an
  empty line turns it into a bullet list and you can then type or paste each item. Same for
  headings. It is only pasted markup that comes through dead.

**Formatting, once the text is in:**

- Bold: select the phrase, `Ctrl+B`
- Bullet list: click the list button, or type `- ` on an empty line first
- Links: select the words, `Ctrl+K`, paste the URL

**If any of this gets annoying,** there is a "Switch to Markdown Editor" toggle in the
composer, and the previous version of this file is in the git history at `reddit-post.md`
before this commit.

---

## Block 1

```
Every RLCraft run I've had hits the same fork. Either you die in lava and lose four hours of progress, or you turn on keepInventory. A week later you're charging head first into any structure you stumble across, knowing you'll respawn at the bed you left outside with all your loot. I've played it both ways and I didn't enjoy either.

So I made the setting in between.

When you die, you keep your gear and lose a heart. Armour, hotbar, both hands, baubles and your backpack all survive. Your main inventory drops. Your maximum health goes down by one heart, permanently.
```

**Format:** bold `When you die, you keep your gear and lose a heart.` — nothing else.

**Then insert:** `images/DeathMessage.png`

---

## Block 2

```
The minimum is the part I think actually makes it work. It sits at 10 hearts by default - exactly what Scaling Health starts you with. So the hearts you were born with can never be taken away. Only the ones you earned from heart containers are ever at risk.

That does two things. Dying while you're still learning the pack costs you nothing at all, so a new player never gets dug into a hole they can't climb out of. And every heart container you find stops being a free upgrade, because spending one puts it permanently on the line. "Do I bank this now or hold it until I trust myself?" turned out to be a much more interesting question than I expected.

Your dropped items also never despawn. Vanilla deletes them after five minutes, which in a pack this size is rarely enough time to fight your way back to where you died. Now the pile just waits. RLCraft already gives you the Return Scroll to get there, so the corpse run becomes a real decision instead of a stopwatch.

Everything you kept takes 10% durability, so walking away with your gear still costs you something. It can never break an item outright.
```

**Format:** bold `The minimum is the part I think actually makes it work.` and
`Your dropped items also never despawn.`

**Then insert:** `images/ConfigItems.png`

---

## Block 3 — last one

```
It's all configurable - 22 settings across four categories, read live so nothing needs a restart. The one I'd point at is DEATHS_PER_PENALTY: set it to 3 and you get two free deaths before the third one takes a heart. Scaling Health already does flat per-death health loss and a minimum health setting, but it has no concept of a grace period, and that's genuinely the reason this exists as a mod rather than a config change.

You can also exempt whole dimensions or specific damage types, turn the item keeping off entirely if you'd rather Corpse Complex handled it, or leave keepInventory on - the mod detects it and won't touch your inventory.

Needs Scaling Health. Baubles and Wearable Backpacks are optional. 1.12.2, works on base RLCraft and Dregora, and on any other 1.12.2 pack with Scaling Health.

CurseForge: https://www.curseforge.com/minecraft/mc-mods/rlcraft-death-overhaul

Source (MIT): https://github.com/ExiledRadio/RLCraftDeathOverhaul

I've tuned the defaults against my own playthrough, which is a sample size of one. If the heart cost feels wrong, or the minimum is in the wrong place, or dropping your main inventory is too harsh or not harsh enough, tell me - those are exactly the numbers I'd like other opinions on.
```

**Format:** bold `CurseForge:` and `Source (MIT):`. The editor turns both URLs into links on
its own once you paste them, so leave them alone.

---

## First comment — post immediately after the post goes live

```
Couple of things worth saying up front:

This is unofficial. Not affiliated with Shivaxi, the Dregora team, or the author of Scaling Health. Just a player-made addon.

If you already run something that keeps items on death - Corpse Complex with its Inventory Module switched on, or a gravestone mod - turn one of the two off. Two mods both trying to save the same inventory can duplicate or lose items. Out of the box there's no clash, since Corpse Complex ships with RLCraft with that module disabled, and the mod warns in the log if it spots one.

The vanilla keepInventory gamerule always wins. If you've got it on, the mod leaves your inventory completely alone and only the heart cost applies.
```

**Format:** bold `This is unofficial.`, `If you already run something that keeps items on death`,
and `The vanilla keepInventory gamerule always wins.`

---

## Before posting

- [ ] CurseForge page loads in a private/incognito window
- [ ] Flair the post, checking what similar mod posts on the sub use
- [ ] Post when the sub is active, generally US evening
- [ ] Stay around for the first couple of hours - early replies drive visibility
- [ ] Consider r/feedthebeast as a second post on a different day, not the same evening.
      Reword the opening if you do; a straight copy-paste across subs reads as spam
