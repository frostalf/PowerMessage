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

package com.dsh105.powermessage.action;

import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;

import java.io.IOException;

public abstract class ActionEvent {

    private String actionType;
    private String name;
    private String data;

    public ActionEvent(String actionType) {
        this.actionType = actionType;
    }

    public ActionEvent withName(String name) {
        this.name = name;
        return this;
    }

    public ActionEvent withData(String data) {
        this.data = data;
        return this;
    }

    public JsonWriter write(JsonWriter writer) {
        try {
            writer.name(actionType + "Event").beginObject().name("action").value(this.name).name("value").value(this.data).endObject();

            return writer.endObject();
        } catch (IOException e) {
            e.printStackTrace();
            return writer;
        }
    }
}