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
import com.dsh105.commodus.StringUtil;
import com.dsh105.powermessage.exception.InvalidMessageException;
import com.dsh105.commodus.ItemUtil;
import com.dsh105.commodus.paginator.Pageable;
import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a message that internally manipulates JSON to allow the sending of fancy, interactive messages to players
 */
public class PowerMessage implements Pageable, JsonWritable, Cloneable, ConfigurationSerializable, Iterable<PowerSnippet> {

    static {
        ConfigurationSerialization.registerClass(PowerMessage.class);
    }

    private static final String SERIALIZED_SNIPPETS = "snippets";

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

    private ArrayList<PowerSnippet> snippets = new ArrayList<>();
    private String rawJson;
    private boolean convertedToJson;

    /**
     * Constructs a new, empty PowerMessage
     */
    public PowerMessage() {
    }

    /**
     * Constructs a new PowerMessage with a starting text snippet
     *
     * @param firstSnippet First snippet to construct the message with
     */
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

    /**
     * Sends a message to a Bukkit {@link org.bukkit.command.CommandSender}
     *
     * @param sender Whom to send the message to
     * @return This object
     */
    public PowerMessage send(CommandSender sender) {
        if (sender instanceof Player) {
            send((Player) sender);
        } else {
            sender.sendMessage(getContent());
        }
        return this;
    }

    /**
     * Sends a message to a group of Bukkit {@link org.bukkit.command.CommandSender}s
     *
     * @param senders Whom to send the message to
     * @return This object
     */
    public PowerMessage send(CommandSender... senders) {
        for (CommandSender sender : senders) {
            send(sender);
        }
        return this;
    }

    /**
     * Sends a message to a Bukkit {@link org.bukkit.entity.Player}
     *
     * @param player Whom to send the message to
     * @return This object
     */
    public PowerMessage send(Player player) {
        // TODO: Fancy packet stuff
        return this;
    }

    /**
     * Sends this message to a group of Bukkit {@link org.bukkit.entity.Player}s
     *
     * @param players Whom to send the message to
     * @return This object
     */
    public PowerMessage send(Player... players) {
        for (Player player : players) {
            send(player);
        }
        return this;
    }

    public PowerMessage edit(String snippetContent) {
        modify().setText(snippetContent);
        return this;
    }

    public PowerMessage then(String snippetContent) {
        return then((Object) snippetContent);
    }

    /**
     * Begins construction of a new message snippet
     *
     * @param snippetContent Content to begin the new snippet with
     * @return This object
     */
    public PowerMessage then(Object snippetContent) {
        return then(new PowerSnippet(snippetContent.toString()));
    }

    /**
     * Adds a new snippet to a PowerMessage
     *
     * @param snippet Snippet to add
     * @return This object
     */
    public PowerMessage then(PowerSnippet snippet) {
        snippets.add(snippet);
        modify();
        return this;
    }

    /**
     * Adds a colour to the current snippet of text
     *
     * @param colour Colour to add
     * @return This object
     */
    public PowerMessage colour(ChatColor colour) {
        modify().withColours(colour);
        return this;
    }

    /**
     * Adds colours to the current snippet of text
     *
     * @param colours Colours to add
     * @return This object
     */
    public PowerMessage colours(ChatColor... colours) {
        modify().withColours(colours);
        return this;
    }

    /**
     * Adds a file event to a PowerMessage
     * </p>
     * Opens a file for the player that clicks the message, where the file path is relative to their computer only
     *
     * @param relativePath Path of the file to open
     * @return This object
     */
    public PowerMessage file(String relativePath) {
        modify().withEvent("click", "open_file", relativePath);
        return this;
    }

    /**
     * Adds a link event to a PowerMessage
     * </p>
     * sOpen a specific URL link when clicked
     *
     * @param urlLink URL link to open
     * @return This object
     */
    public PowerMessage link(String urlLink) {
        modify().withEvent("click", "open_url", urlLink);
        return this;
    }

    /**
     * Adds a suggest event to a PowerMessage
     * </p>
     * Auto-fills a certain command to the clicker's chat box
     *
     * @param commandToSuggest Command to suggest when clicked
     * @return This object
     */
    public PowerMessage suggest(String commandToSuggest) {
        modify().withEvent("click", "suggest_command", commandToSuggest);
        return this;
    }

    /**
     * Adds a perform event to a PowerMessage
     * </p>
     * Performs a command on behalf of the player that clicked
     *
     * @param commandToPerform Command to perform when clicked
     * @return This object
     */
    public PowerMessage perform(String commandToPerform) {
        modify().withEvent("click", "run_command", commandToPerform);
        return this;
    }

    /**
     * Adds a tooltip to a PowerMessage
     * </p>
     * Displays a multiline or single-line tooltip message to the viewer when the message is hovered over
     *
     * @param content Message to show when hovered over
     * @return This object
     */
    public PowerMessage tooltip(String... content) {
        if (content == null || content.length <= 0) {
            throw new InvalidMessageException("Content cannot be empty");
        }

        modify().withEvent("hover", "show_text", content.length == 1 ? content[0] : StringUtil.combineArray(0, "\\n", content));
        return this;
    }

