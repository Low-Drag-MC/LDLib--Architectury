package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.GradientColor;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2023/5/30
 * @implNote GradientColorWidget
 */
public class GradientColorWidget extends WidgetGroup {
    protected final GradientColor gradientColor;
    protected final HsbColorWidget hsbColorWidget;
    protected int selectedAlphaPoint = -1, selectedRGBPoint = 0;
    @Setter
    protected Consumer<GradientColor> onUpdate;
    // runtime
    private boolean isDraggingAlphaPoint, isDraggingRGBPoint;
    private long lastClickTick;

    public GradientColorWidget(int x, int y, int width, GradientColor gradientColor) {
        super(x, y, width, width + 15 + 20 + 3);
        this.gradientColor = gradientColor;
        addWidget(new ImageWidget(3, 3 + 10, width - 6, 15, ColorPattern.WHITE.borderTexture(1)));

        addWidget(hsbColorWidget = new HsbColorWidget(3, 15 + 20 + 3, width - 6, width - 6)
                .setColorSupplier(() -> {
                    var t = selectedAlphaPoint >= 0 ? gradientColor.getAP().get(selectedAlphaPoint).x :
                            selectedRGBPoint >= 0 ? gradientColor.getRP().get(selectedRGBPoint).x :
                            0;
                    return gradientColor.getColor(t);
                })
                .setOnChanged(color -> {
                    if (selectedAlphaPoint >= 0) {
                        gradientColor.getAP().set(selectedAlphaPoint, new Vec2( gradientColor.getAP().get(selectedAlphaPoint).x, ColorUtils.alpha(color)));
                        notifyChanged();
                    }
                    if (selectedRGBPoint >= 0) {
                        gradientColor.getRP().set(selectedRGBPoint, new Vec2( gradientColor.getRP().get(selectedRGBPoint).x, ColorUtils.red(color)));
                        gradientColor.getGP().set(selectedRGBPoint, new Vec2( gradientColor.getGP().get(selectedRGBPoint).x, ColorUtils.green(color)));
                        gradientColor.getBP().set(selectedRGBPoint, new Vec2( gradientColor.getBP().get(selectedRGBPoint).x, ColorUtils.blue(color)));
                        notifyChanged();
                    }
                }));
        hsbColorWidget.setShowAlpha(false);
        hsbColorWidget.setShowRGB(true);
    }

    protected void notifyChanged() {
        if (onUpdate != null) onUpdate.accept(gradientColor);
    }

    private float getXPosition(float t) {
        var size = getSize();
        var pos = getPosition();
        return pos.x + 3 + (size.width - 6) * t;
    }

    private float getXCoord(float x) {
        var size = getSize();
        var position = getPosition();
        return (x - position.x - 3) / (size.width - 6);
    }

