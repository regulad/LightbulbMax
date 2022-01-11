package xyz.regulad.lightbulbmax;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.lightbulbmax.command.NightVisionCommand;
import xyz.regulad.lightbulbmax.command.StsGiveCommand;
import xyz.regulad.lightbulbmax.command.VoidWorldSetCommand;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A template plugin to be used in Minecraft plugins.
 */
public class LightbulbMax extends JavaPlugin implements Listener {
    @Getter
    private static @Nullable LightbulbMax instance;

    private @Nullable HikariDataSource hikariDataSource;

    private boolean usePlayerSpawn;

    @Override
    public void onEnable() {
        // Setup instance access
        instance = this;

        // Load Config
        this.saveDefaultConfig();
        usePlayerSpawn = this.getConfig().getBoolean("use_player_spawn", false);

        // Setup bStats metrics
        new Metrics(this, 13761); // TODO: Replace this in your plugin!
        // Register commands

        final @Nullable PluginCommand nightVisionCommand = this.getCommand("nv");
        if (nightVisionCommand != null) {
            nightVisionCommand.setExecutor(new NightVisionCommand(this));
        }

        final @Nullable PluginCommand voidSetCommand = this.getCommand("voidset");
        if (voidSetCommand != null) {
            final @NotNull VoidWorldSetCommand voidWorldSetCommand = new VoidWorldSetCommand(this);
            // set object as executor & tab completer
            voidSetCommand.setExecutor(voidWorldSetCommand);
            voidSetCommand.setTabCompleter(voidWorldSetCommand);
        }

        final @Nullable PluginCommand stsGiveCommand = this.getCommand("stsgive");
        if (stsGiveCommand != null) {
            stsGiveCommand.setExecutor(new StsGiveCommand(this));
            // Bukkit uses a player tab completer by default.
        }

        // Events
        this.getServer().getPluginManager().registerEvents(this, this);

        // Setup SQLite

        this.getDataFolder().mkdir();

        final @NotNull File sqliteFile = new File(this.getDataFolder(), "db.sqlite");
        final @NotNull HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getPath());
        this.hikariDataSource = new HikariDataSource(hikariConfig);

        // Setup

        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lightbulbmax (uuid VARCHAR(36) PRIMARY KEY, nightvision INTEGER DEFAULT 0);")
                    .execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lightbulbworlds (world VARCHAR(255) PRIMARY KEY, voidsafety INTEGER DEFAULT 0);")
                    .execute();
        }  catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.")) {
                sqlException.printStackTrace();
            }
        }

        // NightVision task

        new NightVisionRunnable().runTaskTimerAsynchronously(this, 20, 20);
    }

    public class NightVisionRunnable extends BukkitRunnable {
        @Override
        public void run() {
            LightbulbMax.this.getServer()
                    .getOnlinePlayers()
                    .stream()
                    .filter(LightbulbMax.this::getNightVision)
                    .forEach(player -> LightbulbMax.this.getServer()
                            .getScheduler()
                            .getMainThreadExecutor(LightbulbMax.this)
                            .execute(() -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 15, 0))));
        }
    }

    /**
     * @param world The {@link World} the that is changing.
     * @param state A {@code boolean} representing the new state.
     */
    public void changeVoidWorldState(final @NotNull World world, final boolean state) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement upsertStatement = connection.prepareStatement("INSERT INTO lightbulbworlds (world, voidsafety) VALUES(?, ?) ON CONFLICT (world) DO UPDATE SET voidsafety=excluded.voidsafety;");
            upsertStatement.setString(1, world.getName());
            upsertStatement.setInt(2, state ? 1 : 0);
            upsertStatement.execute();
        } catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.")) {
                sqlException.printStackTrace();
            }
        }
    }

    /**
     * Gets the state of the player's void world configuration.
     * @param world The {@link World} being queried.
     * @return {@code true} if the player should not take fall damage, {@code false} if they should.
     */
    public boolean getVoidWorldState(final @NotNull World world) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
        final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM lightbulbworlds WHERE world = ?;");
        preparedStatement.setString(1, world.getName());
        final @NotNull ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt("voidsafety") == 1;
        } catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.") && !sqlException.getMessage().equals("ResultSet closed")) {
                sqlException.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets a player's night vision state.
     * @param player The player being {@link Player} set.
     * @param nightVision The state of the night vision.
     */
    public void setNightVision(final @NotNull Player player, final boolean nightVision) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement upsertStatement = connection.prepareStatement("INSERT INTO lightbulbmax (uuid, nightvision) VALUES(?, ?) ON CONFLICT (uuid) DO UPDATE SET nightvision=excluded.nightvision;");
            upsertStatement.setString(1, player.getUniqueId().toString());
            upsertStatement.setInt(2, nightVision ? 1 : 0);
            upsertStatement.execute();
        }  catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.")) {
                sqlException.printStackTrace();
            }
        }
    }

    /**
     * Gets if the player should use Night Vision.
     * @param player The {@link Player} being queried.
     * @return {@code true} if the player should use night vision, {@code false} if the player should not.
     */
    public boolean getNightVision(final @NotNull Player player) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM lightbulbmax WHERE uuid = ?;");
            preparedStatement.setString(1, player.getUniqueId().toString());
            final @NotNull ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt("nightvision") == 1;
        }  catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.") && !sqlException.getMessage().equals("ResultSet closed")) {
                sqlException.printStackTrace();
            }
            return false;
        }
    }

    @EventHandler
    public void onVoidDamage(final @NotNull EntityDamageEvent event) {
        if (event.getCause().equals(EntityDamageEvent.DamageCause.VOID)
                && event.getEntity() instanceof final @NotNull Player player
                && this.getVoidWorldState(event.getEntity().getWorld())) {
            event.setCancelled(true);
            player.setFallDistance(0);
            player.teleport(!usePlayerSpawn ?
                    player.getWorld().getSpawnLocation() :
                    (player.getBedSpawnLocation() != null ? player.getBedSpawnLocation() : this.getServer().getWorlds().get(0).getSpawnLocation())
            );
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
