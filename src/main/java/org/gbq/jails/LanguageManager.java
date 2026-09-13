package org.gbq.jails;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер локализации с поддержкой русского и английского языков.
 * При первом запуске создаёт файлы lang_ru.yml и lang_en.yml.
 * Язык выбирается в config.yml (параметр "language").
 */
public class LanguageManager {

    private final JavaPlugin plugin;
    private YamlConfiguration langConfig;
    private final Map<String, String> cache = new HashMap<>();
    private String currentLang;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Перезагружает файл локализации из диска
     */
    public void reload() {
        currentLang = plugin.getConfig().getString("language", "ru");
        String fileName = "lang_" + currentLang + ".yml";
        
        File langFile = new File(plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            createDefaultFile(langFile, currentLang);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();
    }

    /**
     * Создаёт файл локализации для указанного языка
     */
    private void createDefaultFile(File langFile, String lang) {
        Map<String, String> defaults;
        if (lang.equals("en")) {
            defaults = getEnglishMessages();
        } else {
            defaults = getRussianMessages();
        }
        
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            cfg.set(entry.getKey(), entry.getValue());
        }
        try {
            cfg.save(langFile);
            plugin.getLogger().info("Создан файл локализации: " + langFile.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось создать файл локализации: " + e.getMessage());
        }
    }

    /**
     * Возвращает сообщение по ключу с заменой плейсхолдеров
     */
    public String getMessage(String key, Object... placeholders) {
        String raw = cache.computeIfAbsent(key, k -> {
            String value = langConfig.getString(k);
            if (value == null) {
                Map<String, String> defaults;
                if (currentLang.equals("en")) {
                    defaults = getEnglishMessages();
                } else {
                    defaults = getRussianMessages();
                }
                value = defaults.getOrDefault(k, "&cMissing: " + k);
                plugin.getLogger().warning("В файле локализации отсутствует ключ: " + key);
            }
            return value;
        });
        
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = "{" + placeholders[i] + "}";
                String replacement = String.valueOf(placeholders[i + 1]);
                colored = colored.replace(placeholder, replacement);
            }
        }
        return colored;
    }

    /**
     * Русские сообщения
     */
    private Map<String, String> getRussianMessages() {
        Map<String, String> m = new HashMap<>();

        // ---------- ОБЩИЕ ----------
        m.put("general.no-permission", "&cНедостаточно прав.");
        m.put("general.player-not-found", "&cИгрок не найден.");
        m.put("general.command-only-player", "&cЭта команда доступна только игрокам.");
        m.put("general.config-reloaded", "&aКонфигурация плагина перезагружена.");
        m.put("general.unknown-command", "&cНеизвестная команда. Используйте /jails для справки.");
        m.put("general.positive-time", "§cВремя должно быть положительным числом.");

        // ---------- /JAILS HELP ----------
        m.put("jails.help.header", "&8»»━━━━━━━━━━━━§r &6ТЮРЕМНЫЙ ИНФОРМАТОР §8━━━━━━━━━━━━««");
        m.put("jails.help.main-info-title", "§a▸ ОСНОВНАЯ ИНФОРМАЦИЯ ◂");
        m.put("jails.help.you-are-jailed", "§eВы находитесь в заключении. Чтобы выйти, вам нужно:");
        m.put("jails.help.time-line-prefix", "§a • §eОтбыть срок §c{time}§e (");
        m.put("jails.help.check-time-hover", "§6Проверить точное время");
        m.put("jails.help.reduce-time-ways", "§a • §eВыполнять работы для сокращения срока");
        m.put("jails.help.escape-risky", "§a • §eПопытаться сбежать (рискованно!)");
        m.put("jails.help.reduce-ways-title", "§a▸ СПОСОБЫ СОКРАЩЕНИЯ СРОКА:");
        m.put("jails.help.work-reward-template", "§7 • §eРаботы: §a-{seconds} сек §eза выполнение");
        m.put("jails.help.afk-slower-text-prefix", "§7 • §eПри нахождении в AFK время заключения идёт медленнее! (");
        m.put("jails.help.afk-hover", "§6Подробнее о правилах AFK");
        m.put("jails.help.check-time-click", "§eЧтобы узнать точное оставшееся время, используйте: ");
        m.put("jails.help.check-time-hover-long", "§6Нажмите, чтобы вставить команду в чат\n§7Команда не отправится автоматически");
        m.put("jails.help.afk-multiplier-template", "§eВ режиме AFK время идет §cв {multiplier} раза медленнее§e! ({command})");
        m.put("jails.help.rules-hover", "§eВажные правила и штрафы\n§7AFK, поимка, удвоение срока\n§fПроверить время: §e/jtime");
        m.put("jails.help.footer", "§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««");
        m.put("jails.help.clickable.works", "РАБОТЫ");
        m.put("jails.help.clickable.escape", "ПОБЕГ");
        m.put("jails.help.clickable.rules", "ПРАВИЛА");
        m.put("jails.help.works-hover", "§e3 вида работ для сокращения срока\n§7(Кирпичи, Прачечная, Кухня)\n§fНаграда: §a-{seconds} секунд §fза задание");
        m.put("jails.help.escape-hover", "§eКак сбежать из тюрьмы\n§7Отмычка, броня, билет\n§c⚠ Риск: §fпри поимке срок ×{multiplier}");

        // ---------- ДЕТАЛИ РАБОТ ----------
        m.put("jails.work-details.title", "§8»»━━━━━━━━━━━━§r §6ДЕТАЛИ РАБОТ §8━━━━━━━━━━━━««");
        m.put("jails.work-details.bricks.name", "§a1. §6Работа с кирпичами:");
        m.put("jails.work-details.bricks.desc", "§7 • §eПеренос кирпичей из склада на стройку");
        m.put("jails.work-details.bricks.reward", "§7 • §fНаграда: §a-{seconds} сек §fза доставку");
        m.put("jails.work-details.bricks.extra", "§7 • §fШанс найти: §9Отмычку §f(для побега)");
        m.put("jails.work-details.laundry.name", "§a2. §6Работа в прачечной:");
        m.put("jails.work-details.laundry.desc", "§7 • §eСтирка и сортировка грязной одежды");
        m.put("jails.work-details.laundry.reward", "§7 • §fНаграда: §a-{seconds} сек §fза загрузку");
        m.put("jails.work-details.laundry.extra", "§7 • §fШанс найти: §9Броню надзирателя §f(часть комплекта)");
        m.put("jails.work-details.kitchen.name", "§a3. §6Работа на кухне:");
        m.put("jails.work-details.kitchen.desc", "§7 • §eПриготовление и подача еды");
        m.put("jails.work-details.kitchen.reward", "§7 • §fНаграда: §a-{seconds} сек §fза порцию");
        m.put("jails.work-details.kitchen.extra", "§7 • §fЗаработок: §e{swcoin} §f(на билет)");

        // ---------- ДЕТАЛИ ПОБЕГА ----------
        m.put("jails.escape-details.title", "§8»»━━━━━━━━━━━━§r §6ДЕТАЛИ ПОБЕГА §8━━━━━━━━━━━━««");
        m.put("jails.escape-details.requirements", "§eДля успешного побега вам нужно:");
        m.put("jails.escape-details.lockpick", "§a1. §6Найти отмычку §7(работа с кирпичами)\n§7 • Используйте её, чтобы взломать двери");
        m.put("jails.escape-details.armor", "§a2. §6Полный комплект брони надзирателя §7(прачечная)\n§7 • Шлем, нагрудник, поножи, ботинки\n§7 • Маскирует вас от охраны");
        m.put("jails.escape-details.ticket", "§a3. §6Билет на паром\n§7 • Купите на чёрном рынке за §e{swcoin}\n§7 • Команда: §e/buypassport");
        m.put("jails.escape-details.destination", "§a4. §6Добраться до места побега\n§7 • Используйте предмет побега\n§7 • Телепортируйтесь к свободе!");
        m.put("jails.escape-details.warning-title", "§c⚠ ВНИМАНИЕ:");
        m.put("jails.escape-details.warning-time", "§7 • На побег даётся §e{minutes} минут");
        m.put("jails.escape-details.warning-caught", "§7 • Если вас поймают: §cсрок ×{multiplier}");
        m.put("jails.escape-details.warning-reward", "§7 • Убийство сбежавшего даёт §e{swcoin}");

        // ---------- ПРАВИЛА ----------
        m.put("jails.rules-details.title", "§8»»━━━━━━━━━━━━§r §6ПРАВИЛА ТЮРЬМЫ §8━━━━━━━━━━━━««");
        m.put("jails.rules-details.basic.header", "§a📋 Основные правила:");
        m.put("jails.rules-details.basic.gamemode", "§7 • Режим игры: §eADVENTURE");
        m.put("jails.rules-details.basic.teleport", "§7 • Телепортация: §cзапрещена");
        m.put("jails.rules-details.basic.portal", "§7 • Портал: §cзапрещён");
        m.put("jails.rules-details.time.header", "§a⏱ Время заключения:");
        m.put("jails.rules-details.time.normal", "§7 • Обычный режим: §e1 секунда/тик");
        m.put("jails.rules-details.time.afk", "§7 • AFK режим: §e1 секунда/{multiplier} тика §7(в {multiplier} раза медленнее)");
        m.put("jails.rules-details.time.afk-disabled", "§7 • AFK режим: §cотключён");
        m.put("jails.rules-details.time.check", "§7 • Проверить время: §e/jtime");
        m.put("jails.rules-details.penalties.header", "§a⚖ Штрафы и награды:");
        m.put("jails.rules-details.penalties.work", "§7 • Успешная работа: §a-{seconds} секунд");
        m.put("jails.rules-details.penalties.escape-fail", "§7 • Неудачный побег: §cсрок ×{multiplier}");
        m.put("jails.rules-details.penalties.capture", "§7 • Поимка сбежавшего: §e{swcoin}");
        m.put("jails.rules-details.afk-info.header", "§a🎯 Статус AFK:");
        m.put("jails.rules-details.afk-info.desc", "§7 • Определяется по отсутствию движения");
        m.put("jails.rules-details.afk-info.timeout", "§7 • Таймаут: §e{seconds} секунд");
        m.put("jails.rules-details.afk-info.auto", "§7 • Автоматическое переключение");
        m.put("jails.rules-details.back-button", "§7[§a← Назад§7]");
        m.put("jails.rules-details.back-hover", "§eВернуться в главное меню");

        // ---------- /JTIME ----------
        m.put("jtime.self", "§eОставшееся время: {minutes} мин {seconds} сек.");
        m.put("jtime.other", "§eВремя игрока {player}: {minutes} мин {seconds} сек.");
        m.put("jtime.not-jailed", "§cВы не находитесь в тюрьме.");
        m.put("jtime.other-not-jailed", "§cИгрок {player} не в тюрьме.");
        m.put("jtime.target-notified-add", "§eВаше время заключения увеличено на {seconds} сек. Теперь: {time}");
        m.put("jtime.target-notified-remove", "§eВаше время заключения уменьшено на {seconds} сек. Теперь: {time}");
        m.put("jtime.target-notified-set", "§eВаше время заключения изменено на {time}");
        m.put("jtime.admin.usage-add", "§cИспользование: /jtime add <игрок> <секунды>");
        m.put("jtime.admin.usage-remove", "§cИспользование: /jtime remove <игрок> <секунды>");
        m.put("jtime.admin.usage-set", "§cИспользование: /jtime set <игрок> <секунды>");
        m.put("jtime.admin.seconds-number", "§cСекунды должны быть числом.");
        m.put("jtime.admin.add-success", "§aДобавлено {seconds} сек игроку {player}. Новое время: {time}");
        m.put("jtime.admin.remove-success", "§aУбавлено {seconds} сек игроку {player}. Новое время: {time}");
        m.put("jtime.admin.set-success", "§aУстановлено время {time} для игрока {player}");
        // ---------- СООБЩЕНИЯ ИЗ КОНФИГА (ESCAPE) ----------
        m.put("escape.start", "&aВы сбежали из тюрьмы! У вас есть &e{time} минут&a, чтобы добраться до места побега и активировать его!");
        m.put("escape.passport-missing", "&cУ вас нет билета на паром! Купите его у торговца на чёрном рынке за {swcoin}.");
        m.put("escape.armor-missing", "&cДля побега вам нужен полный комплект брони надзирателя!");
        m.put("escape.area-found", "&aВы нашли место побега! Используйте &6{interact-item}&a, чтобы сбежать.");
        m.put("escape.success", "&aПоздравляем! Вы успешно сбежали из тюрьмы!");
        m.put("escape.fail", "&cВы не успели сбежать и вернулись в тюрьму. Срок увеличен в {multiplier} раз!");
        m.put("escape.caught", "&cВас поймали при попытке побега! Срок увеличен в {multiplier} раз!");
        m.put("escape.broadcast-escape", "&7[Тюрьма] &eИгрок {player} сбежал из тюрьмы!");
        m.put("escape.broadcast-caught", "&7[Тюрьма] &cИгрок {player} был пойман при попытке побега!");
        m.put("escape.broadcast-location", "&7[Тюрьма] &eСбежавший заключённый {player} был замечен на X: {x}, Y: {y}, Z: {z}.");

        // ---------- AFK СООБЩЕНИЯ ----------
        m.put("afk.on", "&eВы находитесь в состоянии AFK. Ваше время заключения идет медленнее.");
        m.put("afk.off", "&eВы вышли из состояния AFK. Время заключения теперь идет в обычном режиме.");

        // ---------- НАЗВАНИЯ ПРЕДМЕТОВ ИЗ КОНФИГА ----------
        m.put("swcoin.name", "&6SWcoin");
        m.put("swcoin.lore", "&dТюремная валюта");

        m.put("items.lockpick.name", "&6Деревянная отмычка");
        m.put("items.lockpick.lore.0", "&7может взламывать железные двери");

        m.put("items.guard-armor.helmet.name", "&7Шлем надзирателя");
        m.put("items.guard-armor.chestplate.name", "&7Нагрудник надзирателя");
        m.put("items.guard-armor.leggings.name", "&7Поножи надзирателя");
        m.put("items.guard-armor.boots.name", "&7Ботинки надзирателя");

        m.put("blackmarket.trader.name", "&6&lТорговец чёрного рынка");
        m.put("blackmarket.trader.spawn-item.name", "&6Яйцо призыва торговца");
        m.put("blackmarket.trader.spawn-item.lore.0", "&7Нажмите ПКМ по блоку,");
        m.put("blackmarket.trader.spawn-item.lore.1", "&7чтобы призвать торговца");
        m.put("blackmarket.trader.offers.ticket.item.name", "&6Билет на паром");
        m.put("blackmarket.trader.offers.ticket.item.lore.0", "&7Одноразовый билет для переправы");
        m.put("blackmarket.trader.offers.ticket.item.lore.1", "&7через море. Используйте на месте побега.");
        m.put("blackmarket.map-settings.name", "&6Карта чёрного рынка");
        m.put("blackmarket.map-settings.lore.0", "&7Используйте эту карту, чтобы найти вход на чёрный рынок.");
        m.put("blackmarket.map-settings.lore.1", "&eТам вы сможете приобрести билет на паром.");

        // ---------- ТАЙМЕР ПОБЕГА (ACTION BAR) ----------
        m.put("escape.escape-time-left", "§eВремя побега: %d мин %02d сек");

        // ---------- СООБЩЕНИЯ ПРИ СОЗДАНИИ КАМЕРЫ ----------
        m.put("cell.create.usage", "§cИспользование: /cell create <name>");
        // ---------- /JAIL ----------
        m.put("jail.usage", "§cИспользование: /jail <player> <minutes> <cell> <причина...>");
        m.put("jail.already-jailed", "§cДанный игрок уже находится в тюрьме.");
        m.put("jail.cell-not-found", "§cКамера с таким именем не найдена.");
        m.put("jail.world-not-found", "§cМир не найден.");
        m.put("jail.broadcast-jailed", "§7[Тюрьма] §eИгрок {player} заключён в тюрьму на {minutes} мин.");
        m.put("jail.positive-time", "§cВремя должно быть положительным числом.");
        m.put("jail.minutes-number", "§cМинуты должны быть числом.");

        // ---------- /UNJAIL ----------
        m.put("unjail.usage", "§cИспользование: /unjail <player>");
        m.put("unjail.not-jailed", "§cИгрок не находится в тюрьме.");
        m.put("unjail.broadcast-released", "§7[Тюрьма] §eИгрок {player} освобождён из тюрьмы.");
        m.put("unjail.inventory-not-found", "§cИнвентарь игрока не найден.");

        // ---------- /JAILSTATUS ----------
        m.put("jailstatus.empty", "§cНет заключённых игроков.");
        m.put("jailstatus.header", "§eСписок заключённых:");
        m.put("jailstatus.entry", "§6 {player} | Камера: {cell} | Оставшееся время: {minutes} мин {seconds} сек.");

        // ---------- /CELL ----------
        m.put("cell.help.0", "§6=== Управление камерами ===");
        m.put("cell.help.1", "§e/cell create <name> §7- создать камеру на текущей позиции (только игрок)");
        m.put("cell.help.2", "§e/cell delete <name> §7- удалить камеру");
        m.put("cell.help.3", "§e/cell rename <old> <new> §7- переименовать камеру");
        m.put("cell.create.usage", "§cИспользование: /cell create <name>");
        m.put("cell.create.success", "§aКамера {name} создана.");
        m.put("cell.delete.usage", "§cИспользование: /cell delete <name>");
        m.put("cell.delete.not-found", "§cКамера не найдена.");
        m.put("cell.delete.success", "§aКамера {name} удалена.");
        m.put("cell.delete.has-prisoners", "§cНельзя удалить камеру, пока в ней есть заключённые.");
        m.put("cell.rename.usage", "§cИспользование: /cell rename <old> <new>");
        m.put("cell.rename.not-found", "§cКамера с именем {old} не найдена.");
        m.put("cell.rename.already-exists", "§cКамера с именем {new} уже существует.");
        m.put("cell.rename.success", "§aКамера {old} переименована в {new}.");
        m.put("cell.rename.has-prisoners", "§cНельзя переименовать камеру, пока в ней есть заключённые.");

        // ---------- /BUYPASSPORT ----------
        m.put("buypassport.disabled", "§cЧёрный рынок отключён.");
        m.put("buypassport.not-in-market", "&cВы должны находиться на чёрном рынке, чтобы купить билет!");
        m.put("buypassport.already-have", "&cУ вас уже есть билет!");
        m.put("buypassport.not-enough-money", "&cУ вас недостаточно {swcoin} для покупки билета!");
        m.put("buypassport.success", "&aВы успешно купили билет на паром за {price} {swcoin}!");

        // ---------- /GIVEPICK ----------
        m.put("givepick.usage", "§cИспользование: /givepick <игрок>");
        m.put("givepick.success", "§aВыдана отмычка игроку {player}");
        m.put("givepick.fail", "§cНе удалось создать отмычку (проверьте конфиг)");

        // ---------- /GIVEARMOUR ----------
        m.put("givearmour.usage", "§cИспользование: /givearmour <игрок>");
        m.put("givearmour.success", "§aВыдана броня надзирателя игроку {player}");

        // ---------- /GIVETABLE ----------
        m.put("givetable.usage", "§cИспользование: /givetable <игрок>");
        m.put("givetable.success", "§aВыдан предмет побега игроку {player}");

        // ---------- /GIVEMARKETMAP ----------
        m.put("givemarketmap.usage", "§cИспользование: /givemarketmap <игрок>");
        m.put("givemarketmap.success", "§aКарта сокровищ выдана игроку {player}");
        m.put("givemarketmap.not-configured", "§cЧёрный рынок не настроен, карта не создана.");
        m.put("givemarketmap.map-error", "§cНастройки карты не найдены в конфиге.");

        // ---------- /JAILTOOLS ----------
        m.put("jailtools.usage", "§cИспользование: /jailtools [give <тип>]");
        m.put("jailtools.invalid-type", "§cНеизвестный тип. Доступны: {types}");
        m.put("jailtools.gave-items", "§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««\n§6⚒ Вы получили набор для работы: §e{type}\n§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««");
        m.put("jailtools.trader-egg-given", "§aВы получили яйцо призыва торговца чёрного рынка.");
        m.put("jailtools.trader-egg-help", "§7Используйте ПКМ по блоку на территории чёрного рынка, чтобы призвать торговца.");
        m.put("jailtools.trader-egg-fail", "§cНе удалось создать яйцо призыва. Проверьте конфиг.");
        m.put("jailtools.library-error", "§cОшибка: в конфиге нет категорий библиотеки.");

        // ---------- /JAILBOARD ----------
        m.put("jailboard.help.0", "§6=== Управление табло ===");
        m.put("jailboard.help.1", "§e/jailboard create §7- создать новое табло (выделение)");
        m.put("jailboard.help.2", "§e/jailboard confirm §7- подтвердить создание табло");
        m.put("jailboard.help.3", "§e/jailboard cancel §7- отменить создание табло");
        m.put("jailboard.help.4", "§e/jailboard clear §7- очистить текущее выделение");
        m.put("jailboard.selection-enabled", "§aРежим выделения табличек включен!");
        m.put("jailboard.selection-instruction1", "§7Кликайте по табличкам в том порядке, в котором они должны идти на табло");
        m.put("jailboard.selection-instruction2", "§7Когда закончите, напишите §e/jailboard confirm");
        m.put("jailboard.selection-cancel", "§7или §c/jailboard cancel §7для отмены");
        m.put("jailboard.sign-added", "§aТабличка добавлена в выделение §7(теперь {count})");
        m.put("jailboard.sign-removed", "§cТабличка убрана из выделения §7(теперь {count})");
        m.put("jailboard.confirm-no-selection", "§cВы не выделили ни одной таблички!");
        m.put("jailboard.confirm-different-worlds", "§cВсе таблички должны быть в одном мире!");
        m.put("jailboard.confirm-success", "§a✅ Табло успешно создано из {count} табличек!");
        m.put("jailboard.cancel", "§eСоздание табло отменено");
        m.put("jailboard.clear", "§eВыделение очищено");
        m.put("jailboard.need-create-first", "§cСначала включите режим выделения: §e/jailboard create");
        m.put("jailboard.cannot-edit-board-sign", "§cНельзя редактировать табличку, входящую в табло!");

        // ---------- /JAILHISTORY ----------
        m.put("jailhistory.no-history", "§cУ игрока {player} нет истории наказаний.");
        m.put("jailhistory.header", "§6=== История наказаний игрока {player} ===");
        m.put("jailhistory.page-info", "§7Страница {page} из {total} (всего записей: {totalEntries})");
        m.put("jailhistory.entry-line", "§e{number}. §7[{date}] §a{initiator} §7посадил игрока §c{player} §7на §c{minutes} мин §7в §b{cell}");
        m.put("jailhistory.reason-line", "§7 Причина: §f{reason}");
        m.put("jailhistory.navigation-prev", "§7[←] ");
        m.put("jailhistory.navigation-prev-hover", "§7Предыдущая страница");
        m.put("jailhistory.navigation-page", "§7[ §e{page}/{total} §7] ");
        m.put("jailhistory.navigation-next", "§7[→]");
        m.put("jailhistory.navigation-next-hover", "§7Следующая страница");

        // ---------- ПОБЕГ ----------
        m.put("escape.start", "&aВы сбежали из тюрьмы! У вас есть &e{time} минут&a, чтобы добраться до места побега и активировать его!");
        m.put("escape.passport-missing", "&cУ вас нет билета на паром! Купите его у торговца на чёрном рынке за {swcoin}.");
        m.put("escape.armor-missing", "&cДля побега вам нужен полный комплект брони надзирателя!");
        m.put("escape.area-found", "&aВы нашли место побега! Используйте &6{interact-item}&a, чтобы сбежать.");
        m.put("escape.success", "&aПоздравляем! Вы успешно сбежали из тюрьмы!");
        m.put("escape.fail", "&cВы не успели сбежать и вернулись в тюрьму. Срок увеличен в {multiplier} раз!");
        m.put("escape.caught", "&cВас поймали при попытке побега! Срок увеличен в {multiplier} раз!");
        m.put("escape.broadcast-escape", "&7[Тюрьма] &eИгрок {player} сбежал из тюрьмы!");
        m.put("escape.broadcast-caught", "&7[Тюрьма] &cИгрок {player} был пойман при попытке побега!");
        m.put("escape.broadcast-location", "&7[Тюрьма] &eСбежавший заключённый {player} был замечен на X: {x}, Y: {y}, Z: {z}.");
        m.put("escape.escape-ship-title", "§bОтчаливание...");
        m.put("escape.escape-ship-mid", "После нескольких дней в открытом море...");
        m.put("escape.escape-ship-final", "вы, наконец, достигли берега.");
        m.put("escape.default-item-name", "предмет");
        m.put("escape.zone-loaded", "Загружена зона побега: {name}");

        // ---------- РАБОТЫ ----------
        m.put("work.general.cant-drop", "§cВы не можете выбросить этот предмет.");
        m.put("work.general.washing-in-progress", "§cСтирка ещё не закончилась, нельзя трогать одежду!");
        m.put("work.general.inventory-full-drop", "§cВаш инвентарь полон, предмет выпал на землю.");
        m.put("work.general.swcoin-reward", "§aВы получили {swcoin} за работу!");
        
        m.put("work.brick.take", "Вы взяли кирпичи, отнесите их на место сдачи.");
        m.put("work.brick.already-has", "У вас уже есть Кусок стены.");
        m.put("work.brick.put", "У вас нет кирпичей для сдачи.");
        m.put("work.brick.reward-action-bar", "§aВаш срок уменьшен на {seconds} секунд.");
        m.put("work.brick.lockpick-found", "§6Вам повезло! Вы получили Деревянную отмычку.");
        
        m.put("work.laundry.take", "Вы взяли грязный комплект одежды заключённого.");
        m.put("work.laundry.already-has", "У вас уже есть грязный комплект одежды. Сначала постирайте его.");
        m.put("work.laundry.put", "У вас нет чистого комплекта для сдачи.");
        m.put("work.laundry.guard-armor-found", "§6Вы нашли {piece}!");
        
        m.put("work.kitchen.take", "§eВы получили ингредиенты для §a{dish}§e. Приготовьте и сдайте в течение §c{seconds} секунд§e, чтобы уменьшить срок.");
        m.put("work.kitchen.already-task", "Сначала завершите текущее задание.");
        m.put("work.kitchen.put-no-task", "У вас нет активного задания для сдачи.");
        m.put("work.kitchen.put-no-dish", "У вас нет готового {dish} для сдачи.");
        m.put("work.kitchen.recipe-error", "§cОшибка: рецепт не найден.");
        m.put("work.kitchen.task-failed", "§cВы не успели приготовить {dish} в отведенное время. Задание провалено.");
        m.put("work.kitchen.action-bar-time", "§aОсталось: {seconds} сек");
        m.put("work.kitchen.task-failed-action-bar", "§cЗадание провалено!");
        m.put("work.kitchen.no-recipes", "§cРецепты временно недоступны.");
        
        m.put("work.library.already-has-book", "§cВы уже взяли книгу. Сначала положите её на полку.");
        m.put("work.library.no-categories", "§cБиблиотека временно не работает.");
        m.put("work.library.no-books", "§cНет доступных книг.");
        m.put("work.library.book-taken", "§eВы взяли книгу: §a{book}");
        m.put("work.library.no-shelves", "§cНет установленных полок для этой книги!");
        m.put("work.library.no-shelves-for-category", "§cНет установленных полок для категории {category}");
        m.put("work.library.shelf-instruction", "§eПоложите эту книгу на полку с соответствующим разделом ({categories}).");
        m.put("work.library.no-task", "§cУ вас нет книги для работы! Возьмите книгу в разделе выдачи.");
        m.put("work.library.wrong-shelf", "§cЭта полка не подходит для этой книги.");
        m.put("work.library.not-glow-shelf", "§cВы должны положить книгу именно в подсвеченную полку!");
        m.put("work.library.no-book-in-hand", "§cВы должны держать книгу в руке, чтобы положить её на полку!");
        m.put("work.library.wrong-book", "§cОшибка: книга не соответствует вашему заданию!");
        m.put("work.library.shelf-full", "§cВ этой полке нет свободных мест!");
        m.put("work.library.success", "§aКнига успешно помещена на полку!");
        m.put("work.library.fragment-found", "§aВы нашли обрывок карты!");

        // ---------- ПВО ----------
        m.put("pvo.warning", "§eПВО: Вы вошли в охраняемую территорию. покиньте территорию иначе через {seconds} секунды будут запущены ракеты!");
        m.put("pvo.warning-title", "§cТревога!");
        m.put("pvo.warning-subtitle", "§eПокиньте территорию!");
        m.put("pvo.death-message", "§cИгрок {player} был уничтожен системой ПВО!");

        // ---------- ВЗЛОМ ----------
        m.put("lockpick.already-attempt", "§cВы уже пытаетесь взломать тюремный замок!");
        m.put("lockpick.cooldown", "§cЗамок еще не остыл после прошлой попытки. Подождите {seconds} секунд.");
        m.put("lockpick.need-lockpick", "§cВам нужна отмычка, чтобы взломать тюремную дверь!");
        m.put("lockpick.start-title", "§cВзлом замка");
        m.put("lockpick.start-subtitle", "§eНажми SHIFT в нужный момент");
        m.put("lockpick.round-success-title", "§aЩелчок");
        m.put("lockpick.round-success-subtitle", "§7штифт поддался");
        m.put("lockpick.round-fail-title", "§cНеудача ");
        m.put("lockpick.round-fail-subtitle", "§cЗамок заело, попробуй еще раз!");
        m.put("lockpick.complete-title", "§aЗамок открыт!");
        m.put("lockpick.complete-subtitle", "§aВсе штифты встали на место!");
        m.put("lockpick.break-message", "§8[§c✘§8] §cОтмычка сломалась! Придется искать новую.");
        m.put("lockpick.durability-warning", "§8[§c!§8] §7Отмычка истончается...");

        // ---------- ПРЕДМЕТЫ ----------
        m.put("items.brick-piece", "§7Кусок стены");
        m.put("items.dirty-clothes", "§7Грязная одежда заключённого");
        m.put("items.clean-clothes", "§7Чистая одежда заключённого");
        m.put("items.lockpick", "§6Деревянная отмычка");
        m.put("items.lockpick-lore", "§7может взламывать железные двери");
        m.put("items.swcoin", "&6SWcoin");
        m.put("items.swcoin-lore", "&dТюремная валюта");
        m.put("items.escape-ticket", "&6Билет на паром");
        m.put("items.escape-ticket-lore.1", "&7Одноразовый билет для переправы");
        m.put("items.escape-ticket-lore.2", "&7через море. Используйте на месте побега.");
        m.put("items.trader-spawn-egg", "&6Яйцо призыва торговца");
        m.put("items.trader-spawn-egg-lore.1", "&7Нажмите ПКМ по блоку,");
        m.put("items.trader-spawn-egg-lore.2", "&7чтобы призвать торговца");
        m.put("items.market-map", "&6Карта чёрного рынка");
        m.put("items.market-map-lore.1", "&7Используйте эту карту, чтобы найти вход на чёрный рынок.");
        m.put("items.market-map-lore.2", "&eТам вы сможете приобрести билет на паром.");
        m.put("items.map-fragment", "§7Обрывок карты");
        m.put("items.configurator-stick", "§6Конфигуратор табло");
        m.put("items.configurator-lore.1", "§7Нажмите ПКМ по табличке,");
        m.put("items.configurator-lore.2", "§7чтобы создать табло заключённых.");
        m.put("items.prisoner-chestplate", "§7Нагрудник заключённого");
        m.put("items.prisoner-leggings", "§7Штаны заключённого");
        m.put("items.prisoner-boots", "§7Ботинки заключённого");
        m.put("items.guard-armor.helmet", "§7Шлем надзирателя");
        m.put("items.guard-armor.chestplate", "§7Нагрудник надзирателя");
        m.put("items.guard-armor.leggings", "§7Поножи надзирателя");
        m.put("items.guard-armor.boots", "§7Ботинки надзирателя");

        // ---------- GUI ----------
        m.put("work-gui.title", "§8⚒ §7Выберите тип работы §8⚒");
        m.put("work-gui.close-button", "§cЗакрыть");
        m.put("work-gui.work-type.bricks", "§6Работа с кирпичами");
        m.put("work-gui.work-type.laundry", "§6Прачечная");
        m.put("work-gui.work-type.kitchen", "§6Кухня");
        m.put("work-gui.work-type.library", "§6Библиотека");
        m.put("work-gui.work-type.trader", "§6Призыв торговца");
        m.put("work-gui.info-item.name", "§e§lИнформация о работах");
        m.put("work-gui.info-item.lore.0", "§7━━━━━━━━━━━━━━━━━━");
        m.put("work-gui.info-item.lore.1", "§fКирпичи: §a-{brick_seconds} сек §7+ §6отмычка");
        m.put("work-gui.info-item.lore.2", "§fПрачечная: §a-{laundry_seconds} сек §7+ §bброня");
        m.put("work-gui.info-item.lore.3", "§fКухня: §a-{kitchen_seconds} сек §7+ §eSWcoin");
        m.put("work-gui.info-item.lore.4", "§fБиблиотека: §a+ обрывки карты §7→ §6карта рынка");
        m.put("work-gui.info-item.lore.5", "§fТорговец: §aЯйцо призыва §7→ §6торговец на чёрном рынке");
        m.put("work-gui.info-item.lore.6", "§7━━━━━━━━━━━━━━━━━━");
        m.put("work-gui.info-item.lore.7", "§7Ставьте блоки в нужных местах");
        m.put("work-gui.info-item.lore.8", "§7и они автоматически настроятся!");
        m.put("work-gui.info-item.lore.click-to-get", "§eНажмите, чтобы получить набор!");
        m.put("work-gui.library.book-dispenser", "§6Выдача книг");
        m.put("work-gui.library.bookshelf-prefix", "§6Книжная полка: ");
        m.put("work-gui.library.cartography-table", "§6Стол картографа");
        m.put("work-gui.laundry.washing-machine", "§6Стиральная машинка");
        m.put("work-gui.laundry.washing-machine-alt", "Стиралка");

        // ---------- AFK ----------
        m.put("afk.on", "&eВы находитесь в состоянии AFK. Ваше время заключения идет медленнее.");
        m.put("afk.off", "&eВы вышли из состояния AFK. Время заключения теперь идет в обычном режиме.");

        // ---------- ЧЁРНЫЙ РЫНОК ----------
        m.put("blackmarket.spawn-trader-not-in-zone", "§cТорговца можно призвать только на территории чёрного рынка!");
        m.put("blackmarket.spawn-success", "§aВы призвали торговца чёрного рынка!");
        m.put("blackmarket.remove-success", "§cВы убрали торговца чёрного рынка!");
        m.put("blackmarket.trader-works-only-in-market", "§cТорговец работает только на территории чёрного рынка!");
        m.put("blackmarket.map-creation-error", "§cПроизошла ошибка при создании карты.");
        m.put("blackmarket.map-settings-not-found", "§cНастройки карты не найдены в конфиге.");
        m.put("blackmarket.unknown-map-scale", "Неизвестный масштаб карты: {scale}, используется FAR");

        // ---------- РАЗНОЕ ----------
        m.put("misc.sign-escape-line1", "§6Уплыть");
        m.put("misc.sign-escape-line2", "§a[нажмите]");
        m.put("misc.sign-escape-line3", "§a[чтобы сбежать]");
        m.put("misc.not-prisoner", "§cВы не заключённый.");
        m.put("misc.escape-no-ticket", "§cУ вас нет билета на паром.");
        m.put("misc.cannot-use-portal", "§cВы не можете зайти в портал, находясь в тюрьме!");
        m.put("misc.jail-time-served", "§aВы успешно отсидели свой срок и теперь свободны!");
        m.put("misc.jail-time-served-broadcast", "§7[Тюрьма] §eИгрок {player} освобождён из тюрьмы.");
        m.put("misc.capture-reward-message", "§aВы получили {amount} {swcoin} за поимку сбежавшего игрока {player}.");
        m.put("misc.capture-broadcast", "§7[Тюрьма] §cИгрок {killer} поймал сбежавшего игрока {victim}.");
        m.put("misc.work-reward-action-bar", "§aВаш срок уменьшен на {seconds} секунд.");
        m.put("misc.swcoin-reward", "§aВы получили {swcoin} за работу!");
        m.put("misc.configurator-given", "§aВы получили конфигуратор табло.");
        m.put("misc.cell-teleport", "§aВы телепортированы в камеру.");

        return m;
    }

    /**
     * Английские сообщения
     */
    private Map<String, String> getEnglishMessages() {
        Map<String, String> m = new HashMap<>();

        // ---------- GENERAL ----------
        m.put("general.no-permission", "&cInsufficient permissions.");
        m.put("general.player-not-found", "&cPlayer not found.");
        m.put("general.command-only-player", "&cThis command is only for players.");
        m.put("general.config-reloaded", "&aPlugin configuration reloaded.");
        m.put("general.unknown-command", "&cUnknown command. Use /jails for help.");
        m.put("general.positive-time", "§cTime must be positive.");

        // ---------- /JAILS HELP ----------
        m.put("jails.help.header", "&8»»━━━━━━━━━━━━§r &6JAIL INFORMER §8━━━━━━━━━━━━««");
        m.put("jails.help.main-info-title", "§a▸ MAIN INFORMATION ◂");
        m.put("jails.help.you-are-jailed", "§eYou are imprisoned. To get out, you need to:");
        m.put("jails.help.time-line-prefix", "§a • §eServe your sentence §c{time}§e (");
        m.put("jails.help.check-time-hover", "§6Check exact time");
        m.put("jails.help.reduce-time-ways", "§a • §eDo jobs to reduce time");
        m.put("jails.help.escape-risky", "§a • §eTry to escape (risky!)");
        m.put("jails.help.reduce-ways-title", "§a▸ WAYS TO REDUCE SENTENCE:");
        m.put("jails.help.work-reward-template", "§7 • §eJobs: §a-{seconds} sec §eper task");
        m.put("jails.help.afk-slower-text-prefix", "§7 • §eWhile AFK, jail time goes slower! (");
        m.put("jails.help.afk-hover", "§6More about AFK rules");
        m.put("jails.help.check-time-click", "§eTo check your remaining time, use: ");
        m.put("jails.help.check-time-hover-long", "§6Click to insert command in chat\n§7Command won't be sent automatically");
        m.put("jails.help.afk-multiplier-template", "§eIn AFK mode time goes §c{multiplier} times slower§e! ({command})");
        m.put("jails.help.rules-hover", "§eImportant rules and penalties\n§7AFK, capture, time doubling\n§fCheck time: §e/jtime");
        m.put("jails.help.footer", "§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««");
        m.put("jails.help.clickable.works", "JOBS");
        m.put("jails.help.clickable.escape", "ESCAPE");
        m.put("jails.help.clickable.rules", "RULES");
        m.put("jails.help.works-hover", "§e3 types of jobs to reduce sentence\n§7(Bricks, Laundry, Kitchen)\n§fReward: §a-{seconds} seconds §fper task");
        m.put("jails.help.escape-hover", "§eHow to escape prison\n§7Lockpick, armor, ticket\n§c⚠ Risk: §fif caught sentence ×{multiplier}");

        // ---------- WORK DETAILS ----------
        m.put("jails.work-details.title", "§8»»━━━━━━━━━━━━§r §6JOB DETAILS §8━━━━━━━━━━━━««");
        m.put("jails.work-details.bricks.name", "§a1. §6Brick work:");
        m.put("jails.work-details.bricks.desc", "§7 • §eCarry bricks from warehouse to construction site");
        m.put("jails.work-details.bricks.reward", "§7 • §fReward: §a-{seconds} sec §fper delivery");
        m.put("jails.work-details.bricks.extra", "§7 • §fChance to find: §9Lockpick §f(for escape)");
        m.put("jails.work-details.laundry.name", "§a2. §6Laundry work:");
        m.put("jails.work-details.laundry.desc", "§7 • §eWashing and sorting dirty clothes");
        m.put("jails.work-details.laundry.reward", "§7 • §fReward: §a-{seconds} sec §fper load");
        m.put("jails.work-details.laundry.extra", "§7 • §fChance to find: §9Guard armor §f(part of set)");
        m.put("jails.work-details.kitchen.name", "§a3. §6Kitchen work:");
        m.put("jails.work-details.kitchen.desc", "§7 • §eCooking and serving food");
        m.put("jails.work-details.kitchen.reward", "§7 • §fReward: §a-{seconds} sec §fper dish");
        m.put("jails.work-details.kitchen.extra", "§7 • §fEarnings: §e{swcoin} §f(for ticket)");

        // ---------- ESCAPE DETAILS ----------
        m.put("jails.escape-details.title", "§8»»━━━━━━━━━━━━§r §6ESCAPE DETAILS §8━━━━━━━━━━━━««");
        m.put("jails.escape-details.requirements", "§eTo successfully escape, you need:");
        m.put("jails.escape-details.lockpick", "§a1. §6Find a lockpick §7(brick work)\n§7 • Use it to break doors");
        m.put("jails.escape-details.armor", "§a2. §6Full guard armor set §7(laundry)\n§7 • Helmet, chestplate, leggings, boots\n§7 • Disguises you from guards");
        m.put("jails.escape-details.ticket", "§a3. §6Ferry ticket\n§7 • Buy at black market for §e{swcoin}\n§7 • Command: §e/buypassport");
        m.put("jails.escape-details.destination", "§a4. §6Get to the escape point\n§7 • Use the escape item\n§7 • Teleport to freedom!");
        m.put("jails.escape-details.warning-title", "§c⚠ WARNING:");
        m.put("jails.escape-details.warning-time", "§7 • You have §e{minutes} minutes§7 to escape");
        m.put("jails.escape-details.warning-caught", "§7 • If caught: §csentence ×{multiplier}");
        m.put("jails.escape-details.warning-reward", "§7 • Killing an escaped prisoner gives §e{swcoin}");

        // ---------- RULES ----------
        m.put("jails.rules-details.title", "§8»»━━━━━━━━━━━━§r §6PRISON RULES §8━━━━━━━━━━━━««");
        m.put("jails.rules-details.basic.header", "§a📋 Basic rules:");
        m.put("jails.rules-details.basic.gamemode", "§7 • Game mode: §eADVENTURE");
        m.put("jails.rules-details.basic.teleport", "§7 • Teleportation: §cdisabled");
        m.put("jails.rules-details.basic.portal", "§7 • Portal: §cdisabled");
        m.put("jails.rules-details.time.header", "§a⏱ Prison time:");
        m.put("jails.rules-details.time.normal", "§7 • Normal mode: §e1 second/tick");
        m.put("jails.rules-details.time.afk", "§7 • AFK mode: §e1 second/{multiplier} ticks §7({multiplier} times slower)");
        m.put("jails.rules-details.time.afk-disabled", "§7 • AFK mode: §cdisabled");
        m.put("jails.rules-details.time.check", "§7 • Check time: §e/jtime");
        m.put("jails.rules-details.penalties.header", "§a⚖ Penalties and rewards:");
        m.put("jails.rules-details.penalties.work", "§7 • Successful job: §a-{seconds} seconds");
        m.put("jails.rules-details.penalties.escape-fail", "§7 • Failed escape: §csentence ×{multiplier}");
        m.put("jails.rules-details.penalties.capture", "§7 • Capture escaped prisoner: §e{swcoin}");
        m.put("jails.rules-details.afk-info.header", "§a🎯 AFK status:");
        m.put("jails.rules-details.afk-info.desc", "§7 • Detected by lack of movement");
        m.put("jails.rules-details.afk-info.timeout", "§7 • Timeout: §e{seconds} seconds");
        m.put("jails.rules-details.afk-info.auto", "§7 • Automatic switching");
        m.put("jails.rules-details.back-button", "§7[§a← Back§7]");
        m.put("jails.rules-details.back-hover", "§eReturn to main menu");

        // ---------- /JTIME ----------
        m.put("jtime.self", "§eRemaining time: {minutes} min {seconds} sec.");
        m.put("jtime.other", "§ePlayer {player}'s time: {minutes} min {seconds} sec.");
        m.put("jtime.not-jailed", "§cYou are not in prison.");
        m.put("jtime.other-not-jailed", "§cPlayer {player} is not in prison.");
        m.put("jtime.target-notified-add", "§eYour sentence has been increased by {seconds} sec. Now: {time}");
        m.put("jtime.target-notified-remove", "§eYour sentence has been reduced by {seconds} sec. Now: {time}");
        m.put("jtime.target-notified-set", "§eYour sentence has been set to {time}");
        m.put("jtime.admin.usage-add", "§cUsage: /jtime add <player> <seconds>");
        m.put("jtime.admin.usage-remove", "§cUsage: /jtime remove <player> <seconds>");
        m.put("jtime.admin.usage-set", "§cUsage: /jtime set <player> <seconds>");
        m.put("jtime.admin.seconds-number", "§cSeconds must be a number.");
        m.put("jtime.admin.add-success", "§aAdded {seconds} sec to player {player}. New time: {time}");
        m.put("jtime.admin.remove-success", "§aRemoved {seconds} sec from player {player}. New time: {time}");
        m.put("jtime.admin.set-success", "§aSet time {time} for player {player}");

        // ---------- /JAIL ----------
        m.put("jail.usage", "§cUsage: /jail <player> <minutes> <cell> <reason...>");
        m.put("jail.already-jailed", "§cThis player is already in prison.");
        m.put("jail.cell-not-found", "§cCell with that name not found.");
        m.put("jail.world-not-found", "§cWorld not found.");
        m.put("jail.broadcast-jailed", "§7[Prison] §ePlayer {player} has been jailed for {minutes} min.");
        m.put("jail.positive-time", "§cTime must be positive.");
        m.put("jail.minutes-number", "§cMinutes must be a number.");

        // ---------- /UNJAIL ----------
        m.put("unjail.usage", "§cUsage: /unjail <player>");
        m.put("unjail.not-jailed", "§cPlayer is not in prison.");
        m.put("unjail.broadcast-released", "§7[Prison] §ePlayer {player} has been released from prison.");
        m.put("unjail.inventory-not-found", "§cPlayer inventory not found.");

        // ---------- /JAILSTATUS ----------
        m.put("jailstatus.empty", "§cNo imprisoned players.");
        m.put("jailstatus.header", "§eList of imprisoned players:");
        m.put("jailstatus.entry", "§6 {player} | Cell: {cell} | Remaining time: {minutes} min {seconds} sec.");

        // ---------- /CELL ----------
        m.put("cell.help.0", "§6=== Cell Management ===");
        m.put("cell.help.1", "§e/cell create <name> §7- create cell at current position (player only)");
        m.put("cell.help.2", "§e/cell delete <name> §7- delete cell");
        m.put("cell.help.3", "§e/cell rename <old> <new> §7- rename cell");
        m.put("cell.create.usage", "§cUsage: /cell create <name>");
        m.put("cell.create.success", "§aCell {name} created.");
        m.put("cell.delete.usage", "§cUsage: /cell delete <name>");
        m.put("cell.delete.not-found", "§cCell not found.");
        m.put("cell.delete.success", "§aCell {name} deleted.");
        m.put("cell.delete.has-prisoners", "§cCannot delete cell while it has prisoners!");
        m.put("cell.rename.usage", "§cUsage: /cell rename <old> <new>");
        m.put("cell.rename.not-found", "§cCell named {old} not found.");
        m.put("cell.rename.already-exists", "§cCell named {new} already exists.");
        m.put("cell.rename.success", "§aCell {old} renamed to {new}.");
        m.put("cell.rename.has-prisoners", "§cCannot rename cell while it has prisoners!");

        // ---------- /BUYPASSPORT ----------
        m.put("buypassport.disabled", "§cBlack market is disabled.");
        m.put("buypassport.not-in-market", "&cYou must be in the black market area to buy a ticket!");
        m.put("buypassport.already-have", "&cYou already have a ticket!");
        m.put("buypassport.not-enough-money", "&cYou don't have enough {swcoin} to buy a ticket!");
        m.put("buypassport.success", "&aYou successfully bought a ferry ticket for {price} {swcoin}!");

        // ---------- /GIVEPICK ----------
        m.put("givepick.usage", "§cUsage: /givepick <player>");
        m.put("givepick.success", "§aLockpick given to player {player}");
        m.put("givepick.fail", "§cFailed to create lockpick (check config)");

        // ---------- /GIVEARMOUR ----------
        m.put("givearmour.usage", "§cUsage: /givearmour <player>");
        m.put("givearmour.success", "§aGuard armor given to player {player}");

        // ---------- /GIVETABLE ----------
        m.put("givetable.usage", "§cUsage: /givetable <player>");
        m.put("givetable.success", "§aEscape item given to player {player}");

        // ---------- /GIVEMARKETMAP ----------
        m.put("givemarketmap.usage", "§cUsage: /givemarketmap <player>");
        m.put("givemarketmap.success", "§aTreasure map given to player {player}");
        m.put("givemarketmap.not-configured", "§cBlack market is not configured, map not created.");
        m.put("givemarketmap.map-error", "§cMap settings not found in config.");

        // ---------- /JAILTOOLS ----------
        m.put("jailtools.usage", "§cUsage: /jailtools [give <type>]");
        m.put("jailtools.invalid-type", "§cUnknown type. Available: {types}");
        m.put("jailtools.gave-items", "§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««\n§6⚒ You received a work kit: §e{type}\n§8»»━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━««");
        m.put("jailtools.trader-egg-given", "§aYou received a black market trader spawn egg.");
        m.put("jailtools.trader-egg-help", "§7Right-click on a block in the black market area to summon the trader.");
        m.put("jailtools.trader-egg-fail", "§cFailed to create spawn egg. Check config.");
        m.put("jailtools.library-error", "§cError: no library categories in config.");

        // ---------- /JAILBOARD ----------
        m.put("jailboard.help.0", "§6=== Board Management ===");
        m.put("jailboard.help.1", "§e/jailboard create §7- create new board (selection mode)");
        m.put("jailboard.help.2", "§e/jailboard confirm §7- confirm board creation");
        m.put("jailboard.help.3", "§e/jailboard cancel §7- cancel board creation");
        m.put("jailboard.help.4", "§e/jailboard clear §7- clear current selection");
        m.put("jailboard.selection-enabled", "§aSign selection mode enabled!");
        m.put("jailboard.selection-instruction1", "§7Click on signs in the order they should appear on the board");
        m.put("jailboard.selection-instruction2", "§7When done, type §e/jailboard confirm");
        m.put("jailboard.selection-cancel", "§7or §c/jailboard cancel §7to cancel");
        m.put("jailboard.sign-added", "§aSign added to selection §7(now {count})");
        m.put("jailboard.sign-removed", "§cSign removed from selection §7(now {count})");
        m.put("jailboard.confirm-no-selection", "§cYou haven't selected any signs!");
        m.put("jailboard.confirm-different-worlds", "§cAll signs must be in the same world!");
        m.put("jailboard.confirm-success", "§a✅ Board successfully created from {count} signs!");
        m.put("jailboard.cancel", "§eBoard creation cancelled");
        m.put("jailboard.clear", "§eSelection cleared");
        m.put("jailboard.need-create-first", "§cFirst enable selection mode with §e/jailboard create");
        m.put("jailboard.cannot-edit-board-sign", "§cCannot edit a sign that is part of a board!");

        // ---------- /JAILHISTORY ----------
        m.put("jailhistory.no-history", "§cPlayer {player} has no punishment history.");
        m.put("jailhistory.header", "§6=== Punishment history of player {player} ===");
        m.put("jailhistory.page-info", "§7Page {page} of {total} (total entries: {totalEntries})");
        m.put("jailhistory.entry-line", "§e{number}. §7[{date}] §a{initiator} §7jailed player §c{player} §7for §c{minutes} min §7in §b{cell}");
        m.put("jailhistory.reason-line", "§7 Reason: §f{reason}");
        m.put("jailhistory.navigation-prev", "§7[←] ");
        m.put("jailhistory.navigation-prev-hover", "§7Previous page");
        m.put("jailhistory.navigation-page", "§7[ §e{page}/{total} §7] ");
        m.put("jailhistory.navigation-next", "§7[→]");
        m.put("jailhistory.navigation-next-hover", "§7Next page");

        // ---------- ESCAPE ----------
        m.put("escape.start", "&aYou escaped from prison! You have &e{time} minutes&a to reach the escape point and activate it!");
        m.put("escape.passport-missing", "&cYou don't have a ferry ticket! Buy it from the black market trader for {swcoin}.");
        m.put("escape.armor-missing", "&cTo escape you need a full set of guard armor!");
        m.put("escape.area-found", "&aYou found the escape point! Use &6{interact-item}&a to escape.");
        m.put("escape.success", "&aCongratulations! You successfully escaped from prison!");
        m.put("escape.fail", "&cYou didn't escape in time and returned to prison. Sentence increased {multiplier} times!");
        m.put("escape.caught", "&cYou were caught trying to escape! Sentence increased {multiplier} times!");
        m.put("escape.broadcast-escape", "&7[Prison] &ePlayer {player} escaped from prison!");
        m.put("escape.broadcast-caught", "&7[Prison] &cPlayer {player} was caught trying to escape!");
        m.put("escape.broadcast-location", "&7[Prison] &eEscaped prisoner {player} was spotted at X: {x}, Y: {y}, Z: {z}.");
        m.put("escape.escape-ship-title", "§bDeparting...");
        m.put("escape.escape-ship-mid", "After several days on the open sea...");
        m.put("escape.escape-ship-final", "you finally reached the shore.");
        m.put("escape.default-item-name", "item");
        m.put("escape.zone-loaded", "Loaded escape zone: {name}");

        // ---------- WORK ----------
        m.put("work.general.cant-drop", "§cYou cannot drop this item.");
        m.put("work.general.washing-in-progress", "§cWashing is not finished yet, you cannot touch the clothes!");
        m.put("work.general.inventory-full-drop", "§cYour inventory is full, item dropped on the ground.");
        m.put("work.general.swcoin-reward", "§aYou received {swcoin} for work!");
        
        m.put("work.brick.take", "You took the bricks, take them to the drop-off point.");
        m.put("work.brick.already-has", "You already have a Wall Piece.");
        m.put("work.brick.put", "You don't have bricks to deliver.");
        m.put("work.brick.reward-action-bar", "§aYour sentence reduced by {seconds} seconds.");
        m.put("work.brick.lockpick-found", "§6You're lucky! You found a Wooden Lockpick.");
        
        m.put("work.laundry.take", "You took a dirty prisoner outfit.");
        m.put("work.laundry.already-has", "You already have a dirty outfit. Wash it first.");
        m.put("work.laundry.put", "You don't have a clean outfit to deliver.");
        m.put("work.laundry.guard-armor-found", "§6You found {piece}!");
        
        m.put("work.kitchen.take", "§eYou received ingredients for §a{dish}§e. Cook and deliver within §c{seconds} seconds§e to reduce your sentence.");
        m.put("work.kitchen.already-task", "Finish your current task first.");
        m.put("work.kitchen.put-no-task", "You don't have an active task to deliver.");
        m.put("work.kitchen.put-no-dish", "You don't have ready {dish} to deliver.");
        m.put("work.kitchen.recipe-error", "§cError: recipe not found.");
        m.put("work.kitchen.task-failed", "§cYou failed to cook {dish} in time. Task failed.");
        m.put("work.kitchen.action-bar-time", "§aTime left: {seconds} sec");
        m.put("work.kitchen.task-failed-action-bar", "§cTask failed!");
        m.put("work.kitchen.no-recipes", "§cRecipes temporarily unavailable.");
        
        m.put("work.library.already-has-book", "§cYou already took a book. Put it on the shelf first.");
        m.put("work.library.no-categories", "§cLibrary temporarily unavailable.");
        m.put("work.library.no-books", "§cNo books available.");
        m.put("work.library.book-taken", "§eYou took a book: §a{book}");
        m.put("work.library.no-shelves", "§cNo shelves installed for this book!");
        m.put("work.library.no-shelves-for-category", "§cNo shelves installed for category {category}");
        m.put("work.library.shelf-instruction", "§ePut this book on a shelf with the appropriate section ({categories}).");
        m.put("work.library.no-task", "§cYou don't have a book to work with! Get a book from the dispenser.");
        m.put("work.library.wrong-shelf", "§cThis shelf is not suitable for this book.");
        m.put("work.library.not-glow-shelf", "§cYou must put the book on the highlighted shelf!");
        m.put("work.library.no-book-in-hand", "§cYou must hold the book in your hand to put it on the shelf!");
        m.put("work.library.wrong-book", "§cError: book does not match your task!");
        m.put("work.library.shelf-full", "§cThis shelf has no free space!");
        m.put("work.library.success", "§aBook successfully placed on the shelf!");
        m.put("work.library.fragment-found", "§aYou found a map fragment!");

        // ---------- PVO ----------
        m.put("pvo.warning", "§ePVO: You entered a restricted area. Leave immediately or missiles will be launched in {seconds} seconds!");
        m.put("pvo.warning-title", "§cAlert!");
        m.put("pvo.warning-subtitle", "§eLeave the area!");
        m.put("pvo.death-message", "§cPlayer {player} was destroyed by the air defense system!");

        // ---------- LOCKPICK ----------
        m.put("lockpick.already-attempt", "§cYou are already trying to pick the prison lock!");
        m.put("lockpick.cooldown", "§cThe lock hasn't cooled down yet. Wait {seconds} seconds.");
        m.put("lockpick.need-lockpick", "§cYou need a lockpick to pick the prison door!");
        m.put("lockpick.start-title", "§cLockpicking");
        m.put("lockpick.start-subtitle", "§ePress SHIFT at the right moment");
        m.put("lockpick.round-success-title", "§aClick");
        m.put("lockpick.round-success-subtitle", "§7pin gave way");
        m.put("lockpick.round-fail-title", "§cFailure");
        m.put("lockpick.round-fail-subtitle", "§cThe lock jammed, try again!");
        m.put("lockpick.complete-title", "§aLock opened!");
        m.put("lockpick.complete-subtitle", "§aAll pins are in place!");
        m.put("lockpick.break-message", "§8[§c✘§8] §cThe lockpick broke! You'll have to find a new one.");
        m.put("lockpick.durability-warning", "§8[§c!§8] §7The lockpick is thinning...");

        // ---------- ITEMS ----------
        m.put("items.brick-piece", "§7Wall Piece");
        m.put("items.dirty-clothes", "§7Dirty Prisoner Outfit");
        m.put("items.clean-clothes", "§7Clean Prisoner Outfit");
        m.put("items.lockpick", "§6Wooden Lockpick");
        m.put("items.lockpick-lore", "§7can break iron doors");
        m.put("items.swcoin", "&6SWcoin");
        m.put("items.swcoin-lore", "&dPrison currency");
        m.put("items.escape-ticket", "&6Ferry Ticket");
        m.put("items.escape-ticket-lore.1", "&7One-time ticket for crossing");
        m.put("items.escape-ticket-lore.2", "&7across the sea. Use at the escape point.");
        m.put("items.trader-spawn-egg", "&6Trader Spawn Egg");
        m.put("items.trader-spawn-egg-lore.1", "&7Right-click on a block");
        m.put("items.trader-spawn-egg-lore.2", "&7to summon the trader");
        m.put("items.market-map", "&6Black Market Map");
        m.put("items.market-map-lore.1", "&7Use this map to find the black market entrance.");
        m.put("items.market-map-lore.2", "&eThere you can buy a ferry ticket.");
        m.put("items.map-fragment", "§7Map Fragment");
        m.put("items.configurator-stick", "§6Board Configurator");
        m.put("items.configurator-lore.1", "§7Right-click on a sign");
        m.put("items.configurator-lore.2", "§7to create a prisoner board.");
        m.put("items.prisoner-chestplate", "§7Prisoner Chestplate");
        m.put("items.prisoner-leggings", "§7Prisoner Leggings");
        m.put("items.prisoner-boots", "§7Prisoner Boots");
        m.put("items.guard-armor.helmet", "§7Guard Helmet");
        m.put("items.guard-armor.chestplate", "§7Guard Chestplate");
        m.put("items.guard-armor.leggings", "§7Guard Leggings");
        m.put("items.guard-armor.boots", "§7Guard Boots");

        // ---------- GUI ----------
        m.put("work-gui.title", "§8⚒ §7Select work type §8⚒");
        m.put("work-gui.close-button", "§cClose");
        m.put("work-gui.work-type.bricks", "§6Brick Work");
        m.put("work-gui.work-type.laundry", "§6Laundry");
        m.put("work-gui.work-type.kitchen", "§6Kitchen");
        m.put("work-gui.work-type.library", "§6Library");
        m.put("work-gui.work-type.trader", "§6Summon Trader");
        m.put("work-gui.info-item.name", "§e§lWork Information");
        m.put("work-gui.info-item.lore.0", "§7━━━━━━━━━━━━━━━━━━");
        m.put("work-gui.info-item.lore.1", "§fBricks: §a-{brick_seconds} sec §7+ §6lockpick");
        m.put("work-gui.info-item.lore.2", "§fLaundry: §a-{laundry_seconds} sec §7+ §barmor");
        m.put("work-gui.info-item.lore.3", "§fKitchen: §a-{kitchen_seconds} sec §7+ §eSWcoin");
        m.put("work-gui.info-item.lore.4", "§fLibrary: §a+ map fragments §7→ §6market map");
        m.put("work-gui.info-item.lore.5", "§fTrader: §aSpawn egg §7→ §6black market trader");
        m.put("work-gui.info-item.lore.6", "§7━━━━━━━━━━━━━━━━━━");
        m.put("work-gui.info-item.lore.7", "§7Place blocks in the right places");
        m.put("work-gui.info-item.lore.8", "§7and they will configure automatically!");
        m.put("work-gui.info-item.lore.click-to-get", "§eClick to get the kit!");
        m.put("work-gui.library.book-dispenser", "§6Book Dispenser");
        m.put("work-gui.library.bookshelf-prefix", "§6Bookshelf: ");
        m.put("work-gui.library.cartography-table", "§6Cartography Table");
        m.put("work-gui.laundry.washing-machine", "§6Washing Machine");
        m.put("work-gui.laundry.washing-machine-alt", "Washing Machine");

        // ---------- AFK ----------
        m.put("afk.on", "&eYou are now AFK. Your prison time is going slower.");
        m.put("afk.off", "&eYou are no longer AFK. Your prison time is now normal.");

        // ---------- BLACK MARKET ----------
        m.put("blackmarket.spawn-trader-not-in-zone", "§cThe trader can only be summoned in the black market area!");
        m.put("blackmarket.spawn-success", "§aYou summoned the black market trader!");
        m.put("blackmarket.remove-success", "§cYou removed the black market trader!");
        m.put("blackmarket.trader-works-only-in-market", "§cThe trader only works in the black market area!");
        m.put("blackmarket.map-creation-error", "§cAn error occurred while creating the map.");
        m.put("blackmarket.map-settings-not-found", "§cMap settings not found in config.");
        m.put("blackmarket.unknown-map-scale", "Unknown map scale: {scale}, using FAR");

        // ---------- MISC ----------
        m.put("misc.sign-escape-line1", "§6Sail away");
        m.put("misc.sign-escape-line2", "§a[click]");
        m.put("misc.sign-escape-line3", "§a[to escape]");
        m.put("misc.not-prisoner", "§cYou are not a prisoner.");
        m.put("misc.escape-no-ticket", "§cYou don't have a ferry ticket.");
        m.put("misc.cannot-use-portal", "§cYou cannot enter a portal while in prison!");
        m.put("misc.jail-time-served", "§aYou have served your sentence and are now free!");
        m.put("misc.jail-time-served-broadcast", "§7[Prison] §ePlayer {player} has been released from prison.");
        m.put("misc.capture-reward-message", "§aYou received {amount} {swcoin} for capturing escaped player {player}.");
        m.put("misc.capture-broadcast", "§7[Prison] §cPlayer {killer} captured escaped player {victim}.");
        m.put("misc.work-reward-action-bar", "§aYour sentence reduced by {seconds} seconds.");
        m.put("misc.swcoin-reward", "§aYou received {swcoin} for work!");
        m.put("misc.configurator-given", "§aYou received the board configurator.");
        m.put("misc.cell-teleport", "§aYou have been teleported to your cell.");

        return m;
    }
}