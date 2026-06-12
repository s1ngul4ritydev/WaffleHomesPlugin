package dev.wafflycatt.wafflehomes;

import dev.wafflycatt.wafflehomes.command.*;
import dev.wafflycatt.wafflehomes.command.admin.*;
import dev.wafflycatt.wafflehomes.config.ConfigManager;
import dev.wafflycatt.wafflehomes.config.MessageManager;
import dev.wafflycatt.wafflehomes.database.DatabaseManager;
import dev.wafflycatt.wafflehomes.listener.ChatInputListener;
import dev.wafflycatt.wafflehomes.listener.InventoryClickListener;
import dev.wafflycatt.wafflehomes.listener.PlayerQuitListener;
import dev.wafflycatt.wafflehomes.manager.HomeManager;
import dev.wafflycatt.wafflehomes.manager.RtpManager;
import dev.wafflycatt.wafflehomes.manager.TeleportCountdownManager;
import dev.wafflycatt.wafflehomes.manager.TpaManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * WaffleHomes — main plugin entry point.
 *
 * Systems:
 *  - Home: /sethome, /home, /homes, /delhome (SQLite backed, configurable limit)
 *  - RTP:  /rtp (configurable radius + cooldown, safe surface detection,
 *                nearby-player check, 8-second warmup)
 *  - TPA:  /tpa, /tpaccept, /tpdeny, /tphere, /tphereaccept, /tpheredeny, /tpsettings
 *          (all with 8-second stand-still warmup)
 *  - Admin: /adminhomes, /adminedithomes, /admindelhomes
 *
 * Bedrock/Geyser compatibility:
 *  - All menus use Floodgate Forms for Bedrock players automatically.
 *  - Java players use inventory GUIs.
 *  - Detection via FloodgateApi.isFloodgatePlayer().
 */
public final class WaffleHomes extends JavaPlugin {

    private static WaffleHomes instance;

    private ConfigManager            configManager;
    private MessageManager           messageManager;
    private DatabaseManager          databaseManager;
    private HomeManager              homeManager;
    private RtpManager               rtpManager;
    private TpaManager               tpaManager;
    private TeleportCountdownManager countdownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save defaults so users get a copy on first run
        saveDefaultConfig();

        configManager   = new ConfigManager(this);
        messageManager  = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        homeManager      = new HomeManager(this);
        rtpManager       = new RtpManager(this);
        tpaManager       = new TpaManager(this);
        countdownManager = new TeleportCountdownManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("WaffleHomes enabled successfully.");
        getLogger().info("Bedrock/Floodgate support: "
                + (getServer().getPluginManager().getPlugin("Floodgate") != null
                        ? "active" : "inactive (Floodgate not found)"));
    }

    @Override
    public void onDisable() {
        if (countdownManager != null) countdownManager.cancelAll();
        if (tpaManager       != null) tpaManager.cancelAll();
        if (databaseManager  != null) databaseManager.close();
        getLogger().info("WaffleHomes disabled.");
    }

    // ─── Command registration ──────────────────────────────────────────────────

    private void registerCommands() {
        getCommand("sethome"       ).setExecutor(new SetHomeCommand(this));
        getCommand("home"          ).setExecutor(new HomeCommand(this));
        getCommand("homes"         ).setExecutor(new HomesCommand(this));
        getCommand("delhome"       ).setExecutor(new DelHomeCommand(this));
        getCommand("rtp"           ).setExecutor(new RtpCommand(this));
        getCommand("tpa"           ).setExecutor(new TpaCommand(this));
        getCommand("tpaccept"      ).setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny"        ).setExecutor(new TpDenyCommand(this));
        getCommand("tphere"        ).setExecutor(new TpHereCommand(this));
        getCommand("tphereaccept"  ).setExecutor(new TpHereAcceptCommand(this));
        getCommand("tpheredeny"    ).setExecutor(new TpHereDenyCommand(this));
        getCommand("tpsettings"    ).setExecutor(new TpSettingsCommand(this));
        getCommand("adminhomes"    ).setExecutor(new AdminHomesCommand(this));
        getCommand("adminedithomes").setExecutor(new AdminEditHomesCommand(this));
        getCommand("admindelhomes" ).setExecutor(new AdminDelHomesCommand(this));
    }

    // ─── Listener registration ─────────────────────────────────────────────────

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryClickListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new ChatInputListener(), this);
    }

    // ─── Accessors ─────────────────────────────────────────────────────────────

    public static WaffleHomes            getInstance()        { return instance; }
    public ConfigManager                 getConfigManager()   { return configManager; }
    public MessageManager                getMessageManager()  { return messageManager; }
    public DatabaseManager               getDatabaseManager() { return databaseManager; }
    public HomeManager                   getHomeManager()     { return homeManager; }
    public RtpManager                    getRtpManager()      { return rtpManager; }
    public TpaManager                    getTpaManager()      { return tpaManager; }
    public TeleportCountdownManager      getCountdownManager(){ return countdownManager; }
}
