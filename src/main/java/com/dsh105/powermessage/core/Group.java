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
import com.captainbern.minecraft.wrapper.nbt.NbtFactory;
import com.captainbern.minecraft.wrapper.nbt.NbtType;
import com.captainbern.reflection.Reflection;
import com.captainbern.reflection.accessor.MethodAccessor;
import com.dsh105.commodus.ItemUtil;
import com.dsh105.commodus.StringUtil;
import com.dsh105.powermessage.exception.InvalidMessageException;
import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group implements MessageBuilder {

    private static Class<?> NBT_TAG_COMPOUND;
    private static Class<?> CRAFT_ITEMSTACK;
    private static Class<?> CRAFT_STATISTIC;

    static {
        NBT_TAG_COMPOUND = NbtFactory.createTag(NbtType.TAG_COMPOUND).getHandle().getClass();
        CRAFT_ITEMSTACK = MinecraftReflection.getCraftItemStackClass();
        CRAFT_STATISTIC = MinecraftReflection.getCraftBukkitClass("CraftStatistic");
    }

    private PowerMessage powerMessage;
    private int start;
    private int end;

    public Group(PowerMessage powerMessage, int start, int end) {
        this.powerMessage = powerMessage;
        this.start = start;
        this.end = end;
    }

    public Group(PowerMessage powerMessage, int groupCount) {
        this(powerMessage, powerMessage.getSnippets().size() - groupCount, powerMessage.getSnippets().size());
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    private List<PowerSnippet> getSnippets() {
        ArrayList<PowerSnippet> snippets = new ArrayList<>();
        for (int i = start; i < end; i++) {
            snippets.add(powerMessage.getSnippet(i));
        }
        return Collections.unmodifiableList(snippets);
    }

    public PowerMessage exit() {
        return powerMessage;
    }

    @Override
    public Group edit(String snippetContent) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.setText(snippetContent);
        }
        return this;
    }

    @Override
    public Group colour(ChatColor... colours) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withColour(colours);
        }
        return this;
    }

    @Override
    public Group file(String relativePath) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "open_file", relativePath);
        }
        return this;
    }

    @Override
    public Group link(String urlLink) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "open_url", urlLink);
        }
        return this;
    }

    @Override
    public Group suggest(String commandToSuggest) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "suggest_command", commandToSuggest);
        }
        return this;
    }

    @Override
    public Group perform(String commandToPerform) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "run_command", commandToPerform);
        }
        return this;
    }

    @Override
    public Group tooltip(String... content) {
        if (content == null || content.length <= 0) {
            throw new InvalidMessageException("Content cannot be empty");
        }

        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_text", content.length == 1 ? content[0] : StringUtil.combineArray(0, "\n", content));
        }
        return this;
    }

    @Override
    public Group tooltip(PowerMessage powerMessage) {
        if (powerMessage.getContent() == null || powerMessage.getContent().length() <= 0) {
            throw new InvalidMessageException("Content cannot be empty");
        }

        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_text", powerMessage.getContent());
        }
        return this;
    }

    @Override
    public Group achievementTooltip(String achievementName) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_achievement", "achievement." + achievementName);
        }
        return this;
    }

    @Override
    public Group itemTooltip(String itemJson) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_item", itemJson);
        }
        return this;
    }

    @Override
    public Group itemTooltip(String... itemContent) {
        return itemTooltip(ItemUtil.getItem(itemContent));
    }

    @Override
    public Group itemTooltip(ItemStack itemStack) {
        Reflection r = new Reflection();
        Object nmsCopy = r.reflect(CRAFT_ITEMSTACK).getSafeMethod("asNMSCopy", ItemStack.class).getAccessor().invokeStatic(itemStack);
        Object nbtData = r.reflect(nmsCopy.getClass()).getSafeMethod("save", NBT_TAG_COMPOUND).getAccessor().invoke(nmsCopy, r.reflect(NBT_TAG_COMPOUND).getSafeConstructors());
        return itemTooltip(nbtData.toString());
    }

    @Override
    public Group achievementTooltip(Achievement which) {
        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getNMSAchievement", Achievement.class).getAccessor().invokeStatic(which);
        return achievementTooltip(r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    @Override
    public Group statisticTooltip(Statistic which) {
        if (which.getType() != Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires an additional " + which.getType() + " parameter!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getNMSStatistic", Statistic.class).getAccessor().invokeStatic(which);
        return achievementTooltip(r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    @Override
    public Group statisticTooltip(Statistic which, Material item) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if ((which.getType() == Statistic.Type.BLOCK && item.isBlock()) || which.getType() == Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getMaterialStatistic", Statistic.class, Material.class).getAccessor().invokeStatic(which, item);
        return achievementTooltip(r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }

    @Override
    public Group statisticTooltip(Statistic which, EntityType entity) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if (which.getType() != Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Reflection r = new Reflection();
        Object achievement = r.reflect(CRAFT_STATISTIC).getSafeMethod("getEntityStatistic", Statistic.class, EntityType.class).getAccessor().invokeStatic(which, entity);
        return achievementTooltip(r.reflect(achievement.getClass()).getSafeFieldByNameAndType("name", String.class).getAccessor().get(achievement));
    }
}