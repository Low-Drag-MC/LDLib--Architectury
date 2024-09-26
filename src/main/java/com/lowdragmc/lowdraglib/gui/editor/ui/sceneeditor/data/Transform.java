package com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.data;

import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.ISceneObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A transform that represents the position, rotation, and scale of a scene object.
 */
@Accessors(fluent = true)
public final class Transform {
    /**
     * Position of the transform relative to the parent transform.
     */
    @Getter
    private Vector3f localPosition = new Vector3f();

    /**
     * Rotation of the transform relative to the parent transform.
     */
    @Getter
    private Quaternionf localRotation = new Quaternionf();

    /**
     * Scale of the transform relative to the parent transform.
     */
    @Getter
    private Vector3f localScale = new Vector3f(1, 1, 1);

    /**
     * The parent transform of the transform.
     */
    @Nullable
    @Getter
    private Transform parent;

    /**
     * The children transforms of the transform.
     */
    @Getter
    private List<Transform> children = new ArrayList<>();

    /**
     * The transform owner.
     */
    @Getter
    @Nonnull
    private final ISceneObject sceneObject;

    // runtime
    @Nullable
    private Vector3f position = null;
    @Nullable
    private Quaternionf rotation = null;
    @Nullable
    private Vector3f scale = null;
    @Nullable
    private Matrix4f localTransformMatrix = null;
    @Nullable
    private Matrix4f worldToLocalMatrix = null;
    private Matrix4f localToWorldMatrix = null;

    public Transform(ISceneObject sceneObject) {
        this.sceneObject = sceneObject;
    }


    /**
     * Notify the transform that the transform has changed.
     * This will clean cache of the world space position, rotation, scale, and matrices.
     */
    private void onTransformChanged() {
        position = null;
        rotation = null;
        scale = null;
        localTransformMatrix = null;
        worldToLocalMatrix = null;
        localToWorldMatrix = null;
        for (Transform child : children) {
            child.onTransformChanged();
        }
        sceneObject.onTransformChanged();
    }

    /**
     * Set the parent transform of the transform.
     */
    public void parent(@Nullable Transform parent) {
        if (this.parent == parent) {
            return;
        }
        if (this.parent != null) {
            this.parent.children.remove(this);
            this.parent.sceneObject.onChildChanged();
        }
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
            this.sceneObject.setScene(parent.sceneObject.getScene());
            parent.sceneObject.onChildChanged();
        }
        var lastPosition = position();
        var lastRotation = rotation();
        var lastScale = scale();
        onTransformChanged();
        position(lastPosition);
        rotation(lastRotation);
        scale(lastScale);
    }

    /**
     * Matrix that represents the local transform of the transform.
     */
    public Matrix4f localTransformMatrix() {
        if (localTransformMatrix == null) {
            localTransformMatrix = new Matrix4f().translate(localPosition).rotate(localRotation).scale(localScale);
        }
        return localTransformMatrix;
    }

    /**
     * Matrix that transforms from world space to local space.
     */
    public Matrix4f worldToLocalMatrix() {
        if (worldToLocalMatrix == null) {
            worldToLocalMatrix = parent == null ?
                    localTransformMatrix() :
                    new Matrix4f(parent.worldToLocalMatrix()).mul(localTransformMatrix());
        }
        return worldToLocalMatrix;
    }

    /**
     * Matrix that transforms from local space to world space.
     */
    public Matrix4f localToWorldMatrix() {
        if (localToWorldMatrix == null) {
            localToWorldMatrix = worldToLocalMatrix().invert(new Matrix4f());
        }
        return localToWorldMatrix;
    }

    /**
     * Set the position, rotation, and scale of the transform.
     */
    public Transform set(Transform transform) {
        position(transform.position());
        rotation(transform.rotation());
        scale(transform.scale());
        return this;
    }

    /**
     * The world space position of the transform.
     */
    public Vector3f position() {
        if (position == null) {
            position = parent == null ?
                    localPosition :
                    new Vector3f(localPosition).mulPosition(parent.localToWorldMatrix());
        }
        return position;
    }

    public void position(Vector3f position) {
        onTransformChanged();
        this.position = new Vector3f(position);
        if (parent == null) {
            this.localPosition = new Vector3f(position);
        } else {
            this.localPosition = new Vector3f(position).mulPosition(parent.worldToLocalMatrix());
        }
    }

    public void localPosition(Vector3f localPosition) {
        this.localPosition = localPosition;
        onTransformChanged();
    }

    /**
     * The world space rotation of the transform.
     */
    public Quaternionf rotation() {
        if (rotation == null) {
            rotation = parent == null ?
                    localRotation :
                    new Quaternionf(localRotation).mul(parent.rotation());
        }
        return rotation;
    }

    public void rotation(Quaternionf rotation) {
        onTransformChanged();
        this.rotation = new Quaternionf(rotation);
        if (parent == null) {
            this.localRotation = new Quaternionf(rotation);
        } else {
            this.localRotation = new Quaternionf(rotation).mul(parent.rotation().invert(new Quaternionf()));
        }
    }

    public void localRotation(Quaternionf localRotation) {
        this.localRotation = localRotation;
        onTransformChanged();
    }

    /**
     * The world space scale of the transform.
     */
    public Vector3f scale() {
        if (scale == null) {
            scale = parent == null ?
                    localScale :
                    new Vector3f(localScale).mul(parent.scale());
        }
        return scale;
    }

    public void scale(Vector3f scale) {
        onTransformChanged();
        this.scale = new Vector3f(scale);
        if (parent == null) {
            this.localScale = new Vector3f(scale);
        } else {
            this.localScale = new Vector3f(scale).div(parent.scale());
        }
    }

    public void localScale(Vector3f localScale) {
        this.localScale = localScale;
        onTransformChanged();
    }
}
