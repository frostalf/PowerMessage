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

import com.dsh105.powermessage.action.ActionEvent;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a particular snippet of a {@link com.dsh105.powermessage.core.PowerMessage}
 */
public class PowerSnippet {

    private String text;
    private ArrayList<ChatColor> colours = new ArrayList<>();
    private ArrayList<ActionEvent> actionEvents = new ArrayList<>();

    /**
     * Constructs a new PowerSnippet with text
     *
     * @param text Textual content to be included in the snippet
     */
    public PowerSnippet(String text) {
        this.text = text;
    }

    /**
     * Gets the text of a snippet
     *
     * @return Textual content of a snippet
     */
    public String getText() {
        return text;
    }

    protected void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the colours of a snippet
     *
     * @return Colours in a snippet
     */
    public ArrayList<ChatColor> getColours() {
        return new ArrayList<>(colours);
    }

    /**
     * Gets the action events of a snippet
     *
     * @return Action events of a snippet
     */
    public ArrayList<ActionEvent> getActionEvents() {
        return new ArrayList<>(actionEvents);
    }

    /**
     * Adds colours to a snippet
     *
     * @param colours Colours to add
     * @return This object
     */
    public PowerSnippet withColours(ChatColor... colours) {
        for (ChatColor c : colours) {
            this.colours.add(c);
        }
        return this;
    }

    /**
     * Adds events to a snippet
     *
     * @param events Events to add
     * @return This object
     */
    public PowerSnippet withEvents(ActionEvent... events) {
        for (ActionEvent event : events) {
            this.actionEvents.add(event);
        }
        return this;
    }

    /**
     * Adds an event to a snippet
     *
     * @param eventType Type of event to add
     * @param eventName Name of event being added
     * @param eventData Data of event being added
     * @return This object
     */
    public PowerSnippet withEvent(String eventType, String eventName, String eventData) {
        this.actionEvents.add(new ActionEvent(eventType) {
            @Override
            public ActionEvent withName(String name) {
                return super.withName(name);
            }

            @Override
            public ActionEvent withData(String data) {
                return super.withData(data);
            }
        }.withName(eventName).withData(eventData));
        return this;
    }

    protected JsonWriter write(JsonWriter writer) throws IOException {
        writer.beginObject().name("text").value(text);

        for (ChatColor colour : colours) {
            if (colour.isFormat()) {
                String formatName;
                switch (colour) {
                    case MAGIC:
                        formatName = "obfuscated";
                        break;
                    case UNDERLINE:
                        formatName = "underlined";
                        break;
                    default:
                        formatName = colour.name().toLowerCase();
                }
                writer.name(formatName).value(true);
            } else {
                writer.name("color").value(colour.name().toLowerCase());
            }
        }

        for (ActionEvent event : actionEvents) {
            event.write(writer);
        }

        return writer.endObject();
    }
}