/*
 * This file is part of hephaestus-engine, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.unnamed.hephaestus;

import net.kyori.examination.Examinable;
import net.kyori.examination.ExaminableProperty;
import net.kyori.examination.string.StringExaminer;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.base.Vector3Float;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents an in-game {@link Model} movable part.
 *
 * @since 1.0.0
 */
public class Bone implements Examinable {

    private final String name;

    private final Vector3Float position;
    private final Vector3Float rotation;

    private final Map<String, Bone> children;

    private final int customModelData;
    private final float scale;

    private final boolean parentOnly;

    public Bone(
            String name,
            Vector3Float position,
            Vector3Float rotation,
            Map<String, Bone> children,
            int customModelData,
            float scale,
            boolean parentOnly
    ) {
        this.name = name;
        this.position = position;
        this.rotation = rotation;
        this.children = children;
        this.customModelData = customModelData;
        this.scale = scale;
        this.parentOnly = parentOnly;
    }

    /**
     * Returns this bone unique name, bone names
     * are unique in the {@link Model} scope
     *
     * @return The bone name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the bone position relative to parent
     * bone position, in Minecraft blocks
     *
     * @return The bone position
     */
    public Vector3Float position() {
        return position;
    }

    /**
     * Returns this bone initial rotation
     *
     * @return The bone initial rotation
     */
    public Vector3Float rotation() {
        return rotation;
    }

    /**
     * Returns this bone custom model data,
     * which must be applied to the creative
     * Model representing this bone
     *
     * <p>Consider this number as a "handle"
     * or "connection" to the resource-pack
     * model information</p>
     *
     * <strong>This number must be unique in
     * the resource-pack</strong>
     *
     * @return The bone custom model data
     */
    public int customModelData() {
        return customModelData;
    }

    /**
     * Returns this bone child bones
     *
     * @return The child bones
     */
    public Collection<Bone> children() {
        return children.values();
    }

    /**
     * Returns a map of this bone children
     * bones, keys are bone names
     *
     * @return The child bone map
     */
    public Map<String, Bone> childrenMap() {
        return children;
    }

    /**
     * Returns this bone initial scale, which is just
     * a compensation to make big models show with
     * their correct size, since resource-packs are
     * limited.
     *
     * <p>If the model already fits, scale is one.</p>
     *
     * <p>The returned scale is always one, or greater.</p>
     *
     * @return The bone initial scale
     * @since 1.0.0
     */
    public float scale() {
        return scale;
    }

    public boolean isParentOnly() {
        return parentOnly;
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(
                ExaminableProperty.of("name", name),
                ExaminableProperty.of("rotation", rotation),
                ExaminableProperty.of("bones", children),
                ExaminableProperty.of("offset", position),
                ExaminableProperty.of("customModelData", customModelData)
        );
    }

    @Override
    public String toString() {
        return examine(StringExaminer.simpleEscaping());
    }

}