    /**
     * Adds an achievement tooltip to a PowerMessage
     * </p>
     * Displays an achievement to the viewer when the message is hovered over
     *
     * @param achievementName Name of the achievement to show
     * @return This object
     */
    public PowerMessage achievementTooltip(String achievementName) {
        modify().withEvent("hover", "show_achievement", "achievement." + achievementName);
        return this;
    }

    /**
     * Adds an item tooltip to a PowerMessage
     * </p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemJson JSON value of the item to add
     * @return This object
     */
    public PowerMessage itemTooltip(String itemJson) {
        modify().withEvent("hover", "show_item", itemJson);
        return this;
    }

    /**
     * Adds an item tooltip to a PowerMessage
     * </p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemContent A group of strings to represent an item with a name and description
     * @return This object
     */
    public PowerMessage itemTooltip(String... itemContent) {
        return itemTooltip(ItemUtil.getItem(itemContent));
    }

    /**
     * Adds an item tooltip to a PowerMessage
     * </p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemStack ItemStack to show
     * @return This object
     */
    public PowerMessage itemTooltip(ItemStack itemStack) {
        Reflection r = new Reflection();
        Object nmsCopy = r.reflect(CRAFT_ITEMSTACK).getSafeMethod("asNMSCopy", ItemStack.class).getAccessor().invokeStatic(itemStack);
        Object nbtData = r.reflect(nmsCopy.getClass()).getSafeMethod("save", NBT_TAG_COMPOUND).getAccessor().invoke(nmsCopy, r.reflect(NBT_TAG_COMPOUND).getSafeConstructors());
        return itemTooltip(nbtData.toString());
    }

    /**
     * Adds an achievement tooltip to a PowerMessage
     * </p>
     * Displays an achievement to the viewer when the message is hovered over
     *
     * @param which Achievement to show
     * @return This object
     */
    public PowerMessage achievementTooltip(Achievement which) {
        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getNMSAchievement", Achievement.class).getAccessor().invokeStatic(which);
        return achievementTooltip((String) r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    /**
     * Adds a statistic tooltip to a PowerMessage
     * </p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which Achievement to show
     * @return This object
     */
    public PowerMessage statisticTooltip(Statistic which) {
        if (which.getType() != Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires an additional " + which.getType() + " parameter!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getNMSStatistic", Statistic.class).getAccessor().invokeStatic(which);
        return achievementTooltip((String) r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    /**
     * Adds an item statistic tooltip to a PowerMessage
     * </p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which Statistic to show
     * @param item  Item to show
     * @return This object
     */
    public PowerMessage statisticTooltip(Statistic which, Material item) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if ((which.getType() == Statistic.Type.BLOCK && item.isBlock()) || which.getType() == Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getMaterialStatistic", Statistic.class, Material.class).getAccessor().invokeStatic(which, item);
        return achievementTooltip((String) r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    /**
     * Adds an entity statistic tooltip to a PowerMessage
     * </p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which  Statistic to show
     * @param entity Entity type to show
     * @return This object
     */
    public PowerMessage statisticTooltip(Statistic which, EntityType entity) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if (which.getType() != Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getEntityStatistic", Statistic.class, EntityType.class).getAccessor().invokeStatic(which, entity);
        return achievementTooltip((String) r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    /**
     * Gets a copy of the snippets in a PowerMessage
     * </p>
     * Editing this list will not change the content of the original PowerMessage
     *
     * @return List of snippets in a PowerMessage
     */
    public ArrayList<PowerSnippet> getSnippets() {
        return new ArrayList<>(snippets);
    }

    /**
     * Gets a snippet at a particular index
     *
     * @param index Index to retrieve
     * @return A particular snippet in a PowerMessage
     */
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

    private boolean isConvertedToJson() {
        return convertedToJson;
    }

    @Override
    public Iterator<PowerSnippet> iterator() {
        return snippets.iterator();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put(SERIALIZED_SNIPPETS, snippets);
        return serialized;
    }

    public static PowerMessage deserialize(Map<String, Object> serialized) {
        if (!serialized.containsKey(SERIALIZED_SNIPPETS)) {
            throw new IllegalArgumentException("Failed to deserialize PowerMessage from provided data");
        }
        PowerMessage powerMessage = new PowerMessage();
        powerMessage.snippets = (ArrayList<PowerSnippet>) serialized.get(SERIALIZED_SNIPPETS);
        return powerMessage;
    }

    // TODO: fromJson

    /**
     * Converts a PowerMessage to raw JSON, ready to be sent to a player
     *
     * @return Raw JSON to represent a PowerMessage
     */
    public String toJson() {
        if (!isConvertedToJson() || rawJson == null) {
            StringWriter stringWriter = new StringWriter();
            JsonWriter writer = new JsonWriter(stringWriter);

            try {
                writeJson(writer);
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

    @Override
    public JsonWriter writeJson(JsonWriter writer) throws IOException {
        if (snippets.size() == 1) {
            lastSnippet().writeJson(writer);
        } else {
            writer.beginObject().name("text").value("").name("extra").beginArray();
            for (PowerSnippet snippet : snippets) {
                snippet.writeJson(writer);
            }
            writer.endArray().endObject();
        }
        return writer;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PowerMessage cloned = (PowerMessage) super.clone();
        for (int i = 0; i < this.getSnippets().size(); i++) {
            cloned.snippets.add(this.getSnippet(i));
        }
        return cloned;
    }
}