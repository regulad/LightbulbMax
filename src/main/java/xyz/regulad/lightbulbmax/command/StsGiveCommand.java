package xyz.regulad.lightbulbmax.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.lightbulbmax.LightbulbMax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class StsGiveCommand implements CommandExecutor {
    private static final @NotNull HashMap<@NotNull Enchantment, @NotNull Integer> hotsEnchants = new HashMap<>();
    private static final @NotNull Component hotsTitle = Component.text("#TeamSeas", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    private static final @NotNull Function<@NotNull Player, @NotNull List<Component>> hotsLore = player -> {
        final @NotNull ArrayList<Component> scratch = new ArrayList<>();

        scratch.add(Component.empty());
        scratch.add(Component.text("Help us remove 30 million pounds", NamedTextColor.GRAY, TextDecoration.ITALIC));
        scratch.add(Component.text("of trash by January 1st, 2022.", NamedTextColor.GRAY, TextDecoration.ITALIC));
        scratch.add(Component.empty());
        scratch.add(Component.text("This was bought by: ", NamedTextColor.WHITE, TextDecoration.ITALIC).append(player.displayName()).color(NamedTextColor.WHITE));

        return scratch;
    };

    static {
        // enchants
        hotsEnchants.put(Enchantment.WATER_WORKER, 10); // aqua affinity
        hotsEnchants.put(Enchantment.OXYGEN, 10); // Respiration
        hotsEnchants.put(Enchantment.DURABILITY, 10); // Unbreaking
    }

    private final @NotNull LightbulbMax lightbulbMax;

    public StsGiveCommand(final @NotNull LightbulbMax lightbulbMax) {
        this.lightbulbMax = lightbulbMax;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        final @Nullable Player recipient = this.lightbulbMax.getServer().getPlayer(args[0]);

        if (recipient == null) {
            return false; // Bad player
        }

        final @NotNull ItemStack heartOfTheSea = new ItemStack(Material.HEART_OF_THE_SEA);

        final @NotNull ItemMeta heartOfTheSeaItemMeta = heartOfTheSea.getItemMeta();

        heartOfTheSeaItemMeta.displayName(hotsTitle);

        heartOfTheSeaItemMeta.lore(hotsLore.apply(recipient));

        // commit changes

        heartOfTheSea.setItemMeta(heartOfTheSeaItemMeta);

        // enchants

        heartOfTheSea.addUnsafeEnchantments(hotsEnchants);

        // give it to em

         if (recipient.getInventory().addItem(heartOfTheSea).size() > 0) {
             commandSender.sendMessage(Component.text("Did not fit in the recipient's inventory.").color(NamedTextColor.RED));
         }

        return true;
    }
}
