package com.lowdragmc.lowdraglib.gui.editor.ui;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.editor.ILDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.MenuWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.nbt.NbtIo;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/11/30
 * @implNote MainPage
 */
@Getter
public abstract class Editor extends WidgetGroup implements ILDLRegister {
    @OnlyIn(Dist.CLIENT)
    public static Editor INSTANCE;
    protected final File workSpace;
    protected IProject currentProject;
    @Setter
    @Nullable
    protected File currentProjectFile;
    protected MenuPanel menuPanel;
    protected StringTabContainer tabPages;
    protected ConfigPanel configPanel;
    protected ResourcePanel resourcePanel;
    protected WidgetGroup floatView;
    protected ToolPanel toolPanel;
    protected String copyType;
    protected Object copied;

    public Editor(String modID) {
        this(new File(LDLib.getLDLibDir(), "assets/" + modID));
        if (LDLib.isClient()) {
            if (!this.workSpace.exists() && !this.workSpace.mkdirs()) {
                LDLib.LOGGER.error("Failed to create work space for mod: " + modID);
            }
        }
    }

    public Editor(File workSpace) {
        super(0, 0, 10, 10);
        setClientSideWidget();
        this.workSpace = workSpace;
    }

    @Override
    public void setGui(ModularUI gui) {
        if (gui != null) {
            gui.mainGroup.setClientSideWidget();
            gui.mainGroup.setAllowXEIIngredientOverMouse(false);
        }
        super.setGui(gui);
        if (isRemote()) {
            if (gui == null) {
                INSTANCE = null;
            } else {
                INSTANCE = this;
                getGui().registerCloseListener(() -> INSTANCE = null);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        setSize(new Size(screenWidth, screenHeight));
        super.onScreenSizeUpdate(screenWidth, screenHeight);
        this.clearAllWidgets();
        initEditorViews();
        loadProject(currentProject);
    }

    public void initEditorViews() {
        this.toolPanel = new ToolPanel(this);
        this.configPanel = new ConfigPanel(this);
        this.tabPages = new StringTabContainer(this);
        this.resourcePanel = new ResourcePanel(this);
        this.menuPanel = new MenuPanel(this);
        this.floatView = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height);

        this.addWidget(this.tabPages);
        this.addWidget(this.toolPanel);
        this.addWidget(this.configPanel);
        this.addWidget(this.resourcePanel);
        this.addWidget(this.menuPanel);
        this.addWidget(this.floatView);
    }

    public DialogWidget openDialog(DialogWidget dialog) {
        this.addWidget(dialog);
        Position pos = dialog.getPosition();
        Size size = dialog.getSize();
        if (pos.x + size.width > getGui().getScreenWidth()) {
            dialog.addSelfPosition(pos.x + size.width - getGui().getScreenWidth(), 0);
        } else if (pos.x < 0) {
            dialog.addSelfPosition(-pos.x, 0);
        }
        if (pos.y + size.height > getGui().getScreenHeight()) {
            dialog.addSelfPosition(0, pos.y + size.height - getGui().getScreenHeight());
        } else if (pos.y < 0) {
            dialog.addSelfPosition(0, -pos.y);
        }
        return dialog;
    }

    public <T, C> MenuWidget<T, C> openMenu(double posX, double posY, TreeNode<T, C> menuNode) {
        var menu = new MenuWidget<>((int) posX, (int) posY, 14, menuNode)
                .setNodeTexture(MenuWidget.NODE_TEXTURE)
                .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE);
        waitToAdded(menu.setBackground(MenuWidget.BACKGROUND));

        return menu;
    }

    public void openMenu(double posX, double posY, TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null) return;
        openMenu(posX, posY, menuBuilder.build())
                .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                .setKeyNameSupplier(TreeBuilder.Menu::getName)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    public void loadProject(IProject project) {
        if (currentProject != null && currentProject != project) {
            currentProject.onClosed(this);
            currentProjectFile = null;
        }

        currentProject = project;
        tabPages.clearAllWidgets();
        toolPanel.clearAllWidgets();
        toolPanel.hide(false);
        configPanel.clearAllConfigurators();
        resourcePanel.clear();
        floatView.clearAllWidgets();

        if (currentProject != null) {
            currentProject.onLoad(this);
        }
    }

    public void setCopy(String copyType, Object copied) {
        this.copied = copied;
        this.copyType = copyType;
    }

    public void ifCopiedPresent(String copyType, Consumer<Object> consumer) {
        if (Objects.equals(copyType, this.copyType)) {
            consumer.accept(copied);
        }
    }

    public void askToSaveProject(BooleanConsumer result) {
        DialogWidget.showCheckBox(this, "ldlib.gui.editor.tips.save_project", "ldlib.gui.editor.tips.ask_to_save", isSave -> {
            if (isSave) {
                saveProject(result);
            } else {
                result.accept(false);
            }
        });
    }

    public void saveProject(BooleanConsumer result) {
        if (currentProject != null) {
            if (currentProjectFile == null) {
                saveAsProject(result);
            } else {
                currentProject.saveProject(currentProjectFile.toPath());
                DialogWidget.showNotification(this, "ldlib.gui.editor.menu.save", "ldlib.gui.compass.save_success");
                result.accept(true);
            }
        } else {
            result.accept(false);
        }
    }

    public void saveAsProject(BooleanConsumer result) {
        if (currentProject != null) {
            String suffix = "." + currentProject.getSuffix();
            DialogWidget.showFileDialog(this, "ldlib.gui.editor.tips.save_as", currentProject.getProjectWorkSpace(this), false,
                    DialogWidget.suffixFilter(suffix), file -> {
                        if (file != null && !file.isDirectory()) {
                            if (!file.getName().endsWith(suffix)) {
                                file = new File(file.getParentFile(), file.getName() + suffix);
                            }
                            currentProject.saveProject(file.toPath());
                            currentProjectFile = file;
                            result.accept(true);
                        } else {
                            result.accept(false);
                        }
                    });
        }
    }

    public boolean isCurrentProjectSaved() {
        if (currentProject == null) return true;
        if (currentProjectFile == null) return false;
        try {
            var tag = NbtIo.read(currentProjectFile.toPath());
            return tag != null && tag.equals(currentProject.serializeNBT(Platform.getFrozenRegistry()));
        } catch (IOException ignored) {}
        return false;
    }

    private boolean isWaitingForSave = false;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isWaitingForSave) {
                getGui().getModularUIGui().onClose();
                return true;
            }
            // esc
            if (!isCurrentProjectSaved()) {
                isWaitingForSave = true;
                askToSaveProject(result -> getGui().getModularUIGui().onClose());
                return true;
            }
            return false;
        }
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }
}