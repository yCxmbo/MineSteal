package me.ycxmbo.minesteal.config;

public final class ConfigKeys {
    public static final String HP_PER_HEART = "health_points_per_heart";
    public static final String MIN_HEARTS = "min_hearts";
    public static final String MAX_HEARTS = "max_hearts";
    public static final String GAIN_ON_KILL = "hearts_gained_on_kill";
    public static final String LOSE_ON_DEATH = "hearts_lost_on_death";
    public static final String DROP_ON_KILL = "drop_heart_item_on_kill";

    public static final String DEATHBAN_ENABLED = "deathban.enabled";
    public static final String DEATHBAN_MODE = "deathban.mode";
    public static final String DEATHBAN_MINUTES = "deathban.minutes";

    public static final String HEART_ITEM_NAME = "heart_item.name";
    public static final String HEART_ITEM_LORE = "heart_item.lore";
    public static final String HEART_ITEM_REFUSE_AT_CAP = "heart_item.refuse_if_at_cap";

    public static final String SHARD_ENABLED = "heart_shard.enabled";
    public static final String SHARD_NAME = "heart_shard.name";
    public static final String SHARD_LORE = "heart_shard.lore";
    public static final String SHARD_RECIPE_ENABLED = "heart_shard.craft_heart_from_9_shards";
    public static final String SHARD_RECIPE_TYPE = "heart_shard.recipe_type";

    public static final String PVE_ENABLED = "pve_drops.enabled";
    public static final String PVE_ENTITIES = "pve_drops.entities";

    public static final String CD_CONSUME = "cooldowns.consume";
    public static final String CD_WITHDRAW = "cooldowns.withdraw";
    public static final String CD_REQUEST_REVIVE = "cooldowns.request_revive";

    public static final String LB_PAGE_SIZE = "leaderboard.page_size";
    public static final String LB_INCLUDE_OFFLINE = "leaderboard.include_offline";
    public static final String LB_ONLINE_WORLD_ONLY = "leaderboard.online_world_only";

    public static final String MSG_PREFIX = "messages.prefix";
    public static final String MSG_SELF_HEARTS = "messages.self_hearts";
    public static final String MSG_AT_CAP = "messages.at_cap";
    public static final String MSG_CANNOT_CONSUME = "messages.cannot_consume";
    public static final String MSG_RELOADED = "messages.reloaded";
    public static final String MSG_CD_CONSUME = "messages.cooldown_consume";
    public static final String MSG_CD_WITHDRAW = "messages.cooldown_withdraw";
    public static final String MSG_CD_REQ_REVIVE = "messages.cooldown_request_revive";
    public static final String MSG_TOP_HEADER = "messages.top_header";
    public static final String MSG_TOP_LINE = "messages.top_line";
    public static final String MSG_TOP_EMPTY = "messages.top_empty";

    private ConfigKeys() {}
}
