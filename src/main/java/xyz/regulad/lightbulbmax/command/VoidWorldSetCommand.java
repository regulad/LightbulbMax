package xyz.regulad.lightbulbmax.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.lightbulbmax.LightbulbMax;

import java.util.ArrayList;
import java.util.List;

public class VoidWorldSetCommand implements CommandExecutor, TabCompleter {
    private final @NotNull LightbulbMax lightbulbMax;

    private static final @NotNull Component VOID_SAFETY_ON = Component.text("You are now safe from void damage in this world!", NamedTextColor.GREEN, TextDecoration.BOLD);
    private static final @NotNull Component VOID_SAFETY_OFF = Component.text("You are now vulnerable to void damage in this world!", NamedTextColor.RED, TextDecoration.BOLD);
    private static final @NotNull Component DID_NOT_FIND_WORLD = Component.text("Could not find this world.", NamedTextColor.RED);
    private static final @NotNull Component CANNOT_USE_NOT_PLAYER = Component.text("This command can only be executed by a player.", NamedTextColor.RED);

    public VoidWorldSetCommand(final @NotNull LightbulbMax lightbulbMax) {
        this.lightbulbMax = lightbulbMax;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof final @NotNull Player player) {
            if (args.length < 1) {
                return false;
            }

            final @Nullable World possibleWorld = this.lightbulbMax.getServer().getWorld(args[0]);

            if (possibleWorld != null) {
                this.lightbulbMax.getServer().getScheduler().runTaskAsynchronously(this.lightbulbMax, () -> {
                    final boolean currentState = this.lightbulbMax.getVoidWorldState(possibleWorld);
                    this.lightbulbMax.changeVoidWorldState(possibleWorld, !currentState);

                    if (!currentState) {
                        player.sendMessage(VOID_SAFETY_ON);
                    } else {
                        player.sendMessage(VOID_SAFETY_OFF);
                    }
                });
            } else {
                commandSender.sendMessage(DID_NOT_FIND_WORLD);
            }

        } else {
            commandSender.sendMessage(CANNOT_USE_NOT_PLAYER);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        final @NotNull ArrayList<@NotNull String> scratch = new ArrayList<>();
        this.lightbulbMax.getServer().getWorlds().forEach(world -> scratch.add(world.getName()));
        return scratch;
    }
}
