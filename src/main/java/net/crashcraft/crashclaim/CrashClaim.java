package net.crashcraft.crashclaim;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.whip.crashutils.CrashUtils;
import dev.whip.crashutils.menusystem.GUI;
import io.papermc.lib.PaperLib;
import net.crashcraft.crashclaim.migration.MigrationManager;
import net.crashcraft.crashpayment.CrashPayment;
import net.crashcraft.crashpayment.payment.PaymentProcessor;
import net.crashcraft.crashclaim.api.CrashClaimAPI;
import net.crashcraft.crashclaim.commands.*;
import net.crashcraft.crashclaim.config.ConfigManager;
import net.crashcraft.crashclaim.data.ClaimDataManager;
import net.crashcraft.crashclaim.data.MaterialName;
import net.crashcraft.crashclaim.listeners.PaperListener;
import net.crashcraft.crashclaim.listeners.PlayerListener;
import net.crashcraft.crashclaim.listeners.WorldListener;
import net.crashcraft.crashclaim.permissions.PermissionHelper;
import net.crashcraft.crashclaim.visualize.VisualizationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class CrashClaim extends JavaPlugin {
    private static CrashClaim plugin;

    private boolean dataLoaded = false;

    private CrashClaimAPI api;

    private ClaimDataManager manager;
    private VisualizationManager visualizationManager;
    private ProtocolManager protocolManager;
    private CrashUtils crashUtils;
    private MaterialName materialName;
    private PaymentProcessor payment;
    private CrashPayment paymentPlugin;
    private CommandManager commandManager;
    private MigrationManager migrationManager;

    @Override
    public void onLoad() {
        plugin = this;

        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.paymentPlugin = (CrashPayment) Bukkit.getPluginManager().getPlugin("CrashPayment");

        if (paymentPlugin == null){
            disablePlugin("Payment plugin not found, disabling plugin");
        }

        this.crashUtils = new CrashUtils(this);

        this.api = new CrashClaimAPI();
    }

    @Override
    public void onEnable() {
        taskChainFactory = BukkitTaskChainFactory.create(this);

        if (getDataFolder().mkdirs()){
            getLogger().info("Created data directory");
        }

        new ConfigManager(this);

        crashUtils.setupMenuSubSystem();
        crashUtils.setupTextureCache();

        if (paymentPlugin != null) {
            payment = paymentPlugin.setupPaymentProvider(this).getProcessor();
        }

        this.visualizationManager = new VisualizationManager(this, protocolManager);
        this.manager = new ClaimDataManager(this);
        this.materialName = new MaterialName();

        this.dataLoaded = true;

        new PermissionHelper(manager);

        this.migrationManager = new MigrationManager(this);

        Bukkit.getPluginManager().registerEvents(new WorldListener(manager, visualizationManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(manager, visualizationManager), this);

        if (PaperLib.isPaper()){
            getLogger().info("Using extra protections provided by the paper api");
            Bukkit.getPluginManager().registerEvents(new PaperListener(manager, visualizationManager), this);
        } else {
            getLogger().info("Looks like your not running paper, some protections will be disabled.");
            PaperLib.suggestPaper(this);
        }

        commandManager = new CommandManager(this);
    }

    @Override
    public void onDisable() {
        if (dataLoaded) {
            manager.saveClaimsSync();
        }

        manager.cleanupAndClose(); // freezes claim saving and cleans up memory references to claims

        //Unregister all user facing things
        HandlerList.unregisterAll(this);
        commandManager.getCommandManager().unregisterCommands();
        for (Player player : Bukkit.getOnlinePlayers()){
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUI){
                player.closeInventory();
            }
        }

        //Null all references just to be sure, manager will still hold them but this stops this class from being referenced for anything
        dataLoaded = false;
        plugin = null;
        api = null;
        manager = null;
        visualizationManager = null;
        protocolManager = null;
        crashUtils = null;
        materialName = null;
        payment = null;
        paymentPlugin = null;
        commandManager = null;
        migrationManager = null;
    }

    public void disablePlugin(String error){
        getLogger().severe(error);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    private static TaskChainFactory taskChainFactory;
    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }
    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }

    public static CrashClaim getPlugin() {
        return plugin;
    }

    public ClaimDataManager getDataManager() {
        return manager;
    }

    public VisualizationManager getVisualizationManager() {
        return visualizationManager;
    }

    public CrashUtils getCrashUtils() {
        return crashUtils;
    }

    public PaymentProcessor getPayment() {
        return payment;
    }

    public MaterialName getMaterialName() {
        return materialName;
    }

    public CrashClaimAPI getApi() {
        return api;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public MigrationManager getMigrationManager() {
        return migrationManager;
    }
}