# WorldBorderExpander - Simple Negative Challenge Events

## Core Rule

The only ways the world border should expand are:

1. Spending XP in the existing Border Shop
2. Optional time-based automatic expansion, if enabled

Random events/challenges should never expand the border or give positive border rewards.

Random challenges should only create risk:

- If players succeed, nothing bad happens.
- If players fail, the border shrinks.

---

## Add Simple Random Challenge System

Add a configurable random challenge system that triggers after a random amount of active playtime.

Challenges should only run when at least one non-AFK player is online.

If no players are online or all players are AFK:

- Event timers pause
- Active challenge timers pause
- No challenge failure should occur

---

## Challenge Types

Keep the challenge types simple.

### 1. Pay XP Challenge

Players must collectively pay a configured amount of XP levels before time runs out.

Example:

```yaml
pay-xp:
  enabled: true
  display-name: "Border Tribute"
  duration-minutes: 5
  required-levels: 20
  failure-shrink-amount: 10
```

Behavior:

* Challenge starts.
* Players can contribute XP levels through a command or GUI button.
* Contributions reduce the remaining required amount.
* If the required XP is paid before time expires, the challenge succeeds.
* If time expires before payment is complete, shrink the border.

Success reward:

* No border shrink.
* No border expansion.

---

### 2. Kill Entity Challenge

Players must collectively kill a configured number of entities before time runs out.

Example:

```yaml
kill-entities:
  enabled: true
  display-name: "Monster Migration"
  duration-minutes: 10
  entity-type: "ZOMBIE"
  required-kills: 25
  failure-shrink-amount: 10
```

Behavior:

* Challenge starts.
* Track kills by all active players.
* If players reach the required kill count before time expires, the challenge succeeds.
* If time expires first, shrink the border.

Success reward:

* No border shrink.
* No border expansion.

Allow config to support either:

* A specific entity type, such as ZOMBIE, SKELETON, CREEPER
* Or a category like HOSTILE_MOBS if easy to implement

---

### 3. Reach XP Level Challenge

Players must have at least one player reach a configured XP level before time runs out.

Example:

```yaml
reach-level:
  enabled: true
  display-name: "Power Surge"
  duration-minutes: 10
  required-level: 15
  failure-shrink-amount: 10
```

Behavior:

* Challenge starts.
* Check online active players’ XP levels.
* If any player reaches the required XP level before time expires, the challenge succeeds.
* If time expires first, shrink the border.

Success reward:

* No border shrink.
* No border expansion.

---

## Safe Border Shrinking

Whenever the border shrinks for any reason, players should not be killed or trapped outside the new border.

Before shrinking:

1. Calculate the new border size.
2. Check every online player.
3. If a player would be outside the new border, teleport them safely inside first.
4. Use a configurable edge buffer so players are not placed directly on the border.
5. Prefer a safe/highest-block location.
6. After affected players are relocated, apply the border shrink.

Example config:

```yaml
safe-shrink:
  enabled: true
  edge-buffer-blocks: 3
```

---

## Active Player / AFK Detection

Challenge timers should use active playtime, not server uptime.

A player should count as active if they have recently done one of the following:

* Moved
* Broken a block
* Placed a block
* Attacked an entity
* Interacted with a block
* Opened an inventory
* Chatted
* Used a command

If a player has not done any of these for the configured AFK timeout, they count as AFK.

Example config:

```yaml
activity:
  require-active-players: true
  afk-timeout-minutes: 5
```

---

## Random Challenge Timing

Use a random interval based on active playtime.

Example config:

```yaml
random-challenges:
  enabled: true
  min-active-minutes-between-challenges: 30
  max-active-minutes-between-challenges: 60
```

Behavior:

* Pick a random active-playtime delay between the min and max.
* Count down only while at least one non-AFK player is online.
* When the timer reaches zero, start one enabled challenge at random.
* After the challenge succeeds or fails, pick a new random delay.

---

## Player Commands / GUI

Add a simple way to view the current challenge.

Suggested command:

```text
/borderchallenge
```

Aliases:

```text
/bc
/challenge
```

The command should show:

* Current challenge name
* Objective
* Progress
* Time remaining
* Failure penalty

For Pay XP challenges, the GUI or command should allow players to contribute XP.

Example:

```text
/borderchallenge pay 5
```

or a GUI button:

```text
Contribute 5 Levels
Contribute 10 Levels
Contribute All Available
```

---

## Messages

Broadcast clear messages when challenges start, progress, succeed, or fail.

Examples:

Challenge start:

```text
A Border Challenge has begun: Monster Migration!
Kill 25 Zombies in 10 minutes or the border will shrink by 10 blocks.
```

Progress:

```text
Monster Migration: 14/25 Zombies killed.
```

Success:

```text
Challenge completed. The border remains stable.
```

Failure:

```text
Challenge failed. The border shrinks by 10 blocks.
```

Safe teleport:

```text
The border contracted, so you were moved safely inside.
```

---

## Important Design Principle

Do not add rewards to random challenges.

Success only means avoiding border shrinkage.

Failure means the border shrinks.

Expansion should only come from:

* XP spending
* Optional time-based expansion
