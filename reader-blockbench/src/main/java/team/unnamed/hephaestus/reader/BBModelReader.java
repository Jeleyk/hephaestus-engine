/*
 * This file is part of hephaestus-engine, licensed under the MIT license
 *
 * Copyright (c) 2021-2022 Unnamed Team
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
package team.unnamed.hephaestus.reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import team.unnamed.creative.base.Axis3D;
import team.unnamed.creative.base.CubeFace;
import team.unnamed.creative.base.Vector2Float;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.creative.base.Vector4Float;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.model.ElementFace;
import team.unnamed.creative.model.ElementRotation;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.partial.ElementAsset;
import team.unnamed.hephaestus.partial.ModelAsset;
import team.unnamed.hephaestus.Bone;
import team.unnamed.hephaestus.partial.BoneAsset;
import team.unnamed.hephaestus.ModelDataCursor;
import team.unnamed.hephaestus.animation.ModelAnimation;
import team.unnamed.hephaestus.process.ElementProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BBModelReader implements ModelReader {

    private static final JsonParser JSON_PARSER = new JsonParser();

    private final ModelDataCursor cursor;

    private BBModelReader(ModelDataCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public Model read(InputStream input) throws IOException {

        Reader reader = new InputStreamReader(input);
        JsonObject json = JSON_PARSER.parse(reader).getAsJsonObject();
        JsonObject meta = json.get("meta").getAsJsonObject();

        // TODO: we can take the "meta.creation_time" date for generating resource pack
        String modelName = json.get("geometry_name").getAsString();
        if (modelName.isEmpty()) {
            // fallback to "name"
            modelName = json.get("name").getAsString();
        }

        // check for box uv
        if (!GsonUtil.isNullOrAbsent(meta, "box_uv") && meta.get("box_uv").getAsBoolean()) {
            throw new IOException("Box UV not supported, please turn it off");
        }

        JsonObject resolution = json.getAsJsonObject("resolution");
        int width = resolution.get("width").getAsInt();
        int height = resolution.get("height").getAsInt();

        Map<String, Bone> bones = new LinkedHashMap<>();
        Map<String, BoneAsset> boneAssets = new LinkedHashMap<>();
        Map<String, ModelAnimation> animations = new LinkedHashMap<>();
        Map<String, Writable> textures = new HashMap<>();
        Map<Integer, String> textureMapping = new HashMap<>();

        TextureReader.readTextures(json, textures, textureMapping);
        readElements(json, bones, boneAssets, width, height);
        AnimationReader.readAnimations(json, animations);

        return new Model(
                modelName,
                bones,
                new Vector2Float(width, height),
                new ModelAsset(
                        modelName,
                        textures,
                        textureMapping,
                        boneAssets,
                        animations
                )
        );
    }

    /**
     * Creates a new instance of the implementation of this
     * interface to create {@link Model} instances from
     * <a href="https://blockbench.net">Blockbench</a>'s
     * <b>.bbmodel</b> files, which are just JSON files with a
     * special structure
     *
     * @since 1.0.0
     */
    public static ModelReader blockbench() {
        return new BBModelReader(new ModelDataCursor(1));
    }

    /**
     * Creates a new instance of the implementation of this
     * interface to create {@link Model} instances from
     * <a href="https://blockbench.net">Blockbench</a>'s
     * <b>.bbmodel</b> files, which are just JSON files with a
     * special structure
     *
     * <p>Similar to {@link BBModelReader#blockbench()} but
     * a specific {@link ModelDataCursor} can be used</p>
     *
     * @param cursor The custom model data cursor reference
     * @since 1.0.0
     */
    public static ModelReader blockbench(ModelDataCursor cursor) {
        return new BBModelReader(cursor);
    }

    /**
     * Locally reads cubes from the "elements" property from
     * the given {@code json} object and then reads {@link Bone}
     * and {@link BoneAsset} from the "outliner" property
     */
    private void readElements(
            JsonObject json,
            Map<String, Bone> bones,
            Map<String, BoneAsset> boneAssets,
            int textureWidth,
            int textureHeight
    ) throws IOException {

        // Local map holding relations of cube identifier to
        // cube data, used to get bone cubes in constant time
        // when reading them
        Map<String, ElementAsset> cubeIdMap = new HashMap<>();

        for (JsonElement cubeElement : json.getAsJsonArray("elements")) {

            JsonObject cubeJson = cubeElement.getAsJsonObject();

            Vector3Float pivot = GsonUtil.getVector3FloatFromJson(cubeJson.get("origin"))
                    .multiply(-1, 1, 1);
            Vector3Float to = GsonUtil.getVector3FloatFromJson(cubeJson.get("to"));
            Vector3Float from = GsonUtil.getVector3FloatFromJson(cubeJson.get("from"));

            Vector3Float origin = new Vector3Float(-to.x(), from.y(), from.z());
            to = origin.add(to.subtract(from));

            Vector3Float rotation = GsonUtil.isNullOrAbsent(cubeJson, "rotation")
                    ? Vector3Float.ZERO
                    : GsonUtil.getVector3FloatFromJson(cubeJson.get("rotation"));

            float x = rotation.x();
            float y = rotation.y();
            float z = rotation.z();

            // determine axis and check that it is rotated to a single direction
            Axis3D axis;
            float angle;

            if ((((x != 0) ? 1 : 0) ^ ((y != 0) ? 1 : 0) ^ ((z != 0) ? 1 : 0)) == 0 && (x != 0 || y != 0)) {
                throw new UnsupportedOperationException("Cube can't be rotated in multiple axis");
            } else if (x != 0) {
                axis = Axis3D.X;
                angle = x;
            } else if (y != 0) {
                axis = Axis3D.Y;
                angle = y;
            } else {
                axis = Axis3D.Z;
                angle = z;
            }

            Map<CubeFace, ElementFace> faces = new HashMap<>();

            for (Map.Entry<String, JsonElement> faceEntry
                    : cubeJson.getAsJsonObject("faces").entrySet()) {

                CubeFace face = CubeFace.valueOf(faceEntry.getKey().toUpperCase(Locale.ROOT));
                JsonObject faceJson = faceEntry.getValue().getAsJsonObject();

                int textureId = GsonUtil.isNullOrAbsent(faceJson, "texture")
                        ? -1
                        : faceJson.get("texture").getAsInt();

                JsonArray uvJson = faceJson.get("uv").getAsJsonArray();
                Vector4Float uv = new Vector4Float(
                        uvJson.get(0).getAsFloat() / textureWidth,
                        uvJson.get(1).getAsFloat() / textureHeight,
                        uvJson.get(2).getAsFloat() / textureWidth,
                        uvJson.get(3).getAsFloat() / textureHeight
                );

                if (!uv.equals(Vector4Float.ZERO)) {
                    faces.put(face, ElementFace.builder()
                            .uv(uv)
                            .texture("#" + textureId)
                            .build());
                }
            }

            String uuid = cubeJson.get("uuid").getAsString();
            ElementAsset cube = new ElementAsset(
                    origin,
                    to,
                    ElementRotation.of(pivot, axis, angle, ElementRotation.DEFAULT_RESCALE),
                    faces
            );
            cubeIdMap.put(uuid, cube);
        }

        // "outliner" field contains the root elements, like
        // bones and cubes, root cubes aren't supported yet
        for (JsonElement element : json.get("outliner").getAsJsonArray()) {
            if (element.isJsonObject()) {
                // if it's an object, then it represents a bone
                createBone(
                        Vector3Float.ZERO,
                        cubeIdMap,
                        element.getAsJsonObject(),

                        bones,
                        boneAssets
                );
            }
            // TODO: Support elements without a bone
        }
    }

    /**
     * Creates a {@link Bone} and {@link Bone} from
     * the given {@code json} object
     *
     * @param parentScaledPivot The scaled pivot of the parent bone
     * @param cubeIdMap Map containing a relation of the cubes by
     *                  their identifiers for this model
     * @param json The json representation for this bone
     *
     * @param siblings The sibling bone map, the bone will be put
     *                 in this map by its name
     * @param siblingAssets The sibling bone asset map, the asset
     *                      will be put in this map by its name
     */
    private void createBone(
            Vector3Float parentScaledPivot,
            Map<String, ElementAsset> cubeIdMap,
            JsonObject json,

            Map<String, Bone> siblings,
            Map<String, BoneAsset> siblingAssets
    ) throws IOException {

        String name = json.get("name").getAsString();

        // The pivot of this bone
        Vector3Float pivot = GsonUtil.getVector3FloatFromJson(json.get("origin"))
                .multiply(-1, 1, 1);

        // The initial rotation of this bone
        Vector3Float rotation = GsonUtil.isNullOrAbsent(json, "rotation")
                ? Vector3Float.ZERO
                : GsonUtil.getVector3FloatFromJson(json.get("rotation"));

        // scaled pivot of the bone (pivot / 16)
        Vector3Float scaledPivot = pivot.divide(16, 16, -16);
        Vector3Float offset = scaledPivot.subtract(parentScaledPivot);

        List<ElementAsset> cubes = new ArrayList<>();
        Map<String, Bone> bones = new LinkedHashMap<>();
        Map<String, BoneAsset> boneAssets = new LinkedHashMap<>();

        for (JsonElement childElement : json.get("children").getAsJsonArray()) {
            if (childElement.isJsonObject()) {
                // if it's an object, it's a sub-bone,
                // recursively read it
                createBone(
                        scaledPivot,
                        cubeIdMap,
                        childElement.getAsJsonObject(),

                        bones,
                        boneAssets
                );
            } else if (childElement.isJsonPrimitive()
                    && childElement.getAsJsonPrimitive().isString()) {
                // if it's a string, it refers to a cube,
                // find it and add it to the cube map
                String cubeId = childElement.getAsString();
                ElementAsset cube = cubeIdMap.get(cubeId);

                if (cube == null) {
                    throw new IOException("Bone " + name + " contains " +
                            "an invalid cube id: '" + cubeId + "', not present in " +
                            "the 'elements' section");
                } else {
                    cubes.add(cube);
                }
            } else {
                throw new IOException("Invalid child type in bone " + name);
            }
        }


        ElementProcessor.Result processResult = ElementProcessor.process(pivot, cubes);
        BoneAsset asset = new BoneAsset(
                name,
                pivot,
                cursor.next(),
                processResult.offset(),
                processResult.elements(),
                processResult.small(),
                boneAssets
        );

        siblings.put(name, new Bone(name, rotation, bones, offset, asset));
        siblingAssets.put(name, asset);
    }

}