    protected void openMenu(int mouseX, int mouseY) {
        if ((selectedAlphaPoint >= 0 && gradientColor.getAP().size() > 1) || (selectedRGBPoint >= 0 && gradientColor.getRP().size() > 1)) {
            var menu = TreeBuilder.Menu.start().leaf("Remove", () -> {
                if (selectedAlphaPoint >= 0) {
                    gradientColor.getAP().remove(selectedAlphaPoint);
                    selectedAlphaPoint = -1;
                    notifyChanged();
                } else if (selectedRGBPoint >= 0) {
                    gradientColor.getRP().remove(selectedRGBPoint);
                    gradientColor.getGP().remove(selectedRGBPoint);
                    gradientColor.getBP().remove(selectedRGBPoint);
                    selectedRGBPoint = -1;
                    notifyChanged();
                }
            });
            var widget = new MenuWidget<>(mouseX - getPosition().x, mouseY - getPosition().y, 14, menu.build())
                    .setNodeTexture(new IGuiTexture() {
                        @Override
                        @Environment(EnvType.CLIENT)
                        public void draw(PoseStack stack, int mouseX, int mouseY, float x, float y, int width, int height) {
                            ColorPattern.BLACK.rectTexture().draw(stack, mouseX, mouseY, x, y, width, height);
                            Icons.RIGHT.draw(stack, mouseX, mouseY, x + width - height + 3, y + 3, height - 6, height - 6);
                        }
                    })
                    .setLeafTexture(ColorPattern.BLACK.rectTexture())
                    .setNodeHoverTexture(ColorPattern.T_GRAY.rectTexture())
                    .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                    .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                    .setKeyNameSupplier(TreeBuilder.Menu::getName)
                    .setOnNodeClicked(TreeBuilder.Menu::handle);
            waitToAdded(widget.setBackground(new ColorRectTexture(0xff3C4146), ColorPattern.GRAY.borderTexture(1)));
        }

    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 1) {
                openMenu((int) mouseX, (int) mouseY);
            } else {
                var clickTick = gui.getTickCount();
                var size = getSize();
                var pos = getPosition();
                // click alpha point
                for (int i = 0; i < gradientColor.getAP().size(); i++) {
                    if (isMouseOver((int) (getXPosition(gradientColor.getAP().get(i).x) - 5), pos.y + 3, 10, 10, mouseX, mouseY)) {
                        selectedAlphaPoint = i;
                        selectedRGBPoint = -1;
                        isDraggingAlphaPoint = true;
                        hsbColorWidget.setShowAlpha(true);
                        hsbColorWidget.setShowRGB(false);
                        return true;
                    }
                }
                // click rgb point
                for (int i = 0; i < gradientColor.getRP().size(); i++) {
                    if (isMouseOver((int) (getXPosition(gradientColor.getRP().get(i).x) - 5), pos.y + 3 + 10 + 15, 10, 10, mouseX, mouseY)) {
                        selectedRGBPoint = i;
                        selectedAlphaPoint = -1;
                        isDraggingRGBPoint = true;
                        hsbColorWidget.setShowAlpha(false);
                        hsbColorWidget.setShowRGB(true);
                        return true;
                    }
                }
                // double click
                if (clickTick - lastClickTick < 12) {
                    // create alpha point
                    if (isMouseOver(pos.x + 3, pos.y + 3, size.width - 6, 10, mouseX, mouseY)) {
                        int color = gradientColor.getColor(getXCoord((float)mouseX));
                        gradientColor.addAlpha(getXCoord((float)mouseX), ColorUtils.alpha(color));
                        notifyChanged();
                        return true;
                    }
                    // create rgb point
                    if (isMouseOver(pos.x + 3, pos.y + 3 + 10 + 15, size.width - 6, 10, mouseX, mouseY)) {
                        int color = gradientColor.getColor(getXCoord((float)mouseX));
                        gradientColor.addRGB(getXCoord((float) mouseX), ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color));
                        notifyChanged();
                        return true;
                    }
                } else {
                    lastClickTick = clickTick;
                }
            }
        }
        return false;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingRGBPoint = false;
        this.isDraggingAlphaPoint = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var size = getSize();
        var pos = getPosition();
        if (mouseX >= pos.x + 3 && mouseX <= pos.x + size.width - 6) {
            if (selectedAlphaPoint >= 0 && this.isDraggingAlphaPoint) {
                var t = getXCoord((float) Mth.clamp(mouseX, pos.x + 3, pos.x + size.width - 6));
                selectedAlphaPoint = gradientColor.addAlpha(t,  gradientColor.getAP().remove(selectedAlphaPoint).y);
                notifyChanged();
                return true;
            }
            if (selectedRGBPoint >= 0 && this.isDraggingRGBPoint) {
                var t = getXCoord((float) Mth.clamp(mouseX, pos.x + 3, pos.x + size.width - 6));
                selectedRGBPoint = gradientColor.addRGB(t,
                        gradientColor.getRP().remove(selectedRGBPoint).y,
                        gradientColor.getGP().remove(selectedRGBPoint).y,
                        gradientColor.getBP().remove(selectedRGBPoint).y);
                notifyChanged();
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawInBackground(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // render background
        if (backgroundTexture != null) {
            Position pos = getPosition();
            Size size = getSize();
            backgroundTexture.draw(poseStack, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
        if (hoverTexture != null && isMouseOverElement(mouseX, mouseY)) {
            Position pos = getPosition();
            Size size = getSize();
            hoverTexture.draw(poseStack, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }

        var size = getSize();
        var pos = getPosition();

        // render point
        for (int i = 0; i < gradientColor.getAP().size(); i++) {
            Icons.DOWN.copy().setColor(i == selectedAlphaPoint ? ColorPattern.GREEN.color : -1).draw(poseStack, mouseX, mouseY, getXPosition(gradientColor.getAP().get(i).x) - 5, pos.y + 3, 10, 10);
        }

        for (int i = 0; i < gradientColor.getRP().size(); i++) {
            Icons.UP.copy().setColor(i == selectedRGBPoint ? ColorPattern.GREEN.color : -1).draw(poseStack, mouseX, mouseY, getXPosition(gradientColor.getRP().get(i).x) - 5, pos.y + 3 + 10 + 15, 10, 10);
        }
        // render grid
        var width = size.width - 6;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        var matrix = poseStack.last().pose();
        int p = 0;
        for (int x = 0; x < width; x += 3) {
            for (int y = 0; y < 15; y += 3) {
                var isWhite = (p++ % 2) == 0;
                float minX = pos.x + 3 + x;
                float maxX = pos.x + 3 + x + 3;
                float minY = pos.y + 13 + y;
                float maxY = pos.y + 13 + y + 3;
                bufferBuilder.vertex(matrix, minX, maxY, 0.0f).color(isWhite ? -1 : ColorPattern.GRAY.color).endVertex();
                bufferBuilder.vertex(matrix, maxX, maxY, 0.0f).color(isWhite ? -1 : ColorPattern.GRAY.color).endVertex();
                bufferBuilder.vertex(matrix, maxX, minY, 0.0f).color(isWhite ? -1 : ColorPattern.GRAY.color).endVertex();
                bufferBuilder.vertex(matrix, minX, minY, 0.0f).color(isWhite ? -1 : ColorPattern.GRAY.color).endVertex();
            }
        }
        BufferUploader.drawWithShader(bufferBuilder.end());

        // render color bar
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f mat = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var y = pos.y + 13;
        var yh = 15 + y;
        for (int i = 0; i < width; i++) {
            var x = getXPosition(i * 1f / width);
            var xw = getXPosition((i + 1f) / width);
            int startColor = gradientColor.getColor(i * 1f / width);
            int endColor = gradientColor.getColor((i + 1f) / width);
            float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
            float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
            float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
            float startBlue  = (float)(startColor       & 255) / 255.0F;
            float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
            float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
            float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
            float endBlue    = (float)(endColor         & 255) / 255.0F;

            buffer.vertex(mat,xw, y, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            buffer.vertex(mat,x, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.vertex(mat,x, yh, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.vertex(mat,xw, yh, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        }

        tesselator.end();

        // render children
        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.enableBlend();
                if (widget.inAnimate()) {
                    widget.getAnimation().drawInBackground(poseStack, mouseX, mouseY, partialTicks);
                } else {
                    widget.drawInBackground(poseStack, mouseX, mouseY, partialTicks);
                }
            }
        }
    }
}