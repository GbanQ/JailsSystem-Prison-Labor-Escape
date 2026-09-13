# JailsSystem: Prison Labor & Escape

[![Paper 1.21+](https://img.shields.io/badge/Paper-1.21%2B-blue?style=flat-square)](https://papermc.io/)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](https://adoptium.net/)
[![Modrinth](https://img.shields.io/modrinth/dt/gVO8kFlD?style=flat-square&logo=modrinth&label=downloads)](https://modrinth.com/plugin/jailssystem)
[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)

A complete prison system for Minecraft **Paper 1.21** servers.
Serve your sentence, work to reduce it, and plan daring escapes.

[**Download**](../../releases) · [**Report Bug**](../../issues) · [**Request Feature**](../../issues)

---

<p align="center">
  <a href="https://www.youtube.com/watch?v=S91GcRNoVUQ">
    <img src="https://img.youtube.com/vi/S91GcRNoVUQ/maxresdefault.jpg" alt="Trailer" width="600">
  </a>
</p>

---

## About

Everything begins with imprisonment. The player receives a prisoner outfit, is put into **ADVENTURE** mode, and sent to a cell. Time passes in real time, and being AFK slows down sentence progress.

Earn your freedom by working jobs, collecting **SWcoin**, picking locks, and planning a multi-step escape through the **black market**.

> **Note:** This plugin was developed primarily for the **Russian-speaking community**, but **English is the default language**. Some strings in the English version may remain untranslated. See [Language](#language) below.

---

## Features

- **Prison system** — cells, real-time sentences, AFK slowdown, prisoner board
- **4 job types** — Bricks, Laundry, Kitchen, Library
- **SWcoin currency** — earn from jobs, spend on the black market
- **Lockpicking mini-game** — reaction-based, breaks the lockpick
- **Black market & NPC trader** — buy a ferry ticket for escape
- **Map fragments** — collect 9 in the library to craft a black market map
- **Multi-step escape** — lockpick, door, guard armor, ticket, escape zone
- **Alert system** — guards get the escapee's location every few seconds
- **Prison board** — auto-updating list on signs
- **Fully configurable** — mechanics, drops, prices, zones, messages

---

## Jobs

| Job | What you do | Reward |
|-----|-------------|--------|
| Bricks | Carry a "Wall Piece" between chests (slowness effect) | Time reduction + chance for a lockpick |
| Laundry | Load dirty clothes into the washing machine | Time reduction + chance for guard armor |
| Kitchen | Cook a recipe within a time limit | Time reduction + SWcoin |
| Library | Return a book to the correct category shelf | Time reduction + chance for a map fragment |

---

## Escape Flow

1. Find a **lockpick** — from the brick job
2. Break the door — **reaction mini-game**
3. Get the full **guard armor** — from laundry
4. Earn **SWcoin** — from the kitchen
5. Buy a **ferry ticket** — from the black market trader
6. Reach the **escape zone** — configurable
7. Use the ticket on the **escape sign** — freedom

---

## Commands

### Player

| Command | Description |
|---------|-------------|
| `/jtime` | Show remaining sentence time |
| `/jailstatus` | Prisoner list |
| `/jails workinfo` | Job information |
| `/jails escapeinfo` | Escape information |
| `/buypassport` | Buy a ferry ticket |

### Admin

| Command | Description |
|---------|-------------|
| `/jail <player> <minutes> <cell> <reason>` | Jail a player |
| `/unjail <player>` | Release a player |
| `/jtime add/remove/set <player> <seconds>` | Change sentence time |
| `/cell create/delete/rename` | Manage cells |
| `/jailtools` | Open job items menu |
| `/jailboard create/confirm/cancel` | Create a prison board |
| `/jails reload` | Reload config |

---

## Configuration

Almost everything is configurable in `config.yml`:

- Enable or disable any mechanic
- Time reduction values per job
- Drop chances
- Black market prices
- Library categories and book names
- Kitchen recipes
- Escape and black market zones (coordinates)

All messages are stored in `lang_en.yml` and `lang_ru.yml`, so you can customize colors and phrases without touching the code.

---

## Requirements

- **Paper 1.21+**
- **Java 17+**

---

## Installation

1. Download `JailsSystem.jar` from [**Releases**](../../releases)
2. Drop it into `plugins/`
3. Restart the server
4. Create cells: `/cell create <name>`
5. Edit `config.yml` if needed
6. Done

---

## Language

The plugin supports both **English** and **Russian**. English is enabled by default.

To switch to Russian:

1. Open `plugins/Jails/config.yml`
2. Change `language: en` to `language: ru`
3. Save and restart the server, or run `/jails reload`

If the translation file is not created automatically, do a full server restart with `language: ru` already set. The plugin will generate `lang_en.yml` and `lang_ru.yml`, which you can edit freely to customize or complete the translation.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with care for the Minecraft community.

</div>
