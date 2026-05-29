package me.ycxmbo.minesteal.config;

/** Constant config path keys used across the plugin. */
public final class ConfigKeys {

    private ConfigKeys() {}

    public static final String HP_PER_HEART     = "settings.hp-per-heart";
    public static final String MAX_HEARTS        = "settings.max-hearts";
    public static final String MIN_HEARTS        = "settings.minimum-hearts";
    public static final String STARTING_HEARTS   = "settings.starting-hearts";
    public static final String HEARTS_ON_KILL    = "settings.hearts-on-kill";
    public static final String HEARTS_ON_DEATH   = "settings.hearts-on-death";
    public static final String DEBUG             = "settings.debug";

    public static final String DB_TYPE           = "database.type";
    public static final String DB_SQLITE_FILE    = "database.sqlite-file";
    public static final String DB_MYSQL_HOST     = "database.mysql.host";
    public static final String DB_MYSQL_PORT     = "database.mysql.port";
    public static final String DB_MYSQL_DATABASE = "database.mysql.database";
    public static final String DB_MYSQL_USER     = "database.mysql.username";
    public static final String DB_MYSQL_PASS     = "database.mysql.password";

    public static final String DEATHBAN_ENABLED  = "death.deathban.enabled";
    public static final String DEATHBAN_MODE     = "death.deathban.mode";
    public static final String DEATHBAN_REASON   = "death.deathban.reason";
    public static final String BAN_COMMAND       = "death.deathban.ban-command";
    public static final String UNBAN_COMMAND     = "death.deathban.unban-command";
    public static final String DROP_ON_DEATH     = "death.drop-hearts-on-death";
    public static final String AUTO_TRANSFER     = "death.auto-transfer";
    public static final String BROADCAST_KILL    = "death.broadcast-kill";

    public static final String COMBAT_LOG_ENABLED = "combat-log.enabled";
    public static final String COMBAT_DURATION    = "combat-log.combat-duration";

    public static final String BOUNTY_ENABLED    = "bounty.enabled";
    public static final String BOUNTY_MIN        = "bounty.min-amount";
    public static final String BOUNTY_MAX        = "bounty.max-amount";

    public static final String CD_CONSUME        = "cooldowns.consume";
    public static final String CD_WITHDRAW       = "cooldowns.withdraw";
    public static final String CD_GIVE           = "cooldowns.give";
    public static final String CD_BOUNTY         = "cooldowns.bounty-place";

    public static final String LB_PAGE_SIZE      = "leaderboard.page-size";
    public static final String LB_INCLUDE_OFFLINE= "leaderboard.include-offline";
}
