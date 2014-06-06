/*
 * This file is part of PowerMessage.
 *
 * PowerMessage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerMessage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerMessage.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.powermessage.core;

import com.captainbern.minecraft.reflection.MinecraftReflection;
import com.captainbern.reflection.Reflection;
import com.dsh105.powermessage.action.ClickEvent;
import com.dsh105.powermessage.exception.InvalidMessageException;
import com.dsh105.simpleutils.ItemUtil;
import com.dsh105.simpleutils.paginator.Pageable;
import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

public class PowerMessage implements Pageable {

    private static Class<?> NBT_TAG_COMPOUND = null;
    private static Class<?> CRAFT_ITEMSTACK = null;
    private static Class<?> CRAFT_STATISTIC = null;


    static {
        try {
            NBT_TAG_COMPOUND = MinecraftReflection.getMinecraftClass("NBTTagCompound");
            CRAFT_ITEMSTACK = MinecraftReflection.getCraftBukkitClass("inventory.CraftItemStack");
            CRAFT_STATISTIC = MinecraftReflection.getCraftBukkitClass("CraftStatistic");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private final ArrayList<PowerSnippet> snippets = new ArrayList<>();
    private String rawJson;
    private boolean convertedToJson;

    public PowerMessage() {
    }

    public PowerMessage(String firstSnippet) {
        this.snippets.add(new PowerSnippet(firstSnippet));
    }

    @Override
    public String getContent() {
        StringBuilder result = new StringBuilder();
        for (PowerSnippet snippet : getSnippets()) {
            for (ChatColor colour : snippet.getColours()) {
                result.append(colour);
            }
            result.append(snippet.getText());
        }
        return result.toString();
    }

    public PowerMessage send(CommandSender sender) {
        if (sender instanceof Player) {
            send((Player) sender);
        } else {
            sender.sendMessage(getContent());
        }
        return this;
    }

    public PowerMessage send(CommandSender... senders) {
        for (CommandSender sender : senders) {
            send(sender);
        }
        return this;
    }

    public PowerMessage send(Player player) {
        // TODO: Fancy packet stuff
    }

    public PowerMessage send(Player... players) {
        for (Player player : players) {
            send(player);
        }
        return this;
    }

    public PowerMessage then(Object object) {
        snippets.add(new PowerSnippet(object.toString()));
        modify();
        return this;
    }

    public PowerMessage colour(ChatColor colour) {
        modify().withColours(colour);
        return this;
    }

    public PowerMessage file(String relativePath) {
        modify().withEvent("click", "open_file", relativePath);
        return this;
    }

    public PowerMessage link(String urlLink) {
        modify().withEvent("click", "open_url", urlLink);
        return this;
    }

    public PowerMessage suggest(String commandToSuggest) {
        modify().withEvent("click", "suggest_command", commandToSuggest);
        return this;
    }

    public PowerMessage perform(String commandToPerform) {
        modify().withEvent("click", "run_command", commandToPerform);
        return this;
    }

    public PowerMessage achievementTooltip(String achievementName) {
        modify().withEvent("hover", "show_achievement", "achievement." + achievementName);
        return this;
    }

    public PowerMessage itemTooltip(String itemJson) {
        modify().withEvent("hover", "show_item", itemJson);
        return this;
    }

    public PowerMessage itemTooltip(String... itemContent) {
        itemTooltip(ItemUtil.getItem(itemContent));
        return this;
    }

    public PowerMessage tooltip(String content) {
        tooltip(content.split("\\n"));
        return this;
    }

    public PowerMessage tooltip(String... content) {
        if (content.length == 1) {
            modify().withEvent("hover", "show_text", content[0]);
        } else {
            itemTooltip(multilineTooltip(content));
        }
        return this;
    }

    public String multilineTooltip(String... content) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        try {
            writer.beginObject().name("id").value(1);
            writer.name("tag").beginObject().name("display").beginObject();
            writer.name("Name").value("\\u00A7f" + content[0].replace("\"", "\\\""));
            writer.name("Lore").beginArray();
            for (int i = 1; i < content.length; i++) {
                final String line = content[i];
                writer.value(line.isEmpty() ? " " : line.replace("\"", "\\\""));
            }
            writer.endArray().endObject().endObject().endObject();
        } catch (IOException e) {
            throw new InvalidMessageException("Invalid tooltip", e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stringWriter.toString();
    }

    public PowerMessage itemTooltip(ItemStack itemStack) {
        Reflection r = new Reflection();
        try {
            Object nmsCopy = r.reflect(CRAFT_ITEMSTACK, "asNMSCopy", ItemStack.class).getAccessor().invokeStatic(itemStack);
            Object nbtData = r.reflect(nmsCopy.getClass(), "save", NBT_TAG_COMPOUND).getAccessor().invoke(nmsCopy, r.reflect(NBT_TAG_COMPOUND.getConstructor()));
            return itemTooltip(nbtData.toString());
        } catch (NoSuchMethodException e) {
            throw new InvalidMessageException("Invalid ItemStack", e);
        }
    }

    public PowerMessage achievementTooltip(Achievement which) {
        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC, "getNMSAchievement", Achievement.class).getAccessor().invokeStatic(which);
        return achievementTooltip((String) r.reflect(achievement.getClass(), "name").getAccessor().get(achievement));
    }

    public PowerMessage statisticTooltip(Statistic which) {
        if (which.getType() != Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires an additional " + which.getType() + " parameter!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC, "getNMSStatistic", Statistic.class).getAccessor().invokeStatic(which);
        return achievementTooltip((String) r.reflect(achievement.getClass(), "name").getAccessor().get(achievement));
    }

    public PowerMessage statisticTooltip(Statistic which, Material item) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if ((which.getType() == Statistic.Type.BLOCK && item.isBlock()) || which.getType() == Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC, "getMaterialStatistic", Statistic.class, Material.class).getAccessor().invokeStatic(which, item);
        return achievementTooltip((String) r.reflect(achievement.getClass(), "name").getAccessor().get(achievement));
    }

    public PowerMessage statisticTooltip(Statistic which, EntityType entity) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if (which.getType() != Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC, "getEntityStatistic", Statistic.class, EntityType.class).getAccessor().invokeStatic(which, entity);
        return achievementTooltip((String) r.reflect(achievement.getClass(), "name").getAccessor().get(achievement));
    }

    public ArrayList<PowerSnippet> getSnippets() {
        return new ArrayList<>(snippets);
    }

    public PowerSnippet getSnippet(int index) {
        return snippets.get(index);
    }

    private PowerSnippet lastSnippet() {
        return getSnippet(snippets.size() - 1);
    }

    private PowerSnippet modify() {
        this.convertedToJson = false;
        return lastSnippet();
    }

    public boolean isConvertedToJson() {
        return convertedToJson;
    }

    public String toJson() {
        if (!isConvertedToJson() || rawJson == null) {
            StringWriter stringWriter = new StringWriter();
            JsonWriter writer = new JsonWriter(stringWriter);

            try {
                write(writer);
            } catch (IOException e) {
                throw new InvalidMessageException("Invalid message", e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            rawJson = stringWriter.toString();
            convertedToJson = true;
        }

        return rawJson;
    }

    protected JsonWriter write(JsonWriter writer) throws IOException {
        if (snippets.size() == 1) {
            lastSnippet().write(writer);
        } else {
            writer.beginObject().name("text").value("").name("extra").beginArray();
            for (PowerSnippet snippet : snippets) {
                snippet.write(writer);
            }
            writer.endArray().endObject();
        }
        return writer;
    }
}