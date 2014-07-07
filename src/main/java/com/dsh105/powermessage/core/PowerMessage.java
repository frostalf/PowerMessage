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

import com.captainbern.minecraft.protocol.PacketType;
import com.captainbern.minecraft.reflection.MinecraftMethods;
import com.captainbern.minecraft.reflection.MinecraftReflection;
import com.captainbern.minecraft.wrapper.WrappedPacket;
import com.captainbern.reflection.Reflection;
import com.captainbern.reflection.accessor.MethodAccessor;
import com.captainbern.reflection.matcher.Matchers;
import com.dsh105.commodus.ServerUtil;
import com.dsh105.commodus.paginator.Pageable;
import com.dsh105.powermessage.exception.InvalidMessageException;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a message that internally manipulates JSON to allow the sending of fancy, interactive messages to players
 */
public class PowerMessage implements MessageBuilder, Pageable, JsonWritable, Cloneable, ConfigurationSerializable, Iterable<PowerSnippet> {

    protected static final Pattern COLOUR_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([0-9A-FK-OR])", Pattern.CASE_INSENSITIVE);

    private static final MethodAccessor CHAT_FROM_JSON;

    private static final String SERIALIZED_SNIPPETS = "snippets";

    private static Class<?> CHAT_SERIALIZER;
    private static Class<?> I_CHAT_BASE_COMPONENT;

    static {
        ConfigurationSerialization.registerClass(PowerMessage.class);

        CHAT_SERIALIZER = MinecraftReflection.getMinecraftClass("ChatSerializer");
        I_CHAT_BASE_COMPONENT = MinecraftReflection.getMinecraftClass("IChatBaseComponent");
        CHAT_FROM_JSON = new Reflection().reflect(CHAT_SERIALIZER).getSafeMethods(Matchers.withReturnType(I_CHAT_BASE_COMPONENT), Matchers.withArguments(new Class[]{String.class})).get(0).getAccessor();
    }

    private ArrayList<PowerSnippet> snippets = new ArrayList<>();
    private String rawJson;
    private boolean convertedToJson;
    private Group currentGroup;

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
    @Override
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
        if (ServerUtil.MC_VERSION_NUMERIC >= 170) {
            WrappedPacket chat = new WrappedPacket(PacketType.Play.Server.CHAT);
            chat.getAccessor((Class<Object>) I_CHAT_BASE_COMPONENT).write(0, CHAT_FROM_JSON.invokeStatic(toJson()));
            MinecraftMethods.sendPacket(player, chat.getHandle());
        } else {
            player.sendMessage(getContent());
        }
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

    @Override
    public PowerMessage edit(String snippetContent) {
        currentGroup.edit(snippetContent);
        return this;
    }

    @Override
    public PowerMessage colour(ChatColor... colours) {
        currentGroup.colour(colours);
        return this;
    }

    @Override
    public PowerMessage file(String relativePath) {
        currentGroup.file(relativePath);
        return this;
    }

    @Override
    public PowerMessage link(String urlLink) {
        currentGroup.link(urlLink);
        return this;
    }

    @Override
    public PowerMessage suggest(String commandToSuggest) {
        currentGroup.suggest(commandToSuggest);
        return this;
    }

    @Override
    public PowerMessage perform(String commandToPerform) {
        currentGroup.perform(commandToPerform);
        return this;
    }

    @Override
    public PowerMessage tooltip(String... content) {
        currentGroup.tooltip(content);
        return this;
    }

    @Override
    public PowerMessage tooltip(PowerMessage powerMessage) {
        currentGroup.tooltip(powerMessage);
        return this;
    }

    @Override
    public PowerMessage achievementTooltip(String achievementName) {
        currentGroup.achievementTooltip(achievementName);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(String itemJson) {
        currentGroup.itemTooltip(itemJson);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(String... itemContent) {
        currentGroup.itemTooltip(itemContent);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(ItemStack itemStack) {
        currentGroup.itemTooltip(itemStack);
        return this;
    }

    @Override
    public PowerMessage achievementTooltip(Achievement which) {
        currentGroup.achievementTooltip(which);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which) {
        currentGroup.statisticTooltip(which);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which, Material item) {
        currentGroup.statisticTooltip(which, item);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which, EntityType entity) {
        currentGroup.statisticTooltip(which, entity);
        return this;
    }

    /**
     * Begins construction of a new message snippet
     *
     * @param snippetContent Content to begin the new snippet with
     * @return This object
     */
    public PowerMessage then(String snippetContent) {
        String content = ChatColor.translateAlternateColorCodes('&', snippetContent);
        int groupCount = 0;
        if (content.length() > 0) {
            ArrayList<ChatColor> colours = new ArrayList<>();
            Matcher colourMatcher = COLOUR_PATTERN.matcher(content);
            int lastEnd = 0;
            while (colourMatcher.find()) {
                if (colourMatcher.start() > lastEnd) {
                    then(new PowerSnippet(content.substring(lastEnd, colourMatcher.start()))).colour(colours.toArray(new ChatColor[0]));
                    groupCount++;
                }

                ChatColor colour = ChatColor.getByChar(colourMatcher.group(1));
                if (colour == ChatColor.RESET) {
                    colours.clear();
                } else {
                    colours.add(colour);
                }
                lastEnd = colourMatcher.end();
            }
            if (lastEnd < (content.length() - 1)) {
                then(new PowerSnippet(content.substring(lastEnd, content.length()))).colour(colours.toArray(new ChatColor[0]));
                groupCount++;
            }
            // Group everything together so that changes can be applied to all of them
            group(groupCount);
        }
        return this;
    }

    /**
     * Begins construction of a new message snippet
     *
     * @param snippetContent Content to begin the new snippet with
     * @return This object
     */
    public PowerMessage then(Object snippetContent) {
        return then(snippetContent.toString());
    }

    /**
     * Adds a new snippet to a PowerMessage
     *
     * @param snippet Snippet to add
     * @return This object
     */
    public PowerMessage then(PowerSnippet snippet) {
        snippets.add(snippet);
        group(1);
        return this;
    }

    /**
     * Gets a copy of the snippets in a PowerMessage
     * <p/>
     * Editing this list will not change the content of the original PowerMessage
     *
     * @return List of snippets in a PowerMessage
     */
    public List<PowerSnippet> getSnippets() {
        return Collections.unmodifiableList(snippets);
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

    /**
     * Gets a group of all snippets in a PowerMessage so that changes can be applied to them more easily
     *
     * @return A Group representing all snippets in a PowerMessage
     */
    public Group group() {
        this.convertedToJson = false;
        return new Group(this, groupCount());
    }

    /**
     * Gets a group of snippets so that changes can be applied to all more easily
     * <p/>
     * The snippets are counted backwards <i>n</i> times inclusively, where <i>count</i> is
     *
     * @param count Number of snippets to include
     * @return A {@link com.dsh105.powermessage.core.Group} representing a certain number of snippets
     */
    public Group group(int count) {
        this.convertedToJson = false;
        this.currentGroup = new Group(this, count);
        return currentGroup;
    }

    /**
     * Gets the number of snippets in a PowerMessage
     *
     * @return Group count (number of snippets)
     */
    public int groupCount() {
        return snippets.size();
    }

    private PowerSnippet lastSnippet() {
        return getSnippet(snippets.size() - 1);
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