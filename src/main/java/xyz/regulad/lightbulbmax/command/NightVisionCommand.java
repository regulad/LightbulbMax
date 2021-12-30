package xyz.regulad.lightbulbmax.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.regulad.lightbulbmax.LightbulbMax;

public class NightVisionCommand implements CommandExecutor {
    private final @NotNull LightbulbMax lightbulbMax;

    private static final @NotNull Component NIGHT_VISION_ON = Component.text("Your night vision is now on!", NamedTextColor.GREEN, TextDecoration.BOLD);
    private static final @NotNull Component NIGHT_VISION_OFF = Component.text("Your night vision is now off!", NamedTextColor.RED, TextDecoration.BOLD);
    private static final @NotNull Component CANNOT_USE_NOT_PLAYER = Component.text("This command can only be executed by a player.", NamedTextColor.RED);

    public NightVisionCommand(final @NotNull LightbulbMax lightbulbMax) {
        this.lightbulbMax = lightbulbMax;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof final @NotNull Player player) {
            this.lightbulbMax.getServer().getScheduler().runTaskAsynchronously(this.lightbulbMax, () -> {
               final boolean currentState = this.lightbulbMax.getNightVision(player);
               this.lightbulbMax.setNightVision(player, !currentState);

               if (!currentState) {
                   player.sendMessage(NIGHT_VISION_ON);
               } else {
                   player.sendMessage(NIGHT_VISION_OFF);
               }
            });
        } else {
            commandSender.sendMessage(CANNOT_USE_NOT_PLAYER);
        }
        return true;
    }
